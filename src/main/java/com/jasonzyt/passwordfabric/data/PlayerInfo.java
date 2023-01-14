package com.jasonzyt.passwordfabric.data;

import com.jasonzyt.passwordfabric.ModMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.io.IOException;

public class PlayerInfo {

    public double x;
    public double y;
    public double z;
    public float xRot;
    public float yRot;
    public String invNbtFile;

    public PlayerInfo(ServerPlayer player, boolean saveInventory) {
        Vec3 pos = player.position();
        if (saveInventory) {
            Inventory inv = player.getInventory();
            CompoundTag root = new CompoundTag();
            ListTag invTag = inv.save(new ListTag());
            root.put("Inventory", invTag);
            root.putInt("SelectedSlot", inv.selected);
            String invNbtFile = Data.INVENTORY_DATA_DIR + "player_inv_" + player.getStringUUID() + ".dat";
            File file = new File(invNbtFile);
            try {
                NbtIo.write(root, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.invNbtFile = invNbtFile;
        }
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.xRot = player.getXRot();
        this.yRot = player.getYRot();
    }

    public PlayerInfo(ServerPlayer player) {
        this(player, false);
    }

    public void apply(ServerPlayer player) {
        player.teleportTo(player.getLevel(), x, y, z, xRot, yRot);
        try {
            File file = new File(invNbtFile);
            CompoundTag tag = NbtIo.read(file);
            if (tag == null) {
                ModMain.LOGGER.error("Failed to read player inventory data from file: " + invNbtFile);
                return;
            }
            ModMain.logDebug("PlayerInfo.apply: CompoundTag: " + tag);
            ListTag invTag = (ListTag) tag.get("Inventory");
            ModMain.logDebug("PlayerInfo.apply: ListTag: " + invTag);
            Inventory inv = player.getInventory();
            inv.load(invTag);
            inv.selected = tag.getInt("SelectedSlot");
            player.inventoryMenu.slotsChanged(player.getInventory());
            player.containerMenu.broadcastChanges();
            if (!file.delete()) {
                ModMain.LOGGER.error("Failed to delete player inventory data file: " + invNbtFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        return String.format("Position{x=%f, y=%f, z=%f, xRot=%f, yRot=%f}", x, y, z, xRot, yRot);
    }
}
