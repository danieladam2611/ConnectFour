package de.daniel.CFListener;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityPrimedTNT;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.block.*;
import cn.nukkit.event.entity.EntityExplodeEvent;
import cn.nukkit.event.inventory.InventoryCloseEvent;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.Position;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.EventException;
import com.nukkitx.fakeinventories.inventory.ChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.FakeInventoryListener;
import com.nukkitx.fakeinventories.inventory.FakeSlotChangeEvent;
import de.daniel.CFCommands.ConnectFourCommand;
import de.daniel.CFCommands.Statistics;
import de.daniel.CFManagment.GameInfo;
import de.daniel.CFManagment.GameManager;
import de.daniel.CFManagment.MySQL;
import de.daniel.ConnectFourMain;

import java.util.*;

public class Listener implements cn.nukkit.event.Listener {

    private static Config cfgMessages = ConnectFourMain.cfgMessages;

    public static HashMap<Position, ArrayList<Player>> viewGamesPlayerMap = new HashMap<>();
    public static HashMap<Position, ArrayList<Player>> viewGamesViewerMap = new HashMap<>();
    public static HashMap<Player, Position> backToInventory = new HashMap<>();
    private static HashMap<Player, ChestFakeInventory> inventoryMap = new HashMap<>();
    private static HashMap<Player, Position> playerGamePositionMap = new HashMap<>();
    private static HashMap<Player, Player> acceptPlayerMap = new HashMap<>();
    private static HashMap<Player, List<Player>> waitingForAccepting = new HashMap<>();
    //private static HashMap<Position, Position> movingBlockMap = new HashMap<>();
    //private static ArrayList<Position> cooldown = new ArrayList<>();

    private static Item getPlayerWaitingItem(Player player) {
        Item item;

        if (!GameInfo.hasPlayedBefor(player)) {
            item = new Item(298, 0, 1).setCustomName(player.getName());
            item.addEnchantment(Enchantment.get(Enchantment.ID_FROST_WALKER));
            return item;
        }

        int wins = GameManager.getwinningMatches(player.getUniqueId());
        int loses = GameManager.getlosingMatches(player.getUniqueId());

        double kd = (double) wins / loses;

        if (kd > 3) {
            item = Item.get(ItemID.NETHERITE_HELMET);
        } else if (kd > 2) {
            item = Item.get(ItemID.DIAMOND_HELMET);
        } else if (kd > 1.5) {
            item = Item.get(ItemID.GOLD_HELMET);
        } else if (kd > 1) {
            item = Item.get(ItemID.IRON_HELMET);
        } else if (kd > 0.5) {
            item = Item.get(ItemID.CHAIN_HELMET);
        } else {
            item = Item.get(ItemID.LEATHER_CAP);
        }

        item.setCustomName(player.getName());

        return item;
    }

    private static Player getNextPlayer(Player p) {
        if (waitingForAccepting.get(p) == null) {
            return null;
        }
        return waitingForAccepting.get(p).get(0);
    }

    private static void showPlayerViewGames(Position pos, Player p) {
        ChestFakeInventory chestFakeInventory = new ChestFakeInventory();

        for (Player all : viewGamesPlayerMap.get(pos)) {
            chestFakeInventory.addItem(getPlayerWaitingItem(all).setCustomName(all.getName()));
        }

        chestFakeInventory.addListener(new FakeInventoryListener() {
            @Override
            public void onSlotChange(FakeSlotChangeEvent fakeSlotChangeEvent) {
                Item item = fakeSlotChangeEvent.getAction().getSourceItem();
                Player viewer = fakeSlotChangeEvent.getPlayer();
                int slotID = fakeSlotChangeEvent.getAction().getSlot();

                Player player = Server.getInstance().getPlayer(item.getName());

                if (player == null) {
                    return;
                }

                if (slotID == 26) {

                    return;
                }

                chestFakeInventory.clearAll();
                chestFakeInventory.close(p);

                Server.getInstance().getScheduler().scheduleDelayedTask(new TimerTask() {
                    @Override
                    public void run() {
                        viewer.addWindow(GameManager.playerInvMap.get(player));
                        ArrayList<Player> list = new ArrayList<Player>(viewGamesViewerMap.get(pos));
                        list.add(viewer);
                        viewGamesViewerMap.put(pos, list);
                    }
                }, 15);
            }
        });

        p.addWindow(chestFakeInventory);
    }

