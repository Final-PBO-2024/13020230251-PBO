/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models;

/**
 *
 * @author andi.ikhlass
 */
import java.sql.Timestamp;

public class Member {
    private int id;                 // Primary key dari database
    private String memberIdText;    // ID Anggota yang unik (misal: MEM001)
    private String name;
    private String contact;         // Bisa email atau telepon
    private String address;
    private String membershipType;  // Misal: Standard, Premium
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;     // Untuk soft delete

    // Konstruktor default
    public Member() {
    }

    // Konstruktor untuk membuat anggota baru (tanpa DB-generated fields)
    public Member(String memberIdText, String name, String contact, String address, String membershipType) {
        this.memberIdText = memberIdText;
        this.name = name;
        this.contact = contact;
        this.address = address;
        this.membershipType = membershipType;
    }

    // Konstruktor lengkap (misalnya saat mengambil dari database)
    public Member(int id, String memberIdText, String name, String contact, String address, String membershipType, Timestamp createdAt, Timestamp updatedAt, Timestamp deletedAt) {
        this.id = id;
        this.memberIdText = memberIdText;
        this.name = name;
        this.contact = contact;
        this.address = address;
        this.membershipType = membershipType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMemberIdText() { return memberIdText; }
    public void setMemberIdText(String memberIdText) { this.memberIdText = memberIdText; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMembershipType() { return membershipType; }
    public void setMembershipType(String membershipType) { this.membershipType = membershipType; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public Timestamp getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Timestamp deletedAt) { this.deletedAt = deletedAt; }

    @Override
    public String toString() {
        // Berguna untuk debugging atau jika ingin menampilkan Member di JComboBox (meski jarang)
        return name + " (" + memberIdText + ")";
    }
}
