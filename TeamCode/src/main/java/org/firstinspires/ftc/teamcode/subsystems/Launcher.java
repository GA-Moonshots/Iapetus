package org.firstinspires.ftc.teamcode.subsystems;

import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.teamcode.Iapetus;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                         LAUNCHER SUBSYSTEM                                ║
 * ║                                                                           ║
 * ║  Sends game pieces toward the goal. Blank for now — no liftoff yet.       ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Follow PedroDrive's shape: hardware lookups in the constructor, hardware
 * names from Constants, small methods that each do one thing.
 */
public class Launcher extends SubsystemBase {

    private final Iapetus robot;

    public Launcher(Iapetus robot) {
        this.robot = robot;
    }

    @Override
    public void periodic() {
    }
}
