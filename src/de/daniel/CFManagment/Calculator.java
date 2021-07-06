package de.daniel.CFManagment;

import cn.nukkit.Player;

import java.util.ArrayList;
import java.util.HashMap;

public class Calculator {

    public static HashMap<Player, String> userPosMap = new HashMap<>();
    public static HashMap<Integer, ArrayList<Integer>> posMap = new HashMap<>();
    public static HashMap<Integer, ArrayList<Integer>> winMap = new HashMap<>();

    public static int getNewSlotID(int gameID, int slotID) {
        ArrayList<Integer> list = new ArrayList<>();

        if (posMap.containsKey(gameID)) {
            list = posMap.get(gameID);
        }

        for (int i = slotID; i < 53; i = i + 9) {
            if (i > 45) {
                if (!list.contains(i)) {
                    return i;
                }
            }
            if (list.contains(i)) {
                return (i - 9);
            }
        }

        return 0;
    }

    public static boolean hasWon(Player p, int gameID, int slotID) {

        ArrayList<Integer> list = new ArrayList<>();

        String newPosList;
        if (userPosMap.get(p) != null) {
            newPosList = userPosMap.get(p) + slotID + ",";
        } else {
            newPosList = slotID + ",";
        }
        userPosMap.remove(p);
        userPosMap.put(p, newPosList);

        String[] args = userPosMap.get(p).split(",");

        for (String s : args) {
            list.add(Integer.parseInt(s));
        }

        winMap.remove(gameID);
        winMap.put(gameID, new ArrayList<>());

        //oben&unten
        if (winDown(list, slotID, gameID)) {
            list.clear();
            return true;
        }
        //Rechts&Links
        if (winRightAndLeft(list, slotID, gameID)) {
            list.clear();
            return true;
        }
        //schrägLinks
        if (winInclineddownup(list, slotID, gameID)) {
            list.clear();
            return true;
        }
        //schrägRechts
        if (winInclinedupdown(list, slotID, gameID)) {
            list.clear();
            return true;
        }

        /* Vielleicht nicht nötig Test...
        ArrayList<Integer> posList = posMap.get(gameID);
        if (posList == null) {
            posList = new ArrayList<>();
        }

         */

        winMap.remove(gameID);
        winMap.put(gameID, new ArrayList<>());

        list.clear();
        return false;
    }

    private static boolean winDown(ArrayList<Integer> list, int pos, int gameID) {

        int minYPos = 0;
        for (int i = pos; i > 0; i = i - 9) {
            minYPos = i;
        }

        int steak = 0;

        for (int i = minYPos; i < 53; i = i + 9) {
            if (list.contains(i)) {
                steak++;
                ArrayList<Integer> winList = winMap.get(gameID);
                winList.add(i);
                winMap.put(gameID, winList);
                if (steak >= 4) {
                    return true;
                }
            } else {
                steak = 0;
                winMap.remove(gameID);
                winMap.put(gameID, new ArrayList<>());
            }
        }
        return false;
    }

    private static boolean winRightAndLeft(ArrayList<Integer> list, int pos, int gameID) {

        int minXPos = 0;

        //pos = 23

        firstfor:
        for (int i2 = 45; i2 > 0; i2 = i2 - 9) {
            if (pos > i2) {
                for (int i = pos; i >= i2; i--) {
                    if (i == i2) {
                        minXPos = i;
                        break firstfor;
                    }
                }
            }
        }

        int steak = 0;

        for (int i = minXPos; i < (minXPos + 9); i++) {
            if (list.contains(i)) {
                steak++;
                ArrayList<Integer> winList = winMap.get(gameID);
                winList.add(i);
                winMap.put(gameID, winList);
                if (steak >= 4) {
                    return true;
                }
            } else {
                steak = 0;
                winMap.remove(gameID);
                winMap.put(gameID, new ArrayList<>());
            }
        }
        return false;
    }

    private static boolean winInclinedupdown(ArrayList<Integer> list, int pos, int gameID) {
        int steak = 0;

        int minXYPos = 0;
        for (int i = pos; i > 0; i = i - 10) {
            if (!GameManager.isOnGameField(i)) {
                break;
            }
            minXYPos = i;
        }

        for (int i = minXYPos; i < 53; i = i + 10) {
            if (list.contains(i)) {
                steak++;
                ArrayList<Integer> winList = winMap.get(gameID);
                winList.add(i);
                winMap.put(gameID, winList);
                if (steak >= 4) {
                    return true;
                }
            } else {
                steak = 0;
                winMap.remove(gameID);
                winMap.put(gameID, new ArrayList<>());
            }
        }
        return false;
    }

    private static boolean winInclineddownup(ArrayList<Integer> list, int pos, int gameID) {
        int steak = 0;

        int minXYPos = 0;
        for (int i = pos; i > 0; i = i - 8) {
            if (!GameManager.isOnGameField(i)) {
                break;
            }
            minXYPos = i;
        }

        for (int i = minXYPos; i < 53; i = i + 8) {
            if (list.contains(i)) {
                steak++;
                ArrayList<Integer> winList = winMap.get(gameID);
                winList.add(i);
                winMap.put(gameID, winList);
                if (steak >= 4) {
                    return true;
                }
            } else {
                steak = 0;
                winMap.remove(gameID);
                winMap.put(gameID, new ArrayList<>());
            }
        }
        return false;
    }
}
