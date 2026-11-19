package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.utils.Constants;
import org.firstinspires.ftc.teamcode.utils.Tunables;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                         LAUNCHER SUBSYSTEM                                ║
 * ║                                                                           ║
 * ║  Two flywheels launch; four continuous servos feed them.                  ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Speeds are wheel surface speed in m/s, so teleop's trigger and auto's
 * physics share one unit. Modes say who's in charge, for telemetry:
 *   MANUAL — LaunchManual, teleop's default command, following the trigger
 *   AUTO   — a pre-programmed command such as LaunchDynamic
 *   IDLE   — stopped
 *
 * Never reads a gamepad. Whatever command owns the launcher says what to do;
 * this only does it.
 */
public class Launcher extends SubsystemBase {
    private final Iapetus robot;

    private final DcMotorEx frontMotor;
    private final DcMotorEx backMotor;
    private final CRServo feedUpperLeft;
    private final CRServo feedUpperRight;
    private final CRServo feedLowerLeft;
    private final CRServo feedLowerRight;

    private Mode mode;
    private boolean feeding;
    /**
     * Speed of the wheels, in METERS/SEC.
     */
    private double launchSpeed;
    private double rotationSpeed;

    public enum Mode { IDLE, MANUAL, AUTO }

    public Launcher(Iapetus robot) {
        this.robot = robot;
        this.mode = Mode.IDLE;
        frontMotor = robot.hardwareMap.get(DcMotorEx.class, Constants.LAUNCHER_MOTOR_FRONT_NAME);
        backMotor = robot.hardwareMap.get(DcMotorEx.class, Constants.LAUNCHER_MOTOR_BACK_NAME);
        feedUpperLeft = robot.hardwareMap.get(CRServo.class, Constants.LAUNCHER_FEED_UPPER_LEFT_NAME);
        feedUpperRight = robot.hardwareMap.get(CRServo.class, Constants.LAUNCHER_FEED_UPPER_RIGHT_NAME);
        feedLowerLeft = robot.hardwareMap.get(CRServo.class, Constants.LAUNCHER_FEED_LOWER_LEFT_NAME);
        feedLowerRight = robot.hardwareMap.get(CRServo.class, Constants.LAUNCHER_FEED_LOWER_RIGHT_NAME);

        frontMotor.setDirection(Constants.LAUNCHER_MOTOR_FRONT_DIRECTION);
        backMotor.setDirection(Constants.LAUNCHER_MOTOR_BACK_DIRECTION);
        feedUpperLeft.setDirection(Constants.LAUNCHER_FEED_SERVO_DIRECTION);
        feedUpperRight.setDirection(Constants.LAUNCHER_FEED_SERVO_DIRECTION);
        feedLowerLeft.setDirection(Constants.LAUNCHER_FEED_SERVO_DIRECTION);
        feedLowerRight.setDirection(Constants.LAUNCHER_FEED_SERVO_DIRECTION);
    }

    public void toggleFeed() {
        feeding = !feeding;
    }

    public void setFeeding(boolean feeding) {
        this.feeding = feeding;
    }

    /**
     * Teleop: 0 (stopped) to Tunables.LAUNCHER_MAX_POWER. Out-of-range values
     * are ignored and reported on telemetry rather than thrown.
     */
    public void setLaunchPower(double launchPower) {
        if (launchPower < 0 || launchPower > Tunables.LAUNCHER_MAX_POWER) {
            robot.sensors.addTelemetry("Illegal launch power", "%.2f", launchPower);
            return;
        }
        this.launchSpeed = powerToLaunchSpeed(launchPower);
    }

    /** Auto: wheel surface speed, m/s. Clamped to 0..maxLaunchSpeed(). */
    public void setLaunchSpeed(double launchSpeed) {
        this.launchSpeed = Math.max(0, Math.min(launchSpeed, maxLaunchSpeed()));
    }

    /** The speed we're aiming for, m/s, after clamping. */
    public double getLaunchSpeed() {
        return launchSpeed;
    }

    /**
     * Calculate the launch speed (tangential speed) given a certain maximum
     * launch power, clamped between 0 and Tunables.LAUNCHER_MAX_POWER.
     * The bottleneck is the wheel with the SMALLER radius: it requires more
     * angular speed, and thus power, to hit the same tangential one.
     */
    public static double powerToLaunchSpeed(double power) {
        power = Math.max(0, Math.min(power, Tunables.LAUNCHER_MAX_POWER));
        return power * Constants.LAUNCH_SPEED_PER_POWER *
                Math.min(Constants.FRONT_WHEEL_RADIUS, Constants.BACK_WHEEL_RADIUS);
    }


    public static double maxLaunchSpeed() {
        return powerToLaunchSpeed(Tunables.LAUNCHER_MAX_POWER);
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /** Wheels and feed off. Whoever owns the launcher next starts from stopped. */
    public void stopAll() {
        this.mode = Mode.IDLE;
        feeding = false;
        launchSpeed = 0;
    }

    /** Both wheels within Tunables.LAUNCHER_SPEED_TOLERANCE of target. False when stopped. */
    public boolean isAtSpeed() {
        if (mode == Mode.IDLE || launchSpeed <= 0) return false;
        return nearTarget(frontMotor, launchSpeed / Constants.FRONT_WHEEL_RADIUS)
                && nearTarget(backMotor, launchSpeed / Constants.BACK_WHEEL_RADIUS);
    }

    private static boolean nearTarget(DcMotorEx motor, double targetRadPerSec) {
        double actual = motor.getVelocity(AngleUnit.RADIANS);
        return Math.abs(actual - targetRadPerSec) <= Tunables.LAUNCHER_SPEED_TOLERANCE * targetRadPerSec;
    }

    @Override
    public void periodic() {
        // Constantly set the wheels' velocities, even to stop them: skip the write and they
        // keep spinning at whatever velocity they were last told. launchSpeed is in METERS/SEC,
        // and is converted to RADIANS/SEC and fed to the setVelocity() method.
        double speed = (mode == Mode.IDLE) ? 0 : launchSpeed;
        frontMotor.setVelocity(speed / Constants.FRONT_WHEEL_RADIUS, AngleUnit.RADIANS);
        backMotor.setVelocity(speed / Constants.BACK_WHEEL_RADIUS, AngleUnit.RADIANS);

        // Same for the feed servos — continuous servos keep turning at the
        // last power they were given, so the "stop" command must be explicitly given.
        double feedPower = feeding ? 1 : 0;
        feedUpperLeft.setPower(feedPower);
        feedLowerLeft.setPower(feedPower);
        feedUpperRight.setPower(-feedPower);
        feedLowerRight.setPower(-feedPower);

        // Report each wheel rather than an average: if they disagree, that's the news.
        robot.sensors.addTelemetry("═══ Launcher ═══", "");
        robot.sensors.addTelemetry("Launcher mode", "%s%s", mode, feeding ? " + FEEDING" : "");
        robot.sensors.addTelemetry("Launcher RPM", "front %.0f / back %.0f",
                toRpm(frontMotor.getVelocity(AngleUnit.RADIANS)),
                toRpm(backMotor.getVelocity(AngleUnit.RADIANS)));
        robot.sensors.addTelemetry("Launcher ready", isAtSpeed() ? "YES" : "NO");
    }

    private static double toRpm(double radPerSec) {
        return radPerSec * 60 / (2 * Math.PI);
    }
}
