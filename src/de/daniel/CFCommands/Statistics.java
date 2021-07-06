package de.daniel.CFCommands;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.utils.Config;
import de.daniel.CFManagment.GameInfo;
import de.daniel.CFManagment.GameManager;
import de.daniel.CFManagment.MySQL;
import de.daniel.ConnectFourMain;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Statistics implements Listener {

    private static Config cfgMessages = ConnectFourMain.cfgMessages;

    private static List<String> getTopPlayerList() {
        ResultSet rs = MySQL.getResult("SELECT * FROM PlayerInfos ORDER BY Wins DESC LIMIT " + GameInfo.cfg.getString("topstats.playercount"));
        List<String> list = new ArrayList<>();
        try {
            while (rs.next()) {
                list.add(rs.getString("PlayerName"));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return list;
    }

    public static void openStats(Player p) {
        FormWindowSimple gui = new FormWindowSimple(cfgMessages.getString("stats.title"), "");

        gui.getButtons().add(new ElementButton(cfgMessages.getString("stats.button1")));
        if (GameInfo.hasPlayedBefor(p)) {
            gui.getButtons().add(new ElementButton(cfgMessages.getString("stats.button2")));
        } else {
            gui.getButtons().add(new ElementButton(cfgMessages.getString("stats.button2neverplayed")));
        }

        gui.getButtons().add(new ElementButton(cfgMessages.getString("button.back")));

        p.showFormWindow(gui, 18700);
    }

    public static void openPlayerStats(UUID uuid, Player p) {

        int wins = GameManager.getwinningMatches(uuid);
        int loses = GameManager.getlosingMatches(uuid);
        int decided = GameManager.getnotDecided(uuid);

        if (wins == 0 && loses == 0 && decided == 0) {
            p.sendMessage(cfgMessages.getString("window.cantopen"));
            return;
        }

        float kd = (float) wins / (loses == 0 ? 1 : loses);
        long time = 0;
        String playername = null;
        ResultSet rs = MySQL.getResult("SELECT * FROM PlayerInfos WHERE UUID='" + uuid + "'");
        try {
            while (rs.next()) {
                time = (rs.getLong("PlayedTime") / 60);
                playername = rs.getString("Playername");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        ResultSet rsRank = MySQL.getResult("SELECT * FROM PlayerInfos ORDER BY Wins DESC");
        int rank = 1;
        try {
            while (rsRank.next()) {
                if (rsRank.getString("UUID").equals(uuid.toString())) {
                    break;
                }
                rank++;
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        kd = ((int) (kd * 100)) / 100F;

        FormWindowSimple gui = new FormWindowSimple(
                cfgMessages.getString("playerstats.titel").replace("%player", playername),
                cfgMessages.getString("playerstats.content")
                        .replace("%n", "\n")
                        .replace("%wins", String.valueOf(wins))
                        .replace("%loses", String.valueOf(loses))
                        .replace("%decided", String.valueOf(decided))
                        .replace("%kd", (String.valueOf(kd)))
                        .replace("%playedgames", String.valueOf(wins + loses + decided))
                        .replace("%time", String.valueOf(time))
                        .replace("%rank", String.valueOf(rank)));

        gui.getButtons().add(new ElementButton(cfgMessages.getString("button.back")));

        p.showFormWindow(gui, 18701);
    }

    public static void openTopPlayerStats(Player p) {
        FormWindowSimple gui = new FormWindowSimple(cfgMessages.getString("topstats.title"), "");

        for (String topPlayers : getTopPlayerList()) {
            gui.getButtons().add(new ElementButton(topPlayers));
        }
        gui.getButtons().add(new ElementButton(cfgMessages.getString("button.back")));

        p.showFormWindow(gui, 18702);
    }

    @EventHandler
    public void onResponseFormWindow(PlayerFormRespondedEvent e) {
        if (!(e.getWindow() instanceof FormWindowSimple)) {
            return;
        }

        FormWindowSimple gui = (FormWindowSimple) e.getWindow();
        Player p = e.getPlayer();

        if (gui.wasClosed()) {
            if (de.daniel.CFListener.Listener.backToInventory.containsKey(p)) {
                de.daniel.CFListener.Listener.openInventoryWaitingRoom(de.daniel.CFListener.Listener.backToInventory.get(p), p);
                de.daniel.CFListener.Listener.backToInventory.remove(p);
                return;
            }
            return;

        }

        int formID = e.getFormID();
        int responseID = gui.getResponse().getClickedButtonId();
        String buttonText = gui.getResponse().getClickedButton().getText();

        //DefaultWindow
        if (formID == 18700) {
            if (responseID == 0) { //TopPlayerStats
                openTopPlayerStats(p);
            } else if (responseID == 1) { //PlayerStats -> Self
                if (buttonText.equals(cfgMessages.getString("stats.button2neverplayed"))) {
                    return;
                }
                openPlayerStats(p.getUniqueId(), p);
            }
        } else if (formID == 18701) { //PlayerStats -> Self
            if (de.daniel.CFListener.Listener.backToInventory.containsKey(p)) {
                de.daniel.CFListener.Listener.openInventoryWaitingRoom(de.daniel.CFListener.Listener.backToInventory.get(p), p);
                de.daniel.CFListener.Listener.backToInventory.remove(p);
            } else {
                openStats(p);
            }
        } else if (formID == 18702) { //TopPlayerStats
            if (buttonText.equals(cfgMessages.getString("button.back"))) {
                openStats(p);
                return;
            }
            ResultSet rs = MySQL.getResult("SELECT * FROM PlayerInfos WHERE Playername='" + buttonText + "'");
            try {
                if (rs.next()) {
                    openPlayerStats(UUID.fromString(rs.getString("UUID")), p);
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }
}
