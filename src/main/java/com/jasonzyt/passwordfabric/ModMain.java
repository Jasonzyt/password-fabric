package com.jasonzyt.passwordfabric;

import com.jasonzyt.passwordfabric.command.PasswordCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ModMain implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("password");
    public static final List<ServerPlayer> players = new LinkedList<>();
    public static final List<ServerPlayer> playersNotAuthed = new LinkedList<>();
    public static final Map<ServerPlayer, Vec3> playersNotAuthedPositions = new HashMap<>();
    public static final Map<ServerPlayer, LocalDateTime> playerLoginTime = new HashMap<>();


    @Override
    public void onInitialize() {
        LOGGER.info("Password loaded! Author: Jasonzyt");
        CommandRegistrationCallback.EVENT.register(PasswordCommand.INSTANCE::register);
    }

    public static void onPlayerLogIn(ServerPlayer player) {
        players.add(player);
        playersNotAuthedPositions.put(player, player.position());
        playerLoginTime.put(player, LocalDateTime.now());
        playersNotAuthed.add(player);
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
