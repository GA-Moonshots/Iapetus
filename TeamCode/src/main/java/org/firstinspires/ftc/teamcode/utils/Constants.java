package org.firstinspires.ftc.teamcode.utils;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.controllers.Controller;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Matrix;
import com.pedropathing.math.Pose;
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
    //     A typo here throws IllegalArgumentException, naming the device, the
    //     moment the robot is built. Not a NullPointerException mid-match.
    // ============================================================

    // Drivetrain motors
    public static final String LEFT_FRONT_NAME  = "leftFront";
    public static final String RIGHT_FRONT_NAME = "rightFront";
    public static final String LEFT_BACK_NAME   = "leftBack";
    public static final String RIGHT_BACK_NAME  = "rightBack";

    // IMU
    public static final String IMU_NAME = "imu";

    // Intake/Launcher
    public static final String INTAKE_MOTOR_NAME = "intake";
    public static final String LAUNCHER_MOTOR_FRONT_NAME = "launcherFront";
    public static final String LAUNCHER_MOTOR_BACK_NAME = "launcherBack";
    public static final String LAUNCHER_FEED_UPPER_LEFT_NAME = "feedUpperLeft";
    public static final String LAUNCHER_FEED_UPPER_RIGHT_NAME = "feedUpperRight";
    public static final String LAUNCHER_FEED_LOWER_LEFT_NAME = "feedLowerLeft";
    public static final String LAUNCHER_FEED_LOWER_RIGHT_NAME = "feedLowerRight";

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
    public static final DcMotorSimple.Direction RIGHT_BACK_DIRECTION  = DcMotorSimple.Direction.REVERSE;

    public static final DcMotorSimple.Direction INTAKE_MOTOR_DIRECTION = DcMotorSimple.Direction.FORWARD;
    public static final DcMotorSimple.Direction LAUNCHER_MOTOR_FRONT_DIRECTION = DcMotorSimple.Direction.FORWARD;
    public static final DcMotorSimple.Direction LAUNCHER_MOTOR_BACK_DIRECTION = DcMotorSimple.Direction.REVERSE;
    public static final DcMotorSimple.Direction LAUNCHER_FEED_SERVO_DIRECTION = DcMotorSimple.Direction.FORWARD;

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
        c.xPodOffset.set(7.874);
        c.yPodOffset.set(6.496);
        c.offsetUnits.set(DistanceUnit.INCH);
        c.globalDistanceUnit.set(DistanceUnit.INCH);

        /** ⚙ TUNE: pushing forward must INCREASE X; pushing left must INCREASE Y.
         *  Wrong? Flip it here, not the wiring. */
        c.xPodDirection.set(GoBildaPinpointDriver.EncoderDirection.FORWARD);
        c.yPodDirection.set(GoBildaPinpointDriver.EncoderDirection.REVERSED);
    });

    // ============================================================
    //                    FORESIGHT (path following)
    // ============================================================

    public static ForesightConfig foresightConfig = new ForesightConfig(
            c -> {
                Controller primaryTranslationalForward = Controller.proportional(0.30504057557256875);
                Controller secondaryTranslationalForward = Controller.proportional(0.11270434183976881);
                Controller primaryTranslationalLateral = Controller.proportional(0.4804188567554112);
                Controller secondaryTranslationalLateral = Controller.proportional(0.177501930542849);

                c.forwardTranslational.set(Controller.piecewise(secondaryTranslationalForward).put(2.5, primaryTranslationalForward));
                c.strafeTranslational.set(Controller.piecewise(secondaryTranslationalLateral).put(2.5, primaryTranslationalLateral));

                c.coast.set(Controller.proportionalFeedforward(0.020401258408975824));
                c.brake.set(Controller.proportionalFeedforward(0.01734106964762945));

                c.headingFeedback.set(Controller.proportional(5.358187632846964));
                c.headingBrakeCoefficients.set(Vector2D.cartesian(0.049590099759691975, 0.008320282542052358));

                c.linearBrakeCoefficients.set(Matrix.diag(0.03425663979634154, 0.07076832684979194));
                c.quadraticBrakeCoefficients.set(Matrix.diag(0.002954712742378578, 0.0015502133155862383));

                c.maxAchievableForwardVelocity.set(58.18871074373199);
                c.maxAchievableStrafeVelocity.set(43.96786510198571);
                c.naturalForwardDeceleration.set(89.55015768431342);
                c.naturalStrafeDeceleration.set(117.00425023169247);
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
    //                    IMPORTANT POSITIONS
    //  Poses of important positions where we want our robot to be
    //  or to avoid.
    // ============================================================

    public static final double ROBOT_HALF_LENGTH = 8.5;

    public static final Pose RED_START_FACING_OPP = new Pose(ROBOT_HALF_LENGTH, 24, Math.toRadians(0));
    public static final Pose RED_START_FACING_GOAL = new Pose(48, ROBOT_HALF_LENGTH, Math.toRadians(90));
    public static final Pose BLUE_START_FACING_OPP = new Pose(144 - ROBOT_HALF_LENGTH, 120, Math.toRadians(180));
    public static final Pose BLUE_START_FACING_GOAL = new Pose(96, 144 - ROBOT_HALF_LENGTH, Math.toRadians(270));

    public static final Pose RED_PARK_CLOSE = new Pose(ROBOT_HALF_LENGTH, 96, Math.toRadians(0));
    public static final Pose RED_PARK_FAR = new Pose(ROBOT_HALF_LENGTH, 120, Math.toRadians(0));
    public static final Pose BLUE_PARK_CLOSE = new Pose(144 - ROBOT_HALF_LENGTH, 48, Math.toRadians(180));
    public static final Pose BLUE_PARK_FAR = new Pose(144 - ROBOT_HALF_LENGTH, 24, Math.toRadians(180));

    public static final Pose CENTER = new Pose(72, 72);
    public static final Pose UP = new Pose(72, 114);
    public static final Pose LEFT = new Pose(30, 72);
    public static final Pose DOWN = new Pose(72, 30);
    public static final Pose RIGHT = new Pose(114, 72);

    public static final Pose RED_ALLEYWAY = new Pose(60, 72);
    public static final Pose BLUE_ALLEYWAY = new Pose(84, 72);
    public static final Pose RED_ALLEYWAY_AUDIENCE = new Pose(60, 48 - ROBOT_HALF_LENGTH);
    public static final Pose RED_ALLEYWAY_SCORING = new Pose(60, 96 + ROBOT_HALF_LENGTH);
    public static final Pose BLUE_ALLEYWAY_AUDIENCE = new Pose(84, 48 - ROBOT_HALF_LENGTH);
    public static final Pose BLUE_ALLEYWAY_SCORING = new Pose(84, 96 + ROBOT_HALF_LENGTH);

    public static final Pose RED_SHOOT_SCORING = new Pose(60, 120, Math.toRadians(270));
    public static final Pose RED_SHOOT_AUDIENCE = new Pose(60, 24, Math.toRadians(90));
    public static final Pose BLUE_SHOOT_AUDIENCE = new Pose(84, 24, Math.toRadians(90));
    public static final Pose BLUE_SHOOT_SCORING = new Pose(84, 120, Math.toRadians(270));


    // ============================================================
    //                   LAUNCHER CONSTANTS
    // Needed for math calculations for the launcher. May be
    // measurements from the robot or physical constants. All
    // lengths are in METERS.
    // ============================================================

    public static final double GRAVITY = 9.81;   // m/s²
    public static final double SHOOTING_ANGLE = Math.toRadians(75);
    public static final double HIVE_OPENING_ELEVATION = DistanceUnit.INCH.toMeters(55.0);
    /**
     * ⚙ MEASURE: floor to where the ball leaves the launcher, in metres.
     * 27 cm — the old value was 10", so the 27 was centimetres.
     */
    public static final double LAUNCHER_ELEVATION = 0.27;

    /**
     * Teleop trigger power → wheel surface speed: rad/s per unit of power, the
     * unit Launcher uses. Measured: speed = 30.19 * power - 1.64.
     */
    public static final double LAUNCH_SPEED_PER_POWER = 30;

    public static final double FRONT_WHEEL_RADIUS = 0.05;
    public static final double BACK_WHEEL_RADIUS = 0.05;

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