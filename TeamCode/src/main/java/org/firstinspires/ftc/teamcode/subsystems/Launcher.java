package org.firstinspires.ftc.teamcode.subsystems;

import com.pedropathing.utils.Angle;
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

    private double launchSpeedFactor;
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

    public void setLaunchSpeed(double launchSpeed){
        this.launchPower = launchSpeed;
    }

    public void incrementLaunchSpeed(double launchSpeedIncrement) {
        this.launchPower += launchSpeedIncrement;

    }

    @Override
    public void periodic() {
        if (launching) {
            frontMotor.setPower(launchPower);
            backMotor.setPower(launchPower);
        }
        // Take the average of the two motors' speeds
        this.currentRpm = (frontMotor.getVelocity(AngleUnit.RADIANS) * (60 / (2 * Math.PI))
                        + backMotor.getVelocity(AngleUnit.RADIANS) * (60 / (2 * Math.PI)))
                        / 2;
        robot.sensors.addTelemetry("Current RPM", "%.1f", currentRpm);
        robot.sensors.addTelemetry("launchPower", "%.01f", launchPower);
    }
}
