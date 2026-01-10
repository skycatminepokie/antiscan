package com.skycatdev.antiscan.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.permissions.PermissionLevel;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class AntiscanCommands {
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                        CommandBuildContext context,
                                        Commands.CommandSelection environment) {
        var antiscan = literal("antiscan")
                .requires(Permissions.require("antiscan", PermissionLevel.ADMINS))
                .build();
        var blacklist = literal("blacklist")
                .requires(Permissions.require("antiscan.blacklist", PermissionLevel.ADMINS))
                .build();
        var blacklistIp = literal("ip")
                .requires(Permissions.require("antiscan.blacklist.ip", PermissionLevel.ADMINS))
                .build();
        // NOPUSH executes
        var blacklistIpList = literal("list")
                .requires(Permissions.require("antiscan.blacklist.ip.list", PermissionLevel.ADMINS))
                .build();
        var blacklistIpAdd = literal("add")
                .requires(Permissions.require("antiscan.blacklist.ip.add", PermissionLevel.OWNERS))
                .build();
        // NOPUSH executes
        var blacklistIpAddIp = argument("ip", StringArgumentType.word())
                .requires(Permissions.require("antiscan.blacklist.ip.add", PermissionLevel.OWNERS))
                .build();
        var blacklistIpRemove = literal("remove")
                .requires(Permissions.require("antiscan.blacklist.ip.remove", PermissionLevel.ADMINS))
                .build();
        // NOPUSH executes
        var blacklistIpRemoveIp= argument("ip", StringArgumentType.word())
                .requires(Permissions.require("antiscan.blacklist.ip.remove", PermissionLevel.ADMINS))
                .build();
        var blacklistName = literal("name")
                .requires(Permissions.require("antiscan.blacklist.name", PermissionLevel.ADMINS))
                .build();
        // NOPUSH executes
        var blacklistNameList = literal("list")
                .requires(Permissions.require("antiscan.blacklist.name.list", PermissionLevel.ADMINS))
                .build();
        var blacklistNameAdd = literal("add")
                .requires(Permissions.require("antiscan.blacklist.name.add", PermissionLevel.OWNERS))
                .build();
        // NOPUSH executes
        var blacklistNameAddName = argument("name", GameProfileArgument.gameProfile())
                .requires(Permissions.require("antiscan.blacklist.name.add", PermissionLevel.OWNERS))
                .build();
        var blacklistNameRemove = literal("remove")
                .requires(Permissions.require("antiscan.blacklist.name.remove", PermissionLevel.ADMINS))
                .build();
        // NOPUSH executes
        var blacklistNameRemoveName = argument("name", GameProfileArgument.gameProfile())
                .requires(Permissions.require("antiscan.blacklist.name.remove", PermissionLevel.ADMINS))
                .build();

        var whitelist = literal("whitelist")
                .requires(Permissions.require("antiscan.whitelist", PermissionLevel.ADMINS))
                .build();
        var whitelistIp = literal("ip")
                .requires(Permissions.require("antiscan.whitelist.ip", PermissionLevel.ADMINS))
                .build();
        // NOPUSH executes
        var whitelistIpList = literal("list")
                .requires(Permissions.require("antiscan.whitelist.ip.list", PermissionLevel.ADMINS))
                .build();
        var whitelistIpAdd = literal("add")
                .requires(Permissions.require("antiscan.whitelist.ip.add", PermissionLevel.OWNERS))
                .build();
        // NOPUSH executes
        var whitelistIpAddIp = argument("ip", StringArgumentType.word())
                .requires(Permissions.require("antiscan.whitelist.ip.add", PermissionLevel.OWNERS))
                .build();
        var whitelistIpRemove = literal("remove")
                .requires(Permissions.require("antiscan.whitelist.ip.remove", PermissionLevel.ADMINS))
                .build();
        // NOPUSH executes
        var whitelistIpRemoveIp= argument("ip", StringArgumentType.word())
                .requires(Permissions.require("antiscan.whitelist.ip.remove", PermissionLevel.ADMINS))
                .build();
        var whitelistName = literal("name")
                .requires(Permissions.require("antiscan.whitelist.name", PermissionLevel.ADMINS))
                .build();
        // NOPUSH executes
        var whitelistNameList = literal("list")
                .requires(Permissions.require("antiscan.whitelist.name.list", PermissionLevel.ADMINS))
                .build();
        var whitelistNameAdd = literal("add")
                .requires(Permissions.require("antiscan.whitelist.name.add", PermissionLevel.OWNERS))
                .build();
        // NOPUSH executes
        var whitelistNameAddName = argument("name", GameProfileArgument.gameProfile())
                .requires(Permissions.require("antiscan.whitelist.name.add", PermissionLevel.OWNERS))
                .build();
        var whitelistNameRemove = literal("remove")
                .requires(Permissions.require("antiscan.whitelist.name.remove", PermissionLevel.ADMINS))
                .build();
        // NOPUSH executes
        var whitelistNameRemoveName = argument("name", GameProfileArgument.gameProfile())
                .requires(Permissions.require("antiscan.whitelist.name.remove", PermissionLevel.ADMINS))
                .build();

        var report = literal("report")
                .requires(Permissions.require("antiscan.report", PermissionLevel.OWNERS))
                .build();
        var reportIp = argument("ip", StringArgumentType.word())
                .requires(Permissions.require("antiscan.report", PermissionLevel.OWNERS))
                .build();
        // NOPUSH executes
        var reportIpReason = argument("reason", StringArgumentType.string())
                .requires(Permissions.require("antiscan.report", PermissionLevel.OWNERS))
                .build();

        //@formatter:off
        dispatcher.getRoot().addChild(antiscan);
        antiscan.addChild(blacklist);
            blacklist.addChild(blacklistIp);
                blacklistIp.addChild(blacklistIpList);
                blacklistIp.addChild(blacklistIpAdd);
                    blacklistIpAdd.addChild(blacklistIpAddIp);
                blacklistIp.addChild(blacklistIpRemove);
                    blacklistIpRemove.addChild(blacklistIpRemoveIp);
            blacklist.addChild(blacklistName);
                blacklistName.addChild(blacklistNameList);
                blacklistName.addChild(blacklistNameAdd);
                    blacklistNameAdd.addChild(blacklistNameAddName);
                blacklistName.addChild(blacklistNameRemove);
                    blacklistNameRemove.addChild(blacklistNameRemoveName);
        antiscan.addChild(whitelist);
            whitelist.addChild(whitelistIp);
                whitelistIp.addChild(whitelistIpList);
                whitelistIp.addChild(whitelistIpAdd);
                    whitelistIpAdd.addChild(whitelistIpAddIp);
                whitelistIp.addChild(whitelistIpRemove);
                    whitelistIpRemove.addChild(whitelistIpRemoveIp);
            whitelist.addChild(whitelistName);
                whitelistName.addChild(whitelistNameList);
                whitelistName.addChild(whitelistNameAdd);
                    whitelistNameAdd.addChild(whitelistNameAddName);
                whitelistName.addChild(whitelistNameRemove);
                    whitelistNameRemove.addChild(whitelistNameRemoveName);
        antiscan.addChild(report);
            report.addChild(reportIp);
                reportIp.addChild(reportIpReason);
        //@formatter:on
    }
}
