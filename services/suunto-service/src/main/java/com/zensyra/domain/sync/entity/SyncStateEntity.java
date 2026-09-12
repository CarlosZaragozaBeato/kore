package com.zensyra.domain.sync.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sync_state")
public class SyncStateEntity extends PanacheEntityBase {

    @Id
    @Column(name = "source", nullable = false)
    public String source;

    @Column(name = "last_synced_at", nullable = false)
    public Long lastSyncedAt;

    public static SyncStateEntity findBySource(String source) {
        return findById(source);
    }

    public static SyncStateEntity getOrCreate(String source) {
        SyncStateEntity state = findBySource(source);

        if (state != null) {
            return state;
        }

        state = new SyncStateEntity();
        state.source = source;
        state.lastSyncedAt = 0L;
        state.persist();

        return state;
    }
}