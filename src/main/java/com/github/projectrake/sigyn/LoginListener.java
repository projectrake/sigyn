package com.github.projectrake.sigyn;

import com.mojang.authlib.CompleteGameProfileCreationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.Math.min;

/**
 * Created on 09/01/2018.
 */
class LoginListener implements Listener {
    private final static Logger LOG = LogManager.getLogger(LoginListener.class);
    private final PlayerDataManager pinManager;
    private final ConcurrentMap<UUID, Long> warnOnLogin = new ConcurrentHashMap<>();
    private final AtomicInteger warnOnLoginCounter = new AtomicInteger();

    LoginListener(PlayerDataManager pinManager) {
        this.pinManager = pinManager;
    }

    @EventHandler
    public void onGameProfileCompletion(CompleteGameProfileCreationEvent ev) {
        Optional<UntrackedPlayerRecord> namedataopt = pinManager.getPlayerByName(ev.getName());

        if (namedataopt.isPresent()) {
            UntrackedPlayerRecord namedata = namedataopt.get();

            if (namedata.getPlayerUUID().equals(ev.getUuid())) {
                LOG.debug("Player name matches database record: " + ev.getName() + " (" + ev.getUuid() + ")");
                return;
            } else {
                LOG.debug("Player name collision: " + ev.getName() + " (" + ev.getUuid() + ") & " + namedata);
                final String prefix = ":COL:";
                ev.setName(prefix + ev.getName().substring(0, min(ev.getName().length(), 16 - prefix.length())));
                warnOnLogin.putIfAbsent(ev.getUuid(), System.currentTimeMillis());
            }
        }

//        UntrackedPinPlayerRecord data = pinManager.getOrCreateData(ev.getName(), ev.getUuid());
//
//        if (data.getPlayerUUID().equals(ev.getUuid())) {
//            enforce(data, ev);
//        } else {
//            ev.setName("$" + ev.getName());
//        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent ev) {
        warnOnLogin.computeIfPresent(ev.getPlayer().getUniqueId(), (key, value) -> {
            LOG.debug("Delivering player renamed message.");
            SigynPlugin.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(
                    SigynPlugin.getInstance(),
                    () -> {
                        ev.getPlayer().sendMessage(new String[]{
                                ChatColor.YELLOW + "You've been renamed because your name was already taken on this server.",
                                ChatColor.YELLOW + "Contact the admins if you require help with this."
                        });
                    },
                    20
            );
            return null;
        });

        if (warnOnLoginCounter.incrementAndGet() % 1000 == 0) {
            List<UUID> remove = warnOnLogin.entrySet().stream()
                    .filter(en -> System.currentTimeMillis() - en.getValue() > TimeUnit.MINUTES.toMillis(1))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            remove.forEach(warnOnLogin::remove);
        }
    }


    private void enforce(UntrackedPlayerRecord data, CompleteGameProfileCreationEvent ev) {
        LOG.debug("Login event: " + data);
        if (!data.isUnchanged()) {
            ev.setUuid(data.getPlayerUUID());
            ev.setName(data.getMappedName());
            LOG.debug("Mapping player " + data);
        }
    }
}
