package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;
import org.firstinspires.ftc.teamcode.utils.Tunables;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                   LAUNCH MANUAL (DEFAULT COMMAND)                         ║
 * ║                                                                           ║
 * ║  Player 2's right trigger sets flywheel speed; X toggles the feed.        ║
 * ║  Runs whenever no pre-programmed launch owns the launcher, and takes it   ║
 * ║  back the moment one finishes.                                            ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Every time it takes over (start of teleop, after Y's stop, after an auto
 * shot) the trigger has to return to zero before it counts. Otherwise a
 * trigger still held from before spins the wheels straight back up.
 *
 * X is read here rather than bound in Iapetus, so it only works while the
 * operator owns the launcher: a stray press can't start the feed halfway
 * through LaunchDynamic's spin-up.
 */
public class LaunchManual extends LaunchAbstract {

    private final Launcher launcher;
    private final GamepadEx player2;

    /** True until the trigger has been let go since we took over. */
    private boolean waitingForRelease;

    public LaunchManual(Iapetus robot) {
        super(robot, Double.POSITIVE_INFINITY);
        this.launcher = robot.launcher;
        this.player2 = robot.player2;
        addRequirements(launcher);
    }

    @Override
    public void initialize() {
        // The scheduler reuses this one object every time it hands the
        // launcher back, so decide afresh each time.
        waitingForRelease = trigger() > 0;
        if (waitingForRelease) launcher.stopAll();
    }

    @Override
    public void execute() {
        double trigger = trigger();
        if (waitingForRelease) {
            if (trigger > 0) return;      // still held from before: stay stopped
            waitingForRelease = false;    // let go: the trigger is live again
        }

        launcher.setMode(Launcher.Mode.MANUAL);
        launcher.setLaunchPower(trigger * Tunables.LAUNCHER_MAX_POWER);
        if (player2.wasJustPressed(GamepadKeys.Button.X)) launcher.toggleFeed();
    }

    @Override
    public boolean isFinished() {
        return false;  // default commands run until interrupted
    }

    @Override
    public void end(boolean interrupted) {
        launcher.stopAll();   // whoever takes over starts from stopped
    }

    /** Right trigger, 0 to 1. A trigger resting just off zero reads as zero. */
    private double trigger() {
        double t = player2.getTrigger(GamepadKeys.Trigger.RIGHT_TRIGGER);
        return t <= Tunables.INPUT_THRESHOLD ? 0.0 : t;
    }
}
