package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.utils.Constants;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                          INTAKE SUBSYSTEM                                 ║
 * ║                                                                           ║
 * ║  One motor pulls game pieces in. Negative power runs it backwards.        ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
public class Intake extends SubsystemBase {

    private final DcMotorEx intakeMotor;

    public Intake(Iapetus robot) {
        intakeMotor = robot.hardwareMap.get(DcMotorEx.class, Constants.INTAKE_MOTOR_NAME);
        intakeMotor.setDirection(Constants.INTAKE_MOTOR_DIRECTION);
    }

    /** -1 to 1. Negative reverses, e.g. to clear a jam. */
    public void setIntakeSpeed(double intakePower) {
        intakeMotor.setPower(intakePower);
    }
}
