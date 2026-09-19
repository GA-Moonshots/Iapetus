package org.firstinspires.ftc.teamcode.subsystems;

import com.bylazar.telemetry.PanelsTelemetry;
import com.bylazar.telemetry.TelemetryManager;
import com.pedropathing.math.Pose;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.utils.Constants;
import org.firstinspires.ftc.teamcode.utils.FieldMap;
import org.firstinspires.ftc.teamcode.utils.TagSighting;
import org.firstinspires.ftc.teamcode.utils.Tunables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                         SENSORS SUBSYSTEM                                 ║
 * ║                                                                           ║
 * ║  Two jobs:                                                                ║
 * ║    • Own every sensor that isn't bolted to a specific mechanism           ║
 * ║      (the Limelight lives here; add colour/distance sensors here too)     ║
 * ║    • Be the ONLY thing in this entire project that flushes telemetry      ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * !!! THIS IS THE ONLY telemetry.update() IN THE WHOLE PROJECT !!!
 *
 * Everyone else calls robot.sensors.addTelemetry(...) and walks away. The
 * scheduler calls periodic() once per loop and that's when it actually ships.
 * Call update() somewhere else too and you get half a frame of data, a
 * mysteriously empty Driver Station, and an afternoon you'll never get back.
 * Ask last year's team how they found this out.
 *
 * Goes to the Driver Station AND the Panels dashboard at the same time.
 */
public class Sensors extends SubsystemBase {

    private final Iapetus robot;
    private final Telemetry telemetry;
    private final TelemetryManager megaphone;

    // ---- Loop timing ----
    // Pedro integrates position using the time between updates, so a slow or
    // stuttering loop degrades localization directly. A healthy FTC loop is
    // well under 20ms. If "Loop (ms)" starts climbing, that is usually the
    // real cause of "the robot drifted" — not the PID tuning everyone blames.
    private long lastLoopNanos = 0;
    private double loopMs = 0;
    private double worstLoopMs = 0;
    private final ElapsedTime matchClock = new ElapsedTime();

    // ---- Vision ----
    // null means "no camera today", and that is a completely acceptable state.
    // Every call below is guarded. A vision failure that stops the robot is
    // worse than no vision at all.
    private Limelight3A limelight = null;
    private String visionVerdict = "starting up";

    // Tag tracking: the latest sighting of each tag, kept in FIELD coordinates
    // so it stays true while we drive. Old ones age out (Tunables.TAG_MEMORY_MS).
    private final Map<Integer, TagSighting> latestById = new LinkedHashMap<>();

    // Fixed-tag localization: off unless a season has tags that stay put.
    private String localizationVerdict = "off";
    private Pose lastAcceptedPose = null;
    private int acceptedCount = 0;
    private int rejectedCount = 0;

    public Sensors(Iapetus robot) {
        this.robot = robot;
        this.telemetry = robot.telemetry;
        this.megaphone = PanelsTelemetry.INSTANCE.getTelemetry();

        initLimelight();

        // Add the rest of this year's sensors here — colour, distance, touch.
        // Wrap anything that might not be plugged in in a try/catch and leave
        // it null; a missing sensor should degrade the robot, not brick it
        // thirty seconds before a match.
    }

    // ============================================================
    //                        TELEMETRY
    // ============================================================

    /** Queue one line. Prints nothing until periodic() flushes. */
    public void addTelemetry(String key, String value) {
        megaphone.addData(key, value);
    }

    /** Same, but with String.format built in: addTelemetry("X", "%.1f\"", x) */
    public void addTelemetry(String key, String format, Object... args) {
        megaphone.addData(key, String.format(format, args));
    }

    /** Seconds since this OpMode's Sensors was built. Teleop is 120s. */
    public double matchSeconds() {
        return matchClock.seconds();
    }

    public double loopMs() {
        return loopMs;
    }

    // ============================================================
    //                        PERIODIC
    // ============================================================

    @Override
    public void periodic() {
        trackLoopTime();
        updateVision();

        addTelemetry("═══ Health ═══", "");
        addTelemetry("Match (s)", "%.0f", matchClock.seconds());
        addTelemetry("Loop (ms)", "%.1f  (worst %.1f)", loopMs, worstLoopMs);

        // If the field drawing gave up, say so. A blank map with no explanation
        // sends people hunting for a localization bug that isn't there.
        String drawFail = robot.drive == null ? null : robot.drive.drawingDisabledReason();
        if (drawFail != null) {
            addTelemetry("Panels drawing", "OFF — " + drawFail);
        }
        reportVision();

        // The one flush. Sends to Panels and the Driver Station together.
        megaphone.update(telemetry);
    }

    private void trackLoopTime() {
        long now = System.nanoTime();
        if (lastLoopNanos != 0) {
            loopMs = (now - lastLoopNanos) / 1_000_000.0;
            // Skip the first few loops — startup is always slow and would
            // otherwise pin "worst" at a number that never happens again.
            if (matchClock.seconds() > 1.0 && loopMs > worstLoopMs) {
                worstLoopMs = loopMs;
            }
        }
        lastLoopNanos = now;
    }

