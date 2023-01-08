package com.jasonzyt.passwordfabric.data;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
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

    private static final String FILE_NAME = "./config/password/passwords.json";

    public static String doSha256(String str) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] array = md.digest(str.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte item : array) {
                sb.append(Integer.toHexString((item & 0xFF) | 0x100), 1, 3);
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Data read() {
        Gson gson = new Gson();
        File file = new File(FILE_NAME);
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    return new Data();
                }
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

}
