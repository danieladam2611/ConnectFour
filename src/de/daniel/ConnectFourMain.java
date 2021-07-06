package de.daniel;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import de.daniel.CFCommands.ChangeBlocks;
import de.daniel.CFCommands.ConnectFourCommand;
import de.daniel.CFCommands.Statistics;
import de.daniel.CFManagment.GameInfo;
import de.daniel.CFManagment.GameManager;
import de.daniel.CFListener.Listener;
import de.daniel.CFManagment.MySQL;

import java.io.File;

public class ConnectFourMain extends PluginBase {

    private static ConnectFourMain instance;
    public static ConnectFourMain getInstance() {
        return instance;
    }

    public static Config cfgMessages;

    @Override
    public void onEnable() {
        instance = this;

        saveResource("mysql.yml");
        saveResource("config.yml");
        saveResource("messages.yml");


        cfgMessages = new Config(new File(instance.getDataFolder(), "messages.yml"));

        MySQL.connect();

        if (!MySQL.isConnected()) {
            getLogger().alert("§4No MySQL-Server connection. Please check the file \"§emysql.yml§4\"");
            getPluginLoader().disablePlugin(this);
            return;
        }

        MySQL.update("CREATE TABLE IF NOT EXISTS PlayerInfos (UUID VARCHAR(50) PRIMARY KEY, Playername VARCHAR(100), ItemPlayer1 VARCHAR(10), ItemPlayer2 VARCHAR(10), Wins INT(25), Loses INT(25), NotDecided INT(25), PlayedTime BIGINT(50))");
        MySQL.update("CREATE TABLE IF NOT EXISTS GameData (x INT(15), y INT(15), z INT(15), world VARCHAR(100), BlockPlacerUUID VARCHAR(50))");
        MySQL.update("CREATE TABLE IF NOT EXISTS VierGewinntCurrentGames (GameID INT(15), User1 VARCHAR(50), User2 VARCHAR(50), BlockIDUser1 VARCHAR(30), BlockIDUser2 VARCHAR(30), BeginningTime BIGINT(100))");

        getServer().getCommandMap().register(cfgMessages.getString("command.name"), new ConnectFourCommand());
        getServer().getPluginManager().registerEvents(new Listener(), this);
        getServer().getPluginManager().registerEvents(new Statistics(), this);
        getServer().getPluginManager().registerEvents(new GameManager(), this);

        getLogger().info("§b" + getName() + " §fwas successfully §aEnabled");
        getLogger().info("§e" + GameInfo.getGameAmount() + " §aErfasste spiele");
    }

    @Override
    public void onDisable() {

        if (MySQL.isConnected()) {
            MySQL.update("DELETE FROM VierGewinntCurrentGames");
            MySQL.disconnect();
        }

        getLogger().info("§b" + getName() + " §fwas successfully §cDisabled");
    }
}
