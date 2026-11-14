package org.firstinspires.ftc.teamcode.utils;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys.Button;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys.Trigger;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.commands.Drive;
import org.firstinspires.ftc.teamcode.commands.LaunchDynamic;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                        LAUNCHER TUNER                                     ║
 * ║                                                                           ║
 * ║  Find the speed that scores. One person, gamepad 1.                       ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 *   Sticks        drive (right bumper = slow)
 *   B             flywheels on/off
 *   DPAD ↑ / ↓    speed ±0.5 m/s        DPAD → / ←   ±0.1 m/s
 *   A (hold)      feed — wait for "Launcher ready: YES" first
 *   Triggers      intake in (right) / out (left)
 *
 * AUTO SHOT (10 min): park where auto shoots, adjust until it scores 3 in a
 * row, then type the AUTO_LAUNCH_SPEED line into Tunables.java. Hold A until
 * the robot is empty: "Last feed" is LAUNCHER_FEED_SERVO_DURATION_MS (add ~100).
 *
 * PHYSICS SCALE (optional): with the Hive in view, the LAUNCH_SPEED_SCALE line
 * corrects the physics so camera shots work from other distances. Check it
 * at a second distance before trusting it.
 */
@TeleOp(name = "Launcher Tuner", group = "Tuning")
public class LauncherTuner extends CommandOpMode {

    private Iapetus robot;
    private double speed = 10.0;   // m/s, a starting guess
    private boolean spinning = false;

    private final ElapsedTime feedTimer = new ElapsedTime();
    private boolean wasFeeding = false;
    private double lastFeedMs = 0;

    @Override
    public void initialize() {
        robot = new Iapetus(this);
        // Drive only. No initTeleOp(): its trigger command would fight us for the launcher.
        robot.drive.setDefaultCommand(new Drive(robot));

        telemetry.addLine("Launcher Tuner: B = wheels, DPAD = speed, hold A = feed.");
        telemetry.update();
    }

    @Override
    public void run() {
        GamepadEx pad = robot.player1;
        pad.readButtons();

        if (pad.wasJustPressed(Button.DPAD_UP))    speed += 0.5;
        if (pad.wasJustPressed(Button.DPAD_DOWN))  speed -= 0.5;
        if (pad.wasJustPressed(Button.DPAD_RIGHT)) speed += 0.1;
        if (pad.wasJustPressed(Button.DPAD_LEFT))  speed -= 0.1;
        speed = Math.max(0, Math.min(speed, Launcher.maxLaunchSpeed()));
        if (pad.wasJustPressed(Button.B)) spinning = !spinning;

        if (spinning) {
            robot.launcher.setLaunchSpeed(speed);
            robot.launcher.setMode(Launcher.Mode.AUTO);
        } else {
            robot.launcher.stopAll();
        }

        boolean feeding = spinning && pad.getButton(Button.A);
        robot.launcher.setFeeding(feeding);
        if (feeding && !wasFeeding) feedTimer.reset();
        if (!feeding && wasFeeding) lastFeedMs = feedTimer.milliseconds();
        wasFeeding = feeding;

        robot.intake.setIntakeSpeed(Tunables.INTAKE_MAX_POWER
                * (pad.getTrigger(Trigger.RIGHT_TRIGGER) - pad.getTrigger(Trigger.LEFT_TRIGGER)));

        report();
        super.run();
    }

    private void report() {
        robot.sensors.addTelemetry("═══ Launcher Tuner ═══", "");
        robot.sensors.addTelemetry("Speed", "%.1f m/s  (max %.1f)  wheels %s",
                speed, Launcher.maxLaunchSpeed(), spinning ? "ON" : "off (B)");
        robot.sensors.addTelemetry("Last feed", "%.0f ms", lastFeedMs);
        robot.sensors.addTelemetry("→ AUTO_LAUNCH_SPEED", "%.1f", speed);

        // Any Hive will do here, not just ours: we're measuring, not playing.
        TagSighting hive = robot.sensors.nearestTarget();
        if (hive == null) {
            robot.sensors.addTelemetry("Hive", "not in view (scale needs it)");
            return;
        }
        double predicted = LaunchDynamic.physicsSpeed(hive.range());
        robot.sensors.addTelemetry("Hive", "%s, %.0f\" away, physics says %.2f m/s",
                hive.name, hive.range(), predicted);
        if (predicted > 0) {
            robot.sensors.addTelemetry("→ LAUNCH_SPEED_SCALE", "%.2f", speed / predicted);
        }
    }
}
