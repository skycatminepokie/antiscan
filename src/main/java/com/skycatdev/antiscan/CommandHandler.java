package com.skycatdev.antiscan;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.commands.SharedSuggestionProvider;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

// I'd love for a better way of the config commands, but the functional one I came up with was more convoluted.
public class CommandHandler implements CommandRegistrationCallback {
    public static final DynamicCommandExceptionType FAILED_TO_BLACKLIST = new DynamicCommandExceptionType(name -> () -> String.format("Failed to blacklist %s.", name));
    public static final DynamicCommandExceptionType FAILED_TO_UN_BLACKLIST = new DynamicCommandExceptionType(name -> () -> String.format("Failed to un-blacklist %s.", name));
    public static final SimpleCommandExceptionType FAILED_TO_SET_KEY = new SimpleCommandExceptionType(() -> "Failed to set key!");
    public static final SimpleCommandExceptionType FAILED_TO_SET_CONFIG = new SimpleCommandExceptionType(() -> "Failed to set config!");

    private static int blacklistIp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            String ip = StringArgumentType.getString(context, "ip");
            if (Antiscan.CONNECTION_CHECKER.blacklist(ip, true, Antiscan.CONNECTION_CHECKER_FILE)) {
                context.getSource().sendSuccess(() -> Utils.textOf(String.format("Blacklisted %s.", ip)), true);
                return Command.SINGLE_SUCCESS;
            }
            context.getSource().sendSuccess(() -> Utils.textOf(String.format("%s was already blacklisted!", ip)), true);
            return 0;
        } catch (IOException e) {
            throw FAILED_TO_BLACKLIST.create("ip");
        }
    }

    private static int blacklistName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            String name = StringArgumentType.getString(context, "name");
            if (Antiscan.NAME_CHECKER.blacklist(name, Antiscan.NAME_CHECKER_FILE)) {
                context.getSource().sendSuccess(() -> Utils.textOf(String.format("Blacklisted %s.", name)), true);
                return Command.SINGLE_SUCCESS;
            }
            context.getSource().sendSuccess(() -> Utils.textOf(String.format("%s was already blacklisted!", name)), true);
            return 0;
        } catch (IOException e) {
            throw FAILED_TO_BLACKLIST.create("name");
        }
    }

    private static int checkIp(CommandContext<CommandSourceStack> context) {
        String ip = StringArgumentType.getString(context, "ip");
        if (Antiscan.CONNECTION_CHECKER.isBlacklisted(ip)) {
            context.getSource().sendSuccess(() -> Utils.textOf(String.format("%s is blacklisted.", ip)), false);
            return Command.SINGLE_SUCCESS;
        }
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("%s is not blacklisted.", ip)), false);
        return 0;
    }

    private static int checkName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "name");
        int blacklisted = 0;
        for (GameProfile profile : profiles) {
            if (Antiscan.NAME_CHECKER.isBlacklisted(profile.getName())) {
                context.getSource().sendSuccess(() -> Utils.textOf(String.format("%s is blacklisted.", profile.getName())), false);
                blacklisted++;
            } else {
                context.getSource().sendSuccess(() -> Utils.textOf(String.format("%s is not blacklisted.", profile.getName())), false);
            }
        }
        int finalBlacklisted = blacklisted;
        if (profiles.size() != 1) {
            context.getSource().sendSuccess(() -> Utils.textOf(String.format("%d/%d are blacklisted.", finalBlacklisted, profiles.size())), false);
        }
        return finalBlacklisted;
    }

    private static int displayHandshakeAction(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("Handshake action is %s.", Antiscan.CONFIG.getHandshakeAction().getSerializedName())), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayHandshakeMode(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("Handshake mode is %s.", Antiscan.CONFIG.getHandshakeMode().getSerializedName())), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayHandshakeReport(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("Reporting is %s.", Antiscan.CONFIG.isHandshakeReport() ? "on" : "off")), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayLoginAction(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("Login action is %s.", Antiscan.CONFIG.getLoginAction().getSerializedName())), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayLoginMode(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("Login mode is %s.", Antiscan.CONFIG.getLoginMode().getSerializedName())), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayLoginReport(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("Reporting is %s.", Antiscan.CONFIG.isLoginReport() ? "on" : "off")), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayPingAction(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("Ping action is %s.", Antiscan.CONFIG.getPingAction().getSerializedName())), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayPingMode(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("Ping mode is %s.", Antiscan.CONFIG.getPingMode().getSerializedName())), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayPingReport(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("Reporting is %s.", Antiscan.CONFIG.isPingReport() ? "on" : "off")), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayQueryAction(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("Query action is %s.", Antiscan.CONFIG.getQueryAction().getSerializedName())), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayQueryMode(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("Query mode is %s.", Antiscan.CONFIG.getQueryMode().getSerializedName())), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int displayQueryReport(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("Reporting is %s.", Antiscan.CONFIG.isQueryReport() ? "on" : "off")), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int forceUpdateIpBlacklist(CommandContext<CommandSourceStack> context) {
        Antiscan.CONNECTION_CHECKER.updateNow(Antiscan.CONNECTION_CHECKER_FILE);
        context.getSource().sendSuccess(() -> Utils.textOf("IP blacklist will be updated."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int listAllBlacklistedIps(CommandContext<CommandSourceStack> context) {
        for (String ip : Antiscan.CONNECTION_CHECKER.getManualBlacklist()) {
            context.getSource().sendSuccess(() -> Utils.textOf(ip), false);
        }
        int ips = Antiscan.CONNECTION_CHECKER.getManualBlacklist().size();
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("(%d IPs manually blacklisted)", ips)), false);
        for (String ip : Antiscan.CONNECTION_CHECKER.getBlacklistCache()) {
            context.getSource().sendSuccess(() -> Utils.textOf(ip), false);
        }
        int cached = Antiscan.CONNECTION_CHECKER.getBlacklistCache().size();
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("(%d IPs automatically blacklisted)", cached)), false);
        return ips + cached;
    }

    private static int listBlacklistedIps(CommandContext<CommandSourceStack> context) {
        for (String ip : Antiscan.CONNECTION_CHECKER.getManualBlacklist()) {
            context.getSource().sendSuccess(() -> Utils.textOf(ip), false);
        }
        int ips = Antiscan.CONNECTION_CHECKER.getManualBlacklist().size();
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("(%d IPs)", ips)), false);
        return ips;
    }

    private static int listBlacklistedNames(CommandContext<CommandSourceStack> context) {
        for (String name : Antiscan.NAME_CHECKER.getBlacklist()) {
            context.getSource().sendSuccess(() -> Utils.textOf(name), false);
        }
        int names = Antiscan.NAME_CHECKER.getBlacklist().size();
        context.getSource().sendSuccess(() -> Utils.textOf(String.format("(%d names)", names)), false);
        return names;
    }

    private static int displayLogActions(CommandContext<CommandSourceStack> context) {
        if (Antiscan.CONFIG.shouldLogActions()) {
            context.getSource().sendSuccess(() -> Utils.textOf("Action logging is on."), false);
            return Command.SINGLE_SUCCESS;
        }
        context.getSource().sendSuccess(() -> Utils.textOf("Action logging is off."), false);
        return 0;
    }

    private static int setAbuseIpdbKey(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setAbuseIpdbKey(StringArgumentType.getString(context, "key"), Antiscan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_KEY.create();
        }
        context.getSource().sendSuccess(() -> Utils.textOf("Set! Make sure to clear your command history (including the file!) or terminal."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setHandshakeAction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setHandshakeAction(Config.Action.fromId(StringArgumentType.getString(context, "action")), Antiscan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayHandshakeAction(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setHandshakeMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setHandshakeMode(Config.IpMode.fromId(StringArgumentType.getString(context, "mode")), Antiscan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayHandshakeMode(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setHandshakeReport(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setHandshakeReport(BoolArgumentType.getBool(context, "report"), Antiscan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayHandshakeReport(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setLoginAction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setLoginAction(Config.Action.fromId(StringArgumentType.getString(context, "action")), Antiscan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayLoginAction(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setLoginMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setLoginMode(Config.NameIpMode.fromId(StringArgumentType.getString(context, "mode")), Antiscan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayLoginMode(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setLoginReport(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setLoginReport(BoolArgumentType.getBool(context, "report"), Antiscan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayLoginReport(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setPingAction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setPingAction(Config.Action.fromId(StringArgumentType.getString(context, "action")), Antiscan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayPingAction(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setPingMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setPingMode(Config.IpMode.fromId(StringArgumentType.getString(context, "mode")), Antiscan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayPingMode(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setPingReport(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setPingReport(BoolArgumentType.getBool(context, "report"), Antiscan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayPingReport(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setQueryAction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setQueryAction(Config.Action.fromId(StringArgumentType.getString(context, "action")), Antiscan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayQueryAction(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setQueryMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setQueryMode(Config.IpMode.fromId(StringArgumentType.getString(context, "mode")), Antiscan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayQueryMode(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setQueryReport(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setQueryReport(BoolArgumentType.getBool(context, "report"), Antiscan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayQueryReport(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int unBlacklistIp(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            String ip = StringArgumentType.getString(context, "ip");
            if (Antiscan.CONNECTION_CHECKER.unBlacklist(ip, true, Antiscan.CONNECTION_CHECKER_FILE)) {
                context.getSource().sendSuccess(() -> Utils.textOf(String.format("Removed %s from the blacklist!", ip)), true);
                return Command.SINGLE_SUCCESS;
            }
            context.getSource().sendSuccess(() -> Utils.textOf(String.format("%s was not blacklisted!", ip)), true);
            return 0;
        } catch (IOException e) {
            throw FAILED_TO_UN_BLACKLIST.create("ip");
        }
    }

    private static int unBlacklistName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            String name = StringArgumentType.getString(context, "name");
            if (Antiscan.NAME_CHECKER.unBlacklist(name, Antiscan.NAME_CHECKER_FILE)) {
                context.getSource().sendSuccess(() -> Utils.textOf(String.format("Removed %s from the blacklist!", name)), true);
                return Command.SINGLE_SUCCESS;
            }
            context.getSource().sendSuccess(() -> Utils.textOf(String.format("%s was not blacklisted!", name)), true);
            return 0;
        } catch (IOException e) {
            throw FAILED_TO_UN_BLACKLIST.create("name");
        }
    }

    private static int updateIpBlacklist(CommandContext<CommandSourceStack> context) {
        Antiscan.CONNECTION_CHECKER.update(TimeUnit.HOURS.toMillis(5), Antiscan.CONNECTION_CHECKER_FILE);
        context.getSource().sendSuccess(() -> Utils.textOf("IP blacklist will be updated if it has not been updated in the last 5 hours. Add \"force\" to do it now."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setBlacklistUpdateCooldown(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setBlacklistUpdateCooldown(LongArgumentType.getLong(context, "cooldown"), Antiscan.CONFIG_FILE);
            context.getSource().sendSuccess(() -> Utils.textOf("Set cooldown!"), false);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int displayStats(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Utils.textOf("Please wait..."), false);
        new Thread(() -> context.getSource().sendSuccess(() -> Utils.textOf(String.format("Reports sent: %d", Antiscan.STATS.getIpsReported())), false), "Antiscan Stat Reporting").start();

        return Command.SINGLE_SUCCESS;
    }

    private static int displayLogReports(CommandContext<CommandSourceStack> context) {
        if (Antiscan.CONFIG.shouldLogReports()) {
            context.getSource().sendSuccess(() -> Utils.textOf("Report logging is on."), false);
            return Command.SINGLE_SUCCESS;
        }
        context.getSource().sendSuccess(() -> Utils.textOf("Report logging is off."), false);
        return 0;
    }

    private static int reportIp(CommandContext<CommandSourceStack> context) {
        Antiscan.CONNECTION_CHECKER.report(StringArgumentType.getString(context, "ip"), "Reported manually with Antiscan for Fabric", new int[]{14});
        context.getSource().sendSuccess(() -> Utils.textOf("Report sent."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setLogActions(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setLogActions(BoolArgumentType.getBool(context, "log"), Antiscan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayLogActions(context);
        return Command.SINGLE_SUCCESS;
    }

    private static int setLogReports(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            Antiscan.CONFIG.setLogReports(BoolArgumentType.getBool(context, "log"), Antiscan.CONFIG_FILE);
        } catch (IOException e) {
            throw FAILED_TO_SET_CONFIG.create();
        }
        displayLogReports(context);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, CommandSelection environment) {
        SuggestionProvider<CommandSourceStack> actionSuggestionProvider = (context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Config.Action.values()).map(Config.Action::getSerializedName), builder);
        SuggestionProvider<CommandSourceStack> ipModeSuggestionProvider = (context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Config.IpMode.values()).map(Config.IpMode::getSerializedName), builder);
        SuggestionProvider<CommandSourceStack> nameIpModeSuggestionProvider = (context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Config.NameIpMode.values()).map(Config.NameIpMode::getSerializedName), builder);

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
        var nameBlacklistCheckName = argument("name", GameProfileArgument.gameProfile())
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
                .suggests(ipModeSuggestionProvider)
                .executes(CommandHandler::setHandshakeMode)
                .build();
        var configHandshakeAction = literal("action")
                .requires(Permissions.require("antiscan.config.handshake.action", 4))
                .executes(CommandHandler::displayHandshakeAction)
                .build();
        var configHandshakeActionAction = argument("action", StringArgumentType.string())
                .requires(Permissions.require("antiscan.config.handshake.action.set", 4))
                .suggests(actionSuggestionProvider)
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
        var configLogin = literal("login")
                .requires(Permissions.require("antiscan.config.login", 4))
                .build();
        var configLoginMode = literal("mode")
                .requires(Permissions.require("antiscan.config.login.mode", 4))
                .executes(CommandHandler::displayLoginMode)
                .build();
        var configLoginModeMode = argument("mode", StringArgumentType.string())
                .requires(Permissions.require("antiscan.config.login.mode.set", 4))
                .suggests(nameIpModeSuggestionProvider)
                .executes(CommandHandler::setLoginMode)
                .build();
        var configLoginAction = literal("action")
                .requires(Permissions.require("antiscan.config.login.action", 4))
                .executes(CommandHandler::displayLoginAction)
                .build();
        var configLoginActionAction = argument("action", StringArgumentType.string())
                .requires(Permissions.require("antiscan.config.login.action.set", 4))
                .suggests(actionSuggestionProvider)
                .executes(CommandHandler::setLoginAction)
                .build();
        var configLogReport = literal("report")
                .requires(Permissions.require("antiscan.config.login.report", 4))
                .executes(CommandHandler::displayLoginReport)
                .build();
        var configLogReportReport = argument("report", BoolArgumentType.bool())
                .requires(Permissions.require("antiscan.config.login.report.set", 4))
                .executes(CommandHandler::setLoginReport)
                .build();
        var configPing = literal("ping")
                .requires(Permissions.require("antiscan.config.ping", 4))
                .build();
        var configPingMode = literal("mode")
                .requires(Permissions.require("antiscan.config.ping.mode", 4))
                .executes(CommandHandler::displayPingMode)
                .build();
        var configPingModeMode = argument("mode", StringArgumentType.string())
                .requires(Permissions.require("antiscan.config.ping.mode.set", 4))
                .suggests(ipModeSuggestionProvider)
                .executes(CommandHandler::setPingMode)
                .build();
        var configPingAction = literal("action")
                .requires(Permissions.require("antiscan.config.ping.action", 4))
                .executes(CommandHandler::displayPingAction)
                .build();
        var configPingActionAction = argument("action", StringArgumentType.string())
                .requires(Permissions.require("antiscan.config.ping.action.set", 4))
                .suggests(actionSuggestionProvider)
                .executes(CommandHandler::setPingAction)
                .build();
        var configPingReport = literal("report")
                .requires(Permissions.require("antiscan.config.ping.report", 4))
                .executes(CommandHandler::displayPingReport)
                .build();
        var configPingReportReport = argument("report", BoolArgumentType.bool())
                .requires(Permissions.require("antiscan.config.ping.report.set", 4))
                .executes(CommandHandler::setPingReport)
                .build();
        var configQuery = literal("query")
                .requires(Permissions.require("antiscan.config.query", 4))
                .build();
        var configQueryMode = literal("mode")
                .requires(Permissions.require("antiscan.config.query.mode", 4))
                .executes(CommandHandler::displayQueryMode)
                .build();
        var configQueryModeMode = argument("mode", StringArgumentType.string())
                .requires(Permissions.require("antiscan.config.query.mode.set", 4))
                .suggests(ipModeSuggestionProvider)
                .executes(CommandHandler::setQueryMode)
                .build();
        var configQueryAction = literal("action")
                .requires(Permissions.require("antiscan.config.query.action", 4))
                .executes(CommandHandler::displayQueryAction)
                .build();
        var configQueryActionAction = argument("action", StringArgumentType.string())
                .requires(Permissions.require("antiscan.config.query.action.set", 4))
                .suggests(actionSuggestionProvider)
                .executes(CommandHandler::setQueryAction)
                .build();
        var configQueryReport = literal("report")
                .requires(Permissions.require("antiscan.config.query.report", 4))
                .executes(CommandHandler::displayQueryReport)
                .build();
        var configQueryReportReport = argument("report", BoolArgumentType.bool())
                .requires(Permissions.require("antiscan.config.query.report.set", 4))
                .executes(CommandHandler::setQueryReport)
                .build();
        var configBlacklistUpdateCooldown = literal("blacklistUpdateCooldown")
                .requires(Permissions.require("antiscan.config.blacklistUpdateCooldown", 4))
                .build();
        var configBlacklistUpdateCooldownCooldown = argument("milliseconds", LongArgumentType.longArg(1))
                .requires(Permissions.require("antiscan.config.blacklistUpdateCooldown", 4))
                .executes(CommandHandler::setBlacklistUpdateCooldown)
                .build();
        var configLog = literal("log")
                .requires(Permissions.require("antiscan.config.log", 4))
                .build();
        var configLogReports = literal("reports")
                .requires(Permissions.require("antiscan.config.log.reports", 4))
                .executes(CommandHandler::displayLogReports)
                .build();
        var configLogReportsLog = argument("log", BoolArgumentType.bool())
                .requires(Permissions.require("antiscan.config.log.reports.set", 4))
                .executes(CommandHandler::setLogReports)
                .build();
        var configLogActions = literal("actions")
                .requires(Permissions.require("antiscan.config.log.actions", 4))
                .executes(CommandHandler::displayLogActions)
                .build();
        var configLogActionsLog = argument("log", BoolArgumentType.bool())
                .requires(Permissions.require("antiscan.config.log.actions.set", 4))
                .executes(CommandHandler::setLogActions)
                .build();

        var report = literal("report")
                .requires(Permissions.require("antiscan.report", 4))
                .build();
        var reportIp = argument("ip", StringArgumentType.string())
                .requires(Permissions.require("antiscan.report", 4))
                .executes(CommandHandler::reportIp)
                .build();
        var stats = literal("stats")
                .requires(Permissions.require("antiscan.stats", 3))
                .executes(CommandHandler::displayStats)
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
                config.addChild(configLogin);
                    configLogin.addChild(configLoginAction);
                        configLoginAction.addChild(configLoginActionAction);
                    configLogin.addChild(configLoginMode);
                        configLoginMode.addChild(configLoginModeMode);
                    configLogin.addChild(configLogReport);
                        configLogReport.addChild(configLogReportReport);
                config.addChild(configQuery);
                    configQuery.addChild(configQueryAction);
                        configQueryAction.addChild(configQueryActionAction);
                    configQuery.addChild(configQueryMode);
                        configQueryMode.addChild(configQueryModeMode);
                    configQuery.addChild(configQueryReport);
                        configQueryReport.addChild(configQueryReportReport);
                config.addChild(configPing);
                    configPing.addChild(configPingAction);
                        configPingAction.addChild(configPingActionAction);
                    configPing.addChild(configPingMode);
                        configPingMode.addChild(configPingModeMode);
                    configPing.addChild(configPingReport);
                        configPingReport.addChild(configPingReportReport);
                config.addChild(configBlacklistUpdateCooldown);
                    configBlacklistUpdateCooldown.addChild(configBlacklistUpdateCooldownCooldown);
                config.addChild(configLog);
                    configLog.addChild(configLogReports);
                        configLogReports.addChild(configLogReportsLog);
                    configLog.addChild(configLogActions);
                        configLogActions.addChild(configLogActionsLog);
            antiScan.addChild(report);
                report.addChild(reportIp);
            antiScan.addChild(stats);
        //@formatter:on
    }
}
