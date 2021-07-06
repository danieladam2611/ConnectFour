package de.daniel.CFCommands;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.utils.Config;
import de.daniel.CFManagment.GameInfo;
import de.daniel.CFManagment.MySQL;
import de.daniel.ConnectFourMain;

import java.util.ArrayList;

public class ConnectFourCommand extends Command {

    //private static Config cfg = GameInfo.cfg; -> Für UpdateBlockID //Ignore
    private static Config cfgMessages = ConnectFourMain.cfgMessages;

    public ConnectFourCommand() {
        super(cfgMessages.getString("command.name"),
                cfgMessages.getString("command.description"),
                cfgMessages.getString("command.usageMessage"),
                cfgMessages.getString("command.aliases").split(","));

        commandParameters.clear();
        commandParameters.put("default", new CommandParameter[]{
                CommandParameter.newEnum("", new String[]{"allgames", "deletegame", "creategame", "stats", "blocks"})
        });
    }

    //UPDATEBLOCKID BETA-UPDATE -> Ignore
    /*
    private static void updateBlockID(Player p, String id) {
        Block block;

        if (id.contains(":")) {
            String[] ids = id.split(":");
            block = Block.get(Integer.parseInt(ids[0]), Integer.parseInt(ids[1]));
        } else {
            block = Block.get(Integer.parseInt(id));
        }

        if (block == GameInfo.getBlock()) {
            p.sendMessage("Block wurde schon festgelegt");
            return;
        }
        try {
            if (!block.isSolid()) {
                p.sendMessage("Diese ID kann nicht genommen Werden...");
                return;
            }
        } catch (ArrayIndexOutOfBoundsException exception) {
            p.sendMessage("Diese ID kann nicht genommen Werden...");
            return;
        }

        for (Position pos : GameInfo.getPosList()) {
            pos.getLevel().setBlock(pos, block);
        }

        cfg.set("id.block", id);
        cfg.save();
    }
     */

    //CREATE GAME
    private static void creategame(Player p, int amount) {
        Item i = GameInfo.getItem();
        i.setCount(amount);

        if (!p.getInventory().canAddItem(i)) {
            p.sendMessage(cfgMessages.getString("create.fullinventory"));
            return;
        }

        p.getInventory().addItem(i);
    }

    //DELETE GAME
    private static void deletegame(Player p, int gameid) {

        if (gameid >= GameInfo.getPosList().size()) {
            p.sendMessage(cfgMessages.getString("delete.gamedoesntexist"));
            return;
        }

        Position pos = GameInfo.getPosList().get(gameid);

        pos.getLevel().setBlock(pos, Block.get(BlockID.AIR));

        p.sendMessage(cfgMessages.getString("delete.game")
                .replace("%pos", (int) pos.x + " " + (int) pos.y + " " + (int) pos.z)
                .replace("%placer", GameInfo.whoHasPlaced(pos)));

        MySQL.update("DELETE FROM GameData WHERE x='" + pos.x + "' AND y='" + pos.y + "' AND z='" + pos.z + "' AND world='" + pos.getLevel().getName() + "'");
    }

    //VIEW ALL GAMES (LOCATIONS)
    private static void allgames(Player p) {
        ArrayList<Position> posList = GameInfo.getPosList();
        if (posList.isEmpty()) {
            p.sendMessage(cfgMessages.getString("no.games"));
            return;
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < posList.size(); i++) {

            Position pos = posList.get(i);

            int x = (int) pos.x;
            int y = (int) pos.y;
            int z = (int) pos.z;

            sb.append("§f" + i + ") §e" + x + " " + y + " " + z + " §f- §b" + GameInfo.whoHasPlaced(pos) + "\n");
        }

        p.sendMessage(sb.toString());
    }

    @Override
    public boolean execute(CommandSender sender, String s, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(cfgMessages.getString("no.console"));
            return false;
        }

        Player p = (Player) sender;
        String identifierString = args[0];

        if (identifierString.equals("stats") | identifierString.equals("statistics")) {
            Statistics.openStats(p);
            return true;
        } else if (identifierString.equals("blocks") | identifierString.equals("b")) {
            ChangeBlocks.openChangeBlockInventory(p, 1);
            return true;
        }

        if (!p.hasPermission("connectfour.command.manager")) {
            p.sendMessage(cfgMessages.getString("no.permission"));
            return false;
        }

        if (args.length == 1) {
            if (identifierString.equals("allgames")) {
                allgames(p);
                return true;
            } else if (identifierString.equals("manager")) {

            }
        }

        if (args.length != 2) {
            p.sendMessage(usageMessage);
            return false;
        }

        int value = 0;

        if (args[1].equals("*") | args[1].equals("all")) {
            if (identifierString.equals("deletegame") | identifierString.equals("dg")) {
                for (int i = (GameInfo.getPosList().size() - 1); i >= 0; i--) {
                    deletegame(p, i);
                }
                return true;
            }
        } else {
            try {
                value = Integer.parseInt(args[1]);
            } catch (NumberFormatException numberFormatException) {
                p.sendMessage(cfgMessages.getString("no.number").replace("%value", args[1]));
                return false;
            }
        }


        if (identifierString.equals("updateblockid")) {
            //updateBlockID(p, args[1]);
        } else if (identifierString.equals("deletegame") | identifierString.equals("dg")) {
            deletegame(p, value);
            return true;
        } else if (identifierString.equals("creategame") | identifierString.equals("cg")) {
            creategame(p, value);
            return true;
        }

        return false;
    }
}
