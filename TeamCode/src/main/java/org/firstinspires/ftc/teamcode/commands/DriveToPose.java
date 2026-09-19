package org.firstinspires.ftc.teamcode.commands;

import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.utils.Tunables;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                        DRIVE TO POSE                                      ║
 * ║                                                                           ║
 * ║  "Go stand exactly there, facing exactly that way." The workhorse of      ║
 * ║  autonomous — most routines are a stack of these in a                     ║
 * ║  SequentialCommandGroup.                                                  ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Note how empty execute() is. We hand the follower a path during
 * initialize() and it does the actual driving from PedroDrive.periodic().
 * Commands say WHAT; the follower handles HOW.
 */
public class DriveToPose extends DriveAbstract {

    private final Pose targetPose;
    private boolean arrived = false;

    public DriveToPose(Iapetus robot, Pose target, double timeoutSeconds) {
        super(robot, timeoutSeconds);
        this.targetPose = target;
    }

    @Override
    public void initialize() {
        patience.start();
        arrived = false;

        // A path with no heading rule throws the moment it's followed. constant() is the rule.
        follower.follow(Paths.line(drive.getPose(), targetPose).constant(targetPose));
        drive.setTargetPose(targetPose);   // so the dashboard shows intent vs. reality

        robot.sensors.addTelemetry("DriveToPose", "→ (%.1f, %.1f)",
                targetPose.x(), targetPose.y());
    }

    @Override
    public void execute() {
        if (drive.atPose(targetPose, Tunables.POSE_TOLERANCE)) {
            arrived = true;
        }

        Pose current = drive.getPose();
        robot.sensors.addTelemetry("Distance Remaining", "%.1f\"",
                current.distance(targetPose));
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
        robot.sensors.addTelemetry("DriveToPose",
                interrupted ? "INTERRUPTED" : (arrived ? "Arrived" : "TIMED OUT"));
    }
}
