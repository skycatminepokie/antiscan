package com.skycatdev.antiscan;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.server.command.ServerCommandSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandHandler implements CommandRegistrationCallback {
    public static final DynamicCommandExceptionType FAILED_TO_BLACKLIST = new DynamicCommandExceptionType(name -> () -> String.format("Failed to blacklist %s.", name));
    public static final DynamicCommandExceptionType FAILED_TO_UN_BLACKLIST = new DynamicCommandExceptionType(name -> () -> String.format("Failed to un-blacklist %s.", name));
    public static final SimpleCommandExceptionType FAILED_TO_SET_KEY = new SimpleCommandExceptionType(() -> "Failed to set key!");
    public static final SimpleCommandExceptionType FAILED_TO_SET_CONFIG = new SimpleCommandExceptionType(() -> "Failed to set config!");

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

    private static int checkIp(CommandContext<ServerCommandSource> context) {
        String ip = StringArgumentType.getString(context, "ip");
        if (AntiScan.IP_CHECKER.isBlacklisted(ip)) {
            context.getSource().sendFeedback(() -> Utils.textOf(String.format("%s is blacklisted.", ip)), false);
            return Command.SINGLE_SUCCESS;
        }
        context.getSource().sendFeedback(() -> Utils.textOf(String.format("%s is not blacklisted.", ip)), false);
        return 0;
    }

    private static int checkName(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "name");
        int blacklisted = 0;
        for (GameProfile profile : profiles) {
            if (AntiScan.NAME_CHECKER.isBlacklisted(profile.getName())) {
                context.getSource().sendFeedback(() -> Utils.textOf(String.format("%s is blacklisted.", profile.getName())), false);
                blacklisted++;
            } else {
                context.getSource().sendFeedback(() -> Utils.textOf(String.format("%s is not blacklisted.", profile.getName())), false);
            }
        }
        int finalBlacklisted = blacklisted;
        if (profiles.size() != 1) {
            context.getSource().sendFeedback(() -> Utils.textOf(String.format("%d/%d are blacklisted.", finalBlacklisted, profiles.size())), false);
        }
        return finalBlacklisted;
    }

    private static int displayHandshakeAction(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> Utils.textOf(String.format("Handshake action is %s.", AntiScan.CONFIG.getHandshakeAction().asString())), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayHandshakeMode(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> Utils.textOf(String.format("Handshake mode is %s.", AntiScan.CONFIG.getHandshakeMode().asString())), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayHandshakeReport(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> Utils.textOf(String.format("Reporting is %s.", AntiScan.CONFIG.isHandshakeReport() ? "on" : "off")), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int forceUpdateIpBlacklist(CommandContext<ServerCommandSource> context) {
        AntiScan.IP_CHECKER.updateNow(AntiScan.IP_CHECKER_FILE);
        context.getSource().sendFeedback(() -> Utils.textOf("IP blacklist will be updated."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int listAllBlacklistedIps(CommandContext<ServerCommandSource> context) {
        for (String ip : AntiScan.IP_CHECKER.getManualBlacklist()) {
            context.getSource().sendFeedback(() -> Utils.textOf(ip), false);
        }
        int ips = AntiScan.IP_CHECKER.getManualBlacklist().size();
        context.getSource().sendFeedback(() -> Utils.textOf(String.format("(%d IPs manually blacklisted)", ips)), false);
        for (String ip : AntiScan.IP_CHECKER.getBlacklistCache()) {
            context.getSource().sendFeedback(() -> Utils.textOf(ip), false);
        }
        int cached = AntiScan.IP_CHECKER.getBlacklistCache().size();
        context.getSource().sendFeedback(() -> Utils.textOf(String.format("(%d IPs automatically blacklisted)", cached)), false);
        return ips + cached;
    }

    private static int listBlacklistedIps(CommandContext<ServerCommandSource> context) {
        for (String ip : AntiScan.IP_CHECKER.getManualBlacklist()) {
            context.getSource().sendFeedback(() -> Utils.textOf(ip), false);
        }
        int ips = AntiScan.IP_CHECKER.getManualBlacklist().size();
        context.getSource().sendFeedback(() -> Utils.textOf(String.format("(%d IPs)", ips)), false);
        return ips;
    }

    private static int listBlacklistedNames(CommandContext<ServerCommandSource> context) {
        for (String name : AntiScan.NAME_CHECKER.getBlacklist()) {
            context.getSource().sendFeedback(() -> Utils.textOf(name), false);
        }
        int names = AntiScan.NAME_CHECKER.getBlacklist().size();
        context.getSource().sendFeedback(() -> Utils.textOf(String.format("(%d names)", names)), false);
        return names;
    }

    private static int setAbuseIpdbKey(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            AntiScan.CONFIG.setAbuseIpdbKey(StringArgumentType.getString(context, "key"), AntiScan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_KEY.create();
        }
        context.getSource().sendFeedback(() -> Utils.textOf("Set! Make sure to clear your command history (including the file!) or terminal."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setHandshakeAction(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            AntiScan.CONFIG.setHandshakeAction(Config.Action.fromId(StringArgumentType.getString(context, "action")), AntiScan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayHandshakeAction(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setHandshakeMode(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            AntiScan.CONFIG.setHandshakeMode(Config.IpMode.fromId(StringArgumentType.getString(context, "mode")), AntiScan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayHandshakeMode(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setHandshakeReport(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            AntiScan.CONFIG.setHandshakeReport(BoolArgumentType.getBool(context, "report"), AntiScan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayHandshakeReport(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int unBlacklistIp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            String ip = StringArgumentType.getString(context, "ip");
            if (AntiScan.IP_CHECKER.unBlacklist(ip, true, AntiScan.IP_CHECKER_FILE)) {
                context.getSource().sendFeedback(() -> Utils.textOf(String.format("Removed %s from the blacklist!", ip)), true);
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
                context.getSource().sendFeedback(() -> Utils.textOf(String.format("Removed %s from the blacklist!", name)), true);
                return Command.SINGLE_SUCCESS;
            }
            context.getSource().sendFeedback(() -> Utils.textOf(String.format("%s was not blacklisted!", name)), true);
            return 0;
        } catch (IOException e) {
            throw FAILED_TO_UN_BLACKLIST.create("name");
        }
    }

    private static int updateIpBlacklist(CommandContext<ServerCommandSource> context) {
        AntiScan.IP_CHECKER.update(TimeUnit.HOURS.toMillis(5), AntiScan.IP_CHECKER_FILE);
        context.getSource().sendFeedback(() -> Utils.textOf("IP blacklist will be updated if it has not been updated in the last 5 hours. Add \"force\" to do it now."), true);
        return Command.SINGLE_SUCCESS;
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
        var ipBlacklistCheck = literal("check")
                .requires(Permissions.require("antiscan.ip.blacklist.check", 3))
                .build();
        var ipBlacklistCheckIp = argument("ip", StringArgumentType.string())
                .requires(Permissions.require("antiscan.ip.blacklist.check", 3))
                .executes(CommandHandler::checkIp)
                .build();
        var ipBlacklistUpdate = literal("update")
                .requires(Permissions.require("antiscan.ip.blacklist.update", 4))
                .executes(CommandHandler::updateIpBlacklist)
                .build();
        var ipBlacklistUpdateForce = literal("force")
                .requires(Permissions.require("antiscan.ip.blacklist.update.force", 4))
                .executes(CommandHandler::forceUpdateIpBlacklist)
                .build();
        var ipBlacklistList = literal("list")
                .requires(Permissions.require("antiscan.ip.blacklist.list", 3))
                .executes(CommandHandler::listBlacklistedIps)
                .build();
        var ipBlacklistListAll = literal("all")
                .requires(Permissions.require("antiscan.ip.blacklist.list.all", 3))
                .executes(CommandHandler::listAllBlacklistedIps)
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
        var nameBlacklistCheck = literal("check")
                .requires(Permissions.require("antiscan.name.blacklist.check", 3))
                .build();
        var nameBlacklistCheckName = argument("name", GameProfileArgumentType.gameProfile())
                .requires(Permissions.require("antiscan.name.blacklist.check", 3))
                .executes(CommandHandler::checkName)
                .build();
        var nameBlacklistList = literal("list")
                .requires(Permissions.require("antiscan.name.blacklist.list", 3))
                .executes(CommandHandler::listBlacklistedNames)
                .build();
        var config = literal("config")
                .requires(Permissions.require("antiscan.config", 4))
                .build();
        var configAbuseIpdbKey = literal("abuseIpdbKey")
                .requires(Permissions.require("antiscan.config.abuseIpdbKey", 4))
                .build();
        var configAbuseIpdbKeyKey = argument("key", StringArgumentType.string())
                .requires(Permissions.require("antiscan.config.abuseIpdbKey", 4))
                .executes(CommandHandler::setAbuseIpdbKey)
                .build();
        var configHandshake = literal("handshake")
                .requires(Permissions.require("antiscan.config.handshake", 4))
                .build();
        var configHandshakeMode = literal("mode")
                .requires(Permissions.require("antiscan.config.handshake.mode", 4))
                .executes(CommandHandler::displayHandshakeMode)
                .build();
        var configHandshakeModeMode = argument("mode", StringArgumentType.string())
                .requires(Permissions.require("antiscan.config.handshake.mode.set", 4))
                .suggests((context, builder) -> CommandSource.suggestMatching(Arrays.stream(Config.IpMode.values()).map(Config.IpMode::asString), builder))
                .executes(CommandHandler::setHandshakeMode)
                .build();
        var configHandshakeAction = literal("action")
                .requires(Permissions.require("antiscan.config.handshake.action", 4))
                .executes(CommandHandler::displayHandshakeAction)
                .build();
        var configHandshakeActionAction = argument("action", StringArgumentType.string())
                .requires(Permissions.require("antiscan.config.handshake.action.set", 4))
                .suggests((context, builder) -> CommandSource.suggestMatching(Arrays.stream(Config.Action.values()).map(Config.Action::asString), builder))
                .executes(CommandHandler::setHandshakeAction)
                .build();
        var configHandshakeReport = literal("report")
                .requires(Permissions.require("antiscan.config.handshake.report", 4))
                .executes(CommandHandler::displayHandshakeReport)
                .build();
        var configHandshakeReportReport = argument("report", BoolArgumentType.bool())
                .requires(Permissions.require("antiscan.config.handshake.report.set", 4))
                .executes(CommandHandler::setHandshakeReport)
                .build();

        //@formatter:off
        dispatcher.getRoot().addChild(antiScan);
            antiScan.addChild(ip);
                ip.addChild(ipBlacklist);
                    ipBlacklist.addChild(ipBlacklistAdd);
                        ipBlacklistAdd.addChild(ipBlacklistAddIp);
                    ipBlacklist.addChild(ipBlacklistRemove);
                        ipBlacklistRemove.addChild(ipBlacklistRemoveIp);
                    ipBlacklist.addChild(ipBlacklistCheck);
                        ipBlacklistCheck.addChild(ipBlacklistCheckIp);
                    ipBlacklist.addChild(ipBlacklistUpdate);
                        ipBlacklistUpdate.addChild(ipBlacklistUpdateForce);
                    ipBlacklist.addChild(ipBlacklistList);
                        ipBlacklistList.addChild(ipBlacklistListAll);
            antiScan.addChild(name);
                name.addChild(nameBlacklist);
                    nameBlacklist.addChild(nameBlacklistAdd);
                        nameBlacklistAdd.addChild(nameBlacklistAddName);
                    nameBlacklist.addChild(nameBlacklistRemove);
                        nameBlacklistRemove.addChild(nameBlacklistRemoveName);
                    nameBlacklist.addChild(nameBlacklistCheck);
                        nameBlacklistCheck.addChild(nameBlacklistCheckName);
                    nameBlacklist.addChild(nameBlacklistList);
            antiScan.addChild(config);
                config.addChild(configAbuseIpdbKey);
                    configAbuseIpdbKey.addChild(configAbuseIpdbKeyKey);
                config.addChild(configHandshake);
                    configHandshake.addChild(configHandshakeAction);
                        configHandshakeAction.addChild(configHandshakeActionAction);
                    configHandshake.addChild(configHandshakeMode);
                        configHandshakeMode.addChild(configHandshakeModeMode);
                    configHandshake.addChild(configHandshakeReport);
                        configHandshakeReport.addChild(configHandshakeReportReport);
        //@formatter:on
    }
}
