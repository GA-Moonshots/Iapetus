package org.firstinspires.ftc.teamcode.commands;

import org.firstinspires.ftc.teamcode.Iapetus;
import org.firstinspires.ftc.teamcode.utils.Tunables;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                        TURN TO A HEADING                                  ║
 * ║                                                                           ║
 * ║  "Face 90°." Absolute — the robot ends up pointing that way no matter     ║
 * ║  which way it started. For a relative rotation, use DriveTurnBy.          ║
 * ║                                                                           ║
 * ║  Headings use the Pedro convention:                                       ║
 * ║      0° = facing +X (right)     90° = facing +Y (up)                      ║
 * ║      180° = facing -X (left)   -90° = facing -Y (down)                    ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Same reason for wrapping as DriveTurnBy: SolversLib's TurnToCommand has no
 * timeout and no cleanup, so a turn that never settles blocks the match.
 */
public class DriveTurnTo extends DriveAbstract {

    private final double targetDegrees;

    /** @param targetDegrees absolute field heading to face */
    public DriveTurnTo(Iapetus robot, double targetDegrees, double timeoutSeconds) {
        super(robot, timeoutSeconds);
        this.targetDegrees = targetDegrees;
    }

    @Override
    public void initialize() {
        patience.start();
        drive.turnTo(Math.toRadians(targetDegrees));
        robot.sensors.addTelemetry("TurnTo", "%.0f° (from %.0f°)",
                targetDegrees, Math.toDegrees(drive.getNormalizedHeading()));
    }

    @Override
    public boolean isFinished() {
        return drive.isFacing(Math.toRadians(targetDegrees), Tunables.HEADING_TOLERANCE_DEG)
                || patience.done();
    }

    @Override
    public void end(boolean interrupted) {
        standardCleanup();
        robot.sensors.addTelemetry("TurnTo",
                interrupted ? "INTERRUPTED" : (patience.done() ? "TIMED OUT" : "Done"));
    }
}
