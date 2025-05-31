// File: src/dao/MemberDAO.java
package dao; // Pastikan nama paket ini sesuai

import config.DatabaseConnection; // Pastikan import ini sesuai
import models.Member;             // Pastikan import ini sesuai

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
// import javax.swing.JOptionPane; // Dihapus karena DAO tidak boleh akses UI

public class MemberDAO {

    public boolean addMember(Member member) {
        String sql = "INSERT INTO members (member_id, name, contact, address, membership_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                System.err.println("MemberDAO: Koneksi DB null untuk addMember.");
                return false;
            }
            try(PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, member.getMemberIdText());
                pstmt.setString(2, member.getName());
                pstmt.setString(3, member.getContact());
                pstmt.setString(4, member.getAddress());
                pstmt.setString(5, member.getMembershipType());
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) member.setId(generatedKeys.getInt(1));
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat menambah anggota: " + e.getMessage());
        }
        return false;
    }

    public List<Member> getAllMembers(String searchTerm) {
        List<Member> members = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT id, member_id, name, contact, address, membership_type, created_at, updated_at, deleted_at " +
            "FROM members WHERE deleted_at IS NULL "
        );
        List<Object> params = new ArrayList<>();
        if (searchTerm != null && !searchTerm.isEmpty()) {
            sqlBuilder.append("AND (LOWER(name) LIKE LOWER(?) OR LOWER(member_id) LIKE LOWER(?) OR LOWER(contact) LIKE LOWER(?)) ");
            String likeTerm = "%" + searchTerm + "%"; // Jika DB collation sudah CI, toLowerCase tidak wajib
            params.add(likeTerm); params.add(likeTerm); params.add(likeTerm);
        }
        sqlBuilder.append("ORDER BY name ASC");

        try (Connection conn = DatabaseConnection.getConnection()){
            if (conn == null) {
                System.err.println("MemberDAO: Koneksi DB null untuk getAllMembers.");
                return members;
            }
            try(PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        members.add(new Member(
                                rs.getInt("id"), rs.getString("member_id"), rs.getString("name"),
                                rs.getString("contact"), rs.getString("address"), rs.getString("membership_type"),
                                rs.getTimestamp("created_at"), rs.getTimestamp("updated_at"), rs.getTimestamp("deleted_at")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat mengambil semua anggota: " + e.getMessage());
        }
        return members;
    }

    public Member getMemberByInternalId(int internalId) {
        String sql = "SELECT id, member_id, name, contact, address, membership_type, created_at, updated_at, deleted_at " +
                     "FROM members WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                System.err.println("MemberDAO: Koneksi DB null untuk getMemberByInternalId.");
                return null;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, internalId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new Member(
                                rs.getInt("id"), rs.getString("member_id"), rs.getString("name"),
                                rs.getString("contact"), rs.getString("address"), rs.getString("membership_type"),
                                rs.getTimestamp("created_at"), rs.getTimestamp("updated_at"), rs.getTimestamp("deleted_at")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat mengambil anggota by internal ID: " + e.getMessage());
        }
        return null;
    }
    
    public Member getMemberByMemberIdText(String memberIdText) {
        String sql = "SELECT id, member_id, name, contact, address, membership_type, created_at, updated_at, deleted_at " +
                     "FROM members WHERE member_id = ? AND deleted_at IS NULL";
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                System.err.println("MemberDAO: Koneksi DB null untuk getMemberByMemberIdText.");
                return null;
            }
            try(PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, memberIdText);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new Member(
                                rs.getInt("id"), rs.getString("member_id"), rs.getString("name"),
                                rs.getString("contact"), rs.getString("address"), rs.getString("membership_type"),
                                rs.getTimestamp("created_at"), rs.getTimestamp("updated_at"), rs.getTimestamp("deleted_at")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat mengambil anggota by Member ID Text: " + e.getMessage());
        }
        return null;
    }

    public boolean updateMember(Member member) {
        String sql = "UPDATE members SET name = ?, contact = ?, address = ?, membership_type = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ? AND member_id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return false;
            try(PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, member.getName());
                pstmt.setString(2, member.getContact());
                pstmt.setString(3, member.getAddress());
                pstmt.setString(4, member.getMembershipType());
                pstmt.setInt(5, member.getId());
                pstmt.setString(6, member.getMemberIdText());
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error saat memperbarui anggota: " + e.getMessage());
        }
        return false;
    }

    public boolean memberIdTextExists(String memberIdText) {
        String sql = "SELECT COUNT(*) FROM members WHERE member_id = ? AND deleted_at IS NULL";
        try (Connection conn = DatabaseConnection.getConnection()){
            if (conn == null) {
                System.err.println("MemberDAO: Koneksi DB null untuk memberIdTextExists.");
                return true; // Anggap ada jika koneksi gagal (safety)
            }
            try(PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, memberIdText);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat cek Member ID Text: " + e.getMessage());
            return true;
        }
        return false;
    }
    
    public boolean softDeleteMember(int internalId, String adminUsername) {
        Member memberToDelete = getMemberByInternalId(internalId);
        if (memberToDelete == null || memberToDelete.getDeletedAt() != null) {
            System.err.println("Anggota tidak ditemukan atau sudah di soft delete.");
            return false;
        }
        String sql = "UPDATE members SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL";
        try (Connection conn = DatabaseConnection.getConnection()){
            if (conn == null) return false;
            try(PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, internalId);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    String itemData = String.format("MemberID: %s, Name: %s, Contact: %s, Address: %s, Type: %s",
                            memberToDelete.getMemberIdText(), memberToDelete.getName(), memberToDelete.getContact(),
                            memberToDelete.getAddress(), memberToDelete.getMembershipType());
                    logToRecycleBin(internalId, "Member", "deleted", adminUsername, itemData);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat soft delete anggota: " + e.getMessage());
        }
        return false;
    }

    public boolean restoreMember(int internalId, String adminUsername) {
        Member memberToRestore = getMemberByInternalId(internalId);
        if (memberToRestore == null || memberToRestore.getDeletedAt() == null) {
             System.err.println("Anggota tidak ditemukan atau tidak sedang di soft delete.");
            return false;
        }
        String sql = "UPDATE members SET deleted_at = NULL WHERE id = ? AND deleted_at IS NOT NULL";
        try (Connection conn = DatabaseConnection.getConnection()){
            if (conn == null) return false;
            try(PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, internalId);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                     String itemData = String.format("MemberID: %s, Name: %s",
                            memberToRestore.getMemberIdText(), memberToRestore.getName());
                    logToRecycleBin(internalId, "Member", "restored", adminUsername, itemData);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat restore anggota: " + e.getMessage());
        }
        return false;
    }

    public boolean permanentlyDeleteMember(int internalId, String adminUsername) {
        Member memberToDelete = getMemberByInternalId(internalId);
        String sql = "DELETE FROM members WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection()){
            if (conn == null) return false;
            try(PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, internalId);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    String itemData = "Permanently deleted member.";
                    if(memberToDelete != null){
                        itemData = String.format("Permanently deleted Member - MemberID: %s, Name: %s",
                            memberToDelete.getMemberIdText(), memberToDelete.getName());
                    }
                    logToRecycleBin(internalId, "Member", "permanently_deleted", adminUsername, itemData);
                    return true;
                } else {
                    System.err.println("Gagal menghapus permanen anggota dengan ID: " + internalId + ". Mungkin sudah terhapus.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat permanently delete anggota: " + e.getMessage());
            // if (e.getMessage().toLowerCase().contains("foreign key constraint fails")) {
                // Notifikasi bisa ditangani di UI jika method ini return false
            // }
        }
        return false;
    }

    public List<Member> getSoftDeletedMembers() {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT id, member_id, name, contact, address, membership_type, created_at, updated_at, deleted_at " +
                     "FROM members WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC";
        try (Connection conn = DatabaseConnection.getConnection()){
            if (conn == null) return members;
            try(Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    members.add(new Member(
                            rs.getInt("id"), rs.getString("member_id"), rs.getString("name"),
                            rs.getString("contact"), rs.getString("address"), rs.getString("membership_type"),
                            rs.getTimestamp("created_at"), rs.getTimestamp("updated_at"), rs.getTimestamp("deleted_at")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat mengambil soft deleted members: " + e.getMessage());
        }
        return members;
    }

    public void logToRecycleBin(int entityId, String entityType, String actionType, String adminUsername, String itemDataDetails) {
        String sqlLog = "INSERT INTO recycle_bin_logs (entity_id, entity_type, item_data, action_type, action_by) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()){
            if (conn == null) return;
            try(PreparedStatement pstmtLog = conn.prepareStatement(sqlLog)) {
                pstmtLog.setInt(1, entityId);
                pstmtLog.setString(2, entityType);
                pstmtLog.setString(3, itemDataDetails);
                pstmtLog.setString(4, actionType);
                pstmtLog.setString(5, adminUsername);
                pstmtLog.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error logging to recycle_bin_logs for " + entityType + ": " + e.getMessage());
        }
    }
    
    // === METODE STATISTIK DASHBOARD (dengan penanganan koneksi null lebih baik) ===
    public int getTotalActiveMembersCount() {
        String sql = "SELECT COUNT(*) FROM members WHERE deleted_at IS NULL";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                System.err.println("MemberDAO: Koneksi DB null untuk getTotalActiveMembersCount.");
                return 0;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat menghitung total anggota aktif: " + e.getMessage());
        }
        return 0;
    }

    public int getTotalSoftDeletedMembersCount() {
        String sql = "SELECT COUNT(*) FROM members WHERE deleted_at IS NOT NULL";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                System.err.println("MemberDAO: Koneksi DB null untuk getTotalSoftDeletedMembersCount.");
                return 0;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat menghitung total anggota terhapus (soft): " + e.getMessage());
        }
        return 0;
    }
}
