package com.github.projectrake.sigyn.commands;

import com.github.projectrake.sigyn.SigynPlugin;
import com.github.projectrake.plugin.commands.ArgumentChecker;
import com.github.projectrake.plugin.commands.CommandBinding;
import com.github.projectrake.plugin.commands.ExtendedCommandExecutor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;

/**
 * Created on 10/01/2018.
 */
@CommandBinding("renameplayeruuid")
public class PinRenamePlayerUUIDCommand extends ExtendedCommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String alias, String[] args, List<String> errors) {
        UUID playerUUID = UUID.fromString(args[0]);
        String newname = args[1];

        SigynPlugin.getPlayerDataByUUID(playerUUID).ifPresentOrElse(
                data -> {
                    commandSender.sendMessage("Renaming player \"" + data.getOriginalName() + "\"(" + playerUUID + ") currently mapped as \"" + data.getMappedName() + "\" to " + newname + ".");
                    SigynPlugin.getInstance().setPlayerNameByUUID(playerUUID, newname);
                },
                () -> {
                    errors.add(ChatColor.RED + "Can't find player with UUID " + playerUUID + ".");
                });

        return true;
    }


    @Override
    protected ArgumentChecker getArgumentChecker() {
        return (commandSender, command, alias, args, errors) -> {
            if (args.length < 2) {
                errors.add(ChatColor.RED + "Not enough arguments, old name and new name required.");
            } else if (args.length > 2) {
                errors.add(ChatColor.RED + "Too many arguments, old name and new name required.");
            } else {
                String uuidstr = args[0];
                String newname = args[1];

                try {
                    UUID uuid = UUID.fromString(uuidstr);
                } catch (IllegalArgumentException ex) {
                    errors.add(ChatColor.RED + "Player uuid doesn't match UUID format. (" + uuidstr + ")");
                }

                if (newname.length() > 16) {
                    errors.add(ChatColor.RED + "Player new name \"" + newname + "\" is too long, this will lead to issues.");
                } else if (newname.length() < 3) {
                    errors.add(ChatColor.RED + "Player new name \"" + newname + "\" is too short, this will lead to issues.");
                }
            }

            return errors.isEmpty() ? ArgumentChecker.ArgumentCheckState.ARGUMENTS_OK : ArgumentChecker.ArgumentCheckState.ARGUMENTS_NOT_OK;
        };
    }
}
