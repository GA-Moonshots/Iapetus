package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;
import com.seattlesolvers.solverslib.util.Timing;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;

import java.util.concurrent.TimeUnit;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                    LAUNCH COMMAND BASE CLASS                              ║
 * ║                                                                           ║
 * ║  Every pre-programmed launch extends this. It hands you robot /           ║
 * ║  launcher / timer already wired up, and claims the launcher so the        ║
 * ║  operator's trigger (LaunchManual) can't fight it mid-shot.               ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Same deal as DriveAbstract: the timeout is not optional. Call
 * patience.start() in initialize() (a Timing.Timer starts paused) and
 * standardCleanup() in end().
 *
 * Works in auto and teleop alike: bind it to a button and it interrupts
 * LaunchManual, which takes the trigger back when it's done.
 *
 * Subclasses: LaunchDynamic.
 */
public abstract class LaunchAbstract extends CommandBase {

    protected final Iapetus robot;
    protected final Launcher launcher;

    /** How long we'll wait on a shot before giving up on it. */
    protected final Timing.Timer patience;

    public LaunchAbstract(Iapetus robot, double timeoutSeconds) {
        this.robot = robot;
        this.launcher = robot.launcher;
        this.patience = new Timing.Timer((long) (timeoutSeconds * 1000), TimeUnit.MILLISECONDS);

        addRequirements(launcher);
    }

    /** Call from end(). Wheels and feed off; the next owner starts from stopped. */
    protected void standardCleanup() {
        launcher.stopAll();
    }
}
