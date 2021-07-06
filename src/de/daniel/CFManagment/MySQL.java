package de.daniel.CFManagment;

import cn.nukkit.Server;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.MainLogger;
import de.daniel.ConnectFourMain;

import java.io.File;
import java.sql.*;

public class MySQL {

    private static Config cfg = new Config(new File(ConnectFourMain.getInstance().getDataFolder(), "mysql.yml"));

    private static String host = cfg.getString("host");
    private static String port = cfg.getString("port");
    private static String database = cfg.getString("database");
    private static String username = cfg.getString("username");
    private static String password = cfg.getString("password");

    private static Connection connection;
    private static String prefix = "[" + ConnectFourMain.getInstance().getName() + " - MySQL] ";
    private static MainLogger logger = Server.getInstance().getLogger();

    public static void connect() {
        if (isConnected()) {
            logger.warning(prefix + "The database is already connected to the server!");
            return;
        }
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.error(prefix + "Connection failed!\n" + "Error Message:" + e.getMessage());
            return;
        }
        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, username, password);

            logger.info(prefix + "successfully connected");
        } catch (SQLException e) {
            logger.error(prefix + "Connection failed!\n" + "Error Message:" + e.getMessage());
            return;
        }
    }

    public static void disconnect() {
        if (!isConnected()) {
            logger.warning(prefix + "The database is not connected to the server!");
            return;
        }
        try {
            connection.close();
            logger.info(prefix + "successfully disconnected");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isConnected() {
        if (connection == null) {
            return false;
        }
        return true;
    }

    public static void update(String qry) {
        if (!isConnected()) {
            logger.warning(prefix + "The database is not connected to the server!");
            return;
        }
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(qry);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static ResultSet getResult(String qry) {
        if (!isConnected()) {
            logger.warning(prefix + "The database is not connected to the server!");
            return null;
        }
        try {
            Statement statement = connection.createStatement();
            return statement.executeQuery(qry);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
