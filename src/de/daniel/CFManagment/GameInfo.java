package de.daniel.CFManagment;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.Position;
import cn.nukkit.utils.Config;
import de.daniel.ConnectFourMain;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class GameInfo {

    public static Config cfg = new Config(new File(ConnectFourMain.getInstance().getDataFolder(), "config.yml"));

    private static HashMap<Position, String> getGames() {
        ResultSet rs = MySQL.getResult("SELECT * FROM GameData");
        HashMap<Position, String> gameMap = new HashMap<>();

        try {
            while (rs.next()) {

                Position pos = new Position(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"), Server.getInstance().getLevelByName(rs.getString("world")));

                String uuid = rs.getString("BlockPlacerUUID");
                String playerName;

                try {
                    playerName = Server.getInstance().getOnlinePlayers().get(UUID.fromString(uuid)).getName();
                } catch (NullPointerException e) {
                    playerName = Server.getInstance().getOfflinePlayer(UUID.fromString(uuid)).getName();
                }

                gameMap.put(pos, playerName);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return gameMap;
    }

    public static ArrayList<Position> getPosList() {
        return new ArrayList<>(getGames().keySet());
    }

    public static String whoHasPlaced(Position pos) {
        return getGames().get(pos);
    }

    public static boolean hasPlayedBefor(Player p) {
        try {
            return MySQL.getResult("SELECT * FROM PlayerInfos WHERE UUID='" + p.getUniqueId() + "'").next();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

    public static int getGameAmount() {
        return getGames().size();
    }

    public static boolean isAGame(Position pos) {
        return getGames().containsKey(pos);
    }

    public static Item getCompleteItem(String id) {
        return Item.fromString(id);
    }

    public static Item getItem() {
        Item item = GameInfo.getBlock().toItem();
        item.addEnchantment(Enchantment.get(Enchantment.ID_FORTUNE_DIGGING).setLevel(4, false));
        item.setCustomName(ConnectFourMain.cfgMessages.getString("fwblock.name"));

        return item;
    }

    public static Block getBlock() {
        return getCompleteItem(cfg.getString("block.gameblock")).getBlock();
    }

    public static Item getStandartItem() {
        return getCompleteItem(cfg.getString("block.inventorydefault"));
    }

    public static Item getSideItem() {
        return getCompleteItem(cfg.getString("block.inventoryside"));
    }

}
