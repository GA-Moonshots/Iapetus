package org.firstinspires.ftc.teamcode.commands;

import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.utils.Tunables;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                     FORWARD BY DISTANCE                                   ║
 * ║                                                                           ║
 * ║  "Go that way N inches." Keeps the current heading. Negative = backward.  ║
 * ║                                                                           ║
 * ║  PEDRO COORDINATE SYSTEM (memorize this, it explains 90% of the           ║
 * ║  autonomous bugs you will ever write):                                    ║
 * ║      0 rad     = facing RIGHT   (+X)                                      ║
 * ║      π/2 rad   = facing UP      (+Y)                                      ║
 * ║      ±π rad    = facing LEFT    (-X)                                      ║
 * ║      -π/2 rad  = facing DOWN    (-Y)                                      ║
 * ║                                                                           ║
 * ║  So for heading θ:   ΔX = distance × cos(θ)                               ║
 * ║                      ΔY = distance × sin(θ)                               ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Same shape as DriveToPose — the only real difference is that this one
 * computes its target instead of being handed one. That's what the abstract
 * base is for: two commands, one set of safety rails.
 */
public class DriveFwdByDist extends DriveAbstract {

    private final double distance;
    private Pose targetPose;
    private boolean arrived = false;

    /**
     * @param distance inches; positive is forward, negative is backward
     */
    public DriveFwdByDist(Iapetus robot, double distance, double timeoutSeconds) {
        super(robot, timeoutSeconds);
        this.distance = distance;
    }

    @Override
    public void initialize() {
        patience.start();
        arrived = false;

        Pose current = drive.getPose();
        double heading = current.heading();

        targetPose = new Pose(
                current.x() + distance * Math.cos(heading),
                current.y() + distance * Math.sin(heading),
                heading  // same direction we're already pointed
        );

        // A path with no heading rule throws the moment it's followed. constant() is the rule.
        follower.follow(Paths.line(current, targetPose).constant(heading));
        drive.setTargetPose(targetPose);   // so the dashboard shows intent vs. reality

        robot.sensors.addTelemetry("FwdByDist", "%.1f\" @ %.1f°",
                distance, Math.toDegrees(heading));
    }

    @Override
    public void execute() {
        if (drive.atPose(targetPose, Tunables.POSE_TOLERANCE)) {
            arrived = true;
        }
    }

    @Override
    public boolean isFinished() {
        return arrived || patience.done();
    }

    @Override
    public void end(boolean interrupted) {
        standardCleanup();
        drive.clearTargetPose();
        robot.sensors.addTelemetry("FwdByDist",
                interrupted ? "INTERRUPTED" : (arrived ? "Arrived" : "TIMED OUT"));
    }
}
