package org.firstinspires.ftc.teamcode.utils;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;

import org.firstinspires.ftc.teamcode.Iapetus;

/**
 * ██████╗ ██████╗ ██╗██╗   ██╗███████╗██╗   ██╗
 * ██╔══██╗██╔══██╗██║██║   ██║██╔════╝╚██╗ ██╔╝
 * ██║  ██║██████╔╝██║██║   ██║█████╗   ╚████╔╝
 * ██║  ██║██╔══██╗██║╚██╗ ██╔╝██╔══╝    ╚██╔╝
 * ██████╔╝██║  ██║██║ ╚████╔╝ ███████╗   ██║
 * ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═══╝  ╚══════╝   ╚═╝
 *              M c D R I V E R S O N
 *
 * The driver-controlled OpMode. Note how little lives here: build the robot,
 * bind the buttons, step back. If this file outgrows one screen, something
 * in it belongs in a Subsystem or a Command instead.
 */
@TeleOp(name = "Drivey McDriverson", group = "Competition")
public class DriveyMcDriverson extends CommandOpMode {

    private Iapetus robot;

    @Override
    public void initialize() {
        robot = new Iapetus(this);
        robot.initTeleOp();

        telemetry.addData("Status", "Ready. Try not to hit the wall.");
        telemetry.update();
    }

    /**
     * DO NOT DELETE THE readButtons() CALLS.
     *
     * GamepadEx only notices a press when asked to look. Skip these and every
     * button binding silently does nothing — robot drives fine, no error
     * anywhere, and you lose an afternoon.
     */
    @Override
    public void run() {
        robot.player1.readButtons();
        robot.player2.readButtons();
        super.run();
    }
}
