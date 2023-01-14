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

    @Inject(method = "handleChat(Lnet/minecraft/network/protocol/game/ServerboundChatPacket;)V",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void onChatMessage(ServerboundChatPacket serverboundChatPacket, CallbackInfo ci) {
        ModMain.onPlayerChat(player, ci);
    }

    @Inject(method = "handleSetCarriedItem", at = @At("HEAD"), cancellable = true)
    private void onSelectSlot(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
        ModMain.onPlayerSelectSlot(player, ci);
    }

    @Inject(method = "handleUseItem", at = @At("HEAD"), cancellable = true)
    private void onUseItem(ServerboundUseItemPacket packet, CallbackInfo ci) {
        ModMain.onPlayerUseItem(player, ci);
    }

    @Inject(method = "handleUseItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        ModMain.onPlayerUseItem(player, ci);
    }

    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    private void onInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        ModMain.onPlayerInteract(player, ci);
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void onMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        ModMain.onPlayerMove(player, ci);
    }
}
