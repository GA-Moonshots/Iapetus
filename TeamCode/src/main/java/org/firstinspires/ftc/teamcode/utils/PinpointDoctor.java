package org.firstinspires.ftc.teamcode.utils;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.util.Set;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                        PINPOINT DOCTOR                                    ║
 * ║                                                                           ║
 * ║  Is the odometry computer alive, and is it seeing the wheels turn?        ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * WHY THIS EXISTS
 *
 * When the robot won't localize, the question is always "which layer is
 * broken?" — the sensor, the Pedro config, or our code. This OpMode is a plain
 * LinearOpMode that talks to the Pinpoint DIRECTLY. No Pedro, no SolversLib,
 * no Iapetus, no dashboard. If numbers move here, the hardware is fine and the
 * problem is above it. If they don't, stop reading code and look at the robot.
 *
 * HOW TO USE IT
 *
 *   1. Run it. Read the top three lines — they answer "is it plugged in?"
 *   2. PUSH THE ROBOT BY HAND, about a foot forward, then a foot sideways.
 *   3. Watch TICKS. They must change. That's the whole test.
 *
 * WHAT THE STATUS LINE MEANS
 *
 *   READY                      good — the device is happy
 *   NOT_READY                  powered but not initialised; usually wiring
 *   CALIBRATING                hold still for a second, it's zeroing the IMU
 *   FAULT_NO_PODS_DETECTED     neither pod is plugged into the Pinpoint
 *   FAULT_X_POD_NOT_DETECTED   the forward pod's cable
 *   FAULT_Y_POD_NOT_DETECTED   the strafe pod's cable
 *   FAULT_IMU_RUNAWAY          the Pinpoint moved while calibrating
 *   FAULT_BAD_READ             I2C trouble — cable, or an address clash
 *
 * Nothing here moves the robot, so it's safe to run any time.
 */
@TeleOp(name = "Pinpoint Doctor", group = "Diagnostics")
public class PinpointDoctor extends LinearOpMode {

    private GoBildaPinpointDriver pinpoint;
    private String foundUnderName = null;

    @Override
    public void runOpMode() {
        findPinpoint();

        telemetry.setMsTransmissionInterval(50);

        int startX = 0, startY = 0;
        boolean baselineTaken = false;
        int maxDeltaX = 0, maxDeltaY = 0;

        waitForStart();

        while (opModeIsActive()) {
            if (pinpoint == null) {
                reportMissing();
                telemetry.update();
                sleep(200);
                continue;
            }

            // Ask the device for fresh numbers. Nothing below is meaningful
            // without this call.
            pinpoint.update();

            int x = pinpoint.getEncoderX();
            int y = pinpoint.getEncoderY();

            if (!baselineTaken) {
                startX = x;
                startY = y;
                baselineTaken = true;
            }
            maxDeltaX = Math.max(maxDeltaX, Math.abs(x - startX));
            maxDeltaY = Math.max(maxDeltaY, Math.abs(y - startY));

            GoBildaPinpointDriver.DeviceStatus status = pinpoint.getDeviceStatus();

            telemetry.addLine("═══ IS IT THERE? ═══");
            telemetry.addData("Found as", foundUnderName);
            telemetry.addData("Configured name", Constants.PINPOINT_NAME);
            telemetry.addData("Device ID / version", "%d / %d",
                    pinpoint.getDeviceID(), pinpoint.getDeviceVersion());

            telemetry.addLine();
            telemetry.addLine("═══ IS IT HEALTHY? ═══");
            telemetry.addData("Status", status);
            telemetry.addData("Update rate", "%.0f Hz  (loop %d us)",
                    pinpoint.getFrequency(), pinpoint.getLoopTime());

            telemetry.addLine();
            telemetry.addLine("═══ PUSH THE ROBOT — DO THESE MOVE? ═══");
            telemetry.addData("TICKS x / y", "%d / %d", x, y);
            telemetry.addData("Moved since start", "x:%d  y:%d", maxDeltaX, maxDeltaY);
            telemetry.addData("Verdict", verdict(status, maxDeltaX, maxDeltaY));

            telemetry.addLine();
            telemetry.addLine("═══ COMPUTED POSITION ═══");
            telemetry.addData("Pos (in)", "X:%.1f  Y:%.1f",
                    pinpoint.getPosX(DistanceUnit.INCH), pinpoint.getPosY(DistanceUnit.INCH));
            telemetry.addData("Heading (deg)", "%.1f",
                    pinpoint.getHeading(AngleUnit.DEGREES));
            telemetry.addData("Velocity (in/s)", "X:%.1f  Y:%.1f",
                    pinpoint.getVelX(DistanceUnit.INCH), pinpoint.getVelY(DistanceUnit.INCH));

            telemetry.update();
        }
    }

    /**
     * Look for the Pinpoint under the configured name first, then under ANY
     * name. A device that exists but is configured as something else is the
     * single most common cause of "it's plugged in and still doesn't work" —
     * and it's invisible unless you go looking.
     */
    private void findPinpoint() {
        try {
            pinpoint = hardwareMap.get(GoBildaPinpointDriver.class, Constants.PINPOINT_NAME);
            foundUnderName = Constants.PINPOINT_NAME + "  (matches Constants)";
            return;
        } catch (Exception ignored) {
            // Not under the expected name. Keep looking before giving up.
        }

        for (GoBildaPinpointDriver device : hardwareMap.getAll(GoBildaPinpointDriver.class)) {
            Set<String> names = hardwareMap.getNamesOf(device);
            if (!names.isEmpty()) {
                pinpoint = device;
                foundUnderName = names.iterator().next() + "  ← NAME MISMATCH";
                return;
            }
        }
    }

    private void reportMissing() {
        telemetry.addLine("═══ NO PINPOINT FOUND ═══");
        telemetry.addData("Looked for", Constants.PINPOINT_NAME);
        telemetry.addLine();
        telemetry.addLine("Nothing of type GoBildaPinpointDriver is in the");
        telemetry.addLine("robot configuration at all. In order:");
        telemetry.addLine("  1. Is it wired to an I2C port on the hub?");
        telemetry.addLine("  2. Is it ADDED to the Driver Station config,");
        telemetry.addLine("     as 'goBILDA Pinpoint Odometry Computer'?");
        telemetry.addLine("  3. Does the config name match Constants.PINPOINT_NAME?");
    }

    private String verdict(GoBildaPinpointDriver.DeviceStatus status, int dx, int dy) {
        if (status == GoBildaPinpointDriver.DeviceStatus.CALIBRATING) {
            return "calibrating — hold still";
        }
        if (status != GoBildaPinpointDriver.DeviceStatus.READY) {
            return "device not READY — fix that first (see header)";
        }
        if (dx == 0 && dy == 0) {
            return "READY but NOTHING MOVED — push the robot; if still 0, the pods aren't turning";
        }
        if (dx == 0) {
            return "only Y moved — check the forward (X) pod";
        }
        if (dy == 0) {
            return "only X moved — check the strafe (Y) pod";
        }
        return "BOTH PODS COUNTING — the sensor is fine, look further up the stack";
    }
}
