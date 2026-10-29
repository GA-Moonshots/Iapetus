package org.firstinspires.ftc.teamcode.commands;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;
import org.firstinspires.ftc.teamcode.utils.Constants;

public class LaunchDynamic extends LaunchAbstract {

    public LaunchDynamic(Iapetus robot) {
        super(robot);
    }

    public void execute() {

    }

    private String getTarget() {
        if (robot.isRed) {
            if (robot.drive.getPose().y() < 72) return Constants.RED_AUDIENCE_HIVE;
            return Constants.RED_SCORING_HIVE;
        }
        if (robot.drive.getPose().y() < 72) return Constants.BLUE_AUDIENCE_HIVE;
        return Constants.BLUE_SCORING_HIVE;
    }

    private void calculateLaunchPower() {
        double xDistance = robot.sensors.target(getTarget()).range();
        double yDistance = robot.sensors.target(getTarget()).up;

    }
}
