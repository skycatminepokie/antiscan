package com.skycatdev.antiscan.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.skycatdev.antiscan.Antiscan;
import com.skycatdev.antiscan.impl.checker.VerificationList;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.*;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.util.CommonColors;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class AntiscanCommands {
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor((runnable) -> {
        Thread thread = new Thread(runnable, "Antiscan command executor thread");
        thread.setDaemon(true);
        return thread;
    });
    public static final int PARTIAL_LIST_SIZE = 25;

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                        CommandBuildContext ignoredContext,
                                        Commands.CommandSelection ignoredEnvironment) {
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
        var blacklistIpAddIp = argument("target", StringArgumentType.word())
                .requires(Permissions.require("antiscan.blacklist.ip.add", PermissionLevel.OWNERS))
                .executes(modifyList(true, true, true))
                .build();
        var blacklistIpRemove = literal("remove")
                .requires(Permissions.require("antiscan.blacklist.ip.remove", PermissionLevel.ADMINS))
                .build();
        var blacklistIpRemoveIp= argument("target", StringArgumentType.word())
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
        var blacklistNameAddName = argument("target", StringArgumentType.string())
                .requires(Permissions.require("antiscan.blacklist.name.add", PermissionLevel.OWNERS))
                .executes(modifyList(true, false, true))
                .build();
        var blacklistNameRemove = literal("remove")
                .requires(Permissions.require("antiscan.blacklist.name.remove", PermissionLevel.ADMINS))
                .build();
        var blacklistNameRemoveName = argument("target", StringArgumentType.string())
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
        var whitelistIpAddIp = argument("target", StringArgumentType.word())
                .requires(Permissions.require("antiscan.whitelist.ip.add", PermissionLevel.OWNERS))
                .executes(modifyList(false, true, true))
                .build();
        var whitelistIpRemove = literal("remove")
                .requires(Permissions.require("antiscan.whitelist.ip.remove", PermissionLevel.ADMINS))
                .build();
        var whitelistIpRemoveIp= argument("target", StringArgumentType.word())
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
        var whitelistNameAddName = argument("target", StringArgumentType.string())
                .requires(Permissions.require("antiscan.whitelist.name.add", PermissionLevel.OWNERS))
                .executes(modifyList(false, false, true))
                .build();
        var whitelistNameRemove = literal("remove")
                .requires(Permissions.require("antiscan.whitelist.name.remove", PermissionLevel.ADMINS))
                .build();
        var whitelistNameRemoveName = argument("target", StringArgumentType.string())
                .requires(Permissions.require("antiscan.whitelist.name.remove", PermissionLevel.ADMINS))
                .executes(modifyList(false, false, false))
                .build();

        var report = literal("report")
                .requires(Permissions.require("antiscan.report", PermissionLevel.OWNERS))
                .build();
        var reportIp = argument("ip", StringArgumentType.word())
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
        //@formatter:on
    }

    @SuppressWarnings("SameReturnValue")
    private static int reportIp(CommandContext<CommandSourceStack> context) {
        String ip = StringArgumentType.getString(context, "ip");
        context.getSource().sendSuccess(() -> Component.literal("Reporting " + ip + "..."), false);
        Antiscan.CONFIG.abuseIpdbChecker().report(ip, EXECUTOR).thenAcceptAsync(success -> {
            if (success) {
                context.getSource().sendSuccess(() -> Component.literal("Successfully reported " + ip + "."), false);
            } else {
                context.getSource().sendFailure(Component.literal("Failed to report " + ip + "."));
            }
        }, context.getSource().getServer());
        return 1;
    }

    private static Command<CommandSourceStack> modifyList(boolean blacklist, boolean ipList, boolean add) {
        return (context) -> {
            String target = StringArgumentType.getString(context, "target");
            context.getSource().sendSuccess(() -> startModifyMessage(blacklist, add, target), false);
            modifyList(getList(blacklist, ipList), add, target).thenAcceptAsync((success) -> {
                if (success) {
                    context.getSource().sendSuccess(() -> endModifyMessage(blacklist, target, add, true), false);
                } else {
                    context.getSource().sendFailure(endModifyMessage(blacklist, target, add, false));
                }
            }, context.getSource().getServer());
            return 1;
        };
    }

    private static Component startModifyMessage(boolean blacklist, boolean add, String target) {
        if (add) {
            if (blacklist) {
                return Component.literal("Blacklisting " + target + "...");
            } else {
                return Component.literal("Whitelisting " + target + "...");
            }
        } else {
            if (blacklist) {
                return Component.literal("Removing " + target + " from the blacklist...");
            } else {
                return Component.literal("Removing " + target + " from the whitelist...");
            }
        }
    }

    private static Component endModifyMessage(boolean blacklist, String target, boolean add, boolean success) {
        if (success) {
            return Component.literal(String.format(
                    "Successfully %s %s %s the %slist.",
                    add ? "added" : "removed",
                    target,
                    add ? "to" : "from",
                    blacklist ? "black" : "white"
            ));
        } else {
            return Component.literal(String.format(
                    "Failed to %s %s %s the %slist",
                    add ? "add" : "remove",
                    target,
                    add ? "to" : "from",
                    blacklist ? "black" : "white"
            ));
        }
    }

    private static Command<CommandSourceStack> showList(boolean blacklist, boolean ipList) {
        return (context) -> {
            context.getSource().sendSuccess(() -> Component.literal("Please wait..."), false);
            getList(blacklist, ipList).getPart(PARTIAL_LIST_SIZE, EXECUTOR).thenAcceptAsync((listPart) -> context.getSource().sendSuccess(() -> {
                MutableComponent response = Component.literal(String.format("Showing %d/%d entries:", Math.min(PARTIAL_LIST_SIZE, listPart.superSize()), listPart.superSize()));
                for (String entry : listPart.part()) {
                    response.append("\n" + entry + " ");
                    String suggestedCommand = String.format("/antiscan %slist %s remove %s",
                            blacklist ? "black" : "white",
                            ipList ? "ip" : "name",
                            entry);
                    Component clickable = Component.literal("[X]")
                            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(CommonColors.RED))
                                    .withClickEvent(new ClickEvent.SuggestCommand(suggestedCommand)));
                    response.append(clickable);
                }
                return response;
            }, false), context.getSource().getServer());
            return 1;
        };
    }

    private static VerificationList getList(boolean blacklist, boolean ipList) {
        if (ipList) {
            if (blacklist) {
                return Antiscan.CONFIG.ipBlacklist();
            } else {
                return Antiscan.CONFIG.ipWhitelist();
            }
        } else {
            if (blacklist) {
                return Antiscan.CONFIG.nameBlacklist();
            } else {
                return Antiscan.CONFIG.nameWhitelist();
            }
        }
    }

    private static CompletableFuture<Boolean> modifyList(VerificationList list, boolean add, String target) {
        if (add) {
            return list.add(target, EXECUTOR);
        } else {
            return list.remove(target, EXECUTOR);
        }
    }
}
