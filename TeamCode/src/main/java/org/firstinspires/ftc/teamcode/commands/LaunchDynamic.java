package org.firstinspires.ftc.teamcode.commands;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;
import org.firstinspires.ftc.teamcode.utils.Constants;

public class LaunchDynamic extends LaunchAbstract {

    public LaunchDynamic(Iapetus robot) {
        super(robot);
    }

    public void execute() {

    }

    private String getTarget() {
        if (robot.isRed) {
            if (robot.drive.getPose().y() < 72) return Constants.RED_AUDIENCE_HIVE;
            return Constants.RED_SCORING_HIVE;
        }
        if (robot.drive.getPose().y() < 72) return Constants.BLUE_AUDIENCE_HIVE;
        return Constants.BLUE_SCORING_HIVE;
    }

    private double calculateLaunchPower() {
        // ────────────────────────────────────────────────────────────
        // PHYSICAL CONSTANTS
        // ────────────────────────────────────────────────────────────
        double xDistance = robot.sensors.target(getTarget()).range();
        double yDistance = Constants.HIVE_OPENING_ELEVATION - Constants.LAUNCHER_ELEVATION;

        // Launch angle in radians (physics functions use radians)
        double theta = Constants.SHOOTING_ANGLE;

        // Velocity-to-power conversion factor
        // Higher k = more velocity per unit power
        double k = Constants.POWER_TO_VELOCITY_RATIO;

        // ────────────────────────────────────────────────────────────
        // DISTANCE CALCULATION
        // ────────────────────────────────────────────────────────────

        // Get current position and calculate distance to goal
        Pose currentPose = robot.drive.getPose();

        // Get target based on alliance
        double targetX = robot.isRed ? Constants.RED_TARGET_X : Constants.BLUE_TARGET_X;
        double targetY = robot.isRed ? Constants.RED_TARGET_Y : Constants.BLUE_TARGET_Y;

        // Calculate position deltas
        double delta_x = currentPose.getX() - targetX;
        double delta_y = currentPose.getY() - targetY;

        // Horizontal distance to goal (Pythagorean theorem)
        // Convert from inches to meters for physics calculation
        double h = Math.sqrt(Math.pow(delta_x, 2) + Math.pow(delta_y, 2)) * Constants.INCHES_TO_METERS;


        // ────────────────────────────────────────────────────────────
        // PROJECTILE MOTION FORMULA
        // ────────────────────────────────────────────────────────────
        //
        // This is the rearranged projectile motion equation solving for v₀²:
        //
        //   v₀² = -g / ((y - x×tan(θ)) × 2 × cos²(θ))
        //
        // The "inside" variable holds the expression under the square root:
        //   inside = -g / ((y - x×tan(θ)) × 2 × cos²(θ))
        //
        // Then v₀ = √(inside)
        // And power = v₀ / k
        // Formula for initial velocity

        double tanTheta = Math.tan(theta);
        double cosTheta = Math.cos(theta);
        double cosThetaSquared = cosTheta * cosTheta;

        // Denominator: (y - x×tan(θ)) × 2 × cos²(θ)
        double denominator = (r * tanTheta - y) * 2 * cosThetaSquared;

        // The full expression: -g / denominator
        // Note: denominator is typically negative (ball must rise then fall),
        // so -g / (negative) gives positive v₀²
        double inside = Constants.GRAVITY / denominator;

        // ────────────────────────────────────────────────────────────
        // SAFETY CHECK
        // ────────────────────────────────────────────────────────────

        // If 'inside' is negative, the target is unreachable at this angle
        // (would require imaginary velocity). Return max power as fallback.
        if (inside < 0) {
            robot.sensors.addTelemetry("KINEMATICS WARNING",
                    "Target unreachable at θ=%.0f°, using max power", 75);
            return Constants.MAX_POWER;
        }
        // ────────────────────────────────────────────────────────────
        // FINAL CALCULATION
        // ────────────────────────────────────────────────────────────

        // v₀ = √(inside)  [this is the required initial velocity in m/s]
        double velocity = Math.sqrt(inside);

        // Convert velocity to motor power using empirical factor
        // power = velocity / k
        double power = velocity / k;

        // Debug telemetry for tuning
        robot.sensors.addTelemetry("Kinematics Debug", "r=%.2fm, y=%.2fm, v₀=%.2fm/s",
                h, y, velocity);

        return power;
    }



    }
}
