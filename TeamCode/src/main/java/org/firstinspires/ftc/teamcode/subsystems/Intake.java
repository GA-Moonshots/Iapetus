package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.utils.Constants;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                          INTAKE SUBSYSTEM                                 ║
 * ║                                                                           ║
 * ║  Pulls game pieces into the robot. Blank for now — nothing to eat yet.    ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Copy the shape of Ganymede's subsystems (Intake, Launcher, Turret):
 * github.com/GA-Moonshots/Ganymede → TeamCode/.../teamcode/subsystems
 * Hardware lookups in the constructor, hardware names from Constants,
 * small methods that each do one thing.
 */
public class Intake extends SubsystemBase {

    private final Iapetus robot;

    private DcMotorEx intakeMotor;

    public Intake(Iapetus robot) {
        this.robot = robot;
        // intakeMotor = robot.hardwareMap.get(DcMotorEx.class, Constants.INTAKE_MOTOR_NAME);
    }

    @Override
    public void periodic() {
    }
}
