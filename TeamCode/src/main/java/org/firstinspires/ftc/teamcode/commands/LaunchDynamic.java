package org.firstinspires.ftc.teamcode.commands;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;
import org.firstinspires.ftc.teamcode.utils.Constants;
import org.firstinspires.ftc.teamcode.utils.TagSighting;
import org.firstinspires.ftc.teamcode.utils.Tunables;


import java.util.concurrent.TimeUnit;
import com.seattlesolvers.solverslib.util.Timing;

public class LaunchDynamic extends LaunchAbstract {

    boolean launchComplete = false;

    public LaunchDynamic(Iapetus robot, double timeoutSeconds) {
        super(robot, timeoutSeconds);
    }

    @Override
    public void execute() {
        Timing.Timer feedDelay = new Timing.Timer(
                Tunables.LAUNCHER_FEED_SERVO_WAIT_MS, TimeUnit.MILLISECONDS);
        Timing.Timer feedDuration = new Timing.Timer(
                Tunables.LAUNCHER_FEED_SERVO_DURATION_MS, TimeUnit.MILLISECONDS);
        launcher.setLaunchSpeed(calculateLaunchVelocity());
        launcher.setMode(Launcher.Mode.AUTO);
        feedDelay.start();
        if (feedDelay.done()) {
            feedDuration.start();
            launcher.toggleFeed();
        }
        if (feedDuration.done()) {
            launcher.toggleFeed();
            launcher.stopAll();
            launchComplete = true;
        }
    }

    @Override
    public boolean isFinished() {
        return launchComplete || patience.done();
    }

    private TagSighting getTarget() {
        return robot.sensors.getOurTarget();
    }

    private double calculateLaunchVelocity() {
        // ────────────────────────────────────────────────────────────
        // SAFETY CHECK 1
        // ────────────────────────────────────────────────────────────

        // See if we can actually see our target.
        if (getTarget() == null) {
            robot.sensors.addTelemetry("TARGET WARNING: ", "Target not found");
            return 0;
        }

        double xDistance = getTarget().range() * Constants.INCHES_TO_METERS;
        double yDistance = (Constants.HIVE_OPENING_ELEVATION - Constants.LAUNCHER_ELEVATION);

        // Launch angle in radians (physics functions use radians)
        double theta = Constants.SHOOTING_ANGLE;

        // ────────────────────────────────────────────────────────────
        // PROJECTILE MOTION FORMULA
        // ────────────────────────────────────────────────────────────
        //
        // This is the rearranged projectile motion equation solving for v₀²:
        //
        //   v₀² = gx² / (2 * (x tan(θ) - y) * cos²(θ))
        //
        // Then:
        //   v₀ = (x / cos(θ)) * √(g / (2 * (x tan(θ) - y)))
        //
        // The "inside" variable holds the expression under the square root:
        //   inside = g / (2 * (x tan(θ) - y))
        // And "outside" holds the rest:
        //   outside = x / cos(θ)
        //
        // Then v₀ = (outside) * √(inside).

        double tanTheta = Math.tan(theta);
        double cosTheta = Math.cos(theta);

        // Denominator: 2 * (x tan(θ) - y)
        double denominator = 2 * (xDistance * tanTheta - yDistance);

        // Inside = g / denominator:
        double inside = Constants.GRAVITY / denominator;

        // Outside = x / cos(θ):
        double outside = xDistance / cosTheta;

        // ────────────────────────────────────────────────────────────
        // SAFETY CHECK 2
        // ────────────────────────────────────────────────────────────

        // If 'inside' is negative, the target is unreachable at this distance
        // (would require imaginary velocity). Return max power as fallback.
        if (inside < 0) {
            robot.sensors.addTelemetry("KINEMATICS WARNING: ", "Unreachable target");
            return 0;
        }
        // ────────────────────────────────────────────────────────────
        // FINAL CALCULATION
        // ────────────────────────────────────────────────────────────

        // v₀ = √(inside)  [this is the required initial velocity in m/s]
        double velocity = outside * Math.sqrt(inside);

        // Debug telemetry for tuning
        robot.sensors.addTelemetry("Kinematics Debug", "r=%.2fm, y=%.2fm, v₀=%.2fm/s",
                xDistance, yDistance, velocity);

        return velocity;
    }
}