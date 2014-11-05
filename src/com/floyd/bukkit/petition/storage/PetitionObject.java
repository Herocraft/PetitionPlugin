package com.floyd.bukkit.petition.storage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

@Entity
@Table(name = "pe_object")
public class PetitionObject {
    @Id
    Long id = 0L;
    @Version
    Date timestamp;
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
    List<PetitionComment> log = new ArrayList<PetitionComment>();
    boolean closed = false;

    public PetitionObject()
    {
        // TODO Auto-generated constructor stub
    }

    // Create a new petition
    public PetitionObject(Long newid, Player player, String newtitle) {
        if (player != null) {
            id = newid;
            timestamp = new Date();
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
            timestamp = new Date();
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

    public Long getId() {
        return id;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    // Return 'true' if this is a valid petition object
    public boolean isValid() {
        return (id != null && id != 0);
    }

    public boolean isNotValid() {
        return !isValid();
    }

    public boolean isOpen() {
        return !closed;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isAssigned()
    {
        return assignee != null;
    }

    public String getOwner() {
        return owner;
    }

    private String getFormattedOwner() {
        return Bukkit.getPlayer(owner) != null ? "§2+§f" + owner : "§4ø§f" + owner;
    }

    public boolean isOwner(Player player) {
        return player != null && getOwner().equalsIgnoreCase(player.getName());
    }

    public String getTitle() {
        return title;
    }

    public String getAssignee() {
        return assignee;
    }

    private String getFormattedAssignee() {
        return Bukkit.getPlayer(assignee) != null ? "§2+§f" + assignee : "§4ø§f" + assignee;
    }

    public String getHeader() {
        return "§6#" + getId() + " " + getFormattedOwner() + "§7 -> " + getFormattedAssignee() + "§7: " + getTitle() + " (" + getLog().size() + ")";
    }

    public List<PetitionComment> getLog() {
        return log;
    }

    public String getWorld() {
        return world;    // World name
    }

    public Location getLocation() {
        List<World> worlds = Bukkit.getWorlds();
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