    // ============================================================
    //                     VISION / APRILTAGS
    // ============================================================

    /** Is there a camera at all? Everything else must cope when this is false. */
    public boolean hasCamera() {
        return limelight != null;
    }

    /** Most recent field pose the camera produced from fixed tags, or null. */
    public Pose lastAcceptedPose() {
        return lastAcceptedPose;
    }

    // ---- What the camera sees. Every answer is relative to where we are NOW. ----

    /** The latest sighting of this tag id, or null if we haven't seen it lately. */
    public TagSighting tag(int id) {
        forgetOldSightings();
        TagSighting s = latestById.get(id);
        return s == null ? null : s.seenFrom(robot.drive.getPose());
    }

    /** The closest single tag we've seen lately, or null. */
    public TagSighting nearestTag() {
        forgetOldSightings();
        Pose here = robot.drive.getPose();
        TagSighting best = null;
        for (TagSighting s : latestById.values()) {
            TagSighting now = s.seenFrom(here);
            if (best == null || now.range() < best.range()) best = now;
        }
        return best;
    }

    /**
     * Every OBJECT we've seen lately, one entry each: tags on the same object
     * (same SDK cluster) are merged; a tag on its own is its own target.
     * This is what to aim at. See TagSighting.merge() for how exact it is.
     */
    public List<TagSighting> targets() {
        forgetOldSightings();
        Map<String, List<TagSighting>> byName = new LinkedHashMap<>();
        for (TagSighting s : latestById.values()) {
            List<TagSighting> group = byName.get(s.name);
            if (group == null) {
                group = new ArrayList<>();
                byName.put(s.name, group);
            }
            group.add(s);
        }

        Pose here = robot.drive.getPose();
        List<TagSighting> out = new ArrayList<>();
        for (List<TagSighting> group : byName.values()) {
            out.add(TagSighting.merge(group, here));
        }
        return out;
    }

    /** The target with this name (an SDK cluster name, or "tag 31"), or null if it isn't in memory. */
    public TagSighting target(String name) {
        for (TagSighting t : targets()) {
            if (t.name.equals(name)) return t;
        }
        return null;
    }

    /** The closest target we've seen lately, or null. */
    public TagSighting nearestTarget() {
        TagSighting best = null;
        for (TagSighting t : targets()) {
            if (best == null || t.range() < best.range()) best = t;
        }
        return best;
    }

    private void forgetOldSightings() {
        Iterator<TagSighting> it = latestById.values().iterator();
        while (it.hasNext()) {
            if (it.next().ageMs() > Tunables.TAG_MEMORY_MS) it.remove();
        }
    }

    /** Localization needs fixed tags. Seasons without them leave both switches off. */
    private static boolean localizationOn() {
        return Tunables.TAG_LOCALIZATION || Tunables.VISION_CORRECTIONS_ENABLED;
    }

    private void initLimelight() {
        try {
            limelight = robot.hardwareMap.get(Limelight3A.class, Constants.LIMELIGHT_NAME);
            limelight.setPollRateHz(Constants.LIMELIGHT_POLL_HZ);
            limelight.pipelineSwitch(Constants.LIMELIGHT_APRILTAG_PIPELINE);
            limelight.start();

            // Push our field map so tag coordinates live in git rather than
            // only on the camera. Best effort — a camera with a good built-in
            // map still works, so we don't fail the robot over this.
            //
            // Only when we'll localize off it, and only if there's something
            // to send: an empty map would wipe whatever the camera already has.
            if (localizationOn() && !FieldMap.localizationTags().isEmpty()) {
                try {
                    limelight.uploadFieldmap(FieldMap.buildLimelightFieldMap(), null);
                } catch (Exception ignored) {
                    // Older firmware, or the map was rejected. Carry on.
                }
            }
        } catch (Exception e) {
            limelight = null;
            visionVerdict = "no camera";
        }
    }

    private void updateVision() {
        if (limelight == null) return;

        try {
            if (localizationOn()) {
                // MegaTag2 needs to know which way we're facing. Pedro already
                // knows, so hand it over every loop — skip this and MT2 quietly
                // returns worse numbers rather than an error.
                limelight.updateRobotOrientation(Math.toDegrees(robot.drive.getNormalizedHeading()));
            }
            LLResult result = limelight.getLatestResult();
            trackTags(result);
            if (localizationOn()) evaluate(result);
        } catch (Exception e) {
            // Camera died mid-match. Stop talking to it; keep driving.
            limelight = null;
            visionVerdict = "camera died — odometry only";
        }
    }