    private static void clearWaitingWindow(Player p) {
        if (!inventoryMap.containsKey(p)) {
            return;
        }

        ChestFakeInventory inv = inventoryMap.get(p);

        for (Player all : inventoryMap.keySet()) {
            if (all != p) {
                ChestFakeInventory cfi = inventoryMap.get(all);
                if (cfi != null) {
                    for (int i = 0; i <= 26; i++) {
                        Item item = cfi.getItem(i);
                        if (item != null) {
                            if (item.getName().equals(p.getName())) {
                                inventoryMap.get(all).remove(item);
                            }
                        }
                    }
                }
            }
        }

        inv.clearAll();

        GameManager.removeWaitingPlayer(playerGamePositionMap.get(p), p);
        playerGamePositionMap.remove(p, playerGamePositionMap.get(p));

        for (Position position : Listener.viewGamesPlayerMap.keySet()) {
            Listener.viewGamesPlayerMap.get(position).remove(p);
        }

        inv.close(p);

        inventoryMap.remove(p, inv);
    }

    @EventHandler
    public void onPlaceBlockEvent(BlockPlaceEvent event) {
        Item item = event.getItem();
        Position pos = event.getBlock().getLocation();
        Player placer = event.getPlayer();

        if (!GameInfo.getItem().equals(item)) {
            return;
        }

        MySQL.update("INSERT INTO GameData (x, y, z, world, BlockPlacerUUID) VALUES ('" + pos.x + "','" + pos.y + "','" + pos.z + "','" + pos.level.getName() + "','" + placer.getUniqueId() + "')");
    }

    @EventHandler
    public void onBreakBlockEvent(BlockBreakEvent event) {
        Position pos = event.getBlock().getLocation();

        if (!GameInfo.isAGame(pos)) {
            return;
        }

        event.setDrops(new Item[]{GameInfo.getItem()});

        MySQL.update("DELETE FROM GameData WHERE world='" + pos.level.getName() + "' AND x='" + pos.x + "' AND y='" + pos.y + "' AND z='" + pos.z + "'");
    }

    @EventHandler
    public void onTouchABlock(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        Block block = event.getBlock();
        Position pos = block.getLocation();

        if (event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!GameInfo.isAGame(pos)) { //&& movingBlockMap.containsValue(pos)) {
            return;
        }

        if (!GameManager.setWaitingPlayer(pos, p)) {
            return;
        }

        openInventoryWaitingRoom(pos, p);

        event.setCancelled();
    }

