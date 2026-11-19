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
import org.firstinspires.ftc.teamcode.commands.DriveToPose;
import org.firstinspires.ftc.teamcode.commands.DriveToPoseByPoint;
import org.firstinspires.ftc.teamcode.commands.DriveTurnBy;
import org.firstinspires.ftc.teamcode.commands.IntakeManual;
import org.firstinspires.ftc.teamcode.commands.LaunchDynamic;
import org.firstinspires.ftc.teamcode.commands.LaunchManual;
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
 * ║    • TeleOp  — alliance + pose handed over by auto                        ║
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
    public boolean facingOpp;
    public enum Mode { TELEOP, AUTONOMOUS }
    public Mode mode;

    // Subsystems
    public PedroDrive drive;
    public Sensors sensors;
    public Intake intake;
    public Launcher launcher;

    public Pose startPose;

    /** TeleOp only: true if the pose came from auto, false if it's a default guess. */
    public boolean poseFromAuto = false;

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
        poseFromAuto = handoff.wasFound;
    }

    /**
     * TeleOp INIT: the drive team confirms the alliance. Field-centric drive
     * flips for blue, so getting this wrong inverts the sticks. Without an
     * auto handoff, also moves the guessed pose to that alliance's default.
     */
    public void setAlliance(boolean red) {
        if (red == isRed) return;
        isRed = red;
        if (!poseFromAuto) drive.setPose(PersistentPoseManager.defaultPose(red));
    }

    // ============================================================
    //                  AUTONOMOUS CONSTRUCTOR
    // ============================================================

    public Iapetus(LinearOpMode opMode, boolean isRed, boolean facingOpp, Pose startPose) {
        this.opMode = opMode;
        this.telemetry = opMode.telemetry;
        this.hardwareMap = opMode.hardwareMap;
        this.isRed = isRed;
        this.facingOpp = facingOpp;
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

        // Sensors registers LAST. The scheduler runs periodic() in this order,
        // and Sensors' periodic() is the telemetry flush: anything after it
        // prints a loop late, under the wrong heading.
        register(drive, intake, launcher, sensors);
    }

    // ============================================================
    //                    HIVES (BIOBUZZ)
    // ============================================================

    /**
     * Our alliance's Hive on the half of the field we're in: the audience Hive
     * below y = 72, the scoring Hive above (see Sensors.getOurTarget()). Null
     * if the camera hasn't seen it lately. Always check.
     */
    public TagSighting ourScoringHive() {
        return sensors.getOurTarget();
    }

    // ============================================================
    //                    TELEOP SETUP
    // ============================================================

    public void initTeleOp() {
        this.mode = Mode.TELEOP;

        // The default command: runs whenever nothing else claims the wheels.
        drive.setDefaultCommand(new Drive(this));

        intake.setDefaultCommand(new IntakeManual(this));
        launcher.setDefaultCommand(new LaunchManual(this));

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

        // X — turn to face our Hive on this half of the field (looked up on press).
        new GamepadButton(player1, GamepadKeys.Button.X)
                .whenPressed(new DriveFaceTarget(this, 1.5, true));

        // DPAD DOWN — 180° turn, for when a driver gets turned around.
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

        // The sticks and trigger live in default commands, dead zones included.
        // Anything pre-programmed requires the subsystem, interrupts these, and
        // hands control back when it finishes — same trick as Drive.

        // LEFT STICK Y — intake, either direction (reverse to clear a jam).


        // RIGHT TRIGGER — flywheel speed. Telemetry "Launcher ready" says when to feed.
        // X — feed on/off. Read inside LaunchManual, so it only works while
        //     the operator owns the launcher, never mid auto-shot.

        // A — auto-shot: spin up, feed, stop. The same command auto uses.
        new GamepadButton(player2, GamepadKeys.Button.A)
                .whenPressed(new LaunchDynamic(this, 5));

        // Y — panic stop for the launcher, auto-shot included (requiring the
        // launcher is what interrupts it). Stays stopped until the trigger is released.
        new GamepadButton(player2, GamepadKeys.Button.Y)
                .whenPressed(new InstantCommand(() -> launcher.stopAll(), launcher));
    }

    // ============================================================
    //                  AUTONOMOUS SETUP
    // ============================================================

    /**
     * Auto INIT: the robot is built before the drive team picks a start, so
     * hardware problems show up during INIT, not after the match starts.
     * Call once at START with the final choice.
     */
    public void setStart(boolean isRed, boolean isCorner, Pose startPose) {
        this.isRed = isRed;
        this.facingOpp = isCorner;
        this.startPose = startPose;
        drive.setPose(startPose);
    }

    /**
     * Schedules the routine for the chosen alliance + start position.
     *
     * Sequential = one after another. Parallel = all at once. Nest them freely;
     * that's how a whole autonomous becomes one schedulable object.
     */
    public void initAuto() {
        this.mode = Mode.AUTONOMOUS;
        drive.follower.update();  // know where we are before we move

        // Exactly ONE routine gets scheduled. Every routine requires the drive,
        // so scheduling two means the second silently cancels the first.
        //
        // ⚙ TEAM TO CONFIRM: the corner routines park on their own wall, but the
        // middle ones cross the field to the OTHER alliance's park pose, by way
        // of a loop through UP/DOWN. Check that against the game manual.
        if (isRed && facingOpp) {
            // Red Facing Opp
            new SequentialCommandGroup(
                    new DriveToPose(this, Constants.RED_SHOOT_AUDIENCE, 3),
                    // new LaunchDynamic(this, 5),
                    new DriveToPose(this, startPose, 3),
                    new DriveToPoseByPoint(this, Constants.RED_PARK, Constants.LEFT, 3)
            ).schedule();
        } else if (isRed) {
            // Red Facing Goal
            new SequentialCommandGroup(
                    new DriveToPose(this, Constants.RED_SHOOT_AUDIENCE, 3),
                    // new LaunchDynamic(this, 5),
                    new DriveToPoseByPoint(this, Constants.UP, Constants.CENTER, 3),
                    new DriveToPose(this, Constants.RED_PARK, 3)
            ).schedule();
        } else if (facingOpp) {
            // Blue Facing Opp
            new SequentialCommandGroup(
                    new DriveToPose(this, Constants.BLUE_SHOOT_SCORING, 3),
                    // new LaunchDynamic(this, 5),
                    new DriveToPose(this, startPose, 3),
                    new DriveToPoseByPoint(this, Constants.BLUE_PARK, Constants.RIGHT, 3)
            ).schedule();
        } else {
            // Blue Facing Goal
            new SequentialCommandGroup(
                    new DriveToPose(this, Constants.BLUE_SHOOT_AUDIENCE, 3),
                    // new LaunchDynamic(this, 5),
                    new DriveToPoseByPoint(this, Constants.DOWN, Constants.CENTER, 3),
                    new DriveToPose(this, Constants.RED_PARK, 3)
            ).schedule();
        }
    }
}