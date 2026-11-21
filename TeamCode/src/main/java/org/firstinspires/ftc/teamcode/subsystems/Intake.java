package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.utils.Constants;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                          INTAKE SUBSYSTEM                                 ║
 * ║                                                                           ║
 * ║  One motor pulls game pieces in. Negative power runs it backwards.        ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Never reads a gamepad. Teleop's stick lives in IntakeManual, the default
 * command; anything pre-programmed is another command that requires this
 * subsystem and interrupts it.
 */
public class Intake extends SubsystemBase {

    private final Iapetus robot;
    private final DcMotorEx intakeMotor;

    /** Which hub + port the DS config says "intake" is on. Read once. */
    private final String wiredTo;

    /** Last power we asked for. Tracked here so telemetry costs no hardware read. */
    private double power = 0;

    public Intake(Iapetus robot) {
        this.robot = robot;
        intakeMotor = robot.hardwareMap.get(DcMotorEx.class, Constants.INTAKE_MOTOR_NAME);
        intakeMotor.setDirection(Constants.INTAKE_MOTOR_DIRECTION);
        wiredTo = intakeMotor.getConnectionInfo();
    }

    /** -1 to 1. Negative reverses, e.g. to clear a jam. */
    public void setIntakeSpeed(double intakePower) {
        power = intakePower;
        intakeMotor.setPower(intakePower);
    }

    public void stop() {
        setIntakeSpeed(0);
    }

    /**
     * Intake not spinning? Read these three lines while pushing the stick:
     *   power stays 0       → the code never asked (wrong gamepad? see IntakeManual)
     *   power, but ~0 A     → asked, nobody home: wire, port, or config
     *   power, lots of amps → motor is trying: something's jammed
     */
    @Override
    public void periodic() {
        robot.sensors.addTelemetry("═══ Intake ═══", "");
        robot.sensors.addTelemetry("Intake power", "%.2f", power);
        // Current isn't in the bulk read, so it's an extra trip to the hub.
        // Only pay for it while the intake is supposed to be running.
        if (power != 0) {
            robot.sensors.addTelemetry("Intake current", "%.2f A",
                    intakeMotor.getCurrent(CurrentUnit.AMPS));
        }
        robot.sensors.addTelemetry("Intake wired to", wiredTo);
    }
}
