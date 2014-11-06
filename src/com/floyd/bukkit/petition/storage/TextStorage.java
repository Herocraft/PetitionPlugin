package com.floyd.bukkit.petition.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;

import com.floyd.bukkit.petition.PetitionPlugin;

public class TextStorage implements Storage
{
    private static final FilenameFilter TICKET_NAME_FILTER = new FilenameFilter() {
        
        @Override
        public boolean accept(File dir, String name)
        {
            return name.endsWith(".ticket");
        }
    };
    private static final String TICKET_FILE = PetitionPlugin.BASE_DIR + File.separator + "last_ticket_id.txt";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy");
    private final Map<Long, PetitionObject> cache = new ConcurrentHashMap<Long, PetitionObject>();

    public TextStorage()
    {
        // Ensure that ticketFile exists
        File f = new File(TICKET_FILE);
        if (!f.exists()) {
            // Ensure that configFile exists
            String newline = System.getProperty("line.separator");
            BufferedWriter output;
            try {
                output = new BufferedWriter(new FileWriter(TICKET_FILE));
                output.write("0" + newline);
                output.close();
                PetitionPlugin.logger.info("[Pe] Created ticket file '" + TICKET_FILE + "'");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public PetitionObject create(Player player, String newtitle) {
        PetitionObject petition = new PetitionObject(IssueUniqueTicketID(), player, newtitle);
        cache.put(petition.getId(), petition);
        save(petition);
        return petition;
    }

    // Load an existing petition
    @Override
    public PetitionObject load(Long petitionId)
    {
        PetitionObject petition = new PetitionObject();

        // Look in the archive first
        File peFile = new File(PetitionPlugin.ARCHIVE_DIR + File.separator + String.valueOf(petitionId) + ".ticket");
        if (peFile.exists()) {
            petition.setClosed(true);
        }
        else {
            petition.setClosed(false);
            peFile = new File(PetitionPlugin.BASE_DIR + File.separator + String.valueOf(petitionId) + ".ticket");
        }

        try {
            BufferedReader input =  new BufferedReader(new FileReader(peFile));
            String line = null;
            while ((line = input.readLine()) != null) {
                // File consists of key=value pairs, parse it 
                String[] parts = line.split("=", 2);
                if (parts[0].equals("id")) { petition.setId(Long.parseLong(parts[1])); }
                if (parts[0].equals("timestamp")) { petition.setTimestamp(DATE_FORMAT.parse(parts[1])); }
                if (parts[0].equals("owner")) { petition.setOwner(parts[1]); }
                if (parts[0].equals("title")) { petition.setTitle(parts[1]); }
                if (parts[0].equals("world")) { petition.setWorld(parts[1]); }
                if (parts[0].equals("x")) { petition.setX(Double.parseDouble(parts[1])); }
                if (parts[0].equals("y")) { petition.setY(Double.parseDouble(parts[1])); }
                if (parts[0].equals("z")) { petition.setZ(Double.parseDouble(parts[1])); }
                if (parts[0].equals("pitch")) { petition.setPitch(Float.parseFloat(parts[1])); }
                if (parts[0].equals("yaw")) { petition.setYaw(Float.parseFloat(parts[1])); }
                if (parts[0].equals("assignee")) { petition.setAssignee(parts[1]); }
                if (parts[0].equals("log")) { petition.getLog().add(new PetitionComment(parts[1])); }
            }
            input.close();
            synchronized(cache) {
                if (cache.containsKey(petition.getId())) {
                    petition = cache.get(petition.getId());
                }
                else {
                    cache.put(petition.getId(), petition);
                }
            }
        }
        catch (Exception e) {
            PetitionPlugin.logger.severe("[Pe] Error reading " + e.getLocalizedMessage());
        }

        return petition;
    }

    @Override
    public List<PetitionObject> list(boolean isArchived, final String filter)
    {
        List<PetitionObject> petitions = Collections.emptyList();
        File dir = isArchived ? new File(PetitionPlugin.ARCHIVE_DIR) : new File(PetitionPlugin.BASE_DIR);
        String[] filenames = dir.list(TICKET_NAME_FILTER);
        Pattern pattern = null;
        if (StringUtils.isNotEmpty(filter)) {
            try {
                pattern = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);
            } catch (Exception ignored) {}
        }
        if (filenames != null) {
            petitions = new ArrayList<PetitionObject>();
            for (String filename : filenames) {
                try {
                    PetitionObject petition = load(Long.valueOf(filename.split("\\.")[0]));
                    if (pattern != null) {
                        if (pattern.matcher(petition.getHeader()).find()) {
                            petitions.add(petition);
                        }
                    } else {
                        petitions.add(petition);
                    }
                } catch (Exception ignored) {}
            }
        }
        Collections.sort(petitions, new Comparator<PetitionObject>() {

            @Override
            public int compare(PetitionObject a, PetitionObject b)
            {
                if (a.getId() > b.getId()) return 1;
                if (a.getId() < b.getId()) return -1;
                return 0;
            }
        });
        return petitions;
    }

    // Save a new or updated petition
    public synchronized void save(PetitionObject petition)
    {
        if (petition.isNotValid()) {
            PetitionPlugin.logger.warning("[Pe] Attempt to save invalid Petition");
            return;
        }

        try {
            File file = new File(new StringBuilder().append(petition.isClosed() ? PetitionPlugin.ARCHIVE_DIR : PetitionPlugin.BASE_DIR).append(File.separator)
                    .append(petition.getId()).append(".ticket").toString());
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            output.write("id=" + String.valueOf(petition.getId()) + "\n");
            petition.setTimestamp(new Date());
            output.write("timestamp=" + DATE_FORMAT.format(petition.getTimestamp()) + "\n");
            output.write("owner=" + petition.getOwner() + "\n");
            output.write("title=" + petition.getTitle() + "\n");
            output.write("world=" + petition.getWorld() + "\n");
            output.write("x=" + String.valueOf(petition.getX()) + "\n");
            output.write("y=" + String.valueOf(petition.getY()) + "\n");
            output.write("z=" + String.valueOf(petition.getZ()) + "\n");
            output.write("pitch=" + String.valueOf(petition.getPitch()) + "\n");
            output.write("yaw=" + String.valueOf(petition.getYaw()) + "\n");
            output.write("assignee=" + petition.getAssignee() + "\n");
            for (PetitionComment entry : petition.getLog()) {
                output.write("log=" + entry + "\n");
            }
            output.close();
            cache.remove(petition.getId());
        }
        catch (Exception e) {
            PetitionPlugin.logger.severe("[Pe] " + e.getMessage());
        }
    }

    @Override
    public void assign(PetitionObject petition, Player player, String name)
    {
        synchronized (petition) {
            String moderator = player != null ? player.getName() : PetitionPlugin.CONSOLE_NAME;
            petition.getLog().add(new PetitionComment("Assigned to " + name + " by " + moderator));
            petition.setAssignee(name);
            save(petition);
        }
    }

    @Override
    public void unassign(PetitionObject petition, Player player)
    {
        synchronized (petition) {
            String moderator = player != null ? player.getName() : PetitionPlugin.CONSOLE_NAME;
            petition.getLog().add(new PetitionComment("Unassigned by " + moderator));
            petition.setAssignee("*");
            save(petition);
        }
    }

    @Override
    public void close(PetitionObject petition, Player player, String message)
    {
        synchronized(petition) {
            String moderator = player != null ? player.getName() : PetitionPlugin.CONSOLE_NAME;
            if (StringUtils.isEmpty(message)) {
                petition.getLog().add(new PetitionComment("Closed by " + moderator));
            }
            else {
                petition.getLog().add(new PetitionComment("Closed by " + moderator + ": " + message));
            }
            save(petition);
            File oldFile = new File(PetitionPlugin.BASE_DIR + File.separator + petition.getId() + ".ticket");
            oldFile.renameTo(new File(PetitionPlugin.ARCHIVE_DIR + File.separator + petition.getId() + ".ticket"));
        }
    }

    @Override
    public void reopen(PetitionObject petition, Player player, String message)
    {
        String moderator = player != null ? player.getName() : PetitionPlugin.CONSOLE_NAME;
        if (StringUtils.isEmpty(message)) {
            petition.getLog().add(new PetitionComment("Reopened by " + moderator));
        }
        else {
            petition.getLog().add(new PetitionComment("Reopened by " + moderator + ": " + message));
        }
        save(petition);
        File oldFile = new File(PetitionPlugin.ARCHIVE_DIR + File.separator + petition.getId() + ".ticket");
        oldFile.renameTo(new File(PetitionPlugin.BASE_DIR + File.separator + petition.getId() + ".ticket"));
    }

    @Override
    public void comment(PetitionObject petition, Player player, String message)
    {
        if (StringUtils.isNotBlank(message)) {
            String moderator = player != null ? player.getName() : PetitionPlugin.CONSOLE_NAME;
            petition.getLog().add(new PetitionComment(moderator + ": " + message));
            save(petition);
        }
    }

    private synchronized Long IssueUniqueTicketID() {
        String fname = TICKET_FILE;
        String line = null;

        // Read the current file (if it exists)
        try {
            BufferedReader input =  new BufferedReader(new FileReader(fname));
            if ((line = input.readLine()) != null) {
                line = line.trim();
            }
            input.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Unsuccessful? Assume the file is invalid or does not exist
        if (line == null) {
            line = "0";
        }

        // Increment the counter
        line = String.valueOf(Integer.parseInt(line) + 1);

        // Write the new last ticket id
        BufferedWriter output;
        String newline = System.getProperty("line.separator");
           try {
               output = new BufferedWriter(new FileWriter(fname));
               output.write(line + newline);
               output.close();
           }
           catch (Exception e) {
            e.printStackTrace();
           }

        PetitionPlugin.logger.fine("[Pe] Issued ticket #" + line);
        return Long.valueOf(line);
    }
}
