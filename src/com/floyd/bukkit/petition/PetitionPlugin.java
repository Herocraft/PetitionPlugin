package com.floyd.bukkit.petition;


import java.io.*;
import java.util.Comparator;


import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Server;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import java.util.logging.Logger;

import com.nijikokun.bukkit.Permissions.Permissions;

import java.util.Arrays;
import java.util.UUID;
import java.util.regex.*;

/**
* Petition plugin for Bukkit
*
* @author FloydATC
*/
public class PetitionPlugin extends JavaPlugin {
    private final PetitionPlayerListener playerListener = new PetitionPlayerListener(this);
    private NotifierThread notifier = null; 
    
    private final ConcurrentHashMap<Player, Boolean> debugees = new ConcurrentHashMap<Player, Boolean>();
    private final ConcurrentHashMap<Integer, String> semaphores = new ConcurrentHashMap<Integer, String>();
    public final ConcurrentHashMap<String, String> settings = new ConcurrentHashMap<String, String>();

    public static Permissions Permissions = null;
    
    String baseDir = "plugins/PetitionPlugin";
    String archiveDir = "archive";
    String mailDir = "mail";
    String ticketFile = "last_ticket_id.txt";
    String configFile = "settings.txt";

/**
*
*Default DB Values, can be changed in settings file.
*/

    static boolean mysql = false;    
    static String driver = "com.mysql.jdbc.Driver";    
    static String user = "root";    
    static String pass = "root";    
    static String db = "jdbc:mysql://localhost:3306/minecraft";

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
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!" );
    }

    public void onEnable() {
        // TODO: Place any custom enable code here including the registration of any events

    	preFlightCheck();
    	setupPermissions();
    	loadSettings();
    	startNotifier();
    	
    	
        // Register our events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args ) {
    	String cmdname = cmd.getName().toLowerCase();
        Player player = null;
        if (sender instanceof Player) {
        	player = (Player)sender;
        }
        
        if (cmdname.equalsIgnoreCase("pe") || cmdname.equalsIgnoreCase("petition")) {
        	if (player == null || Permissions.Security.permission(player, "petition")) {
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
	        			performAssign(player, args);
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
        	}
        }

        return false;
    }

    private void performWarp(Player player, String[] args) {
		Integer id = Integer.valueOf(args[1]);
    	Boolean moderator = false;
    	String name = "(Console)";
    	if (player == null) {
    		respond(player, "[Pe] That would be a neat trick.");
    		return;
    	} else {
       		name = player.getName();
    	}
    	if (Permissions.Security.permission(player, "petition.moderate")) {
    		moderator = true;
    	}
		try {
    		getLock(id, player);
    		PetitionObject petition = new PetitionObject(id);
    		if (petition.isValid() && (petition.isOpen() || moderator)) {
    			if (canWarpTo(player, petition)) {
    				respond(player, "[Pe] �7" + petition.Header(getServer()) );
					if (player.teleport(petition.getLocation(getServer()))) {
	    				respond(player, "[Pe] �7Teleporting you to where the " + settings.get("single").toLowerCase() + " was opened" );
						logger.info(name + " teleported to petition " + id);
					} else {
	    				respond(player, "[Pe] �7Teleport failed" );
						logger.info(name + " teleport to petition " + id + " FAILED");
					}
    			} else {
    				logger.info("[Pe] Access to warp to #" + id + " denied for " + name);
    				respond(player, "�4[Pe] Access denied.");
    			}
    		} else {
    			respond(player, "�4[Pe] No open " + settings.get("single").toLowerCase() + " #" + args[1] + " found." );
    		}
		}
		finally {
			releaseLock(id, player);
		}
    	
    }
    private void performOpen(Player player, String[] args) {
		Integer id = IssueUniqueTicketID();
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
    		PetitionObject petition = new PetitionObject( id, player, title );
    		releaseLock(id, player);
    		if (petition.isValid()) {
    			respond(player, "[Pe] �7Thank you, your ticket is �6#" + petition.ID() + "�7. (Use '/petition' to manage it)");
				String[] except = { petition.Owner() };
    			notifyModerators("[Pe] �7" + settings.get("single") + " �6#" + petition.ID() + "�7 opened by " + name + ": " + title, except);
				logger.info(name + " opened petition " + id + ". " + title);
    		} else {
    			respond(player, "�4[Pe] There was an error creating your ticket, please try again later.");