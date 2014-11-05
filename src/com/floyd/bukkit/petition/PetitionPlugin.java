package com.floyd.bukkit.petition;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.PersistenceException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.floyd.bukkit.petition.storage.DbStorage;
import com.floyd.bukkit.petition.storage.PetitionComment;
import com.floyd.bukkit.petition.storage.PetitionObject;
import com.floyd.bukkit.petition.storage.Storage;
import com.floyd.bukkit.petition.storage.TextStorage;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
* Petition plugin for Bukkit
*
* @author FloydATC
*/

public class PetitionPlugin extends JavaPlugin {
    private BukkitTask task;

    private final Map<Player, Boolean> debugees = new ConcurrentHashMap<Player, Boolean>();
    private final Map<String, String> settings = new ConcurrentHashMap<String, String>();

    Storage storage;
    ActionLog actionLog;

    public static final String BASE_DIR = "plugins/PetitionPlugin";
    public static final String ARCHIVE_DIR = BASE_DIR + File.separator + "archive";
    public static final String MAIL_DIR = BASE_DIR + File.separator + "mail";
    public static final String CONFIG_FILE = BASE_DIR + File.separator + "settings.txt";

    public static final Logger logger = Logger.getLogger("Minecraft.PetitionPlugin");

    private static final String CONSOLE_NAME = "(Console)";
    private static final int TPS = 20;

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
        PluginManager pm = Bukkit.getPluginManager();
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
        classes.add(PetitionComment.class);
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
        Long id = 0L;
        try {
            id = Long.valueOf(args[1]);
        } catch (NumberFormatException e) {
            respond(player, "[Pe] Syntax error.");
            return;
        }
        if (player == null) {
            respond(player, "[Pe] That would be a neat trick.");
            return;
        }
        String name = player.getName();
        Boolean moderator = player.hasPermission("petition.moderate");
        
