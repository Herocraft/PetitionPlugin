package com.floyd.bukkit.petition.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;

import org.bukkit.entity.Player;

public class YamlStorage implements Storage
{
    private static final String PATH = "plugins/PetitionPlugin";
    private static String ARCHIVE = "plugins/PetitionPlugin/archive";

    @Override
    public PetitionObject create(Long id, Player player, String newtitle) {
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
        String fname = ARCHIVE + "/" + String.valueOf(getid) + ".ticket";
        petition.closed = true;
        File f = new File(fname);
        // Not found? Then check the regular ones
        if (!f.exists()) {
            fname = PATH + "/" + String.valueOf(getid) + ".ticket";
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
                if (parts[0].equals("log")) { petition.log.add(new PetitionLog(parts[1])); }
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
        String fname = PATH + "/" + String.valueOf(petition.id) + ".ticket";
        if (petition.closed) {
            fname = PATH + "/archive/" + String.valueOf(petition.id) + ".ticket";
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
            for (PetitionLog entry : petition.log) {
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
        petition.log.add(new PetitionLog("Assigned to " + name + " by " + moderator));
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
        petition.log.add(new PetitionLog("Unassigned by " + moderator));
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
            petition.log.add(new PetitionLog("Closed by " + moderator));
        }
        else {
            petition.log.add(new PetitionLog("Closed by " + moderator + ": " + message));
        }
        save(petition);
        File oldFile = new File(PATH + "/" + petition.id + ".ticket");
        oldFile.renameTo(new File(ARCHIVE + "/" + petition.id + ".ticket"));
    }

    @Override
    public void reopen(PetitionObject petition, Player player, String message)
    {
        String moderator = "(Console)";
        if (player != null) {
            moderator = player.getName();
        }
        if (message.equals("")) {
            petition.log.add(new PetitionLog("Reopened by " + moderator));
        }
        else {
            petition.log.add(new PetitionLog("Reopened by " + moderator + ": " + message));
        }
        save(petition);
        File oldFile = new File(ARCHIVE + "/" + petition.id + ".ticket");
        oldFile.renameTo(new File(PATH + "/" + petition.id + ".ticket"));
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
            petition.log.add(new PetitionLog(moderator + ": " + message));
        }
        save(petition);
    }

}
