package org.firstinspires.ftc.teamcode.utils;

import com.pedropathing.math.Pose;
import com.qualcomm.hardware.limelightvision.LLFieldMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.vision.apriltag.AprilTagClusterMetadata;
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase;
import org.firstinspires.ftc.vision.apriltag.AprilTagLibrary;
import org.firstinspires.ftc.vision.apriltag.AprilTagMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                            FIELD MAP                                      ║
 * ║                                                                           ║
 * ║  Where the AprilTags are, and how to translate between everybody's        ║
 * ║  idea of "where".                                                         ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * WE DO NOT TYPE IN TAG COORDINATES. FIRST ships them in the SDK, they're
 * correct, and they change with the game. We read
 * AprilTagGameDatabase.getCurrentGameTagLibrary() and convert. Next season the
 * SDK updates and this file keeps working — which is the entire reason it
 * reads from the library instead of a hand-maintained table.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 *  NOT EVERY TAG IS A LOCALIZATION TAG
 * ─────────────────────────────────────────────────────────────────────────────
 * Some tags mark a known spot on the field. Others just encode information —
 * which pattern to score, which side to start on — and are mounted somewhere
 * the game manual never pins down. Localize off one of those and your pose is
 * fiction.
 *
 * FIRST distinguishes them for us: informational tags ship with NO field
 * position. So "has a field position" is the filter, and it needs no
 * per-season ID blacklist that somebody will forget to update.
 *
 * The 2025-26 DECODE field, as a worked example: tags 20 and 24 were the goal
 * targets and carried positions; 21/22/23 were Obelisk motif tags and did not.
 * Expect entirely different ids, positions, sizes, and counts every season —
 * nothing below assumes otherwise, and nothing below should be edited when the
 * game changes.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 *  SOME TAGS ARE ON THINGS THAT MOVE
 * ─────────────────────────────────────────────────────────────────────────────
 * A game can put tags on game pieces or field elements that move during the
 * match. Those are no use for "where am I?", but perfect for "where is that?"
 * (see TagSighting). When several tags sit on one object, the SDK groups them
 * into a "cluster", and targetName() is how a sighting learns which object
 * it's on. A season with no fixed tags at all simply has an empty
 * localizationTags(); nothing else changes.
 *
 * How many localization tags a field has matters for the trust policy: DECODE
 * had only two, at opposite ends, so "require several tags at once" would
 * essentially never fire. A field with tags on every wall could afford a
 * stricter rule. Sensors' trust policy reads tag count as a signal rather
 * than assuming a number.
 *
 * See docs/vision.md for tracking and aiming, and docs/coordinates.md for
 * the frame rules.
 */
public class FieldMap {

    /** Half the field, in inches. FTC's frame is centred; ours isn't. */
    public static final double HALF_FIELD_INCHES = 72.0;

    public static final double METERS_TO_INCHES = 39.3701;

    /** One localization tag, already converted into our frame. */
    public static class Tag {
        public final int id;
        public final String name;
        /** Pedro frame: inches, corner origin. */
        public final double x, y, z;
        /** Edge length in inches. Varies by game — never hardcode it. */
        public final double sizeInches;

        Tag(int id, String name, double x, double y, double z, double sizeInches) {
            this.id = id; this.name = name;
            this.x = x; this.y = y; this.z = z;
            this.sizeInches = sizeInches;
        }
    }

    // ============================================================
    //                    UNIT / FRAME CONVERSIONS
    //   Convert once, at the edge. A conversion buried in the middle
    //   of a command is a bug waiting for a Saturday.
    // ============================================================

    public static double metersToInches(double meters) {
        return meters * METERS_TO_INCHES;
    }

