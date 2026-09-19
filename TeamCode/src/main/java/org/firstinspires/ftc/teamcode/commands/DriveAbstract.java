package org.firstinspires.ftc.teamcode.commands;

import com.pedropathing.follower.Follower;
import com.seattlesolvers.solverslib.command.CommandBase;
import com.seattlesolvers.solverslib.util.Timing;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.subsystems.PedroDrive;

import java.util.concurrent.TimeUnit;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                     DRIVE COMMAND BASE CLASS                              ║
 * ║                                                                           ║
 * ║  Every command that moves the robot extends this. It hands you            ║
 * ║  robot / drive / follower / timer already wired up, and claims the        ║
 * ║  drive subsystem so two commands can't fight over the wheels.             ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * THE TIMEOUT IS NOT OPTIONAL. An autonomous path command with no timeout
 * that never quite reaches its tolerance will run until the match ends,
 * blocking every command queued behind it. Ask for a timeout you'd be
 * comfortable watching the robot do nothing for.
 *
 * Subclasses: DriveToPose, DriveFwdByDist, DriveTurnBy, DriveTurnTo,
 * DriveFaceTarget. Adding another is the normal way to extend this — copy the
 * closest one.
 */
public abstract class DriveAbstract extends CommandBase {

    protected final Iapetus robot;
    protected final PedroDrive drive;
    protected final Follower follower;

    /** How long we'll humor this command before pulling the plug. */
    protected final Timing.Timer patience;

    /**
     * @param timeoutSeconds 3–5s for a short hop, 10–15s for a long path.
     */
    public DriveAbstract(Iapetus robot, double timeoutSeconds) {
        this.robot = robot;
        this.drive = robot.drive;
        this.follower = robot.drive.follower;
        this.patience = new Timing.Timer((long) (timeoutSeconds * 1000), TimeUnit.MILLISECONDS);

        addRequirements(drive);
    }

    /**
     * Call from end(). Tells Pedro to stop chasing its path (or holding its
     * turn) and cuts motor power — skip this and the follower keeps fighting
     * the next command, or the driver's joystick, which is worse.
     */
    protected void standardCleanup() {
        drive.stop();
    }
}
