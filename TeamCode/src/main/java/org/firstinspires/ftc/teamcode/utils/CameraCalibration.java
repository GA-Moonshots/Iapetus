package org.firstinspires.ftc.teamcode.utils;

import com.bylazar.configurables.annotations.Configurable;
import com.pedropathing.math.Pose;
import com.pedropathing.utils.Angle;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;

import org.firstinspires.ftc.teamcode.Iapetus;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                       CAMERA CALIBRATION                                  ║
 * ║                                                                           ║
 * ║  Is the camera telling the truth?                                         ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Every setting below is live on the Panels dashboard: move the tag, type
 * where you put it, read the error. No redeploy.
 *
 * TAG CHECK (every season, about five minutes):
 *
 *   1. Put a tag upright at a measured spot: TAG_FORWARD inches in front of
 *      the robot's centre, TAG_LEFT inches to its left, its centre TAG_UP off
 *      the floor (0 skips the height check).
 *   2. More than one tag in view, like a strip on one object? Set TAG_ID to
 *      the one you measured, or the answer hops between them (-1 = nearest).
 *      Or turn on WHOLE_TARGET and measure to the middle of the strip
 *      instead: that merged point is what a mechanism actually aims at.
 *   3. Run this OpMode and read the ERROR line.
 *
 *   Small (< ~1")               → calibrated. Go aim something.
 *   Same offset everywhere      → camera mount position is off. Fix it on the
 *                                 Limelight AND in Constants.CAMERA_*.
 *   Error grows with distance   → wrong tag size in the Limelight pipeline, or
 *                                 camera pitch is off (dUp growing too = pitch).
 *   "left" has the wrong sign   → fix FieldMap.limelightTargetToRobot(), and
 *                                 nowhere else.
 *   No reading at all           → docs/vision.md, "camera setup".
 *
 * FIELD CHECK (only for tags bolted to the field):
 *   Park at KNOWN_X / KNOWN_Y / KNOWN_HEADING_DEG and compare the camera's
 *   field pose. Skipped by itself in a season whose tags all ride on things
 *   that move; FIELD_CHECK = false skips it anyway.
 *
 * Never moves the robot, never corrects the pose, and puts the vision
 * switches back the way it found them. Safe to run any time.
 */
@Configurable
@TeleOp(name = "Camera Calibration", group = "Tuning")
public class CameraCalibration extends CommandOpMode {

    /** The tag you measured. -1 = whichever is nearest. */
    public static int TAG_ID = -1;

    /** Check the merged target (every tag on that object) instead of one tag. */
    public static boolean WHOLE_TARGET = false;

    /** Where you put the tag (or the strip's middle), from the robot's centre on the floor. Measure, don't guess. */
    public static double TAG_FORWARD = 24.0;
    public static double TAG_LEFT = 0.0;
    public static double TAG_UP = 0.0;   // 0 = skip the height check

    /** Compare the camera's field pose to KNOWN_*. Needs fixed tags. */
    public static boolean FIELD_CHECK = true;

    /** Where the robot ACTUALLY is, Pedro frame. Only for the field check. */
    public static double KNOWN_X = 72.0;
    public static double KNOWN_Y = 72.0;
    public static double KNOWN_HEADING_DEG = 0.0;

    private Iapetus robot;

    // Tunables are static, so anything we set would still be set in the next
    // OpMode. Remembered here, put back in end().
    private boolean savedLocalization;
    private boolean savedCorrections;

    @Override
    public void initialize() {
        savedLocalization = Tunables.TAG_LOCALIZATION;
        savedCorrections = Tunables.VISION_CORRECTIONS_ENABLED;

        // Look, don't touch. Set before the robot is built, so Sensors starts
        // up in watch mode (and pushes the field map, if there are fixed tags).
        applyVisionSwitches();
        robot = new Iapetus(this);

        telemetry.addLine("Put a tag at TAG_FORWARD / TAG_LEFT, then press START.");
        telemetry.update();
    }

    @Override
    public void run() {
        applyVisionSwitches();   // FIELD_CHECK is live; corrections stay off whatever the dashboard says
        super.run();

        Pose truth = new Pose(KNOWN_X, KNOWN_Y, Math.toRadians(KNOWN_HEADING_DEG));
        robot.drive.setPose(truth);   // we know where we are; assert it

        if (!robot.sensors.hasCamera()) {
            robot.sensors.addTelemetry("Calibration", "camera OFFLINE — nothing to check");
            return;
        }
        tagCheck();
        fieldCheck();
    }

    @Override
    public void end() {
        Tunables.TAG_LOCALIZATION = savedLocalization;
        Tunables.VISION_CORRECTIONS_ENABLED = savedCorrections;
    }

    /** Watch, never correct. Sensors ignores TAG_LOCALIZATION anyway when there are no fixed tags. */
    private static void applyVisionSwitches() {
        Tunables.TAG_LOCALIZATION = FIELD_CHECK;
        Tunables.VISION_CORRECTIONS_ENABLED = false;
    }

    /** The tag (or merged target) we're checking, or null if it isn't in view. */
    private TagSighting subject() {
        if (WHOLE_TARGET) {
            return TAG_ID >= 0 ? robot.sensors.target(FieldMap.targetName(TAG_ID))
                               : robot.sensors.nearestTarget();
        }
        return TAG_ID >= 0 ? robot.sensors.tag(TAG_ID) : robot.sensors.nearestTag();
    }

    private void tagCheck() {
        robot.sensors.addTelemetry("═══ Tag check ═══", "");
        TagSighting tag = subject();
        if (tag == null) {
            robot.sensors.addTelemetry("Tag", TAG_ID >= 0
                    ? "tag " + TAG_ID + " not in view (TAG_ID = -1 takes the nearest)"
                    : "none — see the Vision status line");
            return;
        }

        String label = WHOLE_TARGET ? tag.name + " (" + tag.tagCount + " tags)" : "Tag " + tag.id;
        double dF = tag.forward - TAG_FORWARD;
        double dL = tag.left - TAG_LEFT;
        robot.sensors.addTelemetry(label, "fwd %.1f\"  left %.1f\"  up %.1f\"",
                tag.forward, tag.left, tag.up);
        if (TAG_UP != 0) {
            robot.sensors.addTelemetry("ERROR", "dFwd %+.1f\"  dLeft %+.1f\"  dUp %+.1f\"",
                    dF, dL, tag.up - TAG_UP);
        } else {
            robot.sensors.addTelemetry("ERROR", "dFwd %+.1f\"  dLeft %+.1f\"", dF, dL);
        }

        // Cheap hints for the two mistakes that are easy to miss by eye.
        if (Math.abs(TAG_LEFT) > 4 && Math.abs(tag.left + TAG_LEFT) < Math.abs(dL) / 2) {
            robot.sensors.addTelemetry("HINT", "left/right mirrored — see FieldMap.limelightTargetToRobot");
        } else if (TAG_FORWARD > 12 && Math.abs(dF / TAG_FORWARD) > 0.1) {
            robot.sensors.addTelemetry("HINT", "distance off by %.0f%% — check the pipeline's tag size",
                    100 * dF / TAG_FORWARD);
        }
    }

    private void fieldCheck() {
        if (!FIELD_CHECK) {
            robot.sensors.addTelemetry("Field check", "off (FIELD_CHECK)");
            return;
        }
        if (!robot.sensors.fieldHasFixedTags()) {
            robot.sensors.addTelemetry("Field check", "skipped — no fixed tags this season");
            return;
        }
        robot.sensors.addTelemetry("═══ Field check ═══", "");
        Pose seen = robot.sensors.lastAcceptedPose();
        if (seen == null) {
            robot.sensors.addTelemetry("Camera", "no field pose yet — see Localization line");
            return;
        }

        double dx = seen.x() - KNOWN_X;
        double dy = seen.y() - KNOWN_Y;
        // Pedro stores 0..360, humans type -180..180. Compare the difference, wrapped.
        double dh = Math.toDegrees(Angle.normalizeSigned(
                seen.heading() - Math.toRadians(KNOWN_HEADING_DEG)));

        robot.sensors.addTelemetry("Camera says", "X:%.1f Y:%.1f H:%.0f°",
                seen.x(), seen.y(), Math.toDegrees(Angle.normalizeSigned(seen.heading())));
        robot.sensors.addTelemetry("FIELD ERROR", "dX:%+.1f\" dY:%+.1f\" dH:%+.0f°", dx, dy, dh);
    }
}
