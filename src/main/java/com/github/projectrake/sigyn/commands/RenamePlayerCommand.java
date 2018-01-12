package com.github.projectrake.sigyn.commands;

import com.github.projectrake.sigyn.SigynPlugin;
import com.github.projectrake.sigyn.UntrackedPlayerRecord;
import com.github.projectrake.plugin.commands.ArgumentChecker;
import com.github.projectrake.plugin.commands.CommandBinding;
import com.github.projectrake.plugin.commands.ExtendedCommandExecutor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Optional;

/**
 * Created on 10/01/2018.
 */
@CommandBinding("renameplayer")
public class RenamePlayerCommand extends ExtendedCommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String alias, String[] args, List<String> errors) {
        String oldname = args[0];
        String newname = args[1];

        Optional<UntrackedPlayerRecord> opt = SigynPlugin.getPlayerByName(oldname);

        opt.ifPresentOrElse(data -> {
                    commandSender.sendMessage("Renaming player \"" + data.getOriginalName() + "\"(" + data.getPlayerUUID() + ") currently mapped as \"" + data.getMappedName() + "\" to " + newname + ".");
                    SigynPlugin.getInstance().setPlayerNameByUUID(data.getPlayerUUID(), newname);
                },
                () -> {
                    errors.add(ChatColor.RED + "No player for name \"" + oldname + "\".");
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
                String oldname = args[0];
                String newname = args[1];

                if (oldname.length() > 16) {
                    errors.add(ChatColor.RED + "Player old name \"" + oldname + "\" is too long, this will lead to issues.");
                } else if (oldname.length() < 3) {
                    errors.add(ChatColor.RED + "Player old name \"" + oldname + "\" is too short, this will lead to issues.");
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
