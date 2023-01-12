package com.jasonzyt.passwordfabric.mixin;

import com.jasonzyt.passwordfabric.ModMain;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onPlayerDisconnect(Component reason, CallbackInfo ci) {
        ModMain.onPlayerLogOut(player);
    }

    @Inject(method = "handlePlayerAction", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;drop(Z)Z",
            ordinal = 0,
            shift = At.Shift.BEFORE
    ))
    private void onDropItem(ServerboundPlayerActionPacket serverboundPlayerActionPacket, CallbackInfo ci) {
        ModMain.onPlayerDropItem(player, ci);
    }

    @Inject(method = "handlePlayerAction", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;drop(Z)Z",
            ordinal = 1,
            shift = At.Shift.BEFORE
    ))
    private void onDropAllItems(ServerboundPlayerActionPacket serverboundPlayerActionPacket, CallbackInfo ci) {
        ModMain.onPlayerDropItem(player, ci);
    }

    @Inject(method = "handlePlayerAction", cancellable = true, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;",
            ordinal = 0,
            shift = At.Shift.BEFORE
    ))
    private void onSwapHand(ServerboundPlayerActionPacket serverboundPlayerActionPacket, CallbackInfo ci) {
        ModMain.onPlayerSwapHand(player, ci);
    }

    @Inject(method = "handleChat(Lnet/minecraft/network/protocol/game/ServerboundChatPacket;)V",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onChatMessage(ServerboundChatPacket serverboundChatPacket, CallbackInfo ci) {
        ModMain.onPlayerChat(player, ci);
    }

    @Inject(method = "handleSetCarriedItem", at = @At("HEAD"))
    private void onSelectSlot(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
        ModMain.onPlayerSelectSlot(player, ci);
    }

    @Inject(method = "handleUseItem", at = @At("HEAD"))
    private void onUseItem(ServerboundUseItemPacket packet, CallbackInfo ci) {
        ModMain.onPlayerUseItem(player, ci);
    }

    @Inject(method = "handleUseItemOn", at = @At("HEAD"))
    private void onUseItemOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        ModMain.onPlayerUseItem(player, ci);
    }

    @Inject(method = "handleInteract", at = @At("HEAD"))
    private void onInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        ModMain.onPlayerInteract(player, ci);
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void onMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        ModMain.onPlayerMove(player, ci);
    }
}