    /**
     * FTC field coordinates → Pedro's.
     *
     * Two differences, and the second one is the expensive one:
     *   1. Origin. FTC is field CENTRE (-72..+72); Pedro is a CORNER (0..144).
     *   2. Axes. Pedro's frame is FTC's rotated 90° clockwise: FTC's +Y is
     *      Pedro's +X, and FTC's +X is Pedro's -Y. Headings rotate with it.
     *
     *      pedroX = ftcY + 72      pedroY = 72 - ftcX      heading - π/2
     *
     * Source: Pedro Pathing 3's coordinates reference, which publishes this
     * exact conversion. It agrees with the Panels Pedro preset's 90° rotation.
     * (Before Pedro 3 this method only shifted by 72 — see docs/issue-log.md.)
     *
     * ⚠ STILL VERIFY IT ON THE FIELD with CameraCalibration: park at a known
     * spot and see whether the reported pose matches. If it doesn't, fix it
     * HERE, in these methods, and nowhere else.
     */
    public static double[] ftcToPedro(double ftcX, double ftcY) {
        return new double[] { ftcY + HALF_FIELD_INCHES, HALF_FIELD_INCHES - ftcX };
    }

    /** Exactly undoes ftcToPedro(). */
    public static double[] pedroToFtc(double pedroX, double pedroY) {
        return new double[] { HALF_FIELD_INCHES - pedroY, pedroX - HALF_FIELD_INCHES };
    }

    /** FTC heading → Pedro heading, both radians. Same 90° as the axes. */
    public static double ftcHeadingToPedro(double ftcHeadingRad) {
        return ftcHeadingRad - Math.PI / 2;
    }

    /** Pedro heading → FTC heading, both radians, wrapped to [-π, π]. Undoes ftcHeadingToPedro(). */
    public static double pedroHeadingToFtc(double pedroHeadingRad) {
        double h = pedroHeadingRad + Math.PI / 2;
        return Math.atan2(Math.sin(h), Math.cos(h));
    }

    /**
     * A tag as the Limelight sees it in "robot space" → our robot frame.
     * Returns {forward, left, up} in inches, or null if the camera sent no 3D
     * pose for it.
     *
     * Limelight robot space: metres, +X forward, +Y to the robot's RIGHT, +Z
     * up, origin at the robot's centre on the floor. Pedro turns positive
     * toward the LEFT, so Y flips sign here, and nowhere else.
     *
     * Only as good as the camera's mount position in its web UI (see
     * Constants.CAMERA_*). ⚠ VERIFY with CameraCalibration: hold a tag to the
     * robot's left and "left" must come out positive.
     */
    public static double[] limelightTargetToRobot(Pose3D targetRobotSpace) {
        if (targetRobotSpace == null) return null;
        Position p = targetRobotSpace.getPosition();
        // The SDK's placeholder when the camera sent nothing: exactly zero.
        if (p.x == 0 && p.y == 0 && p.z == 0) return null;
        return new double[] { p.unit.toInches(p.x), -p.unit.toInches(p.y), p.unit.toInches(p.z) };
    }

    /** A point relative to the robot → the field, given where the robot is. */
    public static double[] robotToField(Pose robot, double forward, double left) {
        double c = Math.cos(robot.heading());
        double s = Math.sin(robot.heading());
        return new double[] {
                robot.x() + forward * c - left * s,
                robot.y() + forward * s + left * c };
    }

    /** A field point → relative to the robot. Exactly undoes robotToField(). */
    public static double[] fieldToRobot(Pose robot, double fieldX, double fieldY) {
        double c = Math.cos(robot.heading());
        double s = Math.sin(robot.heading());
        double dx = fieldX - robot.x();
        double dy = fieldY - robot.y();
        return new double[] { dx * c + dy * s, -dx * s + dy * c };
    }

    /**
     * A Limelight botpose → a Pedro Pose.
     *
     * The Limelight reports metres, degrees, and (with the stock field map)
     * the FTC centre-origin frame. So: metres→inches, degrees→radians, then
     * the FTC→Pedro rotation and shift above.
     */
    public static Pose limelightToPedro(Pose3D botpose) {
        double xIn = metersToInches(botpose.getPosition().x);
        double yIn = metersToInches(botpose.getPosition().y);
        double[] pedro = ftcToPedro(xIn, yIn);
        double headingRad = ftcHeadingToPedro(Math.toRadians(botpose.getOrientation().getYaw()));
        return new Pose(pedro[0], pedro[1], headingRad);
    }

    // ============================================================
    //                    THE TAGS
    // ============================================================

