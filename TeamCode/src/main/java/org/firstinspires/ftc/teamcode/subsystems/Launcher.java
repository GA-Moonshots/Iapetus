package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

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
 * physics share one unit. Modes:
 *   MANUAL — follows player 2's trigger (teleop's default command)
 *   AUTO   — a command such as LaunchDynamic owns the speed
 *   IDLE   — wheels stopped; back to MANUAL once the trigger is released
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
    private double launchSpeed;

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
        this.launchSpeed = launchPower * Constants.VELOCITY_TO_POWER_RATIO;
    }

    /** Auto: wheel surface speed, m/s. */
    public void setLaunchSpeed(double launchSpeed) {
        this.launchSpeed = launchSpeed;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /** Wheels and feed off. Wheels stay off until the trigger is released. */
    public void stopAll() {
        this.mode = Mode.IDLE;
        feeding = false;
        launchSpeed = 0;   // otherwise MANUAL resumes at the last speed (auto has no trigger to reset it)
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
        if (this.mode == Mode.IDLE) {
            // Actively stop. Skipping the write would leave the motors spinning
            // at whatever velocity they were last told.
            frontMotor.setVelocity(0, AngleUnit.RADIANS);
            backMotor.setVelocity(0, AngleUnit.RADIANS);
            // Hand control back to the trigger only once it's been let go, so a
            // held trigger can't spin the wheels straight back up after Y.
            if (Math.abs(robot.player2.getTrigger(GamepadKeys.Trigger.RIGHT_TRIGGER))
                    < Tunables.INPUT_THRESHOLD) {
                this.mode = Mode.MANUAL;
            }
        } else {
            frontMotor.setVelocity(launchSpeed / Constants.FRONT_WHEEL_RADIUS, AngleUnit.RADIANS);
            backMotor.setVelocity(launchSpeed / Constants.BACK_WHEEL_RADIUS, AngleUnit.RADIANS);
        }

        // Always write the feed servos — continuous servos keep turning at the
        // last power they were given, so "stop" has to be said out loud.
        double feedPower = feeding ? 1 : 0;
        feedUpperLeft.setPower(feedPower);
        feedLowerLeft.setPower(feedPower);
        feedUpperRight.setPower(-feedPower);
        feedLowerRight.setPower(-feedPower);

        // Wheels differ in size, so report each rather than an average.
        robot.sensors.addTelemetry("═══ Launcher ═══", "");
        robot.sensors.addTelemetry("Launcher mode", "%s%s", mode, feeding ? " + FEEDING" : "");
        robot.sensors.addTelemetry("Launcher RPM", "front %.0f / back %.0f",
                toRpm(frontMotor.getVelocity(AngleUnit.RADIANS)),
                toRpm(backMotor.getVelocity(AngleUnit.RADIANS)));
        robot.sensors.addTelemetry("Launcher ready", isAtSpeed() ? "YES" : "no");
    }

    private static double toRpm(double radPerSec) {
        return radPerSec * 60 / (2 * Math.PI);
    }
}