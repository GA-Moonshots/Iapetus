package org.firstinspires.ftc.teamcode.utils;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.controllers.Controller;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Matrix;
import com.pedropathing.math.Vector2D;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.revhub.localizers.PinpointConfig;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                            CONSTANTS                                      ║
 * ║                                                                           ║
 * ║  Every magic string and tunable number the robot owns, in one file.       ║
 * ║  If you're about to type a hardware name in quotes somewhere else,        ║
 * ║  stop and put it here instead. Future you is counting on it.              ║
 * ║                                                                           ║
 * ║  Anything marked ⚙ TUNE is machine-specific and WILL be wrong on a        ║
 * ║  new robot. See docs/tuning.md before your first real drive.              ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Pedro Pathing 3 note: the three configs below (drivetrain, localizer,
 * Foresight) are exactly what AutoTune prints at the end of each tuner. Paste
 * over the matching block — but keep the hardware NAMES pointing at the
 * constants up top, so there's still only one place a name is typed.
 */
public class Constants {

    // ============================================================
    //                    HARDWARE MAP NAMES
    //     Must match the Driver Station config EXACTLY, including case.
    //     A typo here is a NullPointerException three seconds into a match.
    // ============================================================

    public static final String LEFT_FRONT_NAME  = "leftFront";
    public static final String RIGHT_FRONT_NAME = "rightFront";
    public static final String LEFT_BACK_NAME   = "leftBack";
    public static final String RIGHT_BACK_NAME  = "rightBack";

    public static final String IMU_NAME = "imu";

    /**
     * The odometry computer's name in the Driver Station config.
     *
     * "Configured under a different name" is the most common reason a
     * correctly-wired Pinpoint reports nothing. Run the Pinpoint Doctor
     * OpMode — it looks under this name AND under any other, and tells you if
     * they disagree.
     */
    public static final String PINPOINT_NAME = "pinpoint";

    // ============================================================
    //                    MOTOR DIRECTIONS
    //  ⚙ TUNE: AutoTune's Mecanum Tuner spins each wheel and tells you.
    //  Or prop the robot on a block, drive forward, and flip whichever lied.
    // ============================================================

    public static final DcMotorSimple.Direction LEFT_FRONT_DIRECTION  = DcMotorSimple.Direction.REVERSE;
    public static final DcMotorSimple.Direction LEFT_BACK_DIRECTION   = DcMotorSimple.Direction.FORWARD;
    public static final DcMotorSimple.Direction RIGHT_FRONT_DIRECTION = DcMotorSimple.Direction.REVERSE;
    public static final DcMotorSimple.Direction RIGHT_BACK_DIRECTION  = DcMotorSimple.Direction.FORWARD;

    /**
     * Driver lets go of the sticks: true = stop dead, false = coast like it's
     * on ice. Keep true. (Pedro coasts on purpose while following a path —
     * this only covers teleop.)
     */
    public static final boolean BRAKE_WHEN_DRIVER_LETS_GO = true;

    // ============================================================
    //                    DRIVE FEEL
    // ============================================================

    public static final double MIN_DRIVE_SPEED = 0.2;
    public static final double MAX_DRIVE_SPEED = 1.0;

    // The speed cap itself, slow mode, stick deadzone, and pose tolerance live
    // in Tunables — they're read every loop and worth twiddling live. These two
    // are the bounds a dashboard edit can't escape. One number, one home.

    /** Field-centric: push the stick toward the far wall, robot goes there,
     *  regardless of which way it's facing. Turn this off only if a driver
     *  specifically asks — everyone thinks they want robot-centric until
     *  the robot is pointed at them. */
    public static final boolean DEFAULT_FIELD_CENTRIC = true;

    // ============================================================
    //                    FIELD GEOMETRY
    //  Field is 144" x 144", origin at bottom-left. 0 rad points RIGHT.
    //  ⚙ EVERY SEASON: replace these with the game's scoring coordinates.
    // ============================================================

    public static final double BLUE_TARGET_X = 12;
    public static final double BLUE_TARGET_Y = 124;
    public static final double RED_TARGET_X  = 132;
    public static final double RED_TARGET_Y  = 124;

    // ============================================================
    //                    PEDRO PATHING 3
    // ============================================================
    //
    //  ┌───────────────────────────────────────────────────────────────────┐
    //  │  THE TUNING SEQUENCE — AutoTune, in a browser, in this order.     │
    //  │  Robot Wi-Fi → http://192.168.43.1:10158   (see utils/Tuning)     │
    //  │                                                                   │
    //  │  1. Mecanum Tuner    → drivetrainConfig          ~5 min           │
    //  │  2. Pinpoint Tuner   → localizerConfig           ~10 min          │
    //  │        then run the four push/rotate checks below BY HAND         │
    //  │  3. Foresight Tuner  → foresightConfig           ~30–45 min       │
    //  │        needs 1 and 2 pasted in and deployed first                 │
    //  │  4. Tests            → confirms all of the above                  │
    //  │                                                                   │
    //  │  Full walkthrough: docs/tuning.md                                 │
    //  │  Official docs:    https://pedropathing.com/docs/pathing/tuning   │
    //  └───────────────────────────────────────────────────────────────────┘

