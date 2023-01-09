package com.jasonzyt.passwordfabric.command;


import com.jasonzyt.passwordfabric.ModMain;
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


import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class PasswordCommand {
    public static final PasswordCommand INSTANCE = new PasswordCommand();

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
        if (!ModMain.unauthedPlayers.contains(player)) {
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
        if (ModMain.unauthedPlayers.contains(player)) {
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
            ModMain.data.removePlayerAllTrustIPs(player.getStringUUID());
            src.sendSuccess(Component.literal("密码设置成功! 并清除了所有信任IP").withStyle(ChatFormatting.GREEN), false);
            ModMain.onPlayerAuthed(player);
            return Command.SINGLE_SUCCESS;
        }
        if (ModMain.unauthedPlayers.contains(player)) {
            src.sendFailure(Component.literal("你还没有通过验证, 请先通过验证! 发送命令 \"/pwd a <密码>\" 验证").withStyle(ChatFormatting.RED));
            return Command.SINGLE_SUCCESS;
        }
        ModMain.data.addPassword(player.getStringUUID(), pass);
        src.sendSuccess(Component.literal("密码修改成功!").withStyle(ChatFormatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    public int executeInfoCommand(CommandContext<CommandSourceStack> context) {
        // TODO: complete this
        return Command.SINGLE_SUCCESS;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection commandSelection) {
        dispatcher.register(
                literal("pwd")
                        // pwd a <password>
                        .then(literal("a")
                                .then(argument("password", greedyString())).executes(this::executeAuthCommand))
                        .then(literal("s")
                                .then(argument("password", greedyString())
                                        .executes(this::executeSetCommand)))
                        .then(literal("i")
                                .executes(this::executeInfoCommand))
                        .then(literal("t")
                                .executes(this::executeTrustCommand))
                        .then(literal("auth")
                                .then(argument("password", greedyString())).executes(this::executeAuthCommand))
                        .then(literal("set")
                                .then(argument("password", greedyString())
                                        .executes(this::executeSetCommand)))
                        .then(literal("info")
                                .executes(this::executeInfoCommand))
                        .then(literal("trust")
                                .executes(this::executeTrustCommand))
        );

    }

}