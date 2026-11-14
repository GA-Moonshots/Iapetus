package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;
import com.seattlesolvers.solverslib.util.Timing;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;

import java.util.concurrent.TimeUnit;

public class LaunchAbstract extends CommandBase {

    protected final Iapetus robot;
    protected final Launcher launcher;
    protected final Timing.Timer patience;

    public LaunchAbstract(Iapetus robot, double timeoutSeconds) {
        this.robot = robot;
        this.launcher = robot.launcher;
        this.patience = new Timing.Timer((long) (timeoutSeconds * 1000), TimeUnit.MILLISECONDS);

        // Claim the launcher, so teleop's trigger default command can't
        // overwrite our speed while we're shooting.
        addRequirements(launcher);
    }
}
