package com.floyd.bukkit.petition.storage;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

@Entity
@Table(name = "pe_object")
public class PetitionObject {
    @Id
    Long id = 0L;
    String owner = "";
    String title = "";
    String world = "";
    Double x = 0d;
    Double y = 0d;
    Double z = 0d;
    Float pitch = 0.0f;
    Float yaw = 0.0f;
    String assignee = "*";
    @OneToMany(cascade=CascadeType.ALL)
    List<PetitionLog> log = new ArrayList<PetitionLog>();
    Boolean closed = false;

    public PetitionObject()
    {
        // TODO Auto-generated constructor stub
    }

    // Create a new petition
    public PetitionObject(Long newid, Player player, String newtitle) {
        if (player != null) {
            id = newid;
            owner = player.getName();
            title = newtitle;
            world = player.getLocation().getWorld().getName();
            x = player.getLocation().getX();
            y = player.getLocation().getY();
            z = player.getLocation().getZ();
            pitch = player.getLocation().getPitch();
            yaw = player.getLocation().getYaw();
            closed = false;
        } else { 
            id = newid;
            owner = "(Console)";
            title = newtitle;
            world = "";
            x = 64d;
            y = 64d;
            z = 64d;
            pitch = 0f;
            yaw = 0f;
            closed = false;
        }
    }

    // Return 'true' if this is a valid petition object
    public boolean isValid() {
        return (id != 0);
    }

    public Boolean isOpen() {
        return !closed;
    }

    public Boolean isClosed() {
        return closed;
    }

    public String Owner() {
        return owner;
    }

    public String Owner(Server server) {
        if (server.getPlayer(owner) == null) {
            return "§4§§f" + owner;    // Offline
        } else {
            return "§2+§f" + owner;    // Online
        }
    }

    public Boolean ownedBy(Player player) {
        if (player == null) {
            return false;
        }
        return (Owner().equalsIgnoreCase(player.getName()));
    }

    public String Title() {
        return title;
    }

    public String Assignee() {
        return assignee;
    }

    public String Assignee(Server server) {
        if (server.getPlayer(assignee) == null) {
            return "§4§§f" + assignee;        // Offline
        } else {
            return "§2+§f" + assignee;        // Online
        }
    }

    public String ID() {
        return String.valueOf(id);
    }

    public String Header(Server server) {
        return "§6#" + ID() + " " + Owner(server) + "§7 -> " + Assignee(server) + "§7: " + Title() + " (" + Log().length + ")";
    }

    public String[] Log() {
        String[] lines = new String[log.size()];
        log.toArray(lines);
        return lines;
    }

    public String World() {
        return world;    // World name
    }

    public Location getLocation(Server server) {
        List<World> worlds = server.getWorlds();
        World normal = null;
        System.out.println("Examining worlds");
        for (World w : worlds) {
            if (w.getName().equals(world)) {
                return new Location(w, x, y, z, yaw, pitch);
            }
            if (w.getName().equals("world")) {
                normal = w;
            }
        }
        // Use the first world if we can't find the right one
        return new Location(normal, x, y, z, yaw, pitch);
    }
}
