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
 * ║  Sends game pieces toward the goal. Blank for now — no liftoff yet.       ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Copy the shape of Ganymede's subsystems (Intake, Launcher, Turret):
 * github.com/GA-Moonshots/Ganymede → TeamCode/.../teamcode/subsystems
 * Hardware lookups in the constructor, hardware names from Constants,
 * small methods that each do one thing.
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

    public static enum Mode {IDLE, MANUAL, AUTO};

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


    /**
     * Set the launcher to a certain power, from 0 (stopped) to the maximum power
     * allowed in Constants. The method will refuse values outside this range,
     * but will not throw an exception, just let you know in telemetry.
     * @param launchPower The power the motor should drive at.
     */
    public void setLaunchPower(double launchPower) {
        if (launchPower < 0 || launchPower > Tunables.LAUNCHER_MAX_POWER) {
            robot.sensors.addTelemetry("Illegal launch power argument: ", "%.2f", launchPower);
            return;
        }
        this.launchSpeed = launchPower * Constants.VELOCITY_TO_POWER_RATIO;
    }

    /**
     * Set the launcher to fire a game object at a certain speed.
     * @param launchSpeed The speed, in meters/second.
     */
    public void setLaunchSpeed(double launchSpeed) {
        this.launchSpeed = launchSpeed;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void stopAll() {
        this.mode = Mode.IDLE;
        feeding = false;
    }

    @Override
    public void periodic() {
        if (this.mode != Mode.IDLE) {
            frontMotor.setVelocity(launchSpeed / Constants.FRONT_WHEEL_RADIUS, AngleUnit.RADIANS);
            backMotor.setVelocity(launchSpeed / Constants.BACK_WHEEL_RADIUS, AngleUnit.RADIANS);
        } else if (Math.abs(robot.player2.getTrigger(GamepadKeys.Trigger.RIGHT_TRIGGER))
                 < Tunables.INPUT_THRESHOLD) {
            stopAll();
            this.mode = Mode.MANUAL;
        }
        if (feeding) {
            feedUpperLeft.setPower(1);
            feedUpperRight.setPower(1);
            feedLowerLeft.setPower(-1);
            feedLowerRight.setPower(-1);
        }
        // Take the average of the two motors' speeds and report it:
        double currentRpm =  (frontMotor.getVelocity(AngleUnit.RADIANS) * (60 / (2 * Math.PI))
                            + backMotor.getVelocity(AngleUnit.RADIANS) * (60 / (2 * Math.PI)))
                            / 2;
        robot.sensors.addTelemetry("Launcher Control:", mode.toString());
        robot.sensors.addTelemetry("Current RPM", "%.1f", currentRpm);
    }
}