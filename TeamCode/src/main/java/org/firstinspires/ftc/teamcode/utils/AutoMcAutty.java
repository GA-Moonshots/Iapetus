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
    private boolean started = false;

    // Chosen during init, before the match clock starts.
    private boolean isRed = true;
    private boolean isCorner = true;

    @Override
    public void initialize() {
        // Build now, not after START: a missing device or bad config fails
        // here, where you can see it, and the camera gets time to warm up.
        // The real alliance and start pose are applied at START.
        robot = new Iapetus(this, isRed, isCorner, startingPose());
    }

    /** Runs repeatedly between INIT and START. Where the drive team answers. */
    @Override
    public void initialize_loop() {
        // SET, never toggle. This runs hundreds of times a second, so a toggle
        // flips on every loop the button is held and lands wherever it lands.
        if (gamepad1.x) isRed = false;
        if (gamepad1.b) isRed = true;
        if (gamepad1.dpad_up)   isCorner = true;
        if (gamepad1.dpad_down) isCorner = false;

        telemetry.addData("Alliance", isRed ? "RED" : "BLUE");
        telemetry.addData("Position", isCorner ? "CORNER" : "MIDDLE");
        telemetry.addData("Controls", "X = BLUE, B = RED, DPAD UP = CORNER, DPAD DOWN = MIDDLE");
        telemetry.addData("Ready?", "Press START when this looks right");
        telemetry.update();
    }

    @Override
    public void run() {
        // First loop after START: the choices above are final now.
        if (!started) {
            robot.setStart(isRed, isCorner, startingPose());
            robot.initAuto();
            started = true;
        }
        super.run();
    }

    /**
     * Runs when autonomous stops, however it stops. Leaves a note for teleop
     * saying where we ended up and which alliance we're on — otherwise
     * field-centric drive starts from a guess. See PersistentPoseManager.
     * Skipped if auto never started: there's nothing true to hand over.
     */
    @Override
    public void end() {
        if (started) {
            PersistentPoseManager.save(robot.drive.getPose(), robot.isRed);
        }
    }

    /**
     * ⚙ TUNE: measure these against the actual field before your first match.
     * Field is 144"x144", origin bottom-left, 0 rad points right (+X).
     */
    private Pose startingPose() {
        if (isRed) {
            if (isCorner) return Constants.RED_START_POSE_CORNER;
            return Constants.RED_START_POSE_MIDDLE;
        }
        if (isCorner) return Constants.BLUE_START_POSE_CORNER;
        return Constants.BLUE_START_POSE_MIDDLE;
    }
}