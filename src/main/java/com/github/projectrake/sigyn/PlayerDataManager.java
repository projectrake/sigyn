package com.github.projectrake.sigyn;

import com.github.projectrake.hdbm.HDBMPlugin;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.ChatColor;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Created on 10/01/2018.
 * <p>
 * TODO:
 * <p>
 * * It may be possible to replace a lot of if(Optional.isPresent) with Optional.map . Gotta check the semantics of that
 * with immutable data.
 */
class PlayerDataManager {
    private static final Logger LOG = LogManager.getLogger(PlayerDataManager.class);
    private final LoadingCache<UUID, Optional<TrackedPlayerRecord>> nameCache = CacheBuilder.newBuilder()
            .maximumSize(200)
            .initialCapacity(30)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .removalListener((RemovalListener<UUID, Optional<TrackedPlayerRecord>>) removalNotification -> removalNotification.getValue().ifPresent(this::unloadnameCacheEntry))
            .build(new CacheLoader<UUID, Optional<TrackedPlayerRecord>>() {
                @Override
                public Optional<TrackedPlayerRecord> load(UUID uuid) throws Exception {
                    return loadCacheEntry(uuid);
                }
            });

    private Map<String, UUID> mappedResolvingMap = new HashMap<>();

    private void unloadnameCacheEntry(TrackedPlayerRecord playerNameUUID) {
        mappedResolvingMap.remove(playerNameUUID.getMappedName());
        HDBMPlugin.withEntityManager(en -> {
            Transaction transaction = en.beginTransaction();
            en.saveOrUpdate(playerNameUUID);
            transaction.commit();
        });
    }

    private Optional<TrackedPlayerRecord> loadCacheEntry(UUID uuid) {
        Optional<TrackedPlayerRecord> data = HDBMPlugin.withEntityManager((Function<Session, Optional<TrackedPlayerRecord>>) en -> loadByUUID(en, uuid));

        data.ifPresent(d -> mappedResolvingMap.put(d.getMappedName(), d.getPlayerUUID()));
        return data;
    }

    private static Optional<TrackedPlayerRecord> loadByUUID(Session en, UUID uuid) {
        CriteriaBuilder builder = en.getCriteriaBuilder();
        CriteriaQuery<TrackedPlayerRecord> query = builder.createQuery(TrackedPlayerRecord.class);
        Root<TrackedPlayerRecord> root = query.from(TrackedPlayerRecord.class);
        CriteriaQuery<TrackedPlayerRecord> finalquery = query.where(builder.equal(root.get("playerUUID"), uuid));

        List<TrackedPlayerRecord> values = en.createQuery(finalquery).getResultList();
        LOG.debug("Loaded for " + uuid + ": " + values);
        return singleResultOrEmpty(values, uuid);
    }

    public Optional<UntrackedPlayerRecord> getData(UUID uuid) {
        Optional<TrackedPlayerRecord> opt = getDataInternal(uuid);
        if (opt.isPresent()) {
            return Optional.of(new UntrackedPlayerRecord(opt.get()));
        } else {
            return Optional.empty();
        }
    }

    private Optional<TrackedPlayerRecord> getDataInternal(UUID uuid) {
        return nameCache.getUnchecked(uuid);
    }

    public void flush() {
        nameCache.invalidateAll();
    }

    public void flush(UUID uuid) {
        nameCache.invalidate(uuid);
    }

    public Optional<UntrackedPlayerRecord> getNameOptional(UUID playeruuid) {
        //Avoid lingering instances.
        return nameCache.getUnchecked(playeruuid).map(UntrackedPlayerRecord::new);
    }

    public void setPlayerNameByUUID(UUID playerUUID, String newname) {
        if (SigynPlugin.getInstance().getServer().getPlayer(playerUUID) != null) {
            SigynPlugin.getInstance().getServer().getPlayer(playerUUID).kickPlayer(ChatColor.BLUE + "Rename forced. Please come back in a few seconds.");
        }

        nameCache.getUnchecked(playerUUID).ifPresentOrElse(ppr -> {
            ppr.setMappedName(newname);
            HDBMPlugin.withEntityManager(en -> {
                Transaction transaction = en.beginTransaction();
                en.saveOrUpdate(ppr);
                transaction.commit();
            });
        }, () -> {
            throw new IllegalStateException("Tried to rename " + playerUUID + " " + newname + " without a trace of that player.");
        });
    }

    private static <T> Optional<T> singleResultOrEmpty(List<T> values, Object errorHint) {
        if (values.size() > 1) {
            LOG.error("Database returned more than one record for \"" + errorHint + "\", this shouldn't be possible. " + values);
            throw new IllegalStateException("Database query returned more than one record for \"" + errorHint + "\".");
        } else if (values.size() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(values.get(0));
        }
    }

    public void addPlayer(UUID playerUUID, String newname) {
        HDBMPlugin.withEntityManager(en -> {
            Transaction transaction = en.beginTransaction();
            TrackedPlayerRecord rec = new TrackedPlayerRecord(playerUUID, newname);
            en.save(rec);
            transaction.commit();
            nameCache.put(playerUUID, Optional.of(rec));
        });
    }

    public UntrackedPlayerRecord getOrCreateData(String name, UUID uuid) {
        Optional<TrackedPlayerRecord> opt = getDataInternal(uuid);
        if (opt.isPresent()) {
            LOG.debug("Loaded cache entry for \"" + name + "\"(" + uuid + ")");
            return new UntrackedPlayerRecord(opt.get());
        } else {
            LOG.debug("Creating cache entry for \"" + name + "\"(" + uuid + ")");
            TrackedPlayerRecord newEntry = new TrackedPlayerRecord(uuid, name);
            HDBMPlugin.withEntityManager(en -> {
                Transaction transaction = en.beginTransaction();
                en.save(newEntry);
                transaction.commit();
            });

            return new UntrackedPlayerRecord(newEntry);
        }
    }

    public Optional<UntrackedPlayerRecord> getPlayerByName(String name) {
        if (mappedResolvingMap.containsKey(name)) {
            LOG.debug("Resolving \"" + name + "\" directly from cache.");
            Optional<TrackedPlayerRecord> opt = nameCache.getUnchecked(mappedResolvingMap.get(name));
            return Optional.of(new UntrackedPlayerRecord(opt.get()));
        } else {
            LOG.debug("Querying database for \"" + name + "\".");

            return HDBMPlugin.withEntityManager(en -> {
                CriteriaBuilder builder = en.getCriteriaBuilder();
                CriteriaQuery<TrackedPlayerRecord> query = builder.createQuery(TrackedPlayerRecord.class);
                Root<TrackedPlayerRecord> root = query.from(TrackedPlayerRecord.class);
                CriteriaQuery<TrackedPlayerRecord> finalquery = query.where(builder.equal(root.get("mappedName"), name));

                Optional<TrackedPlayerRecord> opt = singleResultOrEmpty(en.createQuery(finalquery).getResultList(), "mappedName");
                if (opt.isPresent()) {
                    return Optional.of(new UntrackedPlayerRecord(opt.get()));
                } else {
                    return Optional.empty();
                }
            });
        }
    }
}
