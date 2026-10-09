package org.firstinspires.ftc.teamcode.utils;

import android.os.Environment;

import com.pedropathing.math.Pose;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║                     PERSISTENT POSE MANAGER                               ║
 * ║                                                                           ║
 * ║  The handoff between autonomous and teleop.                               ║
 * ║                                                                           ║
 * ║  Auto ends wherever it ends. Teleop needs to know that, or field-centric  ║
 * ║  drive starts from a lie. So auto scribbles its final pose to a file on   ║
 * ║  the Control Hub and teleop picks it up.                                  ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 *
 * The note goes stale. If auto ran an hour ago — or didn't run at all today —
 * loading that pose would put the robot somewhere it isn't, which is worse
 * than admitting we don't know. Anything older than MAX_AGE is ignored.
 */
public class PersistentPoseManager {

    private static final String POSE_FILE =
            Environment.getExternalStorageDirectory().getPath() + "/FIRST/artemis_pose.txt";

    /** A pose older than this is from a previous match. Don't trust it. */
    private static final long MAX_AGE_MS = 10 * 60 * 1000;  // 10 minutes

    private static final Pose DEFAULT_POSE = new Pose(9.5, 24, 0);
    private static final boolean DEFAULT_IS_RED = true;

    /** What auto left behind, already checked for staleness. */
    public static class Handoff {
        public final Pose pose;
        public final boolean isRed;
        /** False when we fell back to defaults — worth showing on telemetry. */
        public final boolean wasFound;

        Handoff(Pose pose, boolean isRed, boolean wasFound) {
            this.pose = pose;
            this.isRed = isRed;
            this.wasFound = wasFound;
        }
    }

    /** Call at the end of autonomous. Format: x,y,heading,isRed,timestampMillis */
    public static void save(Pose pose, boolean isRed) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(POSE_FILE))) {
            writer.write(pose.x() + "," + pose.y() + "," + pose.heading()
                    + "," + isRed + "," + System.currentTimeMillis());
        } catch (Exception e) {
            // A failed handoff shouldn't take the OpMode down with it. Teleop
            // will just start from defaults and the driver will notice.
        }
    }

    /**
     * Call at the start of teleop. Reads the file exactly once — the old
     * version read it twice and could disagree with itself.
     */
    public static Handoff load() {
        try (BufferedReader reader = new BufferedReader(new FileReader(POSE_FILE))) {
            String line = reader.readLine();
            if (line == null) return fallback();

            String[] f = line.split(",");
            if (f.length < 5) return fallback();  // old format, don't guess

            long age = System.currentTimeMillis() - Long.parseLong(f[4]);
            if (age < 0 || age > MAX_AGE_MS) return fallback();  // stale or clock weirdness

            return new Handoff(
                    new Pose(Double.parseDouble(f[0]),
                             Double.parseDouble(f[1]),
                             Double.parseDouble(f[2])),
                    Boolean.parseBoolean(f[3]),
                    true);
        } catch (Exception e) {
            return fallback();
        }
    }

    private static Handoff fallback() {
        return new Handoff(DEFAULT_POSE, DEFAULT_IS_RED, false);
    }
}
