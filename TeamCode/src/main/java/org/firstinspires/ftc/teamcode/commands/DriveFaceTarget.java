package org.firstinspires.ftc.teamcode.commands;

import com.pedropathing.utils.Angle;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.utils.TagSighting;
import org.firstinspires.ftc.teamcode.utils.Tunables;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                        FACE A TARGET                                      ║
 * ║                                                                           ║
 * ║  "Point at that." Picks the nearest target the camera is tracking,        ║
 * ║  locks onto it, and turns until the nose is on it.                        ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * The worked example of using TagSighting. An arm does the same thing with
 * bearingFrom() / elevationFrom() instead of turning the whole robot.
 *
 * Why it aims at the target's FIELD position: the robot-relative angle changes
 * as we turn, so chasing it would mean re-aiming at our own motion. The field
 * position holds still. We only re-aim when the target itself has moved.
 *
 * And only while we're nearly still. Every camera frame is a little late, so
 * mid-spin each one puts the target slightly ahead of us. Re-aim at that and
 * we spin faster, which puts it further ahead: the robot chases until the
 * target leaves the frame. Aim on the press, re-aim once we've slowed down.
 *
 * Nothing in view when it starts? It ends immediately and says so. Guessing
 * is how a robot spins in circles for five seconds.
 */
public class DriveFaceTarget extends DriveAbstract {

    /** Don't restart the turn for wobble smaller than this. Restarting resets the controller. */
    private static final double REAIM_DEGREES = 3.0;

    /** Spinning faster than this, frames are too stale to trust. Keep the current aim. */
    private static final double STILL_ENOUGH_DEG_PER_SEC = 30.0;

    private final boolean ourHiveOnly;
    private String lockedOn = null;
    private double headingRad = 0;

    /** Faces the nearest target the camera is tracking. */
    public DriveFaceTarget(Iapetus robot, double timeoutSeconds) {
        this(robot, timeoutSeconds, false);
    }

    /** @param ourHiveOnly true = only our alliance's scoring Hive, ignore everything else */
    public DriveFaceTarget(Iapetus robot, double timeoutSeconds, boolean ourHiveOnly) {
        super(robot, timeoutSeconds);
        this.ourHiveOnly = ourHiveOnly;
    }

    @Override
    public void initialize() {
        lockedOn = null;   // the bumper reuses this object; forget the last press's target
        patience.start();
        TagSighting target = ourHiveOnly ? robot.sensors.getOurTarget() : robot.sensors.nearestTarget();
        if (target == null) {
            robot.sensors.addTelemetry("FaceTarget", "nothing in view");
            return;
        }
        lockedOn = target.name;
        headingRad = target.headingFrom(drive.getPose());
        drive.turnTo(headingRad);
    }

    @Override
    public void execute() {
        if (lockedOn == null) return;
        TagSighting target = robot.sensors.target(lockedOn);
        if (target == null) return;   // briefly out of view: keep the last aim

        robot.sensors.addTelemetry("FaceTarget", "%s, %.0f\" away", lockedOn, target.range());
        if (Math.abs(drive.turnRateDegPerSec()) > STILL_ENOUGH_DEG_PER_SEC) return;

        double fresh = target.headingFrom(drive.getPose());
        if (Math.abs(Math.toDegrees(Angle.error(headingRad, fresh))) > REAIM_DEGREES) {
            headingRad = fresh;
            drive.turnTo(headingRad);
        }
    }

    @Override
    public boolean isFinished() {
        return lockedOn == null
                || drive.isFacing(headingRad, Tunables.HEADING_TOLERANCE_DEG)
                || patience.done();
    }

    @Override
    public void end(boolean interrupted) {
        standardCleanup();
        if (lockedOn != null) {
            robot.sensors.addTelemetry("FaceTarget",
                    interrupted ? "INTERRUPTED" : (patience.done() ? "TIMED OUT" : "Facing " + lockedOn));
        }
    }
}
