package de.daniel.CFCommands;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.item.Item;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.utils.Config;
import com.nukkitx.fakeinventories.inventory.ChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.FakeInventoryListener;
import com.nukkitx.fakeinventories.inventory.FakeSlotChangeEvent;
import de.daniel.CFManagment.GameInfo;
import de.daniel.CFManagment.GameManager;
import de.daniel.CFManagment.MySQL;
import de.daniel.ConnectFourMain;

public class ChangeBlocks {

    private static Config cfgMessages = ConnectFourMain.cfgMessages;

    public static void openChangeBlockInventory(Player p, int block) {
        String[] args = GameInfo.cfg.getString("changeblock.blocklist" + block).split(",");

        if (args.length > 18) {
            return;
        }

        ChestFakeInventory cfi = new ChestFakeInventory();
        cfi.setName(cfgMessages.getString("changeblock.title"));

        Item usedItem = null;

        if (block == 1) {
            Item item = GameManager.getPlayerItem1(p);

            Item defaultItem = GameInfo.getCompleteItem(GameInfo.cfg.getString("block.defaultplayer1"));
            if (!item.equals(defaultItem)) {
                cfi.addItem(defaultItem);
            }

            usedItem = item;

            cfi.setItem(22, item);
            Item inv26 = GameInfo.getCompleteItem(GameInfo.cfg.getString("changeblock.inv26"));
            cfi.setItem(26, inv26.setCustomName(cfgMessages.getString("changeblock.inv26"))); //next page
        } else {
            Item item = GameManager.getPlayerItem2(p);

            Item defaultItem = GameInfo.getCompleteItem(GameInfo.cfg.getString("block.defaultplayer2"));
            if (!item.equals(defaultItem)) {
                cfi.addItem(defaultItem);
            }

            usedItem = item;

            cfi.setItem(22, item);
            Item inv18 = GameInfo.getCompleteItem(GameInfo.cfg.getString("changeblock.inv18"));
            cfi.setItem(18, inv18.setCustomName(cfgMessages.getString("changeblock.inv18"))); //next page
        }

        for (String itemid : args) {
            String[] itemPermString = itemid.split("/");
            Item item = Item.fromString(itemPermString[0]);
            if (!item.equals(usedItem)) {
                if (itemPermString.length == 2) {
                    if (p.hasPermission(itemPermString[1])) {
                        cfi.addItem(item);
                    }
                } else if (itemPermString.length == 1) {
                    cfi.addItem(item);
                }
            }
        }

        cfi.addListener(new FakeInventoryListener() {
            @Override
            public void onSlotChange(FakeSlotChangeEvent fakeSlotChangeEvent) {
                int slotID = fakeSlotChangeEvent.getAction().getSlot();
                Item item = fakeSlotChangeEvent.getAction().getSourceItem();
                Player p = fakeSlotChangeEvent.getPlayer();

                fakeSlotChangeEvent.setCancelled();

                if (item == null) {
                    return;
                }

                if (slotID == 22) {
                    return;
                }

                if (!cfi.slots.containsValue(item)) {
                    return;
                }

                if (slotID == 18) {
                    cfi.close(p);
                    Server.getInstance().getScheduler().scheduleDelayedTask(new AsyncTask() {
                        @Override
                        public void onRun() {
                            openChangeBlockInventory(p, 1);
                        }
                    }, 15);
                    return;
                }
                if (slotID == 26) {
                    cfi.close(p);
                    Server.getInstance().getScheduler().scheduleDelayedTask(new AsyncTask() {
                        @Override
                        public void onRun() {
                            openChangeBlockInventory(p, 2);
                        }
                    }, 15);
                    return;
                }

                int id = item.getId();
                int meta = item.getDamage();

                if (GameInfo.hasPlayedBefor(p)) {
                    MySQL.update("UPDATE PlayerInfos SET ItemPlayer" + block + "='" + item.getId() + ":" + item.getDamage() + "' WHERE UUID='" + p.getUniqueId() + "'");
                } else {
                    MySQL.update("INSERT INTO PlyerInfos (UUID, Playername, ItemPlayer" + block + ") VALUES ('" + p.getUniqueId() + "','" + p.getName() + "','" + item.getId() + ":" + item.getDamage() + "')");
                }
                cfi.close(p);

                Server.getInstance().getScheduler().scheduleDelayedTask(new AsyncTask() {
                    @Override
                    public void onRun() {
                        openChangeBlockInventory(p, block);
                    }
                }, 15);
            }
        });

        p.addWindow(cfi);
    }
}
