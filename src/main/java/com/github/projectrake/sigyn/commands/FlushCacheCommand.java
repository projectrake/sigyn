package com.github.projectrake.sigyn.commands;

import com.github.projectrake.sigyn.SigynPlugin;
import com.github.projectrake.plugin.commands.CommandBinding;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Created on 11/01/2018.
 */
@CommandBinding("flushcache")
public class FlushCacheCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        SigynPlugin.getInstance().flushCache();
        commandSender.sendMessage(ChatColor.GREEN + "Cache flushed.");

        return true;
    }
}
