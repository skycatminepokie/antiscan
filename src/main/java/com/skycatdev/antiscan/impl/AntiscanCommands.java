package com.skycatdev.antiscan.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
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
        var blacklistIpList = literal("list")
                .requires(Permissions.require("antiscan.blacklist.ip.list", PermissionLevel.ADMINS))
                .executes(showList(true, true))
                .build();
        var blacklistIpAdd = literal("add")
                .requires(Permissions.require("antiscan.blacklist.ip.add", PermissionLevel.OWNERS))
                .build();
        var blacklistIpAddIp = argument("ip", StringArgumentType.word())
                .requires(Permissions.require("antiscan.blacklist.ip.add", PermissionLevel.OWNERS))
                .executes(modifyList(true, true, true))
                .build();
        var blacklistIpRemove = literal("remove")
                .requires(Permissions.require("antiscan.blacklist.ip.remove", PermissionLevel.ADMINS))
                .build();
        var blacklistIpRemoveIp= argument("ip", StringArgumentType.word())
                .requires(Permissions.require("antiscan.blacklist.ip.remove", PermissionLevel.ADMINS))
                .executes(modifyList(true, true, false))
                .build();
        var blacklistName = literal("name")
                .requires(Permissions.require("antiscan.blacklist.name", PermissionLevel.ADMINS))
                .build();
        var blacklistNameList = literal("list")
                .requires(Permissions.require("antiscan.blacklist.name.list", PermissionLevel.ADMINS))
                .executes(showList(true, false))
                .build();
        var blacklistNameAdd = literal("add")
                .requires(Permissions.require("antiscan.blacklist.name.add", PermissionLevel.OWNERS))
                .build();
        var blacklistNameAddName = argument("name", GameProfileArgument.gameProfile())
                .requires(Permissions.require("antiscan.blacklist.name.add", PermissionLevel.OWNERS))
                .executes(modifyList(true, false, true))
                .build();
        var blacklistNameRemove = literal("remove")
                .requires(Permissions.require("antiscan.blacklist.name.remove", PermissionLevel.ADMINS))
                .build();
        var blacklistNameRemoveName = argument("name", GameProfileArgument.gameProfile())
                .requires(Permissions.require("antiscan.blacklist.name.remove", PermissionLevel.ADMINS))
                .executes(modifyList(true, false, false))
                .build();

        var whitelist = literal("whitelist")
                .requires(Permissions.require("antiscan.whitelist", PermissionLevel.ADMINS))
                .build();
        var whitelistIp = literal("ip")
                .requires(Permissions.require("antiscan.whitelist.ip", PermissionLevel.ADMINS))
                .build();
        var whitelistIpList = literal("list")
                .requires(Permissions.require("antiscan.whitelist.ip.list", PermissionLevel.ADMINS))
                .executes(showList(false, true))
                .build();
        var whitelistIpAdd = literal("add")
                .requires(Permissions.require("antiscan.whitelist.ip.add", PermissionLevel.OWNERS))
                .build();
        var whitelistIpAddIp = argument("ip", StringArgumentType.word())
                .requires(Permissions.require("antiscan.whitelist.ip.add", PermissionLevel.OWNERS))
                .executes(modifyList(false, true, true))
                .build();
        var whitelistIpRemove = literal("remove")
                .requires(Permissions.require("antiscan.whitelist.ip.remove", PermissionLevel.ADMINS))
                .build();
        var whitelistIpRemoveIp= argument("ip", StringArgumentType.word())
                .requires(Permissions.require("antiscan.whitelist.ip.remove", PermissionLevel.ADMINS))
                .executes(modifyList(false, true, false))
                .build();
        var whitelistName = literal("name")
                .requires(Permissions.require("antiscan.whitelist.name", PermissionLevel.ADMINS))
                .build();
        var whitelistNameList = literal("list")
                .requires(Permissions.require("antiscan.whitelist.name.list", PermissionLevel.ADMINS))
                .executes(showList(false, false))
                .build();
        var whitelistNameAdd = literal("add")
                .requires(Permissions.require("antiscan.whitelist.name.add", PermissionLevel.OWNERS))
                .build();
        var whitelistNameAddName = argument("name", GameProfileArgument.gameProfile())
                .requires(Permissions.require("antiscan.whitelist.name.add", PermissionLevel.OWNERS))
                .executes(modifyList(false, false, true))
                .build();
        var whitelistNameRemove = literal("remove")
                .requires(Permissions.require("antiscan.whitelist.name.remove", PermissionLevel.ADMINS))
                .build();
        var whitelistNameRemoveName = argument("name", GameProfileArgument.gameProfile())
                .requires(Permissions.require("antiscan.whitelist.name.remove", PermissionLevel.ADMINS))
                .executes(modifyList(false, false, false))
                .build();

        var report = literal("report")
                .requires(Permissions.require("antiscan.report", PermissionLevel.OWNERS))
                .build();
        var reportIp = argument("ip", StringArgumentType.word())
                .requires(Permissions.require("antiscan.report", PermissionLevel.OWNERS))
                .build();
        var reportIpReason = argument("reason", StringArgumentType.string())
                .requires(Permissions.require("antiscan.report", PermissionLevel.OWNERS))
                .executes(AntiscanCommands::reportIp)
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

    private static int reportIp(CommandContext<CommandSourceStack> context) {
        // NOPUSH
    }

    private static Command<CommandSourceStack> modifyList(boolean blacklist, boolean ipList, boolean add) {
        // NOPUSH
    }

    private static Command<CommandSourceStack> showList(boolean blacklist, boolean ipList) {
        // NOPUSH
    }
}
