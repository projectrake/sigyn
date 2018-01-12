package com.github.projectrake.sigyn;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

/**
 * Created on 09/01/2018.
 */
@Entity
@Table(name = "pin_player_mapping")
public class TrackedPlayerRecord {
    @Id
    @Column(columnDefinition = "BINARY(16)", nullable = false, unique = true, updatable = false)
    private UUID playerUUID;
    @Column(nullable = false, length = 16)
    private String originalName;
    @Column(nullable = false, length = 16, unique = true)
    private String mappedName;

    TrackedPlayerRecord() {
    }

    public TrackedPlayerRecord(UUID playerUUID, String originalName, String mappedName) {
        this.playerUUID = playerUUID;
        this.originalName = originalName;
        this.mappedName = mappedName;
    }

    public TrackedPlayerRecord(UUID playerUUID, String name) {
        this.playerUUID = playerUUID;
        this.originalName = name;
        this.mappedName = name;
    }


    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getMappedName() {
        return mappedName;
    }

    public void setMappedName(String mappedName) {
        this.mappedName = mappedName;
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
