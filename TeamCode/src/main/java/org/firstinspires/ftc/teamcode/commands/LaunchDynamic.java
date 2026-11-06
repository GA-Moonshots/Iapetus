package org.firstinspires.ftc.teamcode.commands;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.utils.Constants;

public class LaunchDynamic extends LaunchAbstract {

    boolean launchComplete = false;

    public LaunchDynamic(Iapetus robot) {
        super(robot);
    }

    public void execute() {
        launcher.setLaunchSpeed(calculateLaunchVelocity());
        launchComplete = true;
    }

    private String getTarget() {
        return robot.sensors.getOurTarget();
    }

    @Override
    public boolean isFinished() {
        return launchComplete;
    }

    private double calculateLaunchVelocity() {
        // ────────────────────────────────────────────────────────────
        // SAFETY CHECK 1
        // ────────────────────────────────────────────────────────────

        // See if we can actually see our target, and that it is turned upwards.
        // If not, return a velocity of 0 (do not shoot).
        if (robot.sensors.target(getTarget()) == null) {
            robot.sensors.addTelemetry("TARGET WARNING: ", "Target not found");
            return 0;
        } else if (robot.sensors.target(getTarget()).up
                < Constants.HIVE_OPENING_ELEVATION - Constants.HIVE_HEIGHT_TOLERANCE) {
            robot.sensors.addTelemetry("TARGET WARNING: ", "Target is not upright");
            return 0;
        }

        double xDistance = robot.sensors.target(getTarget()).range() * Constants.INCHES_TO_METERS;
        double yDistance = (Constants.HIVE_OPENING_ELEVATION - Constants.LAUNCHER_ELEVATION)
                * Constants.INCHES_TO_METERS;

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
        // Then v₀ = (outside) √(inside).

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