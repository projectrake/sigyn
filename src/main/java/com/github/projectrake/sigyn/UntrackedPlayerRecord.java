package com.github.projectrake.sigyn;

import java.util.UUID;

/**
 * Created on 09/01/2018.
 *
 * Provides player data while preventing interactions with the backing data / database.
 */
public class UntrackedPlayerRecord {
    private final UUID playerUUID;
    private final String originalName;
    private final String mappedName;

    public UntrackedPlayerRecord(UUID playerUUID, String originalName, String mappedName) {
        this.playerUUID = playerUUID;
        this.originalName = originalName;
        this.mappedName = mappedName;
    }

    public UntrackedPlayerRecord(UUID playerUUID, String name) {
        this.playerUUID = playerUUID;
        this.originalName = name;
        this.mappedName = name;
    }

    UntrackedPlayerRecord(UntrackedPlayerRecord up) {
        this(up.playerUUID, up.originalName, up.mappedName);
    }

    public UntrackedPlayerRecord(TrackedPlayerRecord ud) {
        this(ud.getPlayerUUID(), ud.getOriginalName(), ud.getMappedName());
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getMappedName() {
        return mappedName;
    }

    public boolean isUnchanged() {
        return mappedName.equals(originalName);
    }

    @Override
    public String toString() {
        return "PlayerNameUUID{" +
                "playerUUID=" + playerUUID +
                ", originalName='" + originalName + '\'' +
                ", mappedName='" + mappedName + '\'' +
                '}';
    }
}
