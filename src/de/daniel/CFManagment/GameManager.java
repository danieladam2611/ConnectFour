package de.daniel.CFManagment;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.inventory.InventoryCloseEvent;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.Position;
import cn.nukkit.level.Sound;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.utils.Config;
import com.nukkitx.fakeinventories.inventory.DoubleChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.FakeInventoryListener;
import com.nukkitx.fakeinventories.inventory.FakeSlotChangeEvent;
import de.daniel.CFListener.Listener;
import de.daniel.ConnectFourMain;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager implements cn.nukkit.event.Listener {

    public static HashMap<Player, DoubleChestFakeInventory> playerInvMap = new HashMap<>();
    private static Config cfgMessages = ConnectFourMain.cfgMessages;
    private static ConcurrentHashMap<Integer, Player> currentPlayerMap = new ConcurrentHashMap<>();
    //Waiting Players
    private static HashMap<Position, ArrayList<Player>> waitingPlayerMap = new HashMap<>();

    private static ArrayList<String> gameCooldown = new ArrayList<>();


    public static ArrayList<Integer> sideItemList() {
        ArrayList<Integer> list = new ArrayList<>();

        for (int i = 0; i <= 45; i = i + 9) {

            list.add(i);
            list.add((i + 8));
        }

        return list;
    }

    public static boolean isOnGameField(int id) {
        return !sideItemList().contains(id);
    }

    public static HashMap<Integer, Item> getItemForSlotMap() {
        HashMap<Integer, Item> map = new HashMap<>();
        ArrayList<Integer> list = sideItemList();

        Item sideItem = GameInfo.getSideItem();
        Item standartItem = GameInfo.getStandartItem();

        for (int i = 0; i <= 53; i++) {
            if (list.contains(i)) {
                map.put(i, sideItem);
            } else {
                map.put(i, standartItem);
            }
        }

        return map;
    }

    //START / LISTENER
    public static void registerGame(Player p1, Player p2, Position pos) {
        int gameID;

        while (true) {
            gameID = new Random().nextInt(899999) + 100000;

            ResultSet rs = MySQL.getResult("SELECT * FROM VierGewinntCurrentGames WHERE GameID='" + gameID + "'");
            try {
                if (!rs.next()) {
                    break;
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

        MySQL.update("INSERT INTO VierGewinntCurrentGames (GameID, User1, User2, BeginningTime) VALUES ('" + gameID + "','" + p1.getUniqueId() + "','" + p2.getUniqueId() + "','" + System.currentTimeMillis() + "')");
        startGame(p1, p2, gameID, pos);
    }

    private static void startGame(Player p1, Player p2, int gameID, Position pos) {
        DoubleChestFakeInventory cfi = new DoubleChestFakeInventory();

        HashMap<Integer, Item> itemMap = GameManager.getItemForSlotMap();

        cfi.setName("§f§lVIERGEWINNT");

        for (int i = 0; i < 53; i++) {
            cfi.setItem(i, itemMap.get(i));
        }

        Item playerItem = getPlayerItem1(p1);
        playerItem.setCustomName(p1.getName());
        playerItem.addEnchantment(Enchantment.get(Enchantment.ID_DAMAGE_ALL).setLevel(0, false));
        cfi.setItem(53, playerItem);

        cfi.addListener(new FakeInventoryListener() {
            @Override
            public void onSlotChange(FakeSlotChangeEvent fakeSlotChangeEvent) {
                int slotID = fakeSlotChangeEvent.getAction().getSlot();
                Item soureItem = fakeSlotChangeEvent.getAction().getSourceItem();
                Player p = fakeSlotChangeEvent.getPlayer();

                fakeSlotChangeEvent.setCancelled();

                if (!cfi.slots.containsValue(soureItem)) {
                    return;
                }

                if (gameCooldown.contains(String.valueOf(gameID))) {
                    return;
                }

                if (GameManager.sideItemList().contains(slotID)) {
                    return;
                }

                if (!GameManager.isOnGameField(slotID)) {
                    return;
                }

                if (!GameManager.isPlayer(p, gameID)) {
                    return;
                }

                if (getCurrentPlayer(gameID) == null) {
                    setCurrentPlayer(p2, p1, gameID);
                }

                if (!getCurrentPlayer(gameID).equals(p)) {
                    return;
                }

                int newSlotID = Calculator.getNewSlotID(gameID, slotID);

                ArrayList<Integer> list = new ArrayList<>();

                if (Calculator.posMap.get(gameID) != null) {
                    list = Calculator.posMap.get(gameID);
                }

                if (list.contains(newSlotID)) {
                    return;
                }

                list.add(newSlotID);
                Calculator.posMap.put(gameID, list);

                Player nextPlayer = getNextPlayer(p, gameID);

                int length = 0;

                if (Calculator.userPosMap.get(nextPlayer) != null) {
                    length = de.daniel.CFManagment.Calculator.userPosMap.get(nextPlayer).split(",").length;
                }

                Item playerItem;

                if (getCurrentPlayer(gameID) == getPlayer1(p)) {
                    cfi.setItem(newSlotID, getPlayerItem1(p), true);
                    playerItem = getPlayerItem2(nextPlayer);
                } else {
                    cfi.setItem(newSlotID, getPlayerItem2(p), true);
                    playerItem = getPlayerItem1(nextPlayer);
                }
                playerItem.setCustomName(nextPlayer.getName());
                playerItem.addEnchantment(Enchantment.get(Enchantment.ID_DAMAGE_ALL).setLevel(length, false));
                cfi.setItem(53, playerItem, true);

                cfi.sendSlot(newSlotID, nextPlayer);

                ArrayList<Player> viewerList = new ArrayList<>();
                if (Listener.viewGamesViewerMap.get(pos) != null) {
                    viewerList = Listener.viewGamesViewerMap.get(pos);

                    if (!viewerList.isEmpty()) {
                        for (Player all : viewerList) {
                            cfi.sendSlot(newSlotID, all);
                        }
                    }
                }


                int cooldown = 20;


                if (Calculator.hasWon(p, gameID, newSlotID)) {
                    ArrayList<Player> finalViewerList1 = viewerList;
                    Server.getInstance().getScheduler().scheduleDelayedTask(new TimerTask() {
                        @Override
                        public void run() {
                            finish(p, getOtherPlayer(p), gameID, "");
                            cfi.clearAll();
                            cfi.close(p1);
                            cfi.close(p2);
                            if (!finalViewerList1.isEmpty()) {
                                for (Player all : finalViewerList1) {
                                    cfi.close(all);
                                }
                            }
                        }
                    }, 20 * 5);

                    ArrayList<Integer> wL = Calculator.winMap.get(gameID);

                    for (int i = 0; i < wL.size(); i++) {
                        int slot = wL.get(i);
                        Item item = cfi.getItem(slot);
                        item.addEnchantment(Enchantment.get(Enchantment.ID_BOW_KNOCKBACK));
                        cfi.setItem(slot, item, true);
                        cfi.sendSlot(slot, p); cfi.sendSlot(slot, nextPlayer);
                        if (!viewerList.isEmpty()) {
                            for (Player all : viewerList) {
                                cfi.sendSlot(slot, all);
                            }
                        }
                    }

                    Calculator.winMap.remove(gameID);

                    cooldown = cooldown * 5;
                }

                if (!fakeSlotChangeEvent.getInventory().slots.containsValue(de.daniel.CFManagment.GameInfo.getStandartItem())) {
                    ArrayList<Player> finalViewerList = viewerList;
                    Server.getInstance().getScheduler().scheduleDelayedTask(new TimerTask() {
                        @Override
                        public void run() {
                            finish(p, getOtherPlayer(p), gameID, "u");
                            cfi.clearAll();
                            cfi.close(p1);
                            cfi.close(p2);
                            if (!finalViewerList.isEmpty()) {
                                for (Player all : finalViewerList) {
                                    cfi.close(all);
                                }
                            }
                        }
                    }, 20 * 5);
                    cooldown = cooldown * 5;
                }

                setCurrentPlayer(p, nextPlayer, gameID);

                gameCooldown.add(String.valueOf(gameID));

                Server.getInstance().getScheduler().scheduleDelayedTask(new AsyncTask() {
                    @Override
                    public void onRun() {
                        gameCooldown.remove(String.valueOf(gameID));
                    }
                }, cooldown);
            }
        });


        p1.addWindow(cfi);
        p2.addWindow(cfi);
        playerInvMap.put(p1, cfi);
        playerInvMap.put(p2, cfi);

        Listener.viewGamesPlayerMap.computeIfAbsent(pos, k -> new ArrayList<>());
        ArrayList<Player> viewGameMap = Listener.viewGamesPlayerMap.get(pos);
        viewGameMap.add(p1);
        viewGameMap.add(p2);
        Listener.viewGamesPlayerMap.put(pos, viewGameMap);
    }

    public static int getGameID(Player p1, Player p2) {
        ResultSet rs = MySQL.getResult("SELECT * FROM VierGewinntCurrentGames WHERE User1='" + p1.getUniqueId() + "' AND User2='" + p2.getUniqueId() + "'");
        try {
            while (rs.next()) {
                return rs.getInt("GameID");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return 0;
    }

    public static int wichPlayer(Player p) {
        if (p.equals(getPlayer1(p))) {
            return 1;
        }
        if (p.equals(getPlayer2(p))) {
            return 2;
        }
        return 0;
    }

    public static Player getOtherPlayer(Player player) {
        int i = wichPlayer(player);
        if (i == 1) {
            return getPlayer2(player);
        } else if (i == 2) {
            return getPlayer1(player);
        }
        return null;
    }

    public static boolean isPlayer(Player p, int gameID) {
        ResultSet rs = MySQL.getResult("SELECT * FROM VierGewinntCurrentGames WHERE GameID='" + gameID + "'");
        try {
            while (rs.next()) {

                if (Server.getInstance().getOnlinePlayers().get(UUID.fromString(rs.getString("User1"))).equals(p) | Server.getInstance().getOnlinePlayers().get(UUID.fromString(rs.getString("User2"))).equals(p)) {
                    return true;
                }

            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return false;
    }

    public static Player getPlayer1(Player otherPlayer) {
        ResultSet rs = MySQL.getResult("SELECT * FROM VierGewinntCurrentGames WHERE User2='" + otherPlayer.getUniqueId() + "'");
        try {
            while (rs.next()) {
                return Server.getInstance().getOnlinePlayers().get(UUID.fromString(rs.getString("User1")));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return otherPlayer;
    }

    public static Player getPlayer2(Player otherPlayer) {
        ResultSet rs = MySQL.getResult("SELECT * FROM VierGewinntCurrentGames WHERE User1='" + otherPlayer.getUniqueId() + "'");
        try {
            while (rs.next()) {
                return Server.getInstance().getOnlinePlayers().get(UUID.fromString(rs.getString("User2")));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return otherPlayer;
    }

    public static long getPlayedTimeFromGame(int gameID) {
        ResultSet rs = MySQL.getResult("SELECT * FROM VierGewinntCurrentGames WHERE GameID='" + gameID + "'");
        try {
            while (rs.next()) {
                return Long.parseLong(rs.getString("BeginningTime"));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return 0;
    }

    public static Player getCurrentPlayer(int gameID) {
        return currentPlayerMap.get(gameID);
    }

    public static void setCurrentPlayer(Player currentPlayer, Player nextPlayer, int gameID) {
        currentPlayerMap.remove(gameID, currentPlayer);
        currentPlayerMap.put(gameID, nextPlayer);
    }

    public static Player getNextPlayer(Player currentPlayer, int gameID) {
        if (getCurrentPlayer(gameID).equals(getPlayer1(currentPlayer))) {
            return getPlayer2(currentPlayer);
        }
        if (getCurrentPlayer(gameID).equals(getPlayer2(currentPlayer))) {
            return getPlayer1(currentPlayer);
        }
        return null;
    }

    public static boolean setWaitingPlayer(Position pos, Player p) {
        if (waitingPlayerMap.size() > 18) {
            p.sendMessage(cfgMessages.getString("waitingroom.full").replace("%n", "\n"));
            return false;
        }

        ArrayList<Player> list = waitingPlayerMap.get(pos);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(p);

        waitingPlayerMap.put(pos, list);
        return true;
    }

    public static ArrayList<Player> getWaitingPlayers(Position pos) {
        for (Position position : waitingPlayerMap.keySet()) {
            if (position.equals(pos)) {

                return waitingPlayerMap.get(pos);
            }
        }
        return null;
    }

    public static void removeWaitingPlayer(Position pos, Player p) {
        waitingPlayerMap.get(pos).remove(p);
    }

    private static void finish(Player winner, Player loser, int gameID, String decided) {

        Position pos = null;
        for (Position position : Listener.viewGamesPlayerMap.keySet()) {
            if (Listener.viewGamesPlayerMap.get(position).contains(winner) | Listener.viewGamesPlayerMap.get(position).contains(loser)) {
                pos = position;
            }
        }

        playerWinsEvent(winner, loser, decided, gameID, pos);

        Player p1 = getPlayer1(winner);
        Player p2 = getPlayer2(winner);

        de.daniel.CFManagment.Calculator.userPosMap.remove(p1);
        de.daniel.CFManagment.Calculator.userPosMap.remove(p2);
        de.daniel.CFManagment.Calculator.posMap.remove(gameID);
        currentPlayerMap.remove(gameID);

        playerInvMap.remove(winner);
        playerInvMap.remove(loser);

        saveData(winner, loser, decided, gameID);

        MySQL.update("DELETE FROM VierGewinntCurrentGames WHERE GameID='" + gameID + "' AND User1='" + p1.getUniqueId() + "' AND User2='" + p2.getUniqueId() + "'");
    }

    private static void playerWinsEvent(Player winner, Player loser, String decided, int gameID, Position position) {

        FormWindowSimple gui = new FormWindowSimple(cfgMessages.getString("gamefinish.tittel"), "");

        StringBuilder sb = new StringBuilder();

        if (decided.equals("u")) {
            sb.append(cfgMessages.getString("gamefinish.decided"));
        } else {
            sb.append(cfgMessages.getString("gamefinish.winner").replace("%winner", winner.getName()) + "\n");
            sb.append(cfgMessages.getString("gamefinish.loser").replace("%loser", loser.getName()) + "\n");
        }

        long time = getPlayedTimeFromGame(gameID);

        sb.append(cfgMessages.getString("gamefinish.time")
                .replace("%min", String.valueOf((int) ((System.currentTimeMillis() - time) / 1000 / 60)))
                .replace("%sec", String.valueOf((int) ((System.currentTimeMillis() - time) / 1000 % 60))) + "\n");

        if (Calculator.userPosMap.get(getNextPlayer(winner, gameID)) != null) {
            int length = Calculator.userPosMap.get(getNextPlayer(winner, gameID)).split(",").length +
                    Calculator.userPosMap.get(getNextPlayer(winner, gameID)).split(",").length;
            sb.append(cfgMessages.getString("gamefinish.placedblocks").replace("%numbers", String.valueOf(length)) + "\n");
        }

        gui.setContent(sb.toString());
        Server.getInstance().getScheduler().scheduleDelayedTask(new AsyncTask() {
            @Override
            public void onRun() {
                winner.showFormWindow(gui);
                loser.showFormWindow(gui);

                //ViewerList
                if (Listener.viewGamesPlayerMap.containsKey(position)) {
                    Listener.viewGamesPlayerMap.get(position).remove(winner);
                    Listener.viewGamesPlayerMap.get(position).remove(loser);

                    ArrayList<Player> viewerList = new ArrayList<>(Listener.viewGamesViewerMap.get(position));

                    if (!viewerList.isEmpty()) {
                        for (Player all : viewerList) {
                            all.showFormWindow(gui);
                            Listener.viewGamesViewerMap.get(position).remove(all);
                        }
                    }
                }


                winner.level.addSound(winner.getPosition(), Sound.valueOf(GameInfo.cfg.getString("game.winnersound")), 1, 1, winner, loser);

                String commandLine = GameInfo.cfg.getString("game.reward").replace("%winner", winner.getName());

                Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), commandLine);
            }
        }, 15);
    }

    private static void saveData(Player winner, Player loser, String decided, int gameID) {
        int winningMatches = 0;
        int losingMatches = 0;
        int notDecided = 0;
        if (decided.contains("u")) {
            notDecided++;
        } else {
            winningMatches++;
            losingMatches++;
        }

        long time = getPlayedTimeFromGame(gameID);
        time = ((System.currentTimeMillis() - time) / 1000);

        MySQL.update("REPLACE INTO PlayerInfos (UUID, Playername, ItemPlayer1, ItemPlayer2, Wins, Loses, NotDecided, PlayedTime) " +
                "VALUES ('" + winner.getUniqueId() + "','" + winner.getName() + "','" + (getPlayerItem1(winner).getId() + ":" + getPlayerItem1(winner).getDamage()) + "','"
                + (getPlayerItem2(winner).getId() + ":" + getPlayerItem2(winner).getDamage()) + "','" + (getwinningMatches(winner.getUniqueId()) + winningMatches) + "','"
                + getlosingMatches(winner.getUniqueId()) + "','" + (getnotDecided(winner.getUniqueId()) + notDecided) + "','" + time + "')");
        MySQL.update("REPLACE INTO PlayerInfos (UUID, Playername, ItemPlayer1, ItemPlayer2, Wins, Loses, NotDecided, PlayedTime) " +
                "VALUES ('" + loser.getUniqueId() + "','" + loser.getName() + "','" + (getPlayerItem1(loser).getId() + ":" + getPlayerItem1(loser).getDamage()) + "','"
                + (getPlayerItem2(loser).getId() + ":" + getPlayerItem2(loser).getDamage()) + "','" + getwinningMatches(loser.getUniqueId()) + "','"
                + (getlosingMatches(loser.getUniqueId()) + losingMatches) + "','" + (getnotDecided(loser.getUniqueId()) + notDecided) + "','" + time + "')");
    }

    public static Item getPlayerItem1(Player p) {
        ResultSet rs = MySQL.getResult("SELECT * FROM PlayerInfos WHERE UUID='" + p.getUniqueId() + "'");
        try {
            while (rs.next()) {
                if (rs.getString("ItemPlayer1") == null) {
                    break;
                }
                return GameInfo.getCompleteItem(rs.getString("ItemPlayer1"));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return GameInfo.getCompleteItem(GameInfo.cfg.getString("block.defaultplayer1"));
    }

    public static Item getPlayerItem2(Player p) {
        ResultSet rs = MySQL.getResult("SELECT * FROM PlayerInfos WHERE UUID='" + p.getUniqueId() + "'");
        try {
            while (rs.next()) {
                if (rs.getString("ItemPlayer2") == null) {
                    break;
                }
                return GameInfo.getCompleteItem(rs.getString("ItemPlayer2"));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return GameInfo.getCompleteItem(GameInfo.cfg.getString("block.defaultplayer2"));
    }

    public static int getwinningMatches(UUID uuid) {
        ResultSet rs = MySQL.getResult("SELECT * FROM PlayerInfos WHERE UUID='" + uuid + "'");
        try {
            while (rs.next()) {
                return rs.getInt("Wins");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return 0;
    }

    public static int getlosingMatches(UUID uuid) {
        ResultSet rs = MySQL.getResult("SELECT * FROM PlayerInfos WHERE UUID='" + uuid + "'");
        try {
            while (rs.next()) {
                return rs.getInt("Loses");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return 0;
    }

    public static int getnotDecided(UUID uuid) {
        ResultSet rs = MySQL.getResult("SELECT * FROM PlayerInfos WHERE UUID='" + uuid + "'");
        try {
            while (rs.next()) {
                return rs.getInt("NotDecided");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return 0;
    }

    @EventHandler
    public void onCloseWindow(InventoryCloseEvent e) {
        Player p = e.getPlayer();

        for (Position pos : Listener.viewGamesViewerMap.keySet()) {
            ArrayList<Player> list = Listener.viewGamesViewerMap.get(pos);

            if (list.contains(p)) {
                Listener.viewGamesViewerMap.get(pos).remove(p);
                return;
            }
        }

        if (!playerInvMap.containsKey(p)) {
            return;
        }

        Player p2 = getOtherPlayer(p);

        int gameID = getGameID(getPlayer1(p), getPlayer2(p));

        finish(p2, p, gameID, "");

        if (playerInvMap.get(p) != null) {
            playerInvMap.get(p).clearAll();
            playerInvMap.get(p).close(p);
        }
        if (playerInvMap.get(p2) != null) {
            playerInvMap.get(p2).clearAll();
            playerInvMap.get(p2).close(p2);
        }
    }
}