    // ============================================================
    //                    MECANUM DRIVETRAIN
    //  We run mecanum every single year. This section is the reason
    //  Artemis exists — don't rebuild it, tune it.
    // ============================================================

    public static MecanumConfig drivetrainConfig = new MecanumConfig(c -> {
        c.frontLeftName.set(LEFT_FRONT_NAME);
        c.backLeftName.set(LEFT_BACK_NAME);
        c.frontRightName.set(RIGHT_FRONT_NAME);
        c.backRightName.set(RIGHT_BACK_NAME);

        c.frontLeftDirection.set(LEFT_FRONT_DIRECTION);
        c.backLeftDirection.set(LEFT_BACK_DIRECTION);
        c.frontRightDirection.set(RIGHT_FRONT_DIRECTION);
        c.backRightDirection.set(RIGHT_BACK_DIRECTION);

        c.manualBrakeMode.set(BRAKE_WHEN_DRIVER_LETS_GO);
        /** Skip motor writes smaller than this. Saves loop time; 0.01 is fine. */
        c.powerThreshold.set(0.01);
    });

    // ============================================================
    //                    PINPOINT LOCALIZATION
    // ============================================================
    //
    //  goBILDA Pinpoint: two dead-wheel pods + IMU, fused on its own chip
    //  so the Control Hub doesn't have to think about it.
    //
    //  ┌───────────────────────────────────────────────────────────────────┐
    //  │  VERIFY BEFORE YOU TUNE ANYTHING ELSE. Four tests, by hand:       │
    //  │                                                                   │
    //  │  1. PUSH    — shove the robot forward 12". Does telemetry say     │
    //  │               it moved 12"?                                       │
    //  │  2. ROTATE  — turn it 90° by hand. Heading change ≈ 90°?          │
    //  │  3. STRAFE  — push it LEFT. Does Y go UP?                         │
    //  │  4. SPIN    — spin it in circles. Does it come back to where      │
    //  │               it started, or wander off?                          │
    //  │                                                                   │
    //  │  Any test fails → fix it HERE, in the direction/offset settings.  │
    //  │  Tuning Foresight on top of bad localization is tuning noise.     │
    //  └───────────────────────────────────────────────────────────────────┘

    public static PinpointConfig localizerConfig = new PinpointConfig(c -> {
        c.name.set(PINPOINT_NAME);
        c.podType.set(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_SWINGARM_POD);

        /** ⚙ TUNE: Pinpoint Tuner measures these. xPodOffset is how far LEFT of
         *  centre the forward pod sits; yPodOffset is how far FORWARD the strafe
         *  pod sits. (Pedro 2 called these forwardPodY and strafePodX.) */
        c.xPodOffset.set(-3.654345114400068);
        c.yPodOffset.set(4.175518666665385);
        c.offsetUnits.set(DistanceUnit.INCH);
        c.globalDistanceUnit.set(DistanceUnit.INCH);

        /** ⚙ TUNE: pushing forward must INCREASE X; pushing left must INCREASE Y.
         *  Wrong? Flip it here, not the wiring. */
        c.xPodDirection.set(GoBildaPinpointDriver.EncoderDirection.REVERSED);
        c.yPodDirection.set(GoBildaPinpointDriver.EncoderDirection.REVERSED);
    });

    // ============================================================
    //                    FORESIGHT (path following)
    // ============================================================

