package com.jasonzyt.passwordfabric;

import com.jasonzyt.passwordfabric.command.PasswordCommand;
import com.jasonzyt.passwordfabric.data.Data;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ModMain implements ModInitializer {
    public static final Logger logger = LoggerFactory.getLogger("PasswordFabric");
    public static final List<ServerPlayer> players = new LinkedList<>();
    public static final List<ServerPlayer> unAuthedPlayers = new LinkedList<>();
    public static final List<ServerPlayer> playersEnteredPassword = new LinkedList<>();
    public static final Map<ServerPlayer, Vec3> playerNotAuthedPositions = new HashMap<>();
    public static final Map<ServerPlayer, LocalDateTime> playerLoginTime = new HashMap<>();
    public static MinecraftServer server;
    public static Data data;

    @Override
    public void onInitialize() {
        logger.info("Password loaded! Author: Jasonzyt");
        data = Data.read();
        if (!data.getPlayerNotAuthedPositions().isEmpty()) {
            logger.warn("Found unauthorized player positions in data file, they will be restored as soon as the player logs in.");
        }
        CommandRegistrationCallback.EVENT.register(PasswordCommand.INSTANCE::register);
    }

    public static void onPlayerLogIn(ServerPlayer player) {
        players.add(player);
        if (player.getIpAddress().equals("127.0.0.1")) {
            logger.info("Player " + player.getName().getString() + " logged in from localhost, skipping authentication.");
            return;
        }
        if (data.hasPlayerNotAuthedPosition(player.getStringUUID())) {
            playerNotAuthedPositions.put(player, data.getPlayerNotAuthedPosition(player.getStringUUID()));
            data.removePlayerNotAuthedPosition(player.getStringUUID());
        } else {
            playerNotAuthedPositions.put(player, player.position());
        }
        if (!data.hasWhitelist(player.getName().getString())) {
            player.connection.disconnect(Component.literal("You are not in the whitelist! Please contact server admin."));
            return;
        }
        data.addPlayerNotAuthedPosition(player.getStringUUID(), player.position()); // Backup position to data file
        playerLoginTime.put(player, LocalDateTime.now());
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
            unAuthedPlayers.add(player);
            if (!data.hasPassword(player.getStringUUID())) {
                player.sendSystemMessage(Component.literal("您还未设置密码, 请发送命令 \"/pwd s <密码>\" 来设置密码, 否则无法进入服务器").withStyle(ChatFormatting.BLUE));
            } else {
                player.sendSystemMessage(Component.literal("你还未通过验证! 请发送命令 \"/pwd a <密码>\" 来验证, 否则无法进入服务器").withStyle(ChatFormatting.RED));
            }
        }
    }

    public static void onPlayerLogOut(ServerPlayer player) {
        if (unAuthedPlayers.contains(player)) {
            Vec3 pos = playerNotAuthedPositions.get(player);
            player.teleportTo(pos.x, pos.y, pos.z);
            data.removePlayerNotAuthedPosition(player.getStringUUID());
        }
        playerLoginTime.remove(player);
        players.remove(player);
    }

    public static void tick() {
        for (ServerPlayer player : unAuthedPlayers) {
            player.teleportTo(0, 1024, 0);
            LocalDateTime loginTime = playerLoginTime.get(player);
            if (loginTime != null && LocalDateTime.now().isAfter(loginTime.plusSeconds(60))) {
                player.connection.disconnect(Component.literal("You did not enter your password within 60 seconds!"));
            }
        }
    }

    public static void onPlayerAuthed(ServerPlayer player) {
        if (unAuthedPlayers.contains(player)) {
            unAuthedPlayers.remove(player);
            if (playerNotAuthedPositions.containsKey(player)) {
                Vec3 pos = playerNotAuthedPositions.get(player);
                player.teleportTo(pos.x, pos.y, pos.z);
                playerNotAuthedPositions.remove(player);
            }
        }
        playersEnteredPassword.add(player);
        data.removePlayerNotAuthedPosition(player.getStringUUID());
        player.sendSystemMessage(Component.literal("验证成功!").withStyle(ChatFormatting.GREEN));
        if (!data.hasTrustIP(player.getStringUUID(), player.getIpAddress())) {
            player.sendSystemMessage(Component.literal("Tips: 如果不想每次进入都输入密码, 可以发送命令 \"/pwd trust\" 信任此IP 30天").withStyle(ChatFormatting.YELLOW));
        }
    }

    public static void onPlayerDropItem(ServerPlayer player, CallbackInfo ci) {
        if (unAuthedPlayers.contains(player)) {
            ci.cancel();
        }
    }

    public static void onPlayerSwapHand(ServerPlayer player, CallbackInfo ci) {
        if (unAuthedPlayers.contains(player)) {
            ci.cancel();
        }
    }

    public static void onPlayerChat(ServerPlayer player, CallbackInfo ci) {
        if (unAuthedPlayers.contains(player)) {
            player.sendSystemMessage(Component.literal("你还未通过验证! 请发送命令 \"/pwd a <密码>\" 来验证, 否则无法进入服务器").withStyle(ChatFormatting.RED));
            ci.cancel();
        }
    }

    public static void onPlayerSelectSlot(ServerPlayer player, CallbackInfo ci) {
        if (unAuthedPlayers.contains(player)) {
            ci.cancel();
        }
    }

    public static void onPlayerUseItem(ServerPlayer player, CallbackInfo ci) {
        if (unAuthedPlayers.contains(player)) {
            ci.cancel();
        }
    }

    public static void onPlayerInteract(ServerPlayer player, CallbackInfo ci) {
        if (unAuthedPlayers.contains(player)) {
            ci.cancel();
        }
    }

    public static void onPlayerMove(ServerPlayer player, CallbackInfo ci) {
        if (unAuthedPlayers.contains(player)) {
            ci.cancel();
        }
    }

    public static void onServerLoaded(MinecraftServer server) {
        ModMain.server = server;
    }
}