        PetitionObject petition = storage.load(id);
        if (petition.isValid() && (petition.isOpen() || moderator)) {
            if (canWarpTo(player, petition)) {
                respond(player, "[Pe] §7" + petition.getHeader());
                if (player.teleport(petition.getLocation())) {
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

    private void performOpen(Player player, String[] args) {
        String name = player != null ? player.getName() : CONSOLE_NAME;
        String title = "";
        Integer index = 1;
        while (index < args.length) {
            title = title.concat(" " + args[index]);
            index++;
        }
        if (title.length() > 0) {
            title = title.substring(1);
        }
        PetitionObject petition = storage.create(player, title);
        Long id = petition.getId();
        if (petition.isValid()) {
            respond(player, "[Pe] §7Thank you, your ticket is §6#" + petition.getId() + "§7. (Use '/petition' to manage it)");
            String[] except = { petition.getOwner() };
            notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + petition.getId() + "§7 opened by " + name + ": " + title, except);
            logger.info(name + " opened " + settings.get("single").toLowerCase() + " #" + id + ". " + title);
            actionLog.logAction(name + " opened " + settings.get("single").toLowerCase() + " #" + id + ". " + title);
        } else {
            respond(player, "§4[Pe] There was an error creating your ticket, please try again later.");
            System.out.println("[Pe] ERROR: PetitionPlugin failed to create a ticket, please check that plugins/PetitionPlugin exists and is writeable!");
        }
    }

    private void performComment(Player player, String[] args) {
        Long id = 0L;
        try {
            id = Long.valueOf(args[1]);
        } catch (NumberFormatException e) {
            respond(player, "[Pe] Syntax error.");
            return;
        }
        Boolean moderator = player == null || player.hasPermission("petition.moderate");
        String name = player != null ? player.getName() : CONSOLE_NAME;
        PetitionObject petition = storage.load(id);
        if (petition.isValid() && petition.isOpen()) {
            if (petition.isOwner(player) || moderator) {
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
                notifyNamedPlayer(petition.getOwner(), "[Pe] §7Your " + settings.get("single").toLowerCase() + " §6#" + id + "§7 was updated: " + message);
                notifyNamedPlayer(petition.getAssignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been updated by " + name + ".");
                String[] except = { petition.getOwner(), petition.getAssignee() };
                notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 comment added by " + name + ".", except);
                storage.comment(petition, player, message);
                logger.info(name + " commented " + settings.get("single").toLowerCase() + " #" + id + ". " + message);
                actionLog.logAction(name + " commented " + settings.get("single").toLowerCase() + " #" + id + ". " + message);
            } else {
                logger.info("[Pe] Access to comment on #" + id + " denied for " + name);
            }
        } else {
            respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found.");
        }
    }

    private void performClose(Player player, String[] args) {
        Long id = 0L;
        try {
            id = Long.valueOf(args[1]);
        } catch (NumberFormatException e) {
            respond(player, "[Pe] Syntax error.");
            return;
        }
        Boolean moderator = player == null || player.hasPermission("petition.moderate");
        String name = player != null ? player.getName() : CONSOLE_NAME;
        PetitionObject petition = storage.load(id);
        if (petition.isValid() && petition.isOpen()) {
            if (petition.isOwner(player) || moderator) {
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
                notifyNamedPlayer(petition.getOwner(), "[Pe] §7Your " + settings.get("single").toLowerCase() + " §6#" + id + "§7 was closed. " + message);
                notifyNamedPlayer(petition.getAssignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 was closed by " + name + ".");
                String[] except = { petition.getOwner(), petition.getAssignee() };
                // Implement 'notify-all-on-close'
                if (Boolean.parseBoolean(settings.get("single"))) {
                    notifyAll("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 was closed.", except);
                } else {
                    notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 was closed. " + message, except);
                }
                storage.close(petition, player, message);
                logger.info(name + " closed " + settings.get("single").toLowerCase() + " #" + id + ". " + message);
                actionLog.logAction(name + " closed " + settings.get("single").toLowerCase() + " #" + id + ". " + message);
            } else {
                logger.info("[Pe] Access to close #" + id + " denied for " + name);
            }
        } else {
            respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found.");
        }
    }

    private void performReopen(Player player, String[] args) {
        Long id = 0L;
        try {
            id = Long.valueOf(args[1]);
        } catch (NumberFormatException e) {
            respond(player, "[Pe] Syntax error.");
            return;
        }
        Boolean moderator = player == null || player.hasPermission("petition.moderate");
        String name = player != null ? player.getName() : CONSOLE_NAME;
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
                notifyNamedPlayer(petition.getOwner(), "[Pe] §7Your " + settings.get("single").toLowerCase() + " §6#" + id + "§7 was reopened. " + message);
                notifyNamedPlayer(petition.getAssignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 was reopened by " + name + ".");
                String[] except = { petition.getOwner(), petition.getAssignee() };
                notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 was reopened. " + message, except);
                storage.reopen(petition, player, message);
                logger.info(name + " reopened " + settings.get("single").toLowerCase() + " #" + id + ". " + message);
                actionLog.logAction(name + " reopened " + settings.get("single").toLowerCase() + " #" + id + ". " + message);
            } else {
                logger.info("[Pe] Access to reopen #" + id + " denied for " + name);
            }
        } else {
            respond(player, "§4[Pe] No closed " + settings.get("single").toLowerCase() + " #" + args[1] + " found.");
        }
    }

    private void performUnassign(Player player, String[] args) {
        Long id = 0L;
        try {
            id = Long.valueOf(args[1]);
        } catch (NumberFormatException e) {
            respond(player, "[Pe] Syntax error.");
            return;
        }
        Boolean moderator = player == null || player.hasPermission("petition.moderate");
        String name = player != null ? player.getName() : CONSOLE_NAME;
        if (!moderator) {
            logger.info("[Pe] Access to unassign #" + id + " denied for " + name);
            respond(player, "§4[Pe] Only moderators may unassign "+settings.get("plural"));
            return;
        }
        PetitionObject petition = storage.load(id);
        if (petition.isValid() && petition.isOpen()) {
            storage.unassign(petition, player);
            // Notify
            if (Boolean.parseBoolean(settings.get("notify-owner-on-unassign"))) {
                notifyNamedPlayer(petition.getOwner(), "[Pe] §7Your " + settings.get("single").toLowerCase() + " §6#" + id + "§7 has been unassigned.");
            }
            notifyNamedPlayer(petition.getAssignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been unassigned from you by " + name + ".");
            String[] except = { petition.getOwner(), petition.getAssignee() };
            notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 unassigned by " + name + ".", except);
            logger.info(name + " unassigned " + settings.get("single").toLowerCase() + " #" + id);
            actionLog.logAction(name + " unassigned " + settings.get("single").toLowerCase() + " #" + id);
        } else {
            respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found.");
        }
    }

    private void performAssign(Player player, String[] args) {
        Long id = 0L;
        try {
            id = Long.valueOf(args[1]);
        } catch (NumberFormatException e) {
            respond(player, "[Pe] Syntax error.");
            return;
        }
        Boolean moderator = player == null || player.hasPermission("petition.moderate");
        String name = player != null ? player.getName() : CONSOLE_NAME;
        if (!moderator) {
            logger.info("[Pe] Access to assign #" + id + " denied for " + name);
            respond(player, "§4[Pe] Only moderators may assign "+settings.get("plural"));
            return;
        }
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
                notifyNamedPlayer(petition.getOwner(), "[Pe] §7Your " + settings.get("single").toLowerCase() + " §6#" + id + "§7 assigned to " + petition.getAssignee() + ".");
            }
            notifyNamedPlayer(petition.getAssignee(), "[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been assigned to you by " + name + ".");
            String[] except = { petition.getOwner(), petition.getAssignee() };
            notifyModerators("[Pe] §7" + settings.get("single") + " §6#" + id + "§7 has been assigned to " + petition.getAssignee() + ".", except);
            logger.info(name + " assigned " + settings.get("single").toLowerCase() + " #" + id + " to " + petition.getAssignee());
            actionLog.logAction(name + " assigned " + settings.get("single").toLowerCase() + " #" + id + " to " + petition.getAssignee());
        } else {
            respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found.");
        }
    }

    private void performView(Player player, String[] args) {
        Long id = 0L;
        try {
            id = Long.valueOf(args[1]);
        } catch (NumberFormatException e) {
            respond(player, "[Pe] Syntax error.");
            return;
        }
        Boolean moderator = player == null || player.hasPermission("petition.moderate");
        String name = player != null ? player.getName() : CONSOLE_NAME;
        PetitionObject petition = storage.load(id);
        if (petition.isValid()) {
            if (petition.isOwner(player) || moderator) {
                respond(player, "[Pe] §7" + petition.getHeader());
                for (PetitionComment line : petition.getLog()) {
                    respond(player, "[Pe] §6#" + petition.getId() + " §7" + line);
                }
            } else {
                logger.info("[Pe] Access to view #" + id + " denied for " + name);
            }
        } else {
            respond(player, "§4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found.");
        }
    }

    private void performList(final Player player, String[] args) {
        Integer count = 0;
        Integer limit = 10;
        Boolean include_offline = true;
        Boolean include_online = true;
        Boolean use_archive = false;
        Boolean sort_reverse = false;
        Boolean ignore_assigned = false;
        String filter = null;
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
                }
            }
        }
        List<PetitionObject> petitions = storage.list(use_archive, filter);
        Boolean moderator = player == null || player.hasPermission("petition.moderate");
        if (!moderator) {
            Iterables.removeIf(petitions, new Predicate<PetitionObject>() {

                @Override
                public boolean apply(PetitionObject petition)
                {
                    return !petition.isOwner(player);
                }
            });
        }
        if (sort_reverse) {
            petitions = Lists.reverse(petitions);
        }
        if (ignore_assigned) {
            Iterables.removeIf(petitions, new Predicate<PetitionObject>() {

                @Override
                public boolean apply(PetitionObject petition)
                {
                    return petition.isAssigned();
                }
            });
        }
        if (include_online && !include_offline) {
            Iterables.removeIf(petitions, new Predicate<PetitionObject>() {

                @Override
                public boolean apply(PetitionObject petition)
                {
                    return Bukkit.getPlayer(petition.getOwner()) != null;
                }
            });
        }
        if (include_offline && !include_online) {
            Iterables.removeIf(petitions, new Predicate<PetitionObject>() {

                @Override
                public boolean apply(PetitionObject petition)
                {
                    return Bukkit.getPlayer(petition.getOwner()) == null;
                }
                
            });
        }
        count = petitions.size();
        if (limit > 0 && petitions.size() > limit) {
            petitions = petitions.subList(0, limit);
        }
        for (PetitionObject petition : petitions) {
            respond(player, "[Pe] " + petition.getHeader());
        }
        respond(player, "[Pe] §7"+(use_archive?"Closed":"Open")+" " + settings.get("plural").toLowerCase() + (filter==null?"":new StringBuilder(" matching ").append(filter).toString()) + ": " + count + " (Showing " + petitions.size() + ")");
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