    public static ForesightConfig foresightConfig = new ForesightConfig(
            c -> {
                Controller primaryTranslationalForward = Controller.proportional(1.3679162777331282);
                Controller secondaryTranslationalForward = Controller.proportional(0.5054085132262731);
                Controller primaryTranslationalLateral = Controller.proportional(0.4956660149135508);
                Controller secondaryTranslationalLateral = Controller.proportional(0.1831353480707122);

                c.forwardTranslational.set(Controller.piecewise(secondaryTranslationalForward).put(2.5, primaryTranslationalForward));
                c.strafeTranslational.set(Controller.piecewise(secondaryTranslationalLateral).put(2.5, primaryTranslationalLateral));

                c.coast.set(Controller.proportionalFeedforward(0.015020759363507967));
                c.brake.set(Controller.proportionalFeedforward(0.012767645458981772));

                c.headingFeedback.set(Controller.proportional(2.0329097693560123));
                c.headingBrakeCoefficients.set(Vector2D.cartesian(0.04920658954550176, 0.007028128200379804));

                c.linearBrakeCoefficients.set(Matrix.diag(0.005705998681946106, 0.01432310470727969));
                c.quadraticBrakeCoefficients.set(Matrix.diag(0.004523530787188707, 0.0032284093429526773));

                c.maxAchievableForwardVelocity.set(69.54036732293439);
                c.maxAchievableStrafeVelocity.set(58.931081559691414);
                c.naturalForwardDeceleration.set(67.53830889197948);
                c.naturalStrafeDeceleration.set(75.77981882324022);

                // ⚠ KEEP THESE WHEN YOU PASTE. AutoTune doesn't generate them, so
                // pasting its block over this one silently deletes them (it has,
                // twice). They decide when a path counts as "done".
                c.parametricTConstraint.set(0.01);             // follow until 99% complete...
                c.velocityConstraint.set(0.1);                 // ...AND slower than 0.1 in/s
                c.translationalConstraint.set(0.5);            // ...AND within 0.5"
                c.headingConstraint.set(Math.toRadians(1.0));  // ...AND within 1°
                c.timeoutConstraint.set(200.0);                // can't settle? give up after 200ms
            }
    );

    // ============================================================
    //                    VISION / LIMELIGHT
    // ============================================================

    public static final String LIMELIGHT_NAME = "limelight";
    public static final int LIMELIGHT_POLL_HZ = 30;
    public static final int LIMELIGHT_APRILTAG_PIPELINE = 0;

    /**
     * ⚙ TUNE — where the camera sits on the robot.
     *
     * Measured from the robot's CENTRE at floor level, named exactly like the
     * fields on the Limelight's robot-space settings, so copying them across
     * is one-to-one. The camera does the maths with ITS copy; this is our
     * version-controlled record, because a reflashed Limelight forgets.
     *
     * Wrong here (or on the camera) = every tag distance is off by the same
     * amount. CameraCalibration measures it. The Limelight's settings are in
     * metres: divide by 39.37.
     */
    public static final double CAMERA_FORWARD_INCHES = 6.0;
    public static final double CAMERA_RIGHT_INCHES = 0.0;
    public static final double CAMERA_UP_INCHES = 12.0;
    public static final double CAMERA_YAW_DEGREES = 0.0;    // 0 = facing forward
    public static final double CAMERA_PITCH_DEGREES = 0.0;  // + = tilted up
    public static final double CAMERA_ROLL_DEGREES = 0.0;

    /** Older than this and the reading describes where we used to be. */
    public static final long VISION_MAX_STALENESS_MS = 200;

    /** Beyond this, angular error turns into large position error. */
    public static final double VISION_MAX_TAG_DISTANCE_INCHES = 96.0;

    /**
     * Largest disagreement with odometry we'll accept as a correction.
     * Bigger than this is far more likely a misread than a teleporting robot.
     * Raise it if real corrections are being declined; lower it if bad reads
     * are getting through.
     */
    public static final double VISION_MAX_JUMP_INCHES = 24.0;

    // ============================================================
    //                    BIOBUZZ HIVES
    //  The SDK's cluster names, so Sensors.target() can find each Hive.
    //  Tag ids and positions come from the SDK; only the names live here.
    //  See docs/biobuzz.md.
    // ============================================================

    public static final String RED_SCORING_HIVE    = "RED SCORING";     // tags 30-33
    public static final String RED_AUDIENCE_HIVE   = "RED AUDIENCE";    // tags 34-37
    public static final String BLUE_AUDIENCE_HIVE  = "BLUE AUDIENCE";   // tags 38-41
    public static final String BLUE_SCORING_HIVE   = "BLUE SCORING";    // tags 42-45

    // ============================================================
    //                    IMU ORIENTATION
    //  ⚙ TUNE: which way is the Control Hub actually bolted on?
    // ============================================================

    public static final RevHubOrientationOnRobot.LogoFacingDirection IMU_LOGO_DIRECTION =
            RevHubOrientationOnRobot.LogoFacingDirection.UP;
    public static final RevHubOrientationOnRobot.UsbFacingDirection IMU_USB_DIRECTION =
            RevHubOrientationOnRobot.UsbFacingDirection.FORWARD;

    // ============================================================
    //    FOLLOWER FACTORY — where all of the above gets assembled
    // ============================================================

    public static Follower createFollower(HardwareMap hardwareMap) {
        return new Follower(
                new PinpointLocalizer(hardwareMap, localizerConfig),
                new Mecanum(hardwareMap, drivetrainConfig),
                new Foresight(foresightConfig)
        );
    }
}
