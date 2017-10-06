package com.floyd.bukkit.petition;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.ddl.DdlGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;


public class EbeanJavaPlugin extends JavaPlugin implements EbeanPlugin {

    protected EbeanServer ebean;

    public EbeanJavaPlugin() {
        ebean = EbeanUtil.createDatabase(this, getClassLoader());
    }

    @Override
    public EbeanServer getDatabase() {
        return ebean;
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        return new ArrayList<Class<?>>();
    }

    @Override
    public void installDDL() {
        SpiEbeanServer serv = (SpiEbeanServer) getDatabase();
        DdlGenerator gen = serv.getDdlGenerator();

        gen.runScript(false, gen.generateCreateDdl());
    }

    @Override
    public void removeDDL() {
        SpiEbeanServer serv = (SpiEbeanServer) getDatabase();
        DdlGenerator gen = serv.getDdlGenerator();

        gen.runScript(true, gen.generateDropDdl());
    }

}
