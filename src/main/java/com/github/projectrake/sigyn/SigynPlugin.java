package com.github.projectrake.sigyn;

import com.github.projectrake.hdbm.HDBMPlugin;
import com.github.projectrake.injector.InjectorMain;
import com.github.projectrake.plugin.ExtendedJavaPlugin;
import com.mojang.authlib.GameProfile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.UUID;

/**
 * Created on 08/01/2018.
 */
public class SigynPlugin extends ExtendedJavaPlugin {
    private final static Logger LOG = LogManager.getLogger(SigynPlugin.class);
    private PlayerDataManager pinManager;

    @Override
    public void onEnable() {
        InjectorMain.requirePatched(GameProfile.class);
        HDBMPlugin.getInstance().addClasses(TrackedPlayerRecord.class);

        pinManager = new PlayerDataManager();
        getServer().getPluginManager().registerEvents(new LoginListener(pinManager), this);
        autoBindCommands(getClass());
    }

    public static Optional<UntrackedPlayerRecord> getPlayerDataByUUID(UUID playeruuid) {
        return getInstance().pinManager.getNameOptional(playeruuid);
    }

    public static SigynPlugin getInstance() {
        return SigynPlugin.getPlugin(SigynPlugin.class);
    }

    public static void setPlayerNameByUUID(UUID playerUUID, String newname) {
        LOG.debug("Renaming player: " + playerUUID + " to \"" + newname + "\".");
        getInstance().pinManager.setPlayerNameByUUID(playerUUID, newname);
    }

    public static void addPlayer(UUID playerUUID, String newname) {
        LOG.debug("Adding player: " + playerUUID + "/\"" + newname + "\".");
        getInstance().pinManager.addPlayer(playerUUID, newname);
    }

    public static Optional<UntrackedPlayerRecord> getPlayerByName(String name) {
        return getInstance().pinManager.getPlayerByName(name);
    }

    public void flushCache() {
        pinManager.flush();
    }
}
