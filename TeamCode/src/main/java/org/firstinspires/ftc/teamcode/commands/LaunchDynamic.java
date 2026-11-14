package org.firstinspires.ftc.teamcode.commands;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;
import org.firstinspires.ftc.teamcode.utils.Constants;
import org.firstinspires.ftc.teamcode.utils.TagSighting;
import org.firstinspires.ftc.teamcode.utils.Tunables;


import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Aim → spin up → feed → done. One step per phase, timed with a single timer
 * that lives across loops (execute() runs every loop, so anything created
 * inside it starts over every loop).
 *
 * Won't feed until the camera has given us a real distance — no target means
 * no shot, and the patience timeout moves auto along.
 */
public class LaunchDynamic extends LaunchAbstract {

    private enum Phase { AIMING, SPINNING_UP, FEEDING, DONE }

    private Phase phase = Phase.AIMING;
    private final ElapsedTime phaseTimer = new ElapsedTime();

    public LaunchDynamic(Iapetus robot, double timeoutSeconds) {
        super(robot, timeoutSeconds);
    }

    @Override
    public void initialize() {
        patience.start();   // Timing.Timer starts paused: without this it never times out
        phase = Phase.AIMING;
        launcher.setFeeding(false);
        launcher.setLaunchSpeed(0);
        launcher.setMode(Launcher.Mode.AUTO);
    }

    @Override
    public void execute() {
        switch (phase) {
            case AIMING:
                // Lock the speed in once; the robot isn't moving while it shoots.
                double velocity = calculateLaunchVelocity();
                if (velocity > 0 && !Double.isInfinite(velocity)) {
                    launcher.setLaunchSpeed(velocity);
                    phase = Phase.SPINNING_UP;
                    phaseTimer.reset();
                }
                break;
            case SPINNING_UP:
                if (phaseTimer.milliseconds() >= Tunables.LAUNCHER_FEED_SERVO_WAIT_MS) {
                    launcher.setFeeding(true);
                    phase = Phase.FEEDING;
                    phaseTimer.reset();
                }
                break;
            case FEEDING:
                if (phaseTimer.milliseconds() >= Tunables.LAUNCHER_FEED_SERVO_DURATION_MS) {
                    phase = Phase.DONE;
                }
                break;
            default:
                break;
        }
        robot.sensors.addTelemetry("Launch", phase.toString());
    }

    @Override
    public boolean isFinished() {
        return phase == Phase.DONE || patience.done();
    }

    @Override
    public void end(boolean interrupted) {
        launcher.stopAll();
        robot.sensors.addTelemetry("Launch",
                interrupted ? "INTERRUPTED" : (phase == Phase.DONE ? "Done" : "TIMED OUT in " + phase));
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
        // (would require imaginary velocity). Return 0: no shot.
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