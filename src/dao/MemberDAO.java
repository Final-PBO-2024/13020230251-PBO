// File: src/dao/MemberDAO.java (atau src/com/perpustakaanku/dao/MemberDAO.java)
package dao;

import config.DatabaseConnection;
import models.Member;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane; // Untuk notifikasi error constraint

public class MemberDAO {

    // ... (metode addMember, getAllMembers, getMemberByInternalId, getMemberByMemberIdText, updateMember, memberIdTextExists tetap sama seperti sebelumnya) ...
    public boolean addMember(Member member) {
        String sql = "INSERT INTO members (member_id, name, contact, address, membership_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (conn == null) return false;
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
            sqlBuilder.append("AND (name LIKE ? OR member_id LIKE ? OR contact LIKE ?) ");
            String likeTerm = "%" + searchTerm.toLowerCase() + "%";
            params.add(likeTerm); params.add(likeTerm); params.add(likeTerm);
        }
        sqlBuilder.append("ORDER BY name ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            if (conn == null) return members;
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
        } catch (SQLException e) {
            System.err.println("Error saat mengambil semua anggota: " + e.getMessage());
        }
        return members;
    }

    public Member getMemberByInternalId(int internalId) {
        String sql = "SELECT id, member_id, name, contact, address, membership_type, created_at, updated_at, deleted_at " +
                     "FROM members WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return null;
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
        } catch (SQLException e) {
            System.err.println("Error saat mengambil anggota by internal ID: " + e.getMessage());
        }
        return null;
    }
    
    public Member getMemberByMemberIdText(String memberIdText) {
        String sql = "SELECT id, member_id, name, contact, address, membership_type, created_at, updated_at, deleted_at " +
                     "FROM members WHERE member_id = ? AND deleted_at IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return null;
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
        } catch (SQLException e) {
            System.err.println("Error saat mengambil anggota by Member ID Text: " + e.getMessage());
        }
        return null;
    }

    public boolean updateMember(Member member) {
        String sql = "UPDATE members SET name = ?, contact = ?, address = ?, membership_type = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ? AND member_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return false;
            pstmt.setString(1, member.getName());
            pstmt.setString(2, member.getContact());
            pstmt.setString(3, member.getAddress());
            pstmt.setString(4, member.getMembershipType());
            pstmt.setInt(5, member.getId());
            pstmt.setString(6, member.getMemberIdText());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saat memperbarui anggota: " + e.getMessage());
        }
        return false;
    }

    public boolean memberIdTextExists(String memberIdText) {
        String sql = "SELECT COUNT(*) FROM members WHERE member_id = ? AND deleted_at IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return true;
            pstmt.setString(1, memberIdText);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error saat cek Member ID Text: " + e.getMessage());
            return true;
        }
        return false;
    }
    
    // --- Metode untuk Recycle Bin ---

    public boolean softDeleteMember(int internalId, String adminUsername) {
        Member memberToDelete = getMemberByInternalId(internalId);
        if (memberToDelete == null || memberToDelete.getDeletedAt() != null) {
            System.err.println("Anggota tidak ditemukan atau sudah di soft delete.");
            return false;
        }

        String sql = "UPDATE members SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return false;
            pstmt.setInt(1, internalId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                String itemData = String.format("MemberID: %s, Name: %s, Contact: %s, Address: %s, Type: %s",
                        memberToDelete.getMemberIdText(), memberToDelete.getName(), memberToDelete.getContact(),
                        memberToDelete.getAddress(), memberToDelete.getMembershipType());
                logToRecycleBin(internalId, "Member", "deleted", adminUsername, itemData);
                return true;
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
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return false;
            pstmt.setInt(1, internalId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                 String itemData = String.format("MemberID: %s, Name: %s",
                        memberToRestore.getMemberIdText(), memberToRestore.getName());
                logToRecycleBin(internalId, "Member", "restored", adminUsername, itemData);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saat restore anggota: " + e.getMessage());
        }
        return false;
    }

    public boolean permanentlyDeleteMember(int internalId, String adminUsername) {
        Member memberToDelete = getMemberByInternalId(internalId);

        String sql = "DELETE FROM members WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return false;
            pstmt.setInt(1, internalId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                String itemData = "Permanently deleted member.";
                if(memberToDelete != null){ // Jika data berhasil diambil sebelum delete
                    itemData = String.format("Permanently deleted Member - MemberID: %s, Name: %s",
                        memberToDelete.getMemberIdText(), memberToDelete.getName());
                }
                logToRecycleBin(internalId, "Member", "permanently_deleted", adminUsername, itemData);
                return true;
            } else {
                System.err.println("Gagal menghapus permanen anggota dengan ID: " + internalId + ". Mungkin sudah terhapus.");
            }
        } catch (SQLException e) {
            // Perhatikan FOREIGN KEY constraint di tabel transactions (ON DELETE RESTRICT)
            System.err.println("Error saat permanently delete anggota: " + e.getMessage());
            if (e.getMessage().toLowerCase().contains("foreign key constraint fails")) {
                JOptionPane.showMessageDialog(null, "Anggota tidak bisa dihapus permanen karena masih terkait dengan data transaksi aktif.", "Error Hapus Permanen", JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    public List<Member> getSoftDeletedMembers() {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT id, member_id, name, contact, address, membership_type, created_at, updated_at, deleted_at " +
                     "FROM members WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (conn == null) return members;
            while (rs.next()) {
                members.add(new Member(
                        rs.getInt("id"),
                        rs.getString("member_id"),
                        rs.getString("name"),
                        rs.getString("contact"),
                        rs.getString("address"),
                        rs.getString("membership_type"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at"),
                        rs.getTimestamp("deleted_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error saat mengambil soft deleted members: " + e.getMessage());
        }
        return members;
    }

    // Metode logToRecycleBin (duplikat dari BookDAO, idealnya di refactor ke kelas utilitas)
    public void logToRecycleBin(int entityId, String entityType, String actionType, String adminUsername, String itemDataDetails) {
        String sqlLog = "INSERT INTO recycle_bin_logs (entity_id, entity_type, item_data, action_type, action_by) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmtLog = conn.prepareStatement(sqlLog)) {
            if (conn == null) return;
            pstmtLog.setInt(1, entityId);
            pstmtLog.setString(2, entityType);
            pstmtLog.setString(3, itemDataDetails);
            pstmtLog.setString(4, actionType);
            pstmtLog.setString(5, adminUsername);
            pstmtLog.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error logging to recycle_bin_logs for " + entityType + ": " + e.getMessage());
        }
    }
}