    public void setupLog() {
        actionLog = new ActionLog();
    }

    private void loadSettings() {
        String line = null;

        // Load the settings hash with defaults
        settings.put("single", "Petition");
        settings.put("plural", "Petitions");

        settings.put("notify-all-on-close", "false");
        settings.put("notify-owner-on-assign", "false");
        settings.put("notify-owner-on-unassign", "false");

        settings.put("notify-interval-seconds", "300");

        settings.put("warp-requires-permission", "false");
        
        settings.put("storage-type", "text");
        // Read the current file (if it exists)
        try {
            BufferedReader input =  new BufferedReader(new FileReader(CONFIG_FILE));
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
            storage = new TextStorage();
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
        File f;

        // Ensure that baseDir exists
        f = new File(BASE_DIR);
        if (!f.exists() && f.mkdir()) {
            logger.info("[Pe] Created directory '" + BASE_DIR + "'");
        }
        // Ensure that archiveDir exists
        f = new File(ARCHIVE_DIR);
        if (!f.exists() && f.mkdir()) {
            logger.info("[Pe] Created directory '" + ARCHIVE_DIR + "'");
        }
        // Ensure that mailDir exists
        f = new File(MAIL_DIR);
        if (!f.exists() && f.mkdir()) {
            logger.info("[Pe] Created directory '" + MAIL_DIR + "'");
        }
        // Ensure that configFile exists
        f = new File(CONFIG_FILE);
        if (!f.exists()) {
            // Ensure that configFile exists
            BufferedWriter output;
            String newline = System.getProperty("line.separator");
            try {
                output = new BufferedWriter(new FileWriter(CONFIG_FILE));
                output.write("single=Petition" + newline);
                output.write("plural=Petitions" + newline);
                output.write("notify-all-on-close=false" + newline);
                output.write("notify-owner-on-assign=true" + newline);
                output.write("notify-owner-on-unassign=true" + newline);
                output.write("notify-interval-seconds=300" + newline);
                output.write("warp-requires-permission=false" + newline);
                output.write("storage-type=text");
                output.close();
                logger.info("[Pe] Created config file '" + CONFIG_FILE + "'");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void notifyNamedPlayer(String name, String message) {
        // Ignore broken filenames -- should probably be improved
        if (name.equals("") || name.equals("*") || name.equalsIgnoreCase(CONSOLE_NAME)) {
            return;
        }
        Player[] players = Bukkit.getOnlinePlayers();
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
            fname = MAIL_DIR + File.separator + name;
            f = new File(fname);
            if (!f.exists() && f.mkdir()) {
                logger.info("[Pe] Created directory '" + fname + "'");
            }
            // Ensure that player's mailDir tmp exists
            fname = MAIL_DIR + File.separator + name + "/tmp";
            f = new File(fname);
            if (!f.exists() && f.mkdir()) {
                logger.info("[Pe] Created directory '" + fname + "'");
            }
            // Ensure that player's mailDir inbox exists
            fname = MAIL_DIR + File.separator + name + "/inbox";
            f = new File(fname);
            if (!f.exists() && f.mkdir()) {
                logger.info("[Pe] Created directory '" + fname + "'");
            }
            // Create a unique file in tmp
            UUID uuid = UUID.randomUUID();
            fname = MAIL_DIR + File.separator + name + "/tmp/" + uuid;
            String fname_final = MAIL_DIR + File.separator + name + "/inbox/" + uuid;
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
        Player[] players = Bukkit.getOnlinePlayers();
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
        Player[] players = Bukkit.getOnlinePlayers();
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
        String pname = MAIL_DIR + File.separator + name + "/inbox";
        File dir = new File(pname);
        String[] filenames = dir.list();
        if (filenames != null) {
            messages = new String[filenames.length];
            Integer index = 0;
            for (String fname : filenames) {
                try {
                    BufferedReader input =  new BufferedReader(new FileReader(pname + File.separator + fname));
                    messages[index] = input.readLine();
                    input.close();
                    boolean success = (new File(pname + File.separator + fname)).delete();
                    if (!success) {
                        logger.warning("[Pe] Could not delete " + pname + File.separator + fname);
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
        if (!petition.isOwner(player)) {
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
        if (petition.getAssignee().equals("*")) {
            return false;
        }
        // It has been assigned, is that sufficient?
        if (player.hasPermission("petition.warp-to-own-assigned")) {
            return true;
        }
        String[] except = { petition.getOwner() };
        notifyModerators("[Pe] " + player.getName() + " requested warp access to " + settings.get("single").toLowerCase() + " #" + petition.getId(), except);
        return false;
    }

    private void startNotifier() {
        Integer seconds = 0;
        try {
            seconds = Integer.parseInt(settings.get("notify-interval-seconds"));
        }
        catch (Exception e) {
            logger.warning("[Pe] Error parsing option 'notify-interval-seconds'; must be an integer.");
            logger.warning("[Pe] Using default value (300)");
        }
        if (seconds > 0) {
            task = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new NotifierThread(this), 0, seconds * TPS);
        } else {
            logger.info("[Pe] Notification thread disabled");
        }
    }

    private void stopNotifier() {
        if (task != null) {
            task.cancel();
        }
    }

    public Storage getStorage() {
        return storage;
    }

    public Map<String, String> getSettings() {
        return settings;
    }
}
