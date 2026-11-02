package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.utils.Constants;

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

    private DcMotorEx frontMotor;
    private DcMotorEx backMotor;

    private double currentRpm;
    private boolean launching;
    private double launchPower = 0;

    public Launcher(Iapetus robot) {
        this.robot = robot;
        frontMotor = robot.hardwareMap.get(DcMotorEx.class, Constants.LAUNCHER_MOTOR_FRONT_NAME);
        backMotor = robot.hardwareMap.get(DcMotorEx.class, Constants.LAUNCHER_MOTOR_BACK_NAME);
        frontMotor.setDirection(Constants.LAUNCHER_MOTOR_FRONT_DIRECTION);
        backMotor.setDirection(Constants.LAUNCHER_MOTOR_BACK_DIRECTION);
        this.launching = false;
    }
    public void toggleLaunch(){
        launching = !launching;
    }

    /**
     * Set the launcher to a certain power, from 0 (stopped) to the maximum power
     * allowed in Constants. The method will refuse values outside this range,
     * but will not throw an exception, just let you know in telemetry.
     * @param launchPower The power the motor should drive at.
     */
    public void setLaunchPower(double launchPower) {
        if (launchPower < 0 || launchPower > Constants.LAUNCHER_MAX_POWER) {
            robot.sensors.addTelemetry("Illegal launch power argument: ", "%.2f", launchPower);
            return;
        }
        this.launchPower = launchPower;
    }

    /**
     * Set the launcher to fire a game object at a certain speed.
     * @param launchSpeed The speed, in meters/second.
     */
    public void setLaunchSpeed(double launchSpeed) {
        setLaunchPower((launchSpeed / Constants.WHEEL_RADIUS) / Constants.POWER_TO_VELOCITY_RATIO);
    }

    @Override
    public void periodic() {
        if (launching) {
            frontMotor.setPower(launchPower);
            backMotor.setPower(launchPower);
        }
        // Take the average of the two motors' speeds and report it:
        this.currentRpm = (frontMotor.getVelocity(AngleUnit.RADIANS) * (60 / (2 * Math.PI))
                + backMotor.getVelocity(AngleUnit.RADIANS) * (60 / (2 * Math.PI)))
                / 2;
        robot.sensors.addTelemetry("Current RPM", "%.1f", currentRpm);
        robot.sensors.addTelemetry("launchPower", "%.2f", launchPower);
    }
}