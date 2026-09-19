package org.firstinspires.ftc.teamcode.subsystems;

import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.teamcode.Iapetus;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                          INTAKE SUBSYSTEM                                 ║
 * ║                                                                           ║
 * ║  Pulls game pieces into the robot. Blank for now — nothing to eat yet.    ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * Follow PedroDrive's shape: hardware lookups in the constructor, hardware
 * names from Constants, small methods that each do one thing.
 */
public class Intake extends SubsystemBase {

    private final Iapetus robot;

    public Intake(Iapetus robot) {
        this.robot = robot;
    }

    @Override
    public void periodic() {
    }
}