    /** Every tag in the frame goes on the field map, stamped with when the camera saw it. */
    private void trackTags(LLResult result) {
        if (result == null || !result.isValid()) {
            visionVerdict = "no tags in view";
            return;
        }
        long staleMs = result.getStaleness();
        if (staleMs > Constants.VISION_MAX_STALENESS_MS) {
            visionVerdict = String.format("stale frame (%dms)", staleMs);
            return;
        }

        // The frame is staleMs old; so is the tag's position in it.
        long seenAt = System.nanoTime() - staleMs * 1_000_000L;
        Pose here = robot.drive.getPose();
        int placed = 0;
        int flat = 0;
        for (LLResultTypes.FiducialResult f : result.getFiducialResults()) {
            double[] rel = FieldMap.limelightTargetToRobot(f.getTargetPoseRobotSpace());
            if (rel == null) {
                flat++;
                continue;
            }
            double[] field = FieldMap.robotToField(here, rel[0], rel[1]);
            int id = f.getFiducialId();
            latestById.put(id, new TagSighting(id, FieldMap.targetName(id), 1,
                    field[0], field[1], rel[2], seenAt, here));
            placed++;
        }

        if (placed == 0 && flat > 0) {
            // Tags detected, but no 3D. Almost always camera setup, not code.
            visionVerdict = "tags seen, no 3D pose — see docs/vision.md";
        } else {
            visionVerdict = placed + (placed == 1 ? " tag" : " tags") + " in view";
        }
    }

    /**
     * The trust policy: snap, but only when confident. Every early return is a
     * reason we declined, and each one shows up on the dashboard — a rejection
     * reason teaches you more than a silent correction ever will.
     *
     * Note what this deliberately does NOT do: demand a particular number of
     * tags. Some fields carry only two localization tags, at opposite ends, so
     * "need three at once" would mean never correcting. Tag count is a signal,
     * not a gate.
     */
    private void evaluate(LLResult result) {
        if (result == null || !result.isValid()) {
            rejectVision("no valid target");
            return;
        }
        if (result.getStaleness() > Constants.VISION_MAX_STALENESS_MS) {
            rejectVision(String.format("stale (%dms)", result.getStaleness()));
            return;
        }

        int tagCount = result.getBotposeTagCount();
        if (tagCount < 1) {
            rejectVision("no tags in view");
            return;
        }

        // Far tags are noisy tags: small angular error becomes large position
        // error with distance.
        double avgDist = FieldMap.metersToInches(result.getBotposeAvgDist());
        if (avgDist > Constants.VISION_MAX_TAG_DISTANCE_INCHES) {
            rejectVision(String.format("too far (%.0f\")", avgDist));
            return;
        }

        Pose3D botpose = result.getBotpose_MT2();
        if (botpose == null) {
            rejectVision("no MT2 pose (is heading being sent?)");
            return;
        }

        Pose visionPose = FieldMap.limelightToPedro(botpose);

        // A pose off the field is a bad read, full stop.
        if (visionPose.x() < -12 || visionPose.x() > 156
                || visionPose.y() < -12 || visionPose.y() > 156) {
            rejectVision("off-field reading");
            return;
        }

        // The big one: how far is this from where we think we are? A small
        // disagreement is drift worth fixing. A large one is far more likely a
        // misread than the robot having teleported.
        Pose current = robot.drive.getPose();
        double jump = visionPose.distance(current);
        if (jump > Constants.VISION_MAX_JUMP_INCHES) {
            rejectVision(String.format("implausible jump (%.0f\")", jump));
            return;
        }

        if (Tunables.VISION_CORRECTIONS_ENABLED) {
            robot.drive.setPose(visionPose);
        }
        lastAcceptedPose = visionPose;
        acceptedCount++;
        localizationVerdict = String.format("%s %.1f\" (%d tag%s)",
                Tunables.VISION_CORRECTIONS_ENABLED ? "SNAP" : "would snap",
                jump, tagCount, tagCount == 1 ? "" : "s");
    }

    private void rejectVision(String why) {
        rejectedCount++;
        localizationVerdict = "reject: " + why;
    }

    private void reportVision() {
        addTelemetry("═══ Vision ═══", "");
        if (limelight == null) {
            addTelemetry("Camera", "OFFLINE — driving on odometry");
            return;
        }

        addTelemetry("Camera", visionVerdict);

        // Nearest few targets: what an arm or a driver would care about.
        List<TagSighting> targets = targets();
        Collections.sort(targets, (a, b) -> Double.compare(a.range(), b.range()));
        for (int i = 0; i < Math.min(3, targets.size()); i++) {
            TagSighting t = targets.get(i);
            addTelemetry(t.name, "%.0f\" at %+.0f° (%d tag%s, %.0fms old)",
                    t.range(), Math.toDegrees(t.bearing()),
                    t.tagCount, t.tagCount == 1 ? "" : "s", t.ageMs());
            if (Tunables.SHOW_TAGS) robot.drive.drawTagSighting(t.fieldX, t.fieldY);
        }

        if (localizationOn()) {
            addTelemetry("Localization", localizationVerdict);
            addTelemetry("Corrections", "%d %s / %d declined", acceptedCount,
                    Tunables.VISION_CORRECTIONS_ENABLED ? "taken" : "watched", rejectedCount);
        }
    }
}
