package com.floyd.bukkit.petition;

import java.util.List;

import org.bukkit.plugin.Plugin;

import com.avaje.ebean.EbeanServer;

public interface EbeanPlugin extends Plugin {

    EbeanServer getDatabase();
    List<Class<?>> getDatabaseClasses();
    void installDDL();
    void removeDDL();

}