    public static void openInventoryWaitingRoom(Position pos, Player p) {
        //create cfi
        ChestFakeInventory cfi = new ChestFakeInventory();
        cfi.setName(cfgMessages.getString("waitingroom.invname"));

        //Add Items to Inventory
        for (Player players : GameManager.getWaitingPlayers(pos)) {

            Item item = getPlayerWaitingItem(players);

            if (players.equals(p)) {
                if (GameManager.getWaitingPlayers(pos).size() >= 2) {
                    for (Player all : inventoryMap.keySet()) {
                        if (all != p) {
                            inventoryMap.get(all).addItem(item);
                        }
                    }
                }
            } else {
                if (!cfi.slots.containsValue(item)) {
                    cfi.addItem(item);
                }
            }
        }

        Item inv18 = GameInfo.getCompleteItem(GameInfo.cfg.getString("waitingroom.inv18")).setCustomName(cfgMessages.getString("waitingroom.inv18"));
        Item inv26 = GameInfo.getCompleteItem(GameInfo.cfg.getString("waitingroom.inv26")).setCustomName(cfgMessages.getString("waitingroom.inv26"));
        cfi.setItem(18, inv18);
        cfi.setItem(21, GameInfo.getCompleteItem(GameInfo.cfg.getString("waitingroom.inv21")).setCustomName(cfgMessages.getString("waitingroom.inv21")));
        cfi.setItem(23, GameInfo.getCompleteItem(GameInfo.cfg.getString("waitingroom.inv23")).setCustomName(cfgMessages.getString("waitingroom.inv23")));
        cfi.setItem(26, inv26);


        //cfi Listener
        cfi.addListener(new FakeInventoryListener() {
            @Override
            public void onSlotChange(FakeSlotChangeEvent fakeSlotChangeEvent) {

                Item item = fakeSlotChangeEvent.getAction().getSourceItem();
                Player p = fakeSlotChangeEvent.getPlayer();
                int slotID = fakeSlotChangeEvent.getAction().getSlot();

                fakeSlotChangeEvent.setCancelled();

                if (!cfi.slots.containsValue(item)) {
                    return;
                }

                //Challenge other players
                if (!item.getName().equals(p.getName())) {
                    Player p2 = Server.getInstance().getPlayer(item.getName());

                    if (p2 != null) {

                        ChestFakeInventory inv = inventoryMap.get(p2);

                        if (inv.slots.get(22) == null) {
                            inv.setItem(22, getPlayerWaitingItem(p));
                            acceptPlayerMap.put(p2, p);
                            return;
                        } else {

                            List<Player> list = new ArrayList<>();

                            if (waitingForAccepting.get(p2) != null) {
                                list = waitingForAccepting.get(p2);
                            }

                            list.add(p);

                            waitingForAccepting.put(p2, list);
                        }

                        return;
                    }
                }

                //View other Games
                if (slotID == 18) {

                    Item inv18Replace = GameInfo.getCompleteItem(GameInfo.cfg.getString("waitingroom.inv18replacement"));
                    Item inv18R = new Item(inv18Replace.getId(), inv18Replace.getDamage()).setCustomName(cfgMessages.getString("waitingroom.inv18replacement"));

                    if (fakeSlotChangeEvent.getInventory().getItem(18).equals(inv18R)) {
                        return;
                    }

                    if (!viewGamesPlayerMap.containsKey(pos)) {
                        cfi.setItem(18, inv18R);
                        Server.getInstance().getScheduler().scheduleDelayedTask(new AsyncTask() {
                            @Override
                            public void onRun() {
                                cfi.setItem(18, inv18);
                            }
                        }, 20 * 5);
                        return;
                    }

                    cfi.close(p);

                    Server.getInstance().getScheduler().scheduleDelayedTask(new AsyncTask() {
                        @Override
                        public void onRun() {
                            showPlayerViewGames(pos, p);
                        }
                    }, 15);
                }

                //Accept Game request
                if (slotID == 21) {
                    Player p2 = acceptPlayerMap.get(p);

                    if (p2 == null) {
                        return;
                    }

                    acceptPlayerMap.remove(p, p2);

                    clearWaitingWindow(p);
                    clearWaitingWindow(p2);

                    Server.getInstance().getScheduler().scheduleDelayedTask(new AsyncTask() {
                        @Override
                        public void onRun() {
                            GameManager.registerGame(p, p2, pos);
                        }
                    }, 20);
                    return;
                }

                //Deny Game request
                if (slotID == 23) {
                    Item item22 = fakeSlotChangeEvent.getInventory().slots.get(22);

                    if (item22 == null) {
                        return;
                    }

                    Player p2 = Server.getInstance().getPlayer(item22.getName());

                    if (p2 == null) {
                        return;
                    }

                    acceptPlayerMap.remove(p, p2);

                    Player nextWaitingPlayer = getNextPlayer(p);

                    inventoryMap.get(p).setItem(22, Item.get(0));

                    if (nextWaitingPlayer == null) {
                        return;
                    }

                    inventoryMap.get(p).setItem(22, getPlayerWaitingItem(nextWaitingPlayer));
                    acceptPlayerMap.put(p, nextWaitingPlayer);
                    return;
                }

                //stats
                if (slotID == 26) {
                    if (!GameInfo.hasPlayedBefor(p)) {
                        Item inv26Replace = GameInfo.getCompleteItem(GameInfo.cfg.getString("waitingroom.inv26replacement"));
                        Item inv26R = new Item(inv26Replace.getId(), inv26Replace.getDamage()).setCustomName(cfgMessages.getString("waitingroom.inv26replacement"));

                        if (fakeSlotChangeEvent.getInventory().getItem(26).equals(inv26R)) {
                            return;
                        }

                        cfi.setItem(26, inv26R);
                        Server.getInstance().getScheduler().scheduleDelayedTask(new AsyncTask() {
                            @Override
                            public void onRun() {
                                cfi.setItem(26, inv26);
                            }
                        }, 20 * 5);
                        return;
                    }

                    backToInventory.put(p, pos);

                    cfi.close(p);

                    Server.getInstance().getScheduler().scheduleDelayedTask(new AsyncTask() {
                        @Override
                        public void onRun() {
                            Statistics.openPlayerStats(p.getUniqueId(), p);
                        }
                    }, 15);
                }
            }
        });

        try {
            p.addWindow(cfi);
            playerGamePositionMap.put(p, pos);
            inventoryMap.put(p, cfi);
        } catch (EventException e) {
            p.sendMessage(cfgMessages.getString("waitingroom.cantopen"));
        }
    }

