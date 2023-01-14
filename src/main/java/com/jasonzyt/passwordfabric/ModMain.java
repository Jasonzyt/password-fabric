package com.jasonzyt.passwordfabric;

import com.jasonzyt.passwordfabric.command.PasswordCommand;
import com.jasonzyt.passwordfabric.data.Data;
import com.jasonzyt.passwordfabric.data.PlayerInfo;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ModMain implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("PasswordFabric");
    public static final List<ServerPlayer> PLAYERS = new LinkedList<>();
    public static final List<ServerPlayer> UNAUTHED_PLAYERS = new LinkedList<>();
    public static final List<ServerPlayer> PLAYERS_ENTERED_PASSWORD = new LinkedList<>();
    public static final Map<ServerPlayer, PlayerInfo> UNAUTHED_PLAYER_INFO = new HashMap<>();
    public static final Map<ServerPlayer, LocalDateTime> PLAYER_LOGIN_TIME = new HashMap<>();
    public static MinecraftServer server;
    public static Data data;

    public static void logDebug(String message) {
        if (data.isDebugMode()) {
            LOGGER.info(message);
        }
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Password loaded! Author: Jasonzyt");
        if (!new File(Data.INVENTORY_DATA_DIR).exists()) {
            new File(Data.INVENTORY_DATA_DIR).mkdirs();
        }
        data = Data.read();
        if (!data.getUnAuthedPlayerInfo().isEmpty()) {
            LOGGER.warn("Found unauthorized player positions in data file, they will be restored as soon as the player logs in.");
        }
        CommandRegistrationCallback.EVENT.register(PasswordCommand.INSTANCE::register);
    }

    public static void onPlayerLogIn(ServerPlayer player) {
        PLAYERS.add(player);
        // Loopback address check
        if (player.getIpAddress().equals("127.0.0.1")) {
            LOGGER.info("Player " + player.getName().getString() + " logged in from localhost, skipping authentication.");
            return;
        }
        // Whitelist check
        if (!data.hasWhitelist(player.getName().getString())) {
            player.connection.disconnect(Component.literal("You are not in the whitelist! Please contact server admin."));
            return;
        }
        // Backed-up info check
        if (data.hasUnAuthedPlayerInfo(player.getStringUUID())) {
            UNAUTHED_PLAYER_INFO.put(player, data.getUnAuthedPlayerInfo(player.getStringUUID()));
            data.removeUnAuthedPlayerInfo(player.getStringUUID());
        } else {
            UNAUTHED_PLAYER_INFO.put(player, new PlayerInfo(player, true));
        }
        logDebug("onPlayerLogIn: " + new PlayerInfo(player));
        data.addUnAuthedPlayerInfo(player.getStringUUID(), new PlayerInfo(player)); // Backup position to data file
        PLAYER_LOGIN_TIME.put(player, LocalDateTime.now());
        data.addIPLoginTime(player.getStringUUID(), player.getIpAddress(), System.currentTimeMillis());
//        try {
//            Utils.IPInfo ipInfo = Utils.getIPInfo(player.getIpAddress());
//            if (ipInfo != null) {
//                logger.info("Player " + player.getName().getString() + "(" + player.getStringUUID() + ") logged in from [" + player.getIpAddress() + "] " + ipInfo.getCountry() + " " + ipInfo.getRegionName() + " " + ipInfo.getCity());
//                player.sendSystemMessage(Component.literal("欢迎来自 " + ipInfo.getCountry() + " " + ipInfo.getRegionName() + " " + ipInfo.getCity() + " 的朋友!").withStyle(ChatFormatting.GREEN));
//            } else {
//                logger.info("Player " + player.getName().getString() + "(" + player.getStringUUID() + ") logged in from " + player.getIpAddress());
//                player.sendSystemMessage(Component.literal("欢迎!").withStyle(ChatFormatting.GREEN));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        if (data.hasTrustIP(player.getStringUUID(), player.getIpAddress())) {
            //player.sendSystemMessage(Component.literal("信任IP免验证生效, 无需输入密码").withStyle(ChatFormatting.GREEN));
        } else {
            // Clear inventory
            player.getInventory().clearContent();
            player.getInventory().armor.clear();
            player.getInventory().offhand.clear();
            player.getInventory().items.clear();
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.slotsChanged(player.getInventory());
            UNAUTHED_PLAYERS.add(player);
            if (!data.hasPassword(player.getStringUUID())) {
                player.sendSystemMessage(Component.literal("您还未设置密码, 请发送命令 \"/pwd s <密码>\" 来设置密码, 否则无法进入服务器").withStyle(ChatFormatting.BLUE));
            } else {
                player.sendSystemMessage(Component.literal("你还未通过验证! 请发送命令 \"/pwd a <密码>\" 来验证, 否则无法进入服务器").withStyle(ChatFormatting.RED));
            }
        }
    }

    public static void onPlayerLogOut(ServerPlayer player) {
        if (UNAUTHED_PLAYERS.contains(player)) {
            logDebug("Unauthorized player " + player.getName().getString() + " logged out, restoring info.");
            PlayerInfo info = UNAUTHED_PLAYER_INFO.get(player);
            info.apply(player);
            data.removeUnAuthedPlayerInfo(player.getStringUUID());
        }
        PLAYER_LOGIN_TIME.remove(player);
        PLAYERS.remove(player);
    }

    public static void tick() {
        for (ServerPlayer player : UNAUTHED_PLAYERS) {
            player.teleportTo(0, 1024, 0);
            LocalDateTime loginTime = PLAYER_LOGIN_TIME.get(player);
            if (loginTime != null && LocalDateTime.now().isAfter(loginTime.plusSeconds(60))) {
                player.connection.disconnect(Component.literal("You did not enter your password within 60 seconds!"));
            }
        }
    }

    public static void onPlayerAuthed(ServerPlayer player) {
        if (UNAUTHED_PLAYERS.contains(player)) {
            logDebug("onPlayerAuthed: Player " + player.getName().getString() + " authenticated, restoring info.");
            UNAUTHED_PLAYERS.remove(player);
            if (UNAUTHED_PLAYER_INFO.containsKey(player)) {
                PlayerInfo info = UNAUTHED_PLAYER_INFO.get(player);
                info.apply(player);
                UNAUTHED_PLAYER_INFO.remove(player);
            }
        }
        PLAYERS_ENTERED_PASSWORD.add(player);
        data.removeUnAuthedPlayerInfo(player.getStringUUID());
        player.sendSystemMessage(Component.literal("验证成功!").withStyle(ChatFormatting.GREEN));
        if (!data.hasTrustIP(player.getStringUUID(), player.getIpAddress())) {
            player.sendSystemMessage(Component.literal("Tips: 如果不想每次进入都输入密码, 可以发送命令 \"/pwd trust\" 信任此IP 30天").withStyle(ChatFormatting.YELLOW));
        }
    }

    public static void onPlayerChat(ServerPlayer player, CallbackInfo ci) {
        if (UNAUTHED_PLAYERS.contains(player)) {
            logDebug("onPlayerChat: Cancelling for Player " + player.getName().getString());
            player.sendSystemMessage(Component.literal("你还未通过验证! 请发送命令 \"/pwd a <密码>\" 来验证, 否则无法进入服务器").withStyle(ChatFormatting.RED));
            ci.cancel();
        }
    }

    public static void onPlayerSelectSlot(ServerPlayer player, CallbackInfo ci) {
        if (UNAUTHED_PLAYERS.contains(player)) {
            logDebug("onPlayerSelectSlot: Cancelling for Player " + player.getName().getString());
            ci.cancel();
            player.inventoryMenu.slotsChanged(player.getInventory());
            player.containerMenu.broadcastChanges();
        }
    }

    public static void onPlayerUseItem(ServerPlayer player, CallbackInfo ci) {
        if (UNAUTHED_PLAYERS.contains(player)) {
            logDebug("onPlayerUseItem: Cancelling for Player " + player.getName().getString());
            ci.cancel();
            player.inventoryMenu.slotsChanged(player.getInventory());
            player.containerMenu.broadcastChanges();
        }
    }

    public static void onPlayerInteract(ServerPlayer player, CallbackInfo ci) {
        if (UNAUTHED_PLAYERS.contains(player)) {
            logDebug("onPlayerInteract: Cancelling for Player " + player.getName().getString());
            ci.cancel();
        }
    }

    public static void onPlayerMove(ServerPlayer player, CallbackInfo ci) {
        if (UNAUTHED_PLAYERS.contains(player)) {
            //logDebug("onPlayerMove: Cancelling for Player " + player.getName().getString());
            ci.cancel();
        }
    }

    public static void onServerLoaded(MinecraftServer server) {
        ModMain.server = server;
    }
}
