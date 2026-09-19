package org.firstinspires.ftc.teamcode.utils;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.seattlesolvers.solverslib.command.CommandOpMode;

import org.firstinspires.ftc.teamcode.Iapetus;

/**
 *  █████╗ ██╗   ██╗████████╗ ██████╗
 * ██╔══██╗██║   ██║╚══██╔══╝██╔═══██╗
 * ███████║██║   ██║   ██║   ██║   ██║
 * ██╔══██║██║   ██║   ██║   ██║   ██║
 * ██║  ██║╚██████╔╝   ██║   ╚██████╔╝
 * ╚═╝  ╚═╝ ╚═════╝    ╚═╝    ╚═════╝
 *            M c A U T T Y
 *
 * Thirty seconds, no driver, no second chances.
 *
 * Alliance and start position are picked on the Driver Station BEFORE start —
 * that's what initialize_loop() is for. Read the prompts on the DS screen.
 */
@Autonomous(name = "Auto McAutty", group = "Competition")
public class AutoMcAutty extends CommandOpMode {

    private Iapetus robot;

    // Chosen during init, before the match clock starts.
    private boolean isRed = true;
    private boolean isNearGoal = true;

    @Override
    public void initialize() {
        // Nothing built yet — we don't know the alliance until the drive team
        // tells us below.
        telemetry.addData("Alliance", "X = BLUE, B = RED");
        telemetry.addData("Position", "DPAD UP = near goal, DOWN = far");
        telemetry.update();
    }

    /** Runs repeatedly between INIT and START. Where the drive team answers. */
    @Override
    public void initialize_loop() {
        if (gamepad1.x) isRed = false;
        if (gamepad1.b) isRed = true;
        if (gamepad1.dpad_up) isNearGoal = true;
        if (gamepad1.dpad_down) isNearGoal = false;

        telemetry.addData("Alliance", isRed ? "RED" : "BLUE");
        telemetry.addData("Position", isNearGoal ? "NEAR GOAL" : "FAR");
        telemetry.addData("Ready?", "Press START when this looks right");
        telemetry.update();
    }

    @Override
    public void run() {
        // Build on the first loop after START, once the choices above are final.
        if (robot == null) {
            robot = new Iapetus(this, isRed, isNearGoal, startingPose());
            robot.initAuto();
        }
        super.run();
    }

    /**
     * Runs when autonomous stops, however it stops. Leaves a note for teleop
     * saying where we ended up and which alliance we're on — otherwise
     * field-centric drive starts from a guess. See PersistentPoseManager.
     */
    @Override
    public void end() {
        if (robot != null) {
            PersistentPoseManager.save(robot.drive.getPose(), robot.isRed);
        }
    }

    /**
     * ⚙ TUNE: measure these against the actual field before your first match.
     * Field is 144"x144", origin bottom-left, 0 rad points right (+X).
     */
    private Pose startingPose() {
        if (isRed) {
            return isNearGoal ? new Pose(115, 125, Math.toRadians(90))
                              : new Pose(80, 12, Math.toRadians(64));
        }
        return isNearGoal ? new Pose(15, 122, Math.toRadians(90))
                          : new Pose(50, 12, Math.toRadians(109));
    }
}
