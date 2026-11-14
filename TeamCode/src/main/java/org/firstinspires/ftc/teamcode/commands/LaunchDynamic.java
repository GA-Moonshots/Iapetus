package org.firstinspires.ftc.teamcode.commands;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;
import org.firstinspires.ftc.teamcode.utils.Constants;
import org.firstinspires.ftc.teamcode.utils.TagSighting;
import org.firstinspires.ftc.teamcode.utils.Tunables;


import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Aim → spin up → feed → done. The phase timer is a field because execute()
 * runs every loop; a timer created inside it would restart every loop.
 *
 * Speed: Tunables.AUTO_LAUNCH_SPEED (found with the Launcher Tuner). With
 * AUTO_SHOT_USE_CAMERA on, camera range + physics when the Hive is in view.
 * Either way it always takes the shot.
 */
public class LaunchDynamic extends LaunchAbstract {

    private enum Phase { AIMING, SPINNING_UP, FEEDING, DONE }

    private Phase phase;
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
                double velocity = chooseSpeed();
                if (velocity > 0 && !Double.isInfinite(velocity)) {
                    launcher.setLaunchSpeed(velocity);
                    phase = Phase.SPINNING_UP;
                    phaseTimer.reset();
                }
                break;
            case SPINNING_UP:
                // Feed once the wheels are at speed, or after SPINUP_MAX_MS regardless.
                double ms = phaseTimer.milliseconds();
                if (ms >= Tunables.LAUNCHER_FEED_SERVO_WAIT_MS
                        && (launcher.isAtSpeed() || ms >= Tunables.LAUNCHER_SPINUP_MAX_MS)) {
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

    /** m/s to launch at. Physics only if enabled and it has an answer; otherwise the tuned speed. */
    private double chooseSpeed() {
        if ((robot.mode == Iapetus.Mode.AUTONOMOUS) && !Tunables.AUTO_SHOT_USE_CAMERA) {
            return Tunables.AUTO_LAUNCH_SPEED;
        }
        TagSighting hive = robot.sensors.getOurTarget();
        if (hive != null) {
            double speed = physicsSpeed(hive.range()) * Tunables.LAUNCH_SPEED_SCALE;
            if (speed > 0) {
                robot.sensors.addTelemetry("Launch physics", "%.0f\" → %.2f m/s", hive.range(), speed);
                return speed;
            }
        }
        robot.sensors.addTelemetry("Launch physics", "no answer — using AUTO_LAUNCH_SPEED");
        return Tunables.AUTO_LAUNCH_SPEED;
    }

    /**
     * Launch speed (m/s) the projectile maths says reaches the Hive opening
     * from {@code rangeInches} away, or 0 if no speed can at this angle.
     * Uncorrected: multiply by Tunables.LAUNCH_SPEED_SCALE for the real wheel speed.
     */
    public static double physicsSpeed(double rangeInches) {
        double xDistance = rangeInches * Constants.INCHES_TO_METERS;
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
        // SAFETY CHECK
        // ────────────────────────────────────────────────────────────

        // If 'inside' is negative (or infinite), the target is unreachable at
        // this distance — it would take an imaginary velocity. 0 = no shot.
        if (inside < 0 || Double.isInfinite(inside)) return 0;

        // v₀ = outside · √(inside), in m/s
        return outside * Math.sqrt(inside);
    }
}