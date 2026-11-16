package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.utils.Tunables;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                   INTAKE MANUAL (DEFAULT COMMAND)                         ║
 * ║                                                                           ║
 * ║  Player 2's left stick runs the intake: push up to pull in, down to       ║
 * ║  reverse and clear a jam.                                                 ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * The dead zone is the point of this file: a drifting stick would otherwise
 * creep the intake all match.
 *
 * A pre-programmed intake command (auto or teleop) just requires the intake.
 * The scheduler interrupts this one, and hands the stick back when it's done.
 */
public class IntakeManual extends CommandBase {

    private final Intake intake;
    private final GamepadEx player2;

    public IntakeManual(Iapetus robot) {
        this.intake = robot.intake;
        this.player2 = robot.player2;
        addRequirements(intake);
    }

    @Override
    public void execute() {
        double stick = player2.getLeftY();   // + is pushed up
        if (Math.abs(stick) <= Tunables.INPUT_THRESHOLD) stick = 0;
        intake.setIntakeSpeed(stick * Tunables.INTAKE_MAX_POWER);
    }

    @Override
    public boolean isFinished() {
        return false;  // default commands run until interrupted
    }

    @Override
    public void end(boolean interrupted) {
        intake.stop();   // whoever takes over starts from stopped
    }
}
