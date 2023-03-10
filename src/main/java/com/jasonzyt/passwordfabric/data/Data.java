package com.jasonzyt.passwordfabric.data;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Data {

    private final Map<String, String> passwords = new HashMap<>();
    private final Map<String, List<TrustIPInfo>> trustIPs = new HashMap<>();
    private final Map<String, Map<String, Long>> ipLoginTime = new HashMap<>();
    private final Map<String, PlayerInfo> unAuthedPlayerInfo = new HashMap<>();
    private final List<String> whitelist = new LinkedList<>();
    private boolean debugMode = false;

    public static final String FILE_NAME = "./config/password/passwords.json";
    public static final String INVENTORY_DATA_DIR = "./config/password/inventories/";

    public static Data read() {
        Gson gson = new Gson();
        File file = new File(FILE_NAME);
        try {
            if (!file.exists()) {
                Data res = new Data();
                res.save();
                return res;
            }
            return gson.fromJson(new FileReader(FILE_NAME), Data.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new Data();
        }
    }

    public void save() {
        Gson gson = new Gson();
        String json = gson.toJson(this, Data.class);
        FileWriter writer;
        if (!new File(FILE_NAME).getParentFile().exists()) {
            new File(FILE_NAME).getParentFile().mkdirs();
        }
        try {
            writer = new FileWriter(FILE_NAME);
            writer.write(json);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> getPasswords() {
        return passwords;
    }

    public void addPassword(String uuid, String password) {
        passwords.put(uuid, password);
        save();
    }

    public String getPassword(String uuid) {
        return passwords.get(uuid);
    }

    public void removePassword(String uuid) {
        passwords.remove(uuid);
        save();
    }

    public boolean hasPassword(String uuid) {
        return passwords.containsKey(uuid);
    }

    public boolean checkPassword(String uuid, String password) {
        return passwords.get(uuid).equals(password);
    }

    public void checkTrustIPs() {
        for (List<TrustIPInfo> list : trustIPs.values()) {
            list.removeIf(TrustIPInfo::isExpired);
        }
        save();
    }

    public Map<String, List<TrustIPInfo>> getTrustIPs() {
        return trustIPs;
    }

    public void addTrustIP(String uuid, String ip, long time) {
        trustIPs.computeIfAbsent(uuid, k -> new LinkedList<>()).add(new TrustIPInfo(ip, time));
        save();
    }

    public void removeTrustIP(String uuid, String ip) {
        trustIPs.computeIfAbsent(uuid, k -> new LinkedList<>()).removeIf(info -> info.getAddress().equals(ip));
        save();
    }

    public boolean hasTrustIP(String uuid, String ip) {
        checkTrustIPs();
        return trustIPs.computeIfAbsent(uuid, k -> new LinkedList<>()).stream().anyMatch(info -> info.getAddress().equals(ip));
    }

    public void removePlayerAllTrustIPs(String uuid) {
        trustIPs.remove(uuid);
        save();
    }

    public Map<String, Map<String, Long>> getIPLoginTime() {
        return ipLoginTime;
    }

    public void addIPLoginTime(String uuid, String ip, long time) {
        ipLoginTime.computeIfAbsent(uuid, k -> new HashMap<>()).put(ip, time);
        save();
    }

    public void removeIPLoginTime(String uuid, String ip) {
        ipLoginTime.computeIfAbsent(uuid, k -> new HashMap<>()).remove(ip);
        save();
    }

    public Map<String, Long> getPlayerIPLoginTimes(String uuid) {
        return ipLoginTime.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    public Map<String, PlayerInfo> getUnAuthedPlayerInfo() {
        return unAuthedPlayerInfo;
    }

    public void addUnAuthedPlayerInfo(String uuid, PlayerInfo info) {
        unAuthedPlayerInfo.put(uuid, info);
        save();
    }

    public void removeUnAuthedPlayerInfo(String uuid) {
        unAuthedPlayerInfo.remove(uuid);
        save();
    }

    public PlayerInfo getUnAuthedPlayerInfo(String uuid) {
        return unAuthedPlayerInfo.get(uuid);
    }

    public boolean hasUnAuthedPlayerInfo(String uuid) {
        return unAuthedPlayerInfo.containsKey(uuid);
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public void addWhitelist(String name) {
        whitelist.add(name);
        save();
    }

    public void removeWhitelist(String name) {
        whitelist.remove(name);
        save();
    }

    public boolean hasWhitelist(String name) {
        return whitelist.contains(name);
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        save();
    }

    public boolean isDebugMode() {
        return debugMode;
    }

}