    //Closing FakeInventory
    @EventHandler
    public void onCloseWindow(InventoryCloseEvent event) {
        Player p = event.getPlayer();

        if (backToInventory.containsKey(p) | !inventoryMap.containsKey(p)) {
            return;
        }

        clearWaitingWindow(p);
    }

    //Block doesn't Moving
    /* UPDATE...
    @EventHandler
    public void onMovingBlock(BlockFromToEvent e) {
        ArrayList<Position> list = GameInfo.getPosList();
        Position pos = e.getFrom();

        if (cooldown.contains(pos)) {
            e.setCancelled();
            return;
        }

        if (!list.contains(pos) | !movingBlockMap.containsKey(pos)) {
            return;
        }

        Position to = e.getTo();

        if (movingBlockMap.get(pos) == to) {
            movingBlockMap.remove(pos, to);
        } else {
            movingBlockMap.put(to, pos);
            MySQL.update("DELETE FROM GameData WHERE x='" + pos.x + "' AND y='" + pos.y + "' AND z='" + pos.z + "' AND world='" + pos.getLevel().getName() + "'");
        }



        for (int i = 0; i <= list.size(); i++) {
            if (list.get(i) == e.getFrom()) {
                String placerName = GameInfo.whoHasPlaced(pos);
                UUID uuid = Server.getInstance().getPlayerExact(placerName).getUniqueId();
                if (uuid == null) {
                    uuid = Server.getInstance().getOfflinePlayer(placerName).getUniqueId();
                }
                pos = e.getTo();
                MySQL.update("INSERT INTO GameData (x, y, z, world, BlockPlacerUUID) VALUES ('" + pos.x + "','" + pos.y + "','" + pos.z + "','" + pos.level.getName() + "','" + uuid + "')");
            }
        }
    }

     */

    //Godmode {burn}
    @EventHandler
    public void onBurnGameBlock(BlockBurnEvent e) {
        if (!GameInfo.getPosList().contains(e.getBlock())) {
            return;
        }

        e.setCancelled();
    }

    //{tnt}
    @EventHandler
    public void onExplodeGameBlock(EntityExplodeEvent e) {
        Entity entity = e.getEntity();

        if (!(entity instanceof EntityPrimedTNT)) {
            return;
        }

        e.getBlockList().removeIf(block -> GameInfo.getPosList().contains(block));
    }
}



