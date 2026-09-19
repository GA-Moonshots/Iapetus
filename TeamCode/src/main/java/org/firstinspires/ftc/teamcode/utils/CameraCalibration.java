package org.firstinspires.ftc.teamcode.utils;

import com.pedropathing.math.Pose;
import com.pedropathing.utils.Angle;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;

import org.firstinspires.ftc.teamcode.Iapetus;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                       CAMERA CALIBRATION                                  ║
 * ║                                                                           ║
 * ║  Is the camera telling the truth?                                         ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * TAG CHECK (every season, about five minutes):
 *
 *   1. Tape a tag upright at a measured spot: TAG_FORWARD inches in front of
 *      the robot's centre, TAG_LEFT inches to its left. Edit both live from
 *      the Panels dashboard.
 *   2. Run this OpMode and read the TAG ERROR line.
 *
 *   Small (< ~1")               → calibrated. Go aim something.
 *   Same offset everywhere      → camera mount position is off. Fix it on the
 *                                 Limelight AND in Constants.CAMERA_*.
 *   Error grows with distance   → wrong tag size in the Limelight pipeline, or
 *                                 camera pitch is off.
 *   "left" has the wrong sign   → fix FieldMap.limelightTargetToRobot(), and
 *                                 nowhere else.
 *   No reading at all           → docs/vision.md, "camera setup".
 *
 * FIELD CHECK (only in a season with tags that stay put):
 *   Park at KNOWN_X / KNOWN_Y / KNOWN_HEADING_DEG and compare the camera's
 *   field pose. Skipped automatically when there are no fixed tags.
 *
 * Never moves the robot, never corrects the pose. Safe to run any time.
 */
@TeleOp(name = "Camera Calibration", group = "Tuning")
public class CameraCalibration extends CommandOpMode {

    /** Where you put the tag, from the robot's centre. Measure, don't guess. */
    public static double TAG_FORWARD = 24.0;
    public static double TAG_LEFT = 0.0;

    /** Where the robot ACTUALLY is, Pedro frame. Only for the field check. */
    public static double KNOWN_X = 72.0;
    public static double KNOWN_Y = 72.0;
    public static double KNOWN_HEADING_DEG = 0.0;

    private Iapetus robot;

    @Override
    public void initialize() {
        // Look, don't touch. Set before the robot is built, so Sensors starts
        // up in watch mode.
        Tunables.TAG_LOCALIZATION = !FieldMap.localizationTags().isEmpty();
        Tunables.VISION_CORRECTIONS_ENABLED = false;
        robot = new Iapetus(this);

        telemetry.addLine("Put a tag at TAG_FORWARD / TAG_LEFT, then press START.");
        telemetry.update();
    }

    @Override
    public void run() {
        super.run();

        Pose truth = new Pose(KNOWN_X, KNOWN_Y, Math.toRadians(KNOWN_HEADING_DEG));
        robot.drive.setPose(truth);   // we know where we are; assert it

        if (!robot.sensors.hasCamera()) {
            robot.sensors.addTelemetry("Calibration", "camera OFFLINE — nothing to check");
            return;
        }
        tagCheck();
        fieldCheck();
    }

    private void tagCheck() {
        robot.sensors.addTelemetry("═══ Tag check ═══", "");
        TagSighting tag = robot.sensors.nearestTag();
        if (tag == null) {
            robot.sensors.addTelemetry("Tag", "none — see the Vision status line");
            return;
        }

        double dF = tag.forward - TAG_FORWARD;
        double dL = tag.left - TAG_LEFT;
        robot.sensors.addTelemetry("Tag " + tag.id, "fwd %.1f\"  left %.1f\"  up %.1f\"",
                tag.forward, tag.left, tag.up);
        robot.sensors.addTelemetry("TAG ERROR", "dFwd %+.1f\"  dLeft %+.1f\"", dF, dL);

        // Cheap hints for the two mistakes that are easy to miss by eye.
        if (Math.abs(TAG_LEFT) > 4 && Math.abs(tag.left + TAG_LEFT) < Math.abs(dL) / 2) {
            robot.sensors.addTelemetry("HINT", "left/right mirrored — see FieldMap.limelightTargetToRobot");
        } else if (TAG_FORWARD > 12 && Math.abs(dF / TAG_FORWARD) > 0.1) {
            robot.sensors.addTelemetry("HINT", "distance off by %.0f%% — check the pipeline's tag size",
                    100 * dF / TAG_FORWARD);
        }
    }

    private void fieldCheck() {
        Pose seen = robot.sensors.lastAcceptedPose();
        if (!Tunables.TAG_LOCALIZATION) {
            robot.sensors.addTelemetry("Field check", "skipped — this game has no fixed tags");
            return;
        }
        robot.sensors.addTelemetry("═══ Field check ═══", "");
        if (seen == null) {
            robot.sensors.addTelemetry("Camera", "no field pose yet — see Localization line");
            return;
        }

        double dx = seen.x() - KNOWN_X;
        double dy = seen.y() - KNOWN_Y;
        // Pedro stores 0..360, humans type -180..180. Compare the difference, wrapped.
        double dh = Math.toDegrees(Angle.normalizeSigned(
                seen.heading() - Math.toRadians(KNOWN_HEADING_DEG)));

        robot.sensors.addTelemetry("Camera says", "X:%.1f Y:%.1f H:%.0f°",
                seen.x(), seen.y(), Math.toDegrees(Angle.normalizeSigned(seen.heading())));
        robot.sensors.addTelemetry("FIELD ERROR", "dX:%+.1f\" dY:%+.1f\" dH:%+.0f°", dx, dy, dh);
    }
}
