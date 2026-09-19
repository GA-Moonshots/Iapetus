package org.firstinspires.ftc.teamcode.utils;

import com.pedropathing.math.Pose;

import java.util.List;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                           TAG SIGHTING                                    ║
 * ║                                                                           ║
 * ║  "There's a tag over there." Where it is on the field, where it is        ║
 * ║  relative to us right now, and how old the news is.                       ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * The camera sees a tag relative to the robot. Odometry knows where the robot
 * is. Together they put the tag on the FIELD, and that's what we store: a
 * field position stays true while the robot drives and turns, so a tag that
 * slips out of view for half a second is still exactly where we left it.
 * Every query re-expresses it relative to wherever the robot is now.
 *
 * Note which way the maths runs. Localization trusts a fixed tag to say where
 * the ROBOT is. Tracking trusts odometry to say where the TAG is, which is
 * the only option when the tag rides on something that moves. Same camera,
 * opposite direction of trust.
 *
 * Frames, all Pedro-style, all inches:
 *   field:  fieldX, fieldY  — corner origin, same as every Pose in this repo
 *   robot:  forward, left, up — from the robot's centre on the floor.
 *           +left matches Pedro, where a positive turn is counter-clockwise.
 *
 * A "target" is one of these built from several tags on the same object.
 * See Sensors.targets().
 */
public class TagSighting {

    /** The tag's id. For a merged target, the lowest id that was in view. */
    public final int id;

    /** The SDK's name for the object this tag is on, or "tag 31" if it's a loner. */
    public final String name;

    /** How many tags went into this. 1 for a single tag. */
    public final int tagCount;

    /** Field position, Pedro frame. */
    public final double fieldX, fieldY;

    /** Height above the floor. */
    public final double up;

    /** Relative to the robot, as of the pose this was built from. */
    public final double forward, left;

    private final long seenAtNanos;

    public TagSighting(int id, String name, int tagCount,
                       double fieldX, double fieldY, double up,
                       long seenAtNanos, Pose robotNow) {
        this.id = id;
        this.name = name;
        this.tagCount = tagCount;
        this.fieldX = fieldX;
        this.fieldY = fieldY;
        this.up = up;
        this.seenAtNanos = seenAtNanos;

        double[] rel = FieldMap.fieldToRobot(robotNow, fieldX, fieldY);
        this.forward = rel[0];
        this.left = rel[1];
    }

    /** The same sighting, re-measured from where the robot is now. */
    public TagSighting seenFrom(Pose robotNow) {
        return new TagSighting(id, name, tagCount, fieldX, fieldY, up, seenAtNanos, robotNow);
    }

    /** Milliseconds since the camera actually captured this. */
    public double ageMs() {
        return (System.nanoTime() - seenAtNanos) / 1_000_000.0;
    }

    // ============================================================
    //                    FROM THE ROBOT'S CENTRE
    // ============================================================

    /** Flat-floor distance, inches. */
    public double range() {
        return Math.hypot(forward, left);
    }

    /** Radians to turn to face it. + = left (counter-clockwise), like every Pedro heading. */
    public double bearing() {
        return Math.atan2(left, forward);
    }

    /** The field heading that points the robot's nose straight at it. */
    public double headingFrom(Pose robotNow) {
        return Math.atan2(fieldY - robotNow.y(), fieldX - robotNow.x());
    }

    // ============================================================
    //              FROM A MECHANISM (arm pivot, turret, …)
    // ============================================================
    //  An arm doesn't pivot at the robot's centre. Pass where the pivot is,
    //  in the same robot frame (inches: forward, left, up from the centre on
    //  the floor), and get the angles that pivot needs.

    /** Radians the pivot must swing left (+) or right (−). */
    public double bearingFrom(double pivotForward, double pivotLeft) {
        return Math.atan2(left - pivotLeft, forward - pivotForward);
    }

    /** Radians the pivot must tilt up (+) or down (−). */
    public double elevationFrom(double pivotForward, double pivotLeft, double pivotUp) {
        double flat = Math.hypot(forward - pivotForward, left - pivotLeft);
        return Math.atan2(up - pivotUp, flat);
    }

    /** Straight-line inches from the pivot. How far the arm has to reach. */
    public double distanceFrom(double pivotForward, double pivotLeft, double pivotUp) {
        double flat = Math.hypot(forward - pivotForward, left - pivotLeft);
        return Math.hypot(flat, up - pivotUp);
    }

    // ============================================================

    /**
     * Several tags on one object → one estimate of that object: the average
     * of what's in view.
     *
     * Honest limitation: where each tag sits on its object is private to the
     * SDK, so this is the centre of the tags we can SEE, not of the object.
     * Every tag in view → the centre of the tags, which is the object's only
     * if they're laid out evenly around it. One tag at the edge → off by
     * however far that tag is from the middle. tagCount says which case you're
     * in; a season repo that needs better can add the offsets in FieldMap.
     */
    public static TagSighting merge(List<TagSighting> sameObject, Pose robotNow) {
        int lowestId = Integer.MAX_VALUE;
        int count = 0;
        double x = 0, y = 0, z = 0;
        long newest = Long.MIN_VALUE;
        for (TagSighting s : sameObject) {
            lowestId = Math.min(lowestId, s.id);
            count += s.tagCount;
            x += s.fieldX * s.tagCount;
            y += s.fieldY * s.tagCount;
            z += s.up * s.tagCount;
            newest = Math.max(newest, s.seenAtNanos);
        }
        return new TagSighting(lowestId, sameObject.get(0).name, count,
                x / count, y / count, z / count, newest, robotNow);
    }
}
