package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.subsystems.PedroDrive;
import org.firstinspires.ftc.teamcode.utils.Tunables;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                      DRIVE (DEFAULT COMMAND)                              ║
 * ║                                                                           ║
 * ║  Reads player 1's sticks and drives the robot. This is the drive          ║
 * ║  subsystem's DEFAULT command: the scheduler runs it whenever nothing      ║
 * ║  else has claimed the wheels, and quietly resumes it the instant an       ║
 * ║  auto-drive command finishes. That's why the driver never has to press    ║
 * ║  anything to "get control back."                                          ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * isFinished() returns false forever. Default commands don't end — they get
 * interrupted. That's the whole trick.
 */
public class Drive extends CommandBase {

    private final Iapetus robot;
    private final PedroDrive drive;
    private final GamepadEx player1;

    public Drive(Iapetus robot) {
        this.robot = robot;
        this.drive = robot.drive;
        this.player1 = robot.player1;

        addRequirements(robot.drive);
    }

    @Override
    public void execute() {
        // Hold right bumper for precision mode.
        double easyDoesIt = player1.getButton(GamepadKeys.Button.RIGHT_BUMPER)
                ? Tunables.SLOW_MODE_MULTIPLIER
                : 1.0;

        double forward = deadZone(player1.getLeftY());
        double strafe  = deadZone(-player1.getLeftX());
        double turn    = deadZone(-player1.getRightX());  // negated so right-stick-right turns right

        // Field-centric math is written from red's point of view. Blue drives
        // from the opposite wall, so "away from me" is the other direction.
        // Flip translation only — turning is turning no matter where you stand.
        if (drive.isFieldCentric() && !robot.isRed) {
            forward = -forward;
            strafe = -strafe;
        }

        // This also takes the wheels back from any path or hold the last command
        // left running — the driver never fights a ghost.
        drive.drive(forward * easyDoesIt, strafe * easyDoesIt, turn * easyDoesIt);

        robot.sensors.addTelemetry("Speed Mode", easyDoesIt < 1.0 ? "SLOW" : "NORMAL");
    }

    @Override
    public boolean isFinished() {
        return false;  // default commands run until interrupted
    }

    @Override
    public void end(boolean interrupted) {
        drive.stop();
    }

    /** Sticks drift. Anything this small is a lie. */
    private double deadZone(double input) {
        return Math.abs(input) <= Tunables.INPUT_THRESHOLD ? 0.0 : input;
    }
}
