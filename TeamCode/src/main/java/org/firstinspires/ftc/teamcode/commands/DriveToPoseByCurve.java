package org.firstinspires.ftc.teamcode.commands;

import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.utils.Tunables;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                     DRIVE TO POSE BY CURVE                                ║
 * ║                                                                           ║
 * ║  "Get there, but swing past THIS spot on the way." DriveToPose with a     ║
 * ║  bend in it.                                                              ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * The via pose is a Bézier CONTROL POINT: the curve is pulled toward it but
 * doesn't pass through it. Put it further out than you think you need, and
 * watch the result in Panels before trusting it near a wall.
 *
 * Only via's x and y matter; its heading is ignored. The robot holds the
 * target's heading the whole way. Pedro's frame (inches, corner origin).
 * Mirror for the other alliance in FieldMap, not here.
 */
public class DriveToPoseByCurve extends DriveAbstract {

    private final Pose targetPose;
    private final Pose viaPose;
    private boolean arrived = false;

    public DriveToPoseByCurve(Iapetus robot, Pose target, Pose via, double timeoutSeconds) {
        super(robot, timeoutSeconds);
        this.targetPose = target;
        this.viaPose = via;
    }

    @Override
    public void initialize() {
        patience.start();
        arrived = false;

        // start -> via -> target. Read the start now, not in the constructor:
        // where we are depends on whatever command ran before this one.
        follower.follow(Paths.through(drive.getPose(), viaPose, targetPose).constant(targetPose));
        drive.setTargetPose(targetPose);   // so the dashboard shows intent vs. reality

        robot.sensors.addTelemetry("DriveToPoseByCurve", "→ (%.1f, %.1f) via (%.1f, %.1f)",
                targetPose.x(), targetPose.y(), viaPose.x(), viaPose.y());
    }

    @Override
    public void execute() {
        if (drive.atPose(targetPose, Tunables.POSE_TOLERANCE)) {
            arrived = true;
        }

        robot.sensors.addTelemetry("Distance Remaining", "%.1f\"",
                drive.getPose().distance(targetPose));
    }

    @Override
    public boolean isFinished() {
        // Either we got there, or we've run out of patience. Both end the command.
        return arrived || patience.done();
    }

    @Override
    public void end(boolean interrupted) {
        standardCleanup();
        drive.clearTargetPose();
        robot.sensors.addTelemetry("DriveToPoseByCurve",
                interrupted ? "INTERRUPTED" : (arrived ? "Arrived" : "TIMED OUT"));
    }
}