    /**
     * Every tag in the current game that we can localize from, in our frame.
     * Tags without a field position (motif tags, tags on moving objects) are
     * filtered out.
     */
    public static List<Tag> localizationTags() {
        List<Tag> tags = new ArrayList<>();
        try {
            for (AprilTagMetadata meta : gameLibrary().getAllTags()) {
                if (meta.fieldPosition == null) continue;   // motif tag, not a landmark

                // The library reports in its own DistanceUnit; normalise to inches.
                double xIn = meta.distanceUnit.toInches(meta.fieldPosition.get(0));
                double yIn = meta.distanceUnit.toInches(meta.fieldPosition.get(1));
                double zIn = meta.distanceUnit.toInches(meta.fieldPosition.get(2));

                double[] pedro = ftcToPedro(xIn, yIn);
                double sizeIn = meta.distanceUnit.toInches(meta.tagsize);
                tags.add(new Tag(meta.id, meta.name, pedro[0], pedro[1], zIn, sizeIn));
            }
        } catch (Exception e) {
            // A missing tag library shouldn't stop the robot driving.
        }
        return tags;
    }

    /** @return the tag with this id, or null if it isn't a localization tag. */
    public static Tag byId(int id) {
        for (Tag t : localizationTags()) {
            if (t.id == id) return t;
        }
        return null;
    }

    private static AprilTagLibrary library;
    private static final Map<Integer, String> targetNames = new HashMap<>();

    /** Built once: the SDK makes a fresh copy on every call, and we ask every loop. */
    private static AprilTagLibrary gameLibrary() {
        if (library == null) library = AprilTagGameDatabase.getCurrentGameTagLibrary();
        return library;
    }

    /**
     * What a tag is stuck to: its SDK cluster's name, or "tag 31" if it's on
     * its own. Sightings with the same name are the same object, which is how
     * Sensors merges several tags into one target.
     */
    public static String targetName(int id) {
        String name = targetNames.get(id);
        if (name != null) return name;

        name = "tag " + id;
        try {
            AprilTagClusterMetadata cluster = gameLibrary().lookupCluster(id);
            if (cluster != null) name = cluster.name;
        } catch (Exception ignored) {
            // No library, or a game without clusters. A loner is a fine answer.
        }
        targetNames.put(id, name);
        return name;
    }

    // ============================================================
    //                 UPLOADING TO THE LIMELIGHT
    // ============================================================

    /**
     * Builds a Limelight field map from the SDK's tag data.
     *
     * Why bother, when the Limelight has its own map? Because a map that lives
     * only on the camera is one reflash away from gone, and nobody remembers
     * what was in it. This one lives in git, updates itself when the SDK
     * updates, and gets pushed at init.
     *
     * ⚠ UNVERIFIED ON HARDWARE. The Limelight expects a 4x4 row-major
     * transform in METRES. The translation below is right; the rotation is
     * left as identity, which is fine for position-only localization but means
     * tag FACING is not described. If MegaTag results look wrong in a way that
     * smells like orientation, this is the first place to look — and write down
     * what you find in docs/issue-log.md.
     */
    public static LLFieldMap buildLimelightFieldMap() {
        List<LLFieldMap.Fiducial> fiducials = new ArrayList<>();

        for (Tag tag : localizationTags()) {
            double[] ftc = pedroToFtc(tag.x, tag.y);
            double xM = ftc[0] / METERS_TO_INCHES;
            double yM = ftc[1] / METERS_TO_INCHES;
            double zM = tag.z / METERS_TO_INCHES;

            // 4x4 row-major, identity rotation + translation in the last column.
            List<Double> transform = new ArrayList<>();
            double[] m = {
                    1, 0, 0, xM,
                    0, 1, 0, yM,
                    0, 0, 1, zM,
                    0, 0, 0, 1
            };
            for (double v : m) transform.add(v);

            fiducials.add(new LLFieldMap.Fiducial(
                    tag.id,
                    DistanceUnit.INCH.toMeters(tag.sizeInches),   // from the library, not hardcoded
                    "apriltag3_36h11_classic",
                    transform,
                    true));
        }

        // "ftc": the coordinates above are FTC centre-origin. Was "frc" until
        // 2026-09-19; which of the two the camera honors is unverified on hardware.
        return new LLFieldMap(fiducials, "ftc");
    }
}
