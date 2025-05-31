package models;

import java.sql.Timestamp;

public class ActivityLogItem {
    private String activityType;    // Mis: "Peminjaman Buku", "Item Dihapus"
    private String details;         // Mis: "Buku: 'Java Programming' oleh Member: 'Andi'", "Buku 'ID: 10' dihapus"
    private Timestamp activityDate;
    private String actor;           // Mis: "Admin: admin", "Sistem" (atau nama anggota jika relevan)
    private String status;          // Mis: "Selesai", "Dipinjam"

    public ActivityLogItem(String activityType, String details, Timestamp activityDate, String actor, String status) {
        this.activityType = activityType;
        this.details = details;
        this.activityDate = activityDate;
        this.actor = actor;
        this.status = status;
    }

    // Getters
    public String getActivityType() { return activityType; }
    public String getDetails() { return details; }
    public Timestamp getActivityDate() { return activityDate; }
    public String getActor() { return actor; }
    public String getStatus() { return status; }
}
