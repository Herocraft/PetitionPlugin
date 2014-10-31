package com.floyd.bukkit.petition;

import java.io.*;
import java.util.Comparator;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

import com.floyd.bukkit.petition.storage.DbStorage;
import com.floyd.bukkit.petition.storage.PetitionLog;
import com.floyd.bukkit.petition.storage.PetitionObject;
import com.floyd.bukkit.petition.storage.Storage;
import com.floyd.bukkit.petition.storage.YamlStorage;

import java.util.logging.Logger;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.*;

import javax.persistence.PersistenceException;

/**
* Petition plugin for Bukkit
*
* @author FloydATC
*/

public class PetitionPlugin extends JavaPlugin {
    private NotifierThread notifier = null; 

    private final ConcurrentHashMap<Player, Boolean> debugees = new ConcurrentHashMap<Player, Boolean>();
    private final ConcurrentHashMap<Long, String> semaphores = new ConcurrentHashMap<Long, String>();
    public final ConcurrentHashMap<String, String> settings = new ConcurrentHashMap<String, String>();

    public String cache;
    Storage storage;

    String baseDir = "plugins/PetitionPlugin";
    String archiveDir = "archive";
    String mailDir = "mail";
    String ticketFile = "last_ticket_id.txt";
    String configFile = "settings.txt";
    String logFile = "petitionlog.txt";
    String fname = baseDir + "/" + logFile;
    String newline = System.getProperty("line.separator");

    public static final Logger logger = Logger.getLogger("Minecraft.PetitionPlugin");

//    public PetitionPlugin(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
//        super(pluginLoader, instance, desc, folder, plugin, cLoader);
//        // TODO: Place any custom initialization code here
//
//        // NOTE: Event registration should be done in onEnable not here as all events are unregistered when a plugin is disabled
//    }

