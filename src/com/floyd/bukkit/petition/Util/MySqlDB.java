package com.floyd.bukkit.petition.Util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.plugin.Plugin;

import com.mysql.jdbc.ResultSetMetaData;
import com.mysql.jdbc.Statement;

import com.floyd.bukkit.petition.*;;

public class MySqlDB {
public PetitionPlugin plugin;

public MySqlDB(PetitionPlugin instance) {
plugin = instance;
}