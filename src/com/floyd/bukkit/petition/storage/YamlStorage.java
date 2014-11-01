package com.floyd.bukkit.petition.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;

import org.bukkit.entity.Player;

import com.floyd.bukkit.petition.PetitionPlugin;

public class YamlStorage implements Storage
{
    private static final String TICKET_FILE = PetitionPlugin.BASE_DIR + File.separator + "last_ticket_id.txt";

    public YamlStorage()
    {
        String fname = "";
        File f;

        // Ensure that ticketFile exists
        fname =  TICKET_FILE;
        f = new File(fname);
        if (!f.exists()) {
            // Ensure that configFile exists
            String newline = System.getProperty("line.separator");
            BufferedWriter output;
            try {
                output = new BufferedWriter(new FileWriter(fname));
                output.write("0" + newline);
                output.close();
                PetitionPlugin.logger.info("[Pe] Created ticket file '" + fname + "'");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public PetitionObject create(Player player, String newtitle) {
        Long id = IssueUniqueTicketID();
        PetitionObject petition = new PetitionObject(id, player, newtitle);
        save(petition);
        return petition;
    }

    // Load an existing petition
    @Override
    public PetitionObject load(Long getid)
    {
        PetitionObject petition = new PetitionObject();
        
        // Look in the archive first
        String fname = PetitionPlugin.ARCHIVE_DIR + File.separator + String.valueOf(getid) + ".ticket";
        petition.closed = true;
        File f = new File(fname);
        // Not found? Then check the regular ones
        if (!f.exists()) {
            fname = PetitionPlugin.BASE_DIR + File.separator + String.valueOf(getid) + ".ticket";
            petition.closed = false;
        }
        try {
            BufferedReader input =  new BufferedReader(new FileReader(fname));
            String line = null;
            while ((line = input.readLine()) != null) {
                // File consists of key=value pairs, parse it 
                String[] parts = line.split("=", 2);
                if (parts[0].equals("id")) { petition.id = Long.parseLong(parts[1]); }
                if (parts[0].equals("owner")) { petition.owner = parts[1]; }
                if (parts[0].equals("title")) { petition.title = parts[1]; }
                if (parts[0].equals("world")) { petition.world = parts[1]; }
                if (parts[0].equals("x")) { petition.x = Double.parseDouble(parts[1]); }
                if (parts[0].equals("y")) { petition.y = Double.parseDouble(parts[1]); }
                if (parts[0].equals("z")) { petition.z = Double.parseDouble(parts[1]); }
                if (parts[0].equals("pitch")) { petition.pitch = Float.parseFloat(parts[1]); }
                if (parts[0].equals("yaw")) { petition.yaw = Float.parseFloat(parts[1]); }
                if (parts[0].equals("assignee")) { petition.assignee = parts[1]; }
                if (parts[0].equals("log")) { petition.log.add(new PetitionComment(parts[1])); }
            }
            input.close();
        }
        catch (FileNotFoundException e) {
            System.out.println("[Pe] Error reading " + e.getLocalizedMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return petition;
    }

    // Save a new or updated petition
    @Override
    public void save(PetitionObject petition)
    {
        String fname = PetitionPlugin.BASE_DIR + File.separator + String.valueOf(petition.id) + ".ticket";
        if (petition.closed) {
            fname = PetitionPlugin.BASE_DIR + "/archive/" + String.valueOf(petition.id) + ".ticket";
        }
        BufferedWriter output;
        if (!petition.isValid()) {
            return;
        }
        try {
            output = new BufferedWriter(new FileWriter(fname));
            output.write("id=" + String.valueOf(petition.id) + "\n");
            output.write("owner=" + petition.owner + "\n");
            output.write("title=" + petition.title + "\n");
            output.write("world=" + petition.world + "\n");
            output.write("x=" + String.valueOf(petition.x) + "\n");
            output.write("y=" + String.valueOf(petition.y) + "\n");
            output.write("z=" + String.valueOf(petition.z) + "\n");
            output.write("pitch=" + String.valueOf(petition.pitch) + "\n");
            output.write("yaw=" + String.valueOf(petition.yaw) + "\n");
            output.write("assignee=" + petition.assignee + "\n");
            for (PetitionComment entry : petition.log) {
                output.write("log=" + entry + "\n");
            }
            output.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void assign(PetitionObject petition, Player player, String name)
    {
        String moderator = "(Console)";
        if (player != null) {
            moderator = player.getName();
        }
        petition.log.add(new PetitionComment("Assigned to " + name + " by " + moderator));
        petition.assignee = name;
        save(petition);
    }

    @Override
    public void unassign(PetitionObject petition, Player player)
    {
        String moderator = "(Console)";
        if (player != null) {
            moderator = player.getName();
        }
        petition.log.add(new PetitionComment("Unassigned by " + moderator));
        petition.assignee = "*";
        save(petition);
    }

    @Override
    public void close(PetitionObject petition, Player player, String message)
    {
        String moderator = "(Console)";
        if (player != null) {
            moderator = player.getName();
        }
        if (message.equals("")) {
            petition.log.add(new PetitionComment("Closed by " + moderator));
        }
        else {
            petition.log.add(new PetitionComment("Closed by " + moderator + ": " + message));
        }
        save(petition);
        File oldFile = new File(PetitionPlugin.BASE_DIR + File.separator + petition.id + ".ticket");
        oldFile.renameTo(new File(PetitionPlugin.ARCHIVE_DIR + File.separator + petition.id + ".ticket"));
    }

    @Override
    public void reopen(PetitionObject petition, Player player, String message)
    {
        String moderator = "(Console)";
        if (player != null) {
            moderator = player.getName();
        }
        if (message.equals("")) {
            petition.log.add(new PetitionComment("Reopened by " + moderator));
        }
        else {
            petition.log.add(new PetitionComment("Reopened by " + moderator + ": " + message));
        }
        save(petition);
        File oldFile = new File(PetitionPlugin.ARCHIVE_DIR + File.separator + petition.id + ".ticket");
        oldFile.renameTo(new File(PetitionPlugin.BASE_DIR + File.separator + petition.id + ".ticket"));
    }

    @Override
    public void comment(PetitionObject petition, Player player, String message)
    {
        String moderator = "(Console)";
        if (player != null) {
            moderator = player.getName();
        }
        if (message.equals("")) {
            return;
        }
        else {
            petition.log.add(new PetitionComment(moderator + ": " + message));
        }
        save(petition);
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