    public void onDisable() {
        // TODO: Place any custom disable code here
        stopNotifier();

        // NOTE: All registered events are automatically unregistered when a plugin is disabled

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
        logger.info("[Pe] " + pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!");
    }

    public void onEnable() {
        // TODO: Place any custom enable code here including the registration of any events
        preFlightCheck();
        loadSettings();
        setupStorage();
        startNotifier();
        setupLog();

        // Register our events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PetitionPlayerListener(this), this);

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
        logger.info("[Pe] " + pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!");
        
    }

    @Override
    public List<Class<?>> getDatabaseClasses()
    {
        List<Class<?>> classes = super.getDatabaseClasses();
        classes.add(PetitionObject.class);
        classes.add(PetitionLog.class);
        return classes;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        String cmdname = cmd.getName().toLowerCase();
        Player player = null;
        if (sender instanceof Player) {
            player = (Player)sender;
        }

        if (cmdname.equalsIgnoreCase("pe") || cmdname.equalsIgnoreCase("petition")) {
            if (player == null || player.hasPermission("petition.pe")) {
                // Help
                if (args.length == 0) {
                    performHelp(player);
                    return true;
                }
                if (args.length >= 1) {
                    // List
                    if (args[0].equalsIgnoreCase("list")) {
                        performList(player, args);
                        return true;
                    }
                }
                if (args.length >= 2) {
                    // View
                    if (args[0].equalsIgnoreCase("view")) {
                        performView(player, args);
                        return true;
                    }
                    // Assign
                    if (args[0].equalsIgnoreCase("assign")) {
                        performAssign(player, args);
                        return true;
                    }
                    // Unassign
                    if (args[0].equalsIgnoreCase("unassign")) {
                        performUnassign(player, args);
                        return true;
                    }
                    // Close
                    if (args[0].equalsIgnoreCase("close")) {
                        performClose(player, args);
                        return true;
                    }
                    // Reopen
                    if (args[0].equalsIgnoreCase("reopen")) {
                        performReopen(player, args);
                        return true;
                    }
                    // Comment
                    if (args[0].equalsIgnoreCase("comment") || args[0].equalsIgnoreCase("log")) {
                        performComment(player, args);
                        return true;
                    }
                    // Open
                    if (args[0].equalsIgnoreCase("open") || args[0].equalsIgnoreCase("new") || args[0].equalsIgnoreCase("create")) {
                        performOpen(player, args);
                        return true;
                    }
                    // Warp
                    if (args[0].equalsIgnoreCase("warp") || args[0].equalsIgnoreCase("goto")) {
                        performWarp(player, args);
                        return true;
                    }
                }
            } else {
                   logger.info("[Pe] Access denied for " + player.getName());
                   player.sendMessage(ChatColor.DARK_RED + "You do not have permission to use this command");
            }
        }
        return true;
    }

    private void performWarp(Player player, String[] args) {
        Long id = Long.valueOf(args[1]);
        Boolean moderator = false;
        String name = "(Console)";
        if (player == null) {
            respond(player, "[Pe] That would be a neat trick.");
            return;
        } else {
               name = player.getName();
        }
        if (player.hasPermission("petition.moderate")) {
            moderator = true;
        }
        try {
            getLock(id, player);
            PetitionObject petition = storage.load(id);
            if (petition.isValid() && (petition.isOpen() || moderator)) {
                if (canWarpTo(player, petition)) {
                    respond(player, "[Pe] §7" + petition.Header(getServer()));
                    if (player.teleport(petition.getLocation(getServer()))) {
                        respond(player, "[Pe] §7Teleporting you to where the " + settings.get("single").toLowerCase() + " was opened");
                        logger.info(name + " teleported to " + settings.get("single").toLowerCase() + id);
                    } else {
                        respond(player, "[Pe] §7Teleport failed");
                        logger.info(name + " teleport to " + settings.get("single").toLowerCase() + id + " FAILED");
                    }
                } else {
                    logger.info("[Pe] Access to warp to #" + id + " denied for " + name);
                    respond(player, "§4[Pe] Access denied.");
                }
            } else {
                respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found.");
            }
        }
        finally {
            releaseLock(id, player);
        }
    }

    private void performOpen(Player player, String[] args) {
        Long id = IssueUniqueTicketID();
        String name = "(Console)";
        if (player != null) {
            name = player.getName();
        }
        try {
            getLock(id, player);
            String title = "";
            Integer index = 1;
            while (index < args.length) {
                title = title.concat(" " + args[index]);
                index++;
            }
            if (title.length() > 0) {
                title = title.substring(1);
            }
            PetitionObject petition = storage.create(id, player, title);
            releaseLock(id, player);
            if (petition.isValid()) {
                respond(player, "[Pe] §7Thank you, your ticket is §6#" + petition.ID() + "§7. (Use '/petition' to manage it)");
                String[] except = { petition.Owner() };
                notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + petition.ID() + "§7 opened by " + name + ": " + title, except);
                logger.info(name + " opened " + settings.get("single").toLowerCase() + " #" + id + ". " + title);
                logAction(name + " opened " + settings.get("single").toLowerCase() + " #" + id + ". " + title);
            } else {
                respond(player, "§4[Pe] There was an error creating your ticket, please try again later.");
                System.out.println("[Pe] ERROR: PetitionPlugin failed to create a ticket, please check that plugins/PetitionPlugin exists and is writeable!");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void performComment(Player player, String[] args) {
        Long id = Long.valueOf(args[1]);
        Boolean moderator = false;
        if (player == null || player.hasPermission("petition.moderate")) {
            moderator = true;
        }
        String name = "(Console)";
        if (player != null) {
            name = player.getName();
        }
        try {
            getLock(id, player);
            PetitionObject petition = storage.load(id);
            if (petition.isValid() && petition.isOpen()) {
                if (petition.ownedBy(player) || moderator) {
                    String message = "";
                    Integer index = 2;
                    while (index < args.length) {
                        message = message.concat(" " + args[index]);
                        index++;
                    }
                    if (message.length() > 0) {
                        message = message.substring(1);
                    }
                    // Notify
                    notifyNamedPlayer(petition.Owner(), "[Pe] §7Your " + settings.get("single").toLowerCase() + " §6#" + id + "§7 was updated: " + message);
                    notifyNamedPlayer(petition.Assignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been updated by " + name + ".");
                    String[] except = { petition.Owner(), petition.Assignee() };
                    notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 comment added by " + name + ".", except);
                    storage.comment(petition, player, message);
                    logger.info(name + " commented " + settings.get("single").toLowerCase() + " #" + id + ". " + message);
                    logAction(name + " commented " + settings.get("single").toLowerCase() + " #" + id + ". " + message);
                } else {
                    logger.info("[Pe] Access to comment on #" + id + " denied for " + name);
                }
            } else {
                respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found.");
            }
        }
        finally {
            releaseLock(id, player);
        }
    }

    private void performClose(Player player, String[] args) {
        Long id = Long.valueOf(args[1]);
        Boolean moderator = false;
        if (player == null || player.hasPermission("petition.moderate")) {
            moderator = true;
        }
        String name = "(Console)";
        if (player != null) {
            name = player.getName();
        }
        try {
            getLock(id, player);
            PetitionObject petition = storage.load(id);
            if (petition.isValid() && petition.isOpen()) {
                if (petition.ownedBy(player) || moderator) {
                    String message = "";
                    Integer index = 2;
                    while (index < args.length) {
                        message = message.concat(" " + args[index]);
                        index++;
                    }
                    if (message.length() > 0) {
                        message = message.substring(1);
                    }
                    // Notify
                    notifyNamedPlayer(petition.Owner(), "[Pe] §7Your " + settings.get("single").toLowerCase() + " §6#" + id + "§7 was closed. " + message);
                    notifyNamedPlayer(petition.Assignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 was closed by " + name + ".");
                    String[] except = { petition.Owner(), petition.Assignee() };
                    // Implement 'notify-all-on-close'
                    if (Boolean.parseBoolean(settings.get("single"))) {
                        notifyAll("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 was closed.", except);
                    } else {
                        notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 was closed. " + message, except);
                    }
                    storage.close(petition, player, message);
                    logger.info(name + " closed " + settings.get("single").toLowerCase() + " #" + id + ". " + message);
                    logAction(name + " closed " + settings.get("single").toLowerCase() + " #" + id + ". " + message);
                } else {
                    logger.info("[Pe] Access to close #" + id + " denied for " + name);
                }
            } else {
                respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found.");
            }
        }
        finally {
            releaseLock(id, player);
        }
    }

    private void performReopen(Player player, String[] args) {
        Long id = Long.valueOf(args[1]);
        Boolean moderator = false;
        if (player == null || player.hasPermission("petition.moderate")) {
            moderator = true;
        }
        String name = "(Console)";
        if (player != null) {
            name = player.getName();
        }
        try {
            getLock(id, player);
            PetitionObject petition = storage.load(id);
            if (petition.isValid() && petition.isClosed()) {
                if (moderator) {
                    String message = "";
                    Integer index = 2;
                    while (index < args.length) {
                        message = message.concat(" " + args[index]);
                        index++;
                    }
                    if (message.length() > 0) {
                        message = message.substring(1);
                    }
                    // Notify
                    notifyNamedPlayer(petition.Owner(), "[Pe] §7Your " + settings.get("single").toLowerCase() + " §6#" + id + "§7 was reopened. " + message);
                    notifyNamedPlayer(petition.Assignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 was reopened by " + name + ".");
                    String[] except = { petition.Owner(), petition.Assignee() };
                    notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 was reopened. " + message, except);
                    storage.reopen(petition, player, message);
                    logger.info(name + " reopened " + settings.get("single").toLowerCase() + " #" + id + ". " + message);
                    logAction(name + " reopened " + settings.get("single").toLowerCase() + " #" + id + ". " + message);
                } else {
                    logger.info("[Pe] Access to reopen #" + id + " denied for " + name);
                }
            } else {
                respond(player, "§4[Pe] No closed " + settings.get("single").toLowerCase() + " #" + args[1] + " found.");
            }
        }
        finally {
            releaseLock(id, player);
        }
    }

    private void performUnassign(Player player, String[] args) {
        Long id = Long.valueOf(args[1]);
        Boolean moderator = false;
        if (player == null || player.hasPermission("petition.moderate")) {
            moderator = true;
        }
        String name = "(Console)";
        if (player != null) {
            name = player.getName();
        }
        if (!moderator) {
            logger.info("[Pe] Access to unassign #" + id + " denied for " + name);
            respond(player, "§4[Pe] Only moderators may unassign "+settings.get("plural"));
            return;
        }
        try {
            getLock(id, player);
            PetitionObject petition = storage.load(id);
            if (petition.isValid() && petition.isOpen()) {
                storage.unassign(petition, player);
                // Notify
                if (Boolean.parseBoolean(settings.get("notify-owner-on-unassign"))) {
                    notifyNamedPlayer(petition.Owner(), "[Pe] §7Your " + settings.get("single").toLowerCase() + " §6#" + id + "§7 has been unassigned.");
                }
                notifyNamedPlayer(petition.Assignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been unassigned from you by " + name + ".");
                String[] except = { petition.Owner(), petition.Assignee() };
                notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 unassigned by " + name + ".", except);
                logger.info(name + " unassigned " + settings.get("single").toLowerCase() + " #" + id);
                logAction(name + " unassigned " + settings.get("single").toLowerCase() + " #" + id);
            } else {
                respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found.");
            }
        }
        finally {
            releaseLock(id, player);
        }
    }

    private void performAssign(Player player, String[] args) {
        Long id = Long.valueOf(args[1]);
        Boolean moderator = false;
        if (player == null || player.hasPermission("petition.moderate")) {
            moderator = true;
        }
        String name = "(Console)";
        if (player != null) {
            name = player.getName();
        }
        if (!moderator) {
            logger.info("[Pe] Access to assign #" + id + " denied for " + name);
            respond(player, "§4[Pe] Only moderators may assign "+settings.get("plural"));
            return;
        }
        try {
            getLock(id, player);
            PetitionObject petition = storage.load(id);
            if (petition.isValid() && petition.isOpen()) {
                if (args.length == 3) {
                    // Assign to named player
                    storage.assign(petition, player, args[2]);
                } else {
                    // Assign to self
                    storage.assign(petition, player, name);
                }
                // Notify
                if (Boolean.parseBoolean(settings.get("notify-owner-on-assign"))) {
                    notifyNamedPlayer(petition.Owner(), "[Pe] §7Your " + settings.get("single").toLowerCase() + " §6#" + id + "§7 assigned to " + petition.Assignee() + ".");
                }
                notifyNamedPlayer(petition.Assignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been assigned to you by " + name + ".");
                String[] except = { petition.Owner(), petition.Assignee() };
                notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been assigned to " + petition.Assignee() + ".", except);
                logger.info(name + " assigned " + settings.get("single").toLowerCase() + " #" + id + " to " + petition.Assignee());
                logAction(name + " assigned " + settings.get("single").toLowerCase() + " #" + id + " to " + petition.Assignee());
            } else {
                respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found.");
            }
        }
        finally {
            releaseLock(id, player);
        }
    }

    private void performView(Player player, String[] args) {
        Boolean moderator = false;
        if (player == null || player.hasPermission("petition.moderate")) {
            moderator = true;
        }
        String name = "(Console)";
        if (player != null) {
            name = player.getName();
        }
        Long id = Long.valueOf(args[1]);
        try {
            getLock(id, player);
            PetitionObject petition = storage.load(id);
            if (petition.isValid()) {
                if (petition.ownedBy(player) || moderator) {
                    respond(player, "[Pe] §7" + petition.Header(getServer()));
                    for (String line : petition.Log()) {
                        respond(player, "[Pe] §6#" + petition.ID() + " §7" + line);
                    }
                } else {
                    logger.info("[Pe] Access to view #" + id + " denied for " + name);
                }
            } else {
                respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found.");
            }
        }
        finally {
            releaseLock(id, player);
        }
    }

    private void performList(Player player, String[] args) {
        Integer count = 0;
        Integer showing = 0;
        Integer limit = 10;
        Boolean include_offline = true;
        Boolean include_online = true;
        Boolean use_archive = false;
        Boolean sort_reverse = false;
        Boolean ignore_assigned = false;
        Pattern pattern = null;
        String filter = "";
        if (args.length >= 2) {
            for (Integer index = 1; index < args.length; index++) {
                if (args[index].equalsIgnoreCase("closed")) {
                    use_archive = true;
                } else if (args[index].equalsIgnoreCase("newest")) {
                    sort_reverse = true;
                } else if (args[index].equalsIgnoreCase("unassigned")) {
                    ignore_assigned = true;
                } else if (args[index].equalsIgnoreCase("online")) {
                    include_offline = false;
                } else if (args[index].equalsIgnoreCase("offline")) {
                    include_online = false;
                } else if (args[index].matches("^\\d+$")) {
                    limit = Integer.valueOf(args[index]);
                } else {
                    filter = args[index];
                    pattern = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);
                }
            }
        }
        Boolean moderator = false;
        if (player == null || player.hasPermission("petition.moderate")) {
            moderator = true;
        }

        File dir;
        if (use_archive) {
            dir = new File(baseDir + "/" + archiveDir);
        } else {
            dir = new File(baseDir);
        }
        String[] filenames = dir.list();
        // Sort the filenames in numerical order
        // OMG there _has_ to be a more efficient way to do this...!
        Comparator<String> numerical = new Comparator<String>() {
            public int compare(final String o1, final String o2) {
                // logger.info("Comparing " + o1 + " to " + o2);
                String[] parts1 = o1.split("\\.");
                String[] parts2 = o2.split("\\.");
                Integer int1 = 0;
                try {
                    int1 = Integer.parseInt(parts1[0]);
                }
                catch (Exception e) {
                }
                Integer int2 = 0;
                try {
                    int2 = Integer.parseInt(parts2[0]);
                }
                catch (Exception e) {
                }
                // logger.info("Stripped values are " + int1 + " and " + int2);
                if (int1 < int2) { return -1; }
                if (int1 > int2) { return 1; }
                return 0;
            }
        };
        Arrays.sort(filenames, numerical);
        if (sort_reverse) {
            filenames = reverseOrder(filenames);
        }

        if (filenames != null) {
            for (String filename : filenames) {
                if (filename.endsWith(".ticket")) {
                    String[] parts = filename.split("['.']");
                    Long id = Long.valueOf(parts[0]);
                    try {
                        getLock(id, player);
                        PetitionObject petition = storage.load(id);
                        if (petition.isValid() && (petition.ownedBy(player) || moderator)) {
                            Boolean ignore = false;
                            Player p = getServer().getPlayer(petition.Owner());
                            if (p == null && !include_offline) {
                                ignore = true;
                            }
                            if (p != null && !include_online) {
                                ignore = true;
                            }
                            if (pattern != null) {
                                Matcher matcher = pattern.matcher(petition.Header(getServer()));
                                if (!matcher.find()) {
                                    ignore = true;
                                }
                            }
                            if (!petition.Assignee().matches("\\*") && ignore_assigned) {
                                ignore = true;
                            }
                            if (!ignore) {
                                if (count < limit) {
                                    respond(player, "[Pe] " + petition.Header(getServer()));
                                    showing++;
                                }
                                count++;
                            }
                        }
                    }
                    finally {
                        releaseLock(id, player);
                    }
                }
            }
        }
        respond(player, "[Pe] §7"+(use_archive?"Closed":"Open")+" " + settings.get("plural").toLowerCase() + (pattern==null?"":new StringBuilder(" matching ").append(filter).toString()) + ": " + count + " (Showing " + showing + ")");
    }

    private void performHelp(Player player) {
        String cmd = "pe";
        Boolean moderator = false;
        if (player == null || player.hasPermission("petition.moderate")) {
            moderator = true;
        }

        respond(player, "[Pe] §7" + settings.get("single") + " usage:");
        respond(player, "[Pe] §7/" + cmd + " open|create|new <Message>");
        respond(player, "[Pe] §7/" + cmd + " comment|log <#> <Message>");
        respond(player, "[Pe] §7/" + cmd + " close <#> [<Message>]");
        respond(player, "[Pe] §7/" + cmd + " list [online|offline|newest|closed|unassigned] [<count>|<pattern>]");
        respond(player, "[Pe] §7/" + cmd + " view <#>");
        if (canWarpAtAll(player)) {
            respond(player, "[Pe] §7/" + cmd + " warp|goto <#>");
        }
        if (moderator) {
            respond(player, "[Pe] §7/" + cmd + " assign <#> [<Operator>]");
            respond(player, "[Pe] §7/" + cmd + " unassign <#>");
            respond(player, "[Pe] §7/" + cmd + " reopen <#> [<Message>]");
        }
    }

    public boolean isDebugging(final Player player) {
        if (debugees.containsKey(player)) {
            return debugees.get(player);
        } else {
            return false;
        }
    }

    public void setDebugging(final Player player, final boolean value) {
        debugees.put(player, value);
    }

    public synchronized boolean SetPetitionLock(Long id, String owner, Boolean release) {
        if (!release) {
            // Check for lingering lock by the same player
            if (semaphores.containsKey(id) && semaphores.get(id).equals(owner)) {
                logger.severe("[Pe] INTERNAL ERROR! Petition #" + id + " is ALREADY locked by " + semaphores.get(id));
                logger.severe("[Pe] This was probably caused by a previous crash while accessing this petition.");
                logger.severe("[Pe] Please report this issue to the plugin author.");
                return true;
            }
            // Get lock
            if (semaphores.containsKey(id)) {
                logger.warning("[Pe] Denied " + owner + " lock on #" + id + "; currently locked by " + semaphores.get(id));
            } else {
                semaphores.put(id, owner);
                return true;
            }
        } else if (semaphores.containsKey(id) && semaphores.get(id) == owner) {
            // Release lock
            semaphores.remove(id);
            return true;
        }
        return false;
    }

    public synchronized Long IssueUniqueTicketID() {
        String fname = baseDir + "/" + ticketFile;
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

        logger.fine("[Pe] Issued ticket #" + line);
        return Long.valueOf(line);
    }

    public void setupLog() {
        String fname = baseDir + "/" + logFile;

        // Read the current file (if it exists)
        try {
            File f = new File(fname);
            if (!f.exists()) {
                f.createNewFile();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logAction(String line) {
        // Timestamp events
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("MMMM d, yyyy, H:mm");
        String out = "[" + format.format(now) + "] " + line + newline;

        try {
            if (cache == null) {
                cache = readLog();
            }
            // Newest log messages at top
            cache = out + cache;

            // Write to cache
            BufferedWriter output = new BufferedWriter(new FileWriter(fname));
            output.write(cache);
            output.flush();
            output.close();
        } catch (IOException ioe) {
            logger.severe("[Pe] Error writing to the log file!");
        }
        logger.fine("[Pe] Logged action of #" + line);
    }

    public String readLog() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fname)));
        String read = null;
        StringBuilder out = new StringBuilder();

        // Reads the existing log
        while ((read = reader.readLine()) != null) {
            out.append(read).append(newline);
        }
        reader.close();
        return out.toString();
    }

    private void loadSettings() {
        String fname = baseDir + "/" + configFile;
        String line = null;

        // Load the settings hash with defaults
        settings.put("single", "Petition");
        settings.put("plural", "Petitions");

        settings.put("notify-all-on-close", "false");
        settings.put("notify-owner-on-assign", "false");
        settings.put("notify-owner-on-unassign", "false");

        settings.put("notify-interval-seconds", "300");

        settings.put("warp-requires-permission", "false");
        
        settings.put("storage-type", "yaml");
        // Read the current file (if it exists)
        try {
            BufferedReader input =  new BufferedReader(new FileReader(fname));
            while ((line = input.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("#") && line.contains("=")) {
                    String[] pair = line.split("=", 2);
                    settings.put(pair[0], pair[1]);
                    if (pair[0].equals("command") || pair[0].equals("commandalias")) {
                        logger.warning("[Pe] Warning: The '" + pair[0] + "' setting has been deprecated and no longer has any effect");
                    }
                }
            }
            input.close();
        }
        catch (FileNotFoundException e) {
            logger.warning("[Pe] Error reading " + e.getLocalizedMessage() + ", using defaults");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupStorage() {
        if ("db".equalsIgnoreCase(settings.get("storage-type"))) {
            storage = new DbStorage(getDatabase());
            initDb();
        } else {
            storage = new YamlStorage();
        }
    }

    private void initDb() {
        try {
            this.getDatabase().find(PetitionObject.class).findRowCount();
        }
        catch (PersistenceException e) {
            this.installDDL();
        }
    }

    private void preFlightCheck() {
        String fname = "";
        File f;

        // Ensure that baseDir exists
        fname = baseDir;
        f = new File(fname);
        if (!f.exists() && f.mkdir()) {
            logger.info("[Pe] Created directory '" + fname + "'");
        }
        // Ensure that archiveDir exists
        fname = baseDir + "/" + archiveDir;
        f = new File(fname);
        if (!f.exists() && f.mkdir()) {
            logger.info("[Pe] Created directory '" + fname + "'");
        }
        // Ensure that mailDir exists
        fname = baseDir + "/" + mailDir;
        f = new File(fname);
        if (!f.exists() && f.mkdir()) {
            logger.info("[Pe] Created directory '" + fname + "'");
        }
        // Ensure that configFile exists
        fname = baseDir + "/" + configFile;
        f = new File(fname);
        if (!f.exists()) {
            // Ensure that configFile exists
            BufferedWriter output;
            String newline = System.getProperty("line.separator");
            try {
                output = new BufferedWriter(new FileWriter(fname));
                output.write("single=Petition" + newline);
                output.write("plural=Petitions" + newline);
                output.write("notify-all-on-close=false" + newline);
                output.write("notify-owner-on-assign=true" + newline);
                output.write("notify-owner-on-unassign=true" + newline);
                output.write("notify-interval-seconds=300" + newline);
                output.write("warp-requires-permission=false" + newline);
                output.write("storage-type=yaml");
                output.close();
                logger.info("[Pe] Created config file '" + fname + "'");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Ensure that ticketFile exists
        fname = baseDir + "/" + ticketFile;
        f = new File(fname);
        if (!f.exists()) {
            // Ensure that configFile exists
            String newline = System.getProperty("line.separator");
            BufferedWriter output;
            try {
                output = new BufferedWriter(new FileWriter(fname));
                output.write("0" + newline);
                output.close();
                logger.info("[Pe] Created ticket file '" + fname + "'");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Ensure that logFile exists
        fname = baseDir + "/" + logFile;
        f = new File(fname);
        if (!f.exists()) {
            // Ensure that configFile exists
            BufferedWriter output;
            try {
                output = new BufferedWriter(new FileWriter(fname));
                output.write("");
                output.close();
                logger.info("[Pe] Created log file '" + fname + "'");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void getLock(Long id, Player player) {    
        String name = "";
        if (player != null) {
            name = player.getName();
        }
        while (!SetPetitionLock(id, name, false)) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                logger.warning("[Pe] Sleep interrupted while waiting for lock");
            }
        }
    }

    private void releaseLock(Long id, Player player) {
        String name = "";
        if (player != null) {
            name = player.getName();
        }
           SetPetitionLock(id, name, true);
    }

    private void notifyNamedPlayer(String name, String message) {
        // Ignore broken filenames -- should probably be improved
        if (name.equals("") || name.equals("*") || name.equalsIgnoreCase("(Console)")) {
            return;
        }
        Player[] players = getServer().getOnlinePlayers();
        Boolean online = false;
        for (Player player: players) {
            if (player.getName().equalsIgnoreCase(name)) {
                player.sendMessage(message);
                online = true;
            }
        }
        if (!online) {
            name = name.toLowerCase();
            String fname;
            File f;
            // Ensure that player's mailDir exists
            fname = baseDir + "/" + mailDir + "/" + name;
            f = new File(fname);
            if (!f.exists() && f.mkdir()) {
                logger.info("[Pe] Created directory '" + fname + "'");
            }
            // Ensure that player's mailDir tmp exists
            fname = baseDir + "/" + mailDir + "/" + name + "/tmp";
            f = new File(fname);
            if (!f.exists() && f.mkdir()) {
                logger.info("[Pe] Created directory '" + fname + "'");
            }
            // Ensure that player's mailDir inbox exists
            fname = baseDir + "/" + mailDir + "/" + name + "/inbox";
            f = new File(fname);
            if (!f.exists() && f.mkdir()) {
                logger.info("[Pe] Created directory '" + fname + "'");
            }
            // Create a unique file in tmp
            UUID uuid = UUID.randomUUID();
            fname = baseDir + "/" + mailDir + "/" + name + "/tmp/" + uuid;
            String fname_final = baseDir + "/" + mailDir + "/" + name + "/inbox/" + uuid;
            BufferedWriter output;
            String newline = System.getProperty("line.separator");
            try {
                output = new BufferedWriter(new FileWriter(fname));
                output.write(message + newline);
                output.close();
                // Move the file into player's inbox
                f = new File(fname);
                f.renameTo(new File(fname_final));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void notifyModerators(String message, String[] exceptlist) {
        Player[] players = getServer().getOnlinePlayers();
        for (Player player: players) {
            if (player.hasPermission("petition.moderate")) {
                Boolean skip = false;
                for (String except: exceptlist) {
                    if (player.getName().toLowerCase().equals(except.toLowerCase())) {
                        skip = true;
                    }
                }
                if (!skip) {
                    player.sendMessage(message);
                }
            }
        }
    }

    public void notifyAll(String message, String[] exceptlist) {
        Player[] players = getServer().getOnlinePlayers();
        for (Player player: players) {
            Boolean skip = false;
            for (String except: exceptlist) {
                if (player.getName().toLowerCase().equals(except.toLowerCase())) {
                    skip = true;
                }
            }
            if (!skip) {
                player.sendMessage(message);
            }
        }
    }

    public String[] getMessages(Player player) {
        String[] messages = new String[0];
        String name = player.getName().toLowerCase();
        String pname = baseDir + "/" + mailDir + "/" + name + "/inbox";
        File dir = new File(pname);
        String[] filenames = dir.list();
        if (filenames != null) {
            messages = new String[filenames.length];
            Integer index = 0;
            for (String fname : filenames) {
                try {
                    BufferedReader input =  new BufferedReader(new FileReader(pname + "/" + fname));
                    messages[index] = input.readLine();
                    input.close();
                    boolean success = (new File(pname + "/" + fname)).delete();
                    if (!success) {
                        logger.warning("[Pe] Could not delete " + pname + "/" + fname);
                    }
                }
                catch (FileNotFoundException e) {
                    logger.warning("[Pe] Unexpected error reading " + e.getLocalizedMessage());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                index++;
            }
        }
        return messages;
    }

    private void respond(Player player, String message) {
        if (player == null) {
            // Strip color codes
            Pattern pattern = Pattern.compile("\\§[0-9a-f]");
            Matcher matcher = pattern.matcher(message);
            message = matcher.replaceAll("");
            // Print message to console
            System.out.println(message);
        } else {
            player.sendMessage(message);
        }
    }

    // This method is invoked when showing help
    // Check if there are situations where this player can warp
    private Boolean canWarpAtAll(Player player) {
        // Check if this limit is enabled at all
        if (!Boolean.parseBoolean(settings.get("warp-requires-permission"))) {
            return true;
        }
        // Check who is asking
        if (player == null) {
            return true;    // Console
        }
        // Moderator?
        if (player.hasPermission("petition.moderator")) {
            return true;
        }
        if (player.hasPermission("petition.warp-to-own-if-assigned")) {
            return true;
        }
        if (player.hasPermission("petition.warp-to-own")) {
            return true;
        }
        return false;
    }

    // This method is invoked ONLY when a player is attempting to warp to a petition location
    // Implements a set of rules for warp access
    private Boolean canWarpTo(Player player, PetitionObject petition) {
        // Check who is asking
        if (player == null) {
            return true;    // Console
        }
        // Moderator?
        if (player.hasPermission("petition.moderator")) {
            return true;
        }
        // Player owns this petition?
        if (!petition.ownedBy(player)) {
            return false;
        }
        // Check for limitations
        if (!Boolean.parseBoolean(settings.get("warp-requires-permission"))) {
            return true;
        }
        // Player owns this petition, is that sufficient?
        if (player.hasPermission("petition.warp-to-own")) {
            return true;
        }
        // Our last chance is that the petition has been assigned
        if (petition.Assignee().equals("*")) {
            return false;
        }
        // It has been assigned, is that sufficient?
        if (player.hasPermission("petition.warp-to-own-assigned")) {
            return true;
        }
        String[] except = { petition.Owner() };
        notifyModerators("[Pe] " + player.getName() + " requested warp access to " + settings.get("single").toLowerCase() + " #" + petition.ID(), except);
        return false;
    }

    private String[] reverseOrder(String[] list) {
        String[] newlist = new String[list.length];
        Integer i = list.length-1;
        for (String item : list) {
            newlist[i] = item;
            i--;
        }
        return newlist;
    }

    private void startNotifier() {
        Integer seconds = 0;
        notifier = new NotifierThread(this);
        try {
            seconds = Integer.parseInt(settings.get("notify-interval-seconds"));
        }
        catch (Exception e) {
            logger.warning("[Pe] Error parsing option 'notify-interval-seconds'; must be an integer.");
            logger.warning("[Pe] Using default value (300)");
        }
        if (seconds > 0) {
            notifier.setInterval(seconds);
            notifier.start();
        } else {
            logger.info("[Pe] Notification thread disabled");
        }
    }

    private void stopNotifier() {
        if (notifier != null) {
            notifier.signalStop();
        }
    }

    public Storage getStorage()
    {
        return storage;
    }
}
