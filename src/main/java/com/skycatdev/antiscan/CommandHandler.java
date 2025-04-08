package com.skycatdev.antiscan;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.server.command.ServerCommandSource;

import java.io.IOException;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandHandler implements CommandRegistrationCallback {
    public static final DynamicCommandExceptionType FAILED_TO_BLACKLIST = new DynamicCommandExceptionType(name -> () -> String.format("Failed to blacklist %s.", name));
    public static final DynamicCommandExceptionType FAILED_TO_UN_BLACKLIST = new DynamicCommandExceptionType(name -> () -> String.format("Failed to un-blacklist %s.", name));

    private static int blacklistIp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            String ip = StringArgumentType.getString(context, "ip");
            if (AntiScan.IP_CHECKER.blacklist(ip, true, AntiScan.IP_CHECKER_FILE)) {
                context.getSource().sendFeedback(() -> Utils.textOf(String.format("Blacklisted %s.", ip)), true);
                return Command.SINGLE_SUCCESS;
            }
            context.getSource().sendFeedback(() -> Utils.textOf(String.format("%s was already blacklisted!", ip)), true);
            return 0;
        } catch (IOException e) {
            throw FAILED_TO_BLACKLIST.create("ip");
        }
    }

    private static int blacklistName(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            String name = StringArgumentType.getString(context, "name");
            if (AntiScan.NAME_CHECKER.blacklist(name, AntiScan.NAME_CHECKER_FILE)) {
                context.getSource().sendFeedback(() -> Utils.textOf(String.format("Blacklisted %s.", name)), true);
                return Command.SINGLE_SUCCESS;
            }
            context.getSource().sendFeedback(() -> Utils.textOf(String.format("%s was already blacklisted!", name)), true);
            return 0;
        } catch (IOException e) {
            throw FAILED_TO_BLACKLIST.create("name");
        }
    }

    private static int unBlacklistIp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            String ip = StringArgumentType.getString(context, "ip");
            if (AntiScan.IP_CHECKER.unBlacklist(ip, true, AntiScan.IP_CHECKER_FILE)) {
                return Command.SINGLE_SUCCESS;
            }
            context.getSource().sendFeedback(() -> Utils.textOf(String.format("%s was not blacklisted!", ip)), true);
            return 0;
        } catch (IOException e) {
            throw FAILED_TO_UN_BLACKLIST.create("ip");
        }
    }

    private static int unBlacklistName(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            String name = StringArgumentType.getString(context, "name");
            if (AntiScan.NAME_CHECKER.unBlacklist(name, AntiScan.NAME_CHECKER_FILE)) {
                return Command.SINGLE_SUCCESS;
            }
            context.getSource().sendFeedback(() -> Utils.textOf(String.format("%s was not blacklisted!", name)), true);
            return 0;
        } catch (IOException e) {
            throw FAILED_TO_UN_BLACKLIST.create("name");
        }
    }

    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, RegistrationEnvironment environment) {
        var antiScan = literal("antiscan")
                .requires(Permissions.require("antiscan", 3))
                .build();
        var ip = literal("ip")
                .requires(Permissions.require("antiscan.ip", 3))
                .build();
        var ipBlacklist = literal("blacklist")
                .requires(Permissions.require("antiscan.ip.blacklist", 3))
                .build();
        var ipBlacklistAdd = literal("add")
                .requires(Permissions.require("antiscan.ip.blacklist.add", 3))
                .build();
        var ipBlacklistAddIp = argument("ip", StringArgumentType.string())
                .requires(Permissions.require("antiscan.ip.blacklist.add", 3))
                .executes(CommandHandler::blacklistIp)
                .build();
        var ipBlacklistRemove = literal("remove")
                .requires(Permissions.require("antiscan.ip.blacklist.remove", 3))
                .build();
        var ipBlacklistRemoveIp = argument("ip", StringArgumentType.string())
                .requires(Permissions.require("antiscan.ip.blacklist.remove", 3))
                .executes(CommandHandler::unBlacklistIp)
                .build();
        var name = literal("name")
                .requires(Permissions.require("antiscan.name", 3))
                .build();
        var nameBlacklist = literal("blacklist")
                .requires(Permissions.require("antiscan.name.blacklist", 3))
                .build();
        var nameBlacklistAdd = literal("add")
                .requires(Permissions.require("antiscan.name.blacklist.add", 3))
                .build();
        var nameBlacklistAddName = argument("name", StringArgumentType.string())
                .requires(Permissions.require("antiscan.name.blacklist.add", 3))
                .executes(CommandHandler::blacklistName)
                .build();
        var nameBlacklistRemove = literal("remove")
                .requires(Permissions.require("antiscan.name.blacklist.remove", 3))
                .build();
        var nameBlacklistRemoveName = argument("name", StringArgumentType.string())
                .requires(Permissions.require("antiscan.name.blacklist.remove", 3))
                .executes(CommandHandler::unBlacklistName)
                .build();
        //@formatter:off
        dispatcher.getRoot().addChild(antiScan);
            antiScan.addChild(ip);
                ip.addChild(ipBlacklist);
                    ipBlacklist.addChild(ipBlacklistAdd);
                        ipBlacklistAdd.addChild(ipBlacklistAddIp);
                    ipBlacklist.addChild(ipBlacklistRemove);
                        ipBlacklistRemove.addChild(ipBlacklistRemoveIp);
            antiScan.addChild(name);
                name.addChild(nameBlacklist);
                    nameBlacklist.addChild(nameBlacklistAdd);
                        nameBlacklistAdd.addChild(nameBlacklistAddName);
                    nameBlacklist.addChild(nameBlacklistRemove);
                        nameBlacklistRemove.addChild(nameBlacklistRemoveName);
        //@formatter:on
    }
}
