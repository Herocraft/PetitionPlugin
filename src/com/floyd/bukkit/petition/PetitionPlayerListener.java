package com.floyd.bukkit.petition;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.floyd.bukkit.petition.storage.DbStorage;
import com.floyd.bukkit.petition.storage.PetitionTeleport;

/**
 * Handle events for all Player related events
 * @author FloydATC
 */

public class PetitionPlayerListener implements Listener {
    private final PetitionPlugin plugin;

    public PetitionPlayerListener(PetitionPlugin instance) {
        plugin = instance;
    }

    //Insert Player related code here
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getStorage() instanceof DbStorage) {
            DbStorage dbStorage = (DbStorage) plugin.getStorage();
            PetitionTeleport teleport = dbStorage.getTeleport(player);
            if (teleport != null) {
                if (plugin.getServerName().equals(teleport.getPetition().getServer())) {
                    plugin.doTeleport(player, teleport.getPetition());
                } else {
                    plugin.respond(player, "[Pe] §7Teleport failed.");
                }
                dbStorage.deleteTeleport(teleport);
            }
        }

        // Play back messages stored in this player's maildir (if any)
        String[] messages = plugin.getMessages(player);
        if (messages.length > 0) {
            for (String message : messages) {
                player.sendMessage(message);
            }
            player.sendMessage("[Pe] §7Use /petition to view, comment or close");
        }
    }
}
