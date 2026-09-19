package org.firstinspires.ftc.teamcode;

import com.pedropathing.math.Pose;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.Robot;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.button.GamepadButton;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.commands.Drive;
import org.firstinspires.ftc.teamcode.commands.DriveFaceTarget;
import org.firstinspires.ftc.teamcode.commands.DriveFwdByDist;
import org.firstinspires.ftc.teamcode.commands.DriveTurnBy;
import org.firstinspires.ftc.teamcode.commands.DriveTurnTo;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;
import org.firstinspires.ftc.teamcode.subsystems.PedroDrive;
import org.firstinspires.ftc.teamcode.subsystems.Sensors;
import org.firstinspires.ftc.teamcode.utils.Constants;
import org.firstinspires.ftc.teamcode.utils.PersistentPoseManager;
import org.firstinspires.ftc.teamcode.utils.TagSighting;
import org.firstinspires.ftc.teamcode.utils.Tunables;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                              IAPETUS                                      ║
 * ║                                                                           ║
 * ║  The one object that owns everything. Subsystems live here, button        ║
 * ║  bindings live here, the autonomous plan lives here. OpModes do           ║
 * ║  nothing but build one of these and get out of the way.                   ║
 * ║                                                                           ║
 * ║  Two constructors:                                                        ║
 * ║    • TeleOp  — doesn't care about alliance, just drives                   ║
 * ║    • Auto    — needs to know alliance + start position                    ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Our 2026-27 BIOBUZZ robot, built on Artemis. Iapetus, Saturn's two-faced
 * moon: one side bright, one side dark. Much like teleop and auto.
 */
public class Iapetus extends Robot {

    // Core references
    public LinearOpMode opMode;
    public Telemetry telemetry;
    public HardwareMap hardwareMap;
    public GamepadEx player1;
    public GamepadEx player2;

    // Match configuration
    public boolean isRed;
    public boolean isNearGoal;

    // Subsystems
    public PedroDrive drive;
    public Sensors sensors;
    public Intake intake;
    public Launcher launcher;

    public Pose startPose;

    // ============================================================
    //                    TELEOP CONSTRUCTOR
    // ============================================================

    /**
     * TeleOp: picks up wherever autonomous left the robot, so field-centric
     * drive starts from the truth instead of a guess. If auto didn't run (or
     * ran too long ago to trust), falls back to defaults — see
     * PersistentPoseManager.
     */
    public Iapetus(LinearOpMode opMode) {
        this(opMode, PersistentPoseManager.load());
    }

    /** Reads the handoff exactly once, then hands it to the real constructor. */
    private Iapetus(LinearOpMode opMode, PersistentPoseManager.Handoff handoff) {
        this(opMode, handoff.isRed, true, handoff.pose);
        sensors.addTelemetry("Pose handoff",
                handoff.wasFound ? "loaded from autonomous" : "NONE — using defaults");
    }

    // ============================================================
    //                  AUTONOMOUS CONSTRUCTOR
    // ============================================================

    public Iapetus(LinearOpMode opMode, boolean isRed, boolean isNearGoal, Pose startPose) {
        this.opMode = opMode;
        this.telemetry = opMode.telemetry;
        this.hardwareMap = opMode.hardwareMap;
        this.isRed = isRed;
        this.isNearGoal = isNearGoal;
        this.startPose = startPose;

        this.player1 = new GamepadEx(opMode.gamepad1);
        this.player2 = new GamepadEx(opMode.gamepad2);

        // One bulk read per loop instead of a separate USB round-trip for every
        // encoder and sensor. Costs one line, buys milliseconds every loop —
        // and Pedro's localization accuracy is downstream of loop time.
        // MANUAL means "read once per loop, when I ask" and is what you want.
        setBulkReading(opMode.hardwareMap, LynxModule.BulkCachingMode.MANUAL);

        Tunables.refresh();   // pull any values the dashboard is holding

        // Sensors first — everything else wants to log to it.
        sensors = new Sensors(this);
        drive = new PedroDrive(this, startPose);
        intake = new Intake(this);
        launcher = new Launcher(this);

        register(drive, sensors, intake, launcher);
    }

    // ============================================================
    //                    HIVES (BIOBUZZ)
    // ============================================================

    /**
     * Our alliance's scoring Hive, if the camera has seen it lately. Null
     * otherwise — no camera, not in view, or seen too long ago. Always check.
     */
    public TagSighting ourScoringHive() {
        return sensors.target(isRed ? Constants.RED_SCORING_HIVE : Constants.BLUE_SCORING_HIVE);
    }

