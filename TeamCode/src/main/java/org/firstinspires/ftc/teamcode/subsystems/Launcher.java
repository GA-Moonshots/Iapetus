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
    private double launchSpeed;

    public Launcher(Iapetus robot) {
        this.robot = robot;
        frontMotor = robot.hardwareMap.get(DcMotorEx.class, Constants.LAUNCHER_MOTOR_FRONT_NAME);
        backMotor = robot.hardwareMap.get(DcMotorEx.class, Constants.LAUNCHER_MOTOR_BACK_NAME);
        this.launching = false;
    }
    public void toggleLaunch(){
        launching = !launching;
    }

    public void setLaunchSpeed(double launchSpeed){
        this.launchSpeed = launchSpeed;
    }

    @Override
    public void periodic() {
        frontMotor.setPower(launchSpeed);
        backMotor.setPower(launchSpeed);
        this.currentRpm = frontMotor.getVelocity(AngleUnit.RADIANS) * (30/Math.PI);
        robot.sensors.addTelemetry("Current RPM", "%.1f", currentRpm);

    }
}
