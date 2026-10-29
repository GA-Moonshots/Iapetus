package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;

public class LaunchAbstract extends CommandBase {

    protected final Iapetus robot;
    protected final Launcher launcher;

    public LaunchAbstract(Iapetus robot) {
        this.robot = robot;
        this.launcher = robot.launcher;
    }
}
