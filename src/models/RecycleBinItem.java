// File: src/models/RecycleBinItem.java
package models;

import java.sql.Timestamp;

public class RecycleBinItem {
    private int originalEntityId; // ID asli dari tabel books atau members
    private String entityType;    // "Book" atau "Member"
    private String displayName;   // Judul buku atau nama anggota
    private String identifier;    // ISBN untuk buku, Member ID untuk anggota
    private Timestamp deletedAt;
    private String itemData;      // Data lengkap item yang disimpan di recycle_bin_logs.item_data

    // Konstruktor bisa disesuaikan berdasarkan kebutuhan
    public RecycleBinItem(int originalEntityId, String entityType, String displayName, String identifier, Timestamp deletedAt, String itemData) {
        this.originalEntityId = originalEntityId;
        this.entityType = entityType;
        this.displayName = displayName;
        this.identifier = identifier;
        this.deletedAt = deletedAt;
        this.itemData = itemData;
    }

    // Getters
    public int getOriginalEntityId() {
        return originalEntityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Timestamp getDeletedAt() {
        return deletedAt;
    }

    public String getItemData() {
        return itemData;
    }

    // Mungkin perlu setter jika ada pengeditan langsung di level ini (jarang untuk item recycle bin)
    // public void setOriginalEntityId(int originalEntityId) { this.originalEntityId = originalEntityId; }
    // ... dan seterusnya
}