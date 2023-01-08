package com.jasonzyt.passwordfabric;

import com.jasonzyt.passwordfabric.command.PasswordCommand;
import com.jasonzyt.passwordfabric.data.Data;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ModMain implements ModInitializer {
    public static final Logger logger = LoggerFactory.getLogger("password");
    public static final List<ServerPlayer> players = new LinkedList<>();
    public static final List<ServerPlayer> playersNotAuthed = new LinkedList<>();
    public static final Map<ServerPlayer, Vec3> playersNotAuthedPositions = new HashMap<>();
    public static final Map<ServerPlayer, LocalDateTime> playerLoginTime = new HashMap<>();
    public static final Data data = Data.read();

    @Override
    public void onInitialize() {
        logger.info("Password loaded! Author: Jasonzyt");
        CommandRegistrationCallback.EVENT.register(PasswordCommand.INSTANCE::register);
    }

    public static void onPlayerLogIn(ServerPlayer player) {
        players.add(player);
        playersNotAuthedPositions.put(player, player.position());
        playerLoginTime.put(player, LocalDateTime.now());
        data.addIPLoginTime(player.getStringUUID(), player.getIpAddress(), System.currentTimeMillis());
        if (data.hasTrustIP(player.getStringUUID(), player.getIpAddress())) {
            //player.sendSystemMessage(Component.literal("信任IP免验证生效, 无需输入密码").withStyle(ChatFormatting.GREEN));
        } else {
            playersNotAuthed.add(player);
            if (data.hasPassword(player.getStringUUID())) {
                player.sendSystemMessage(Component.literal("您还未设置密码, 请发送命令 /pwd s <密码> 来设置密码, 否则无法进入服务器").withStyle(ChatFormatting.BLUE));
            } else {
                player.sendSystemMessage(Component.literal("你还未通过验证! 请发送命令 /pwd a <密码> 来验证, 否则无法进入服务器").withStyle(ChatFormatting.RED));
            }
        }
    }

    public static void onPlayerLogOut(ServerPlayer player) {
        if (playersNotAuthed.contains(player)) {
            Vec3 pos = playersNotAuthedPositions.get(player);
            player.teleportTo(pos.x, pos.y, pos.z);
        }
        playerLoginTime.remove(player);
        players.remove(player);
    }

    public static void tick() {
        for (ServerPlayer player : playersNotAuthed) {
            player.teleportTo(0, 1024, 0);
            if (LocalDateTime.now().isAfter(playerLoginTime.get(player).plusSeconds(60))) {
                player.connection.disconnect(Component.literal("You did not enter your password within 60 seconds!"));
            }
        }
    }
}