    // ============================================================
    //                    TELEOP SETUP
    // ============================================================

    public void initTeleOp() {
        // The default command: runs whenever nothing else claims the wheels.
        drive.setDefaultCommand(new Drive(this));

        /*
         ██████╗ ██╗      █████╗ ██╗   ██╗███████╗██████╗      ██╗
         ██╔══██╗██║     ██╔══██╗╚██╗ ██╔╝██╔════╝██╔══██╗    ███║
         ██████╔╝██║     ███████║ ╚████╔╝ █████╗  ██████╔╝    ╚██║
         ██╔═══╝ ██║     ██╔══██║  ╚██╔╝  ██╔══╝  ██╔══██╗     ██║
         ██║     ███████╗██║  ██║   ██║   ███████╗██║  ██║     ██║
         ╚═╝     ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚══════╝╚═╝  ╚═╝     ╚═╝

            DRIVER — moves the robot. Doesn't touch game pieces.
        */

        // A — "forward is THIS way now." Fixes a drifted field-centric heading.
        new GamepadButton(player1, GamepadKeys.Button.A)
                .whenPressed(new InstantCommand(() -> drive.resetHeading()));

        // B — toggle field-centric / robot-centric
        new GamepadButton(player1, GamepadKeys.Button.B)
                .whenPressed(new InstantCommand(() -> drive.toggleFieldCentric()));

        // X — nudge forward 12". Example of scheduling a real Command from a button.
        new GamepadButton(player1, GamepadKeys.Button.X)
                .whenPressed(new DriveFwdByDist(this, 12, 3));

        // Y — turn by 90 degrees
        new GamepadButton(player1, GamepadKeys.Button.Y)
                .whenPressed(new DriveTurnBy(this, 90, 3));

        // Bumper-free 180: useful when a driver gets turned around.
        new GamepadButton(player1, GamepadKeys.Button.DPAD_DOWN)
                .whenPressed(new DriveTurnBy(this, 180, true, 3));

        // DPAD UP — panic button. Drops any path and hands the wheels back.
        // Requiring `drive` is the trick: it interrupts whatever command owns
        // the wheels, and the Drive default command picks them straight back up.
        new GamepadButton(player1, GamepadKeys.Button.DPAD_UP)
                .whenPressed(new InstantCommand(() -> drive.stop(), drive));

        // LEFT BUMPER — face the nearest thing the camera is tracking.
        new GamepadButton(player1, GamepadKeys.Button.LEFT_BUMPER)
                .whenPressed(new DriveFaceTarget(this, 2));

        // Right bumper is slow mode — read directly in Drive.execute(), not bound here.

        /*
         ██████╗ ██╗      █████╗ ██╗   ██╗███████╗██████╗     ██████╗
         ██╔══██╗██║     ██╔══██╗╚██╗ ██╔╝██╔════╝██╔══██╗    ╚════██╗
         ██████╔╝██║     ███████║ ╚████╔╝ █████╗  ██████╔╝     █████╔╝
         ██╔═══╝ ██║     ██╔══██║  ╚██╔╝  ██╔══╝  ██╔══██╗    ██╔═══╝
         ██║     ███████╗██║  ██║   ██║   ███████╗██║  ██║    ███████╗
         ╚═╝     ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚══════╝╚═╝  ╚═╝    ╚══════╝

            OPERATOR — runs the mechanisms. Never drives.
        */

        // A — instant action, no Command needed. Servo flips are immediate.
        new GamepadButton(player2, GamepadKeys.Button.A);

        // Y — a real Command, because this one takes time to finish.
        new GamepadButton(player2, GamepadKeys.Button.Y);

        // Bind this year's intake / launcher / arm the same way, then delete
        // the two grabber bindings above.
    }

    // ============================================================
    //                  AUTONOMOUS SETUP
    // ============================================================

    /**
     * This year's autonomous routine. Replace the placeholder with real paths.
     *
     * Sequential = one after another. Parallel = all at once. Nest them freely;
     * that's how a whole autonomous becomes one schedulable object.
     */
    public void initAuto() {
        drive.follower.update();  // know where we are before we move

        new SequentialCommandGroup(
                new DriveFwdByDist(this, 24, 5)
                // ...then the rest of this year's plan.
        ).schedule();
    }
}
