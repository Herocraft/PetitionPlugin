package com.floyd.bukkit.petition;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.dbplatform.SQLitePlatform;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.ddl.DdlGenerator;
import com.avaje.ebeaninternal.server.lib.sql.TransactionIsolation;
import org.apache.commons.lang.Validate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;


public class EbeanUtil {

    public static EbeanServer createDatabase(EbeanPlugin plugin, ClassLoader classLoader) {
        ServerConfig db = new ServerConfig();

        db.setDefaultServer(false);
        db.setRegister(false);
        db.setClasses(plugin.getDatabaseClasses());
        db.setName(plugin.getDescription().getName());
        ConfigurationSection dbConf = plugin.getConfig().getConfigurationSection("database");
        if (dbConf != null) {
            configureDbConfig(db, plugin.getConfig().getConfigurationSection("database"));
        } else {
            throw new RuntimeException("Missing database config");
        }

        DataSourceConfig ds = db.getDataSourceConfig();

        ds.setUrl(replaceDatabaseString(plugin, ds.getUrl()));
        plugin.getDataFolder().mkdirs();

        ClassLoader previous = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(classLoader);
        EbeanServer ebean = EbeanServerFactory.create(db);
        Thread.currentThread().setContextClassLoader(previous);

        return ebean;
    }

    private static void configureDbConfig(ServerConfig config, ConfigurationSection dbConf) {
        Validate.notNull(config, "Config cannot be null");
        DataSourceConfig ds = new DataSourceConfig();
        ds.setDriver(dbConf.getString("driver"));
        ds.setUrl(dbConf.getString("url"));
        ds.setUsername(dbConf.getString("username"));
        ds.setPassword(dbConf.getString("password"));
        ds.setIsolationLevel(TransactionIsolation.getLevel(dbConf.getString("isolation")));
        if (ds.getDriver().contains("sqlite")) {
            config.setDatabasePlatform(new SQLitePlatform());
            config.getDatabasePlatform().getDbDdlSyntax().setIdentity("");
        }

        config.setDataSourceConfig(ds);
    }

    private static String replaceDatabaseString(Plugin plugin, String input) {
        input = input.replaceAll("\\{DIR\\}", plugin.getDataFolder().getPath().replaceAll("\\\\", "/") + "/");
        input = input.replaceAll("\\{NAME\\}", plugin.getDescription().getName().replaceAll("[^\\w_-]", ""));
        return input;
    }

}
