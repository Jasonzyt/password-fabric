package com.jasonzyt.passwordfabric.data;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public record Position(double x, double y, double z, float xRot, float yRot) {
    public Position(double x, double y, double z) {
        this(x, y, z, 0, 0);
    }

    public Position(Vec3 vec3) {
        this(vec3.x(), vec3.y(), vec3.z());
    }

    public Position(Vec3 vec3, float xRot, float yRot) {
        this(vec3.x(), vec3.y(), vec3.z(), xRot, yRot);
    }

    public Position(ServerPlayer player) {
        this(player.position(), player.getXRot(), player.getYRot());
    }

    public void apply(ServerPlayer player) {
        player.teleportTo(x, y, z);
        player.setXRot(xRot);
        player.setYRot(yRot);
    }
}
