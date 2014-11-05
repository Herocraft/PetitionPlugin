package com.floyd.bukkit.petition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import com.floyd.bukkit.petition.storage.PetitionObject;

public class NotifierThread implements Runnable {
    private Map<String, Integer> count = new HashMap<String, Integer>();
    private PetitionPlugin plugin = null;

    public NotifierThread(PetitionPlugin owner) {
        plugin = owner;
    }

    // This method is called when the thread runs
    @Override
    public void run() {
        System.out.println("[Pe] NotifierThread started");

        // Count open petitions per player
        count.clear();
        List<PetitionObject> petitions = plugin.getStorage().list(false, null);
        for (PetitionObject petition : petitions) {
            if (petition.isValid()) {
                Integer found = count.get(petition.getOwner());
                if (found == null) { found = 0; }
                count.put(petition.getOwner(), found + 1);
            }
        }

        // Notify each player and get total
        Integer total = 0;
        for (String name : count.keySet()) {
            Integer found = count.get(name);
            total = total + found;
            Player p = plugin.getServer().getPlayer(name);
            if (p != null) {
                if (found == 1) {
                    p.sendMessage("[Pe] §7You have 1 open " + plugin.getSettings().get("single").toLowerCase() + " waiting, use '/pe list' to review");
                } else {
                    p.sendMessage("[Pe] §7You have " + found + " open " + plugin.getSettings().get("plural").toLowerCase() + " waiting, use '/pe list' to review");
                }
            }
        }

        // Notify admins about the total
        String[] except = new String[0];
        if (total > 0) {
            if (total == 1) {
                plugin.notifyModerators("[Pe] §7There is 1 open " + plugin.getSettings().get("single").toLowerCase() + " waiting, use '/pe list' to review", except);
            } else {
                plugin.notifyModerators("[Pe] §7There are " + total + " open " + plugin.getSettings().get("plural").toLowerCase() + " waiting, use '/pe list' to review", except);
            }
        }
        System.out.println("[Pe] NotifierThread stopped");
    }
}
