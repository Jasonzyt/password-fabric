package com.jasonzyt.passwordfabric.command;


import com.jasonzyt.passwordfabric.ModMain;
import com.jasonzyt.passwordfabric.data.Data;
import com.jasonzyt.passwordfabric.data.Utils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;


import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class PasswordCommand {
    public static final PasswordCommand INSTANCE = new PasswordCommand();

    public enum WhitelistAction {
        ADD,
        REMOVE,
        LIST
    }

    public int executeAuthCommand(CommandContext<CommandSourceStack> context) {
        String pass = context.getArgument("password", String.class);
        CommandSourceStack src = context.getSource();
        if (!src.isPlayer()) {
            src.sendFailure(Component.literal("只有玩家可以使用此命令").withStyle(ChatFormatting.RED));
            return Command.SINGLE_SUCCESS;
        }
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("内部错误").withStyle(ChatFormatting.RED));
            return Command.SINGLE_SUCCESS;
        }
        if (ModMain.playersEnteredPassword.contains(player)) {
            src.sendFailure(Component.literal("你已经通过验证了, 请勿重复验证").withStyle(ChatFormatting.RED));
            return Command.SINGLE_SUCCESS;
        }
        pass = Utils.doSha256(pass);
        if (!ModMain.data.hasPassword(player.getStringUUID())) {
            ModMain.data.addPassword(player.getStringUUID(), pass);
            src.sendSuccess(Component.literal("密码设置成功!").withStyle(ChatFormatting.GREEN), false);
            ModMain.onPlayerAuthed(player);
            return Command.SINGLE_SUCCESS;
        }
        if (ModMain.data.checkPassword(player.getStringUUID(), pass)) {
            ModMain.onPlayerAuthed(player);
        } else {
            src.sendFailure(Component.literal("密码错误").withStyle(ChatFormatting.RED));
        }
        return Command.SINGLE_SUCCESS;
    }

    public int executeTrustCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack src = context.getSource();
        if (!src.isPlayer()) {
            src.sendFailure(Component.literal("只有玩家可以使用此命令").withStyle(ChatFormatting.RED));
            return Command.SINGLE_SUCCESS;
        }
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("内部错误").withStyle(ChatFormatting.RED));
            return Command.SINGLE_SUCCESS;
        }
        if (ModMain.unAuthedPlayers.contains(player)) {
            src.sendFailure(Component.literal("你还没有通过验证, 请先通过验证! 发送命令 \"/pwd a <密码>\" 验证").withStyle(ChatFormatting.RED));
            return Command.SINGLE_SUCCESS;
        }
        String ip = player.getIpAddress();
        ModMain.data.addTrustIP(player.getStringUUID(), ip, System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30);
        src.sendSuccess(Component.literal("已将你的IP地址添加到信任列表, 30天内同IP登录无需输入密码").withStyle(ChatFormatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    public int executeSetCommand(CommandContext<CommandSourceStack> context) {
        String pass = context.getArgument("password", String.class);
        CommandSourceStack src = context.getSource();
        if (!src.isPlayer()) {
            src.sendFailure(Component.literal("只有玩家可以使用此命令").withStyle(ChatFormatting.RED));
            return Command.SINGLE_SUCCESS;
        }
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.literal("内部错误").withStyle(ChatFormatting.RED));
            return Command.SINGLE_SUCCESS;
        }
        pass = Utils.doSha256(pass);
        if (!ModMain.data.hasPassword(player.getStringUUID())) {
            ModMain.data.addPassword(player.getStringUUID(), pass);
            src.sendSuccess(Component.literal("密码设置成功!").withStyle(ChatFormatting.GREEN), false);
            ModMain.onPlayerAuthed(player);
            return Command.SINGLE_SUCCESS;
        }
        if (!ModMain.playersEnteredPassword.contains(player)) {
            src.sendFailure(Component.literal("高危操作, 请先输入密码! 发送命令 \"/pwd a <密码>\" 验证").withStyle(ChatFormatting.RED));
            return Command.SINGLE_SUCCESS;
        }
        if (ModMain.unAuthedPlayers.contains(player)) {
            src.sendFailure(Component.literal("你还没有通过验证, 请先通过验证! 发送命令 \"/pwd a <密码>\" 验证").withStyle(ChatFormatting.RED));
            return Command.SINGLE_SUCCESS;
        }
        ModMain.data.addPassword(player.getStringUUID(), pass);
        ModMain.data.removePlayerAllTrustIPs(player.getStringUUID());
        src.sendSuccess(Component.literal("密码修改成功! 并清除了所有信任IP").withStyle(ChatFormatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    public int executeReloadCommand(CommandContext<CommandSourceStack> context) {
        ModMain.data = Data.read();
        return Command.SINGLE_SUCCESS;
    }

    public int executeInfoCommand(CommandContext<CommandSourceStack> context) {
        // TODO: complete this
        return Command.SINGLE_SUCCESS;
    }

    public int executeWhitelistCommand(CommandContext<CommandSourceStack> context, WhitelistAction action) {
        CommandSourceStack src = context.getSource();
        switch (action) {
            case ADD -> {
                String player = context.getArgument("playerName", String.class);
                ModMain.data.addWhitelist(player);
                src.sendSuccess(Component.literal("已将玩家 " + player + " 添加到白名单").withStyle(ChatFormatting.GREEN), false);
            }
            case REMOVE -> {
                String player = context.getArgument("playerName", String.class);
                ModMain.data.removeWhitelist(player);
                src.sendSuccess(Component.literal("已将玩家 " + player + " 从白名单移除").withStyle(ChatFormatting.GREEN), false);
            }
            case LIST -> {
                List<String> whitelist = ModMain.data.getWhitelist();
                if (whitelist.isEmpty()) {
                    src.sendSuccess(Component.literal("白名单为空").withStyle(ChatFormatting.GREEN), false);
                } else {
                    src.sendSuccess(Component.literal("白名单:").withStyle(ChatFormatting.GREEN), false);
                    for (String player : whitelist) {
                        src.sendSuccess(Component.literal(player).withStyle(ChatFormatting.GREEN), false);
                    }
                }
            }
            default -> {
                src.sendFailure(Component.literal("WTF").withStyle(ChatFormatting.RED));
                return Command.SINGLE_SUCCESS;
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection commandSelection) {
        dispatcher.register(
                literal("pwd")
                        // pwd a <password>
                        .then(literal("a")
                                .requires(CommandSourceStack::isPlayer)
                                .then(argument("password", greedyString())
                                        .executes(this::executeAuthCommand)))
                        .then(literal("s")
                                .requires(CommandSourceStack::isPlayer)
                                .then(argument("password", greedyString())
                                        .executes(this::executeSetCommand)))
                        .then(literal("i")
                                .requires(CommandSourceStack::isPlayer)
                                .executes(this::executeInfoCommand))
                        .then(literal("t")
                                .requires(CommandSourceStack::isPlayer)
                                .executes(this::executeTrustCommand))
                        .then(literal("auth")
                                .requires(CommandSourceStack::isPlayer)
                                .then(argument("password", greedyString())
                                        .executes(this::executeAuthCommand)))
                        .then(literal("set")
                                .requires(CommandSourceStack::isPlayer)
                                .then(argument("password", greedyString())
                                        .executes(this::executeSetCommand)))
                        .then(literal("info")
                                .requires(CommandSourceStack::isPlayer)
                                .executes(this::executeInfoCommand))
                        .then(literal("trust")
                                .requires(CommandSourceStack::isPlayer)
                                .executes(this::executeTrustCommand))
                        .then(literal("reload")
                                .requires(src -> src.hasPermission(4))
                                .executes(this::executeReloadCommand))
                        .then(literal("wl")
                                .requires(src -> src.hasPermission(4))
                                .then(literal("add")
                                        .then(argument("playerName", word())
                                                .executes(ctx -> executeWhitelistCommand(ctx, WhitelistAction.ADD))))
                                .then(literal("remove")
                                        .then(argument("playerName", word())
                                                .executes(ctx -> executeWhitelistCommand(ctx, WhitelistAction.REMOVE))))
                                .then(literal("list")
                                        .executes(ctx -> executeWhitelistCommand(ctx, WhitelistAction.LIST))))
                        .then(literal("whitelist")
                                .requires(src -> src.hasPermission(4))
                                .then(literal("add")
                                        .then(argument("playerName", word())
                                                .executes(ctx -> executeWhitelistCommand(ctx, WhitelistAction.ADD))))
                                .then(literal("remove")
                                        .then(argument("playerName", word())
                                                .executes(ctx -> executeWhitelistCommand(ctx, WhitelistAction.REMOVE))))
                                .then(literal("list")
                                        .executes(ctx -> executeWhitelistCommand(ctx, WhitelistAction.LIST))))
        );

    }

}