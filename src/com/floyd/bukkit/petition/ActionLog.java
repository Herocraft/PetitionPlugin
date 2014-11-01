package com.floyd.bukkit.petition;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ActionLog
{
    public static final String LOG_FILE = PetitionPlugin.BASE_DIR + File.separator + "petitionlog.txt";
    public static final String NEW_LINE = System.getProperty("line.separator");

    public String cache;

    public ActionLog()
    {
        // Ensure that logFile exists
        File f = new File(LOG_FILE);
        if (!f.exists()) {
            BufferedWriter output;
            try {
                output = new BufferedWriter(new FileWriter(LOG_FILE));
                output.write("");
                output.close();
                PetitionPlugin.logger.info("[Pe] Created log file '" + LOG_FILE + "'");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void logAction(String line) {
        // Timestamp events
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("MMMM d, yyyy, H:mm");
        String out = "[" + format.format(now) + "] " + line + NEW_LINE;

        try {
            if (cache == null) {
                cache = readLog();
            }
            // Newest log messages at top
            cache = out + cache;

            // Write to cache
            BufferedWriter output = new BufferedWriter(new FileWriter(LOG_FILE));
            output.write(cache);
            output.flush();
            output.close();
        } catch (IOException ioe) {
            PetitionPlugin.logger.severe("[Pe] Error writing to the log file!");
        }
        PetitionPlugin.logger.fine("[Pe] Logged action of #" + line);
    }

    public String readLog() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(LOG_FILE)));
        String read = null;
        StringBuilder out = new StringBuilder();

        // Reads the existing log
        while ((read = reader.readLine()) != null) {
            out.append(read).append(NEW_LINE);
        }
        reader.close();
        return out.toString();
    }
}
