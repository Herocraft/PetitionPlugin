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

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.floyd.bukkit.petition.PetitionPlugin;

@Entity
@Table(name = "pe_object")
public class PetitionObject {
    @Id
    private Long id = 0L;
    @Version
    private Date timestamp = new Date();
    private String owner = "(Console)";
    private String title = "";
    private String world = "";
    private String server = "";
    private Double x = 0d;
    private Double y = 64d;
    private Double z = 0d;
    private Float pitch = 0.0f;
    private Float yaw = 0.0f;
    private String assignee = "*";
    @OneToMany(cascade=CascadeType.ALL)
    private List<PetitionComment> log = new ArrayList<PetitionComment>();
    private boolean closed = false;

    // Create a new petition
    public PetitionObject()
    {
        // TODO Auto-generated constructor stub
    }

    public PetitionObject(Long id, Player player, String title, String server) {
        this.id = id;
        this.title = title;
        this.server = server;

        if (player != null) {
            owner = player.getName();
            setLocation(player.getLocation());
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        this.timestamp = timestamp;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getWorld() {
        return world;    // World name
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public String getServer()
    {
        return server;
    }

    public void setServer(String server)
    {
        this.server = server;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getZ() {
        return z;
    }

    public void setZ(Double z) {
        this.z = z;
    }

    public Float getPitch() {
        return pitch;
    }

    public void setPitch(Float pitch) {
        this.pitch = pitch;
    }

    public Float getYaw() {
        return yaw;
    }

    public void setYaw(Float yaw) {
        this.yaw = yaw;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public List<PetitionComment> getLog() {
        return log;
    }

    public void setLog(List<PetitionComment> log) {
        this.log = log;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isOpen() {
        return !closed;
    }

    // Return 'true' if this is a valid petition object
    public boolean isValid() {
        return (id != null && id > 0);
    }

    public boolean isNotValid() {
        return !isValid();
    }

    public boolean isAssigned()
    {
        return assignee != null;
    }

    private String getFormattedOwner() {
        return Bukkit.getPlayerExact(owner) != null ? "§2+§f" + owner : "§4ø§f" + owner;
    }

    public boolean isOwner(Player player) {
        return player != null && getOwner().equalsIgnoreCase(player.getName());
    }

    private String getFormattedAssignee() {
        return Bukkit.getPlayerExact(assignee) != null ? "§2+§f" + assignee : "§4ø§f" + assignee;
    }

    public String getHeader() {
        int logSize = getLog().size() == 1 && StringUtils.isEmpty(getLog().get(0).getMessage()) ? 0 : getLog().size(); 
        return "§6#" + getId() + " " + getFormattedOwner() + "§7 -> " + getFormattedAssignee() + "§7: " + getTitle() + " (" + logSize + ")";
    }

    public Location getLocation() {
        List<World> worlds = Bukkit.getWorlds();
        World normal = null;
        PetitionPlugin.logger.info("Examining worlds");
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

    public void setLocation(Location location) {
        if (location != null) {
            world = location.getWorld().getName();
            x = location.getX();
            y = location.getY();
            z = location.getZ();
            pitch = location.getPitch();
            yaw = location.getYaw();
        }
    }
}
