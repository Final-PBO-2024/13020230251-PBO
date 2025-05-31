package dao;

import config.DatabaseConnection;
import models.Transaction;
import models.ActivityLogItem; // Import model baru

import java.sql.*;
import java.math.BigDecimal;
// import java.text.SimpleDateFormat; // Tidak diperlukan di sini jika format di UI
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
// import java.util.Vector; // Tidak digunakan lagi untuk getRecentActivities

public class TransactionDAO {

    // ... (Metode borrowBook, returnBook, getAllTransactionHistory, isBookAvailable, 
    //      isMemberActive, isBookAlreadyBorrowedByMember, getCurrentlyBorrowedBooksCount
    //      TETAP SAMA seperti versi lengkap sebelumnya) ...

    public boolean borrowBook(int memberId, int bookId, Date borrowDate, Date returnDueDate) {
        String insertTransactionSQL = "INSERT INTO transactions (member_id, book_id, borrow_date, return_due_date, status) VALUES (?, ?, ?, ?, ?)";
        String updateBookStockSQL = "UPDATE books SET stock = stock - 1 WHERE id = ? AND stock > 0";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try (PreparedStatement pstmtUpdateStock = conn.prepareStatement(updateBookStockSQL)) {
                pstmtUpdateStock.setInt(1, bookId);
                if (pstmtUpdateStock.executeUpdate() == 0) {
                    conn.rollback(); return false; 
                }
            }
            try (PreparedStatement pstmtInsertTransaction = conn.prepareStatement(insertTransactionSQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmtInsertTransaction.setInt(1, memberId);
                pstmtInsertTransaction.setInt(2, bookId);
                pstmtInsertTransaction.setDate(3, borrowDate);
                pstmtInsertTransaction.setDate(4, returnDueDate);
                pstmtInsertTransaction.setString(5, "borrowed");
                if (pstmtInsertTransaction.executeUpdate() == 0) {
                    conn.rollback(); return false;
                }
            }
            conn.commit(); return true;
        } catch (SQLException e) {
            System.err.println("Error SQL saat proses peminjaman buku: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("Error rollback: " + ex.getMessage());}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) { System.err.println("Error setAutoCommit: " + ex.getMessage());}
        }
    }

    public boolean returnBook(int transactionId, Date actualReturnDate, BigDecimal finePerDay) {
        String getTransactionSQL = "SELECT book_id, return_due_date FROM transactions WHERE id = ? AND (status = 'borrowed' OR status = 'overdue')";
        String updateTransactionSQL = "UPDATE transactions SET return_date = ?, status = ?, fine_amount = ? WHERE id = ?";
        String updateBookStockSQL = "UPDATE books SET stock = stock + 1 WHERE id = ?";
        Connection conn = null; int bookId = -1; java.sql.Date dueDate = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try (PreparedStatement pstmtGetTx = conn.prepareStatement(getTransactionSQL)) {
                pstmtGetTx.setInt(1, transactionId);
                try (ResultSet rs = pstmtGetTx.executeQuery()) {
                    if (rs.next()) { bookId = rs.getInt("book_id"); dueDate = rs.getDate("return_due_date"); } 
                    else { conn.rollback(); System.err.println("Transaksi tidak ditemukan atau status tidak valid untuk pengembalian."); return false; }
                }
            }
            if (bookId == -1 || dueDate == null) { conn.rollback(); return false; }
            BigDecimal calculatedFine = BigDecimal.ZERO; String newStatus = "returned";
            if (actualReturnDate.after(dueDate)) {
                LocalDate localDueDate = dueDate.toLocalDate();
                LocalDate localActualReturnDate = actualReturnDate.toLocalDate();
                long daysOverdue = ChronoUnit.DAYS.between(localDueDate, localActualReturnDate);
                if (daysOverdue > 0) calculatedFine = finePerDay.multiply(new BigDecimal(daysOverdue));
            }
            try (PreparedStatement pstmtUpdateTx = conn.prepareStatement(updateTransactionSQL)) {
                pstmtUpdateTx.setDate(1, actualReturnDate);
                pstmtUpdateTx.setString(2, newStatus);
                pstmtUpdateTx.setBigDecimal(3, calculatedFine);
                pstmtUpdateTx.setInt(4, transactionId);
                if (pstmtUpdateTx.executeUpdate() == 0) { conn.rollback(); System.err.println("Gagal mengupdate transaksi."); return false; }
            }
            try (PreparedStatement pstmtUpdateStock = conn.prepareStatement(updateBookStockSQL)) {
                pstmtUpdateStock.setInt(1, bookId);
                pstmtUpdateStock.executeUpdate(); 
            }
            conn.commit(); return true;
        } catch (SQLException e) {
            System.err.println("Error SQL saat proses pengembalian buku: " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { System.err.println("Error rollback: " + ex.getMessage());}
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); } catch (SQLException ex) { System.err.println("Error setAutoCommit: " + ex.getMessage());}
        }
    }

    public List<Transaction> getAllTransactionHistory(String searchTerm) {
        List<Transaction> transactions = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT t.id, t.member_id, m.name as member_name, m.member_id as member_id_text, " +
            "t.book_id, b.title as book_title, b.isbn as book_isbn, " +
            "t.borrow_date, t.return_due_date, t.return_date, t.fine_amount, t.status, " +
            "t.created_at, t.updated_at " +
            "FROM transactions t " +
            "LEFT JOIN members m ON t.member_id = m.id " +
            "LEFT JOIN books b ON t.book_id = b.id "
        );
        List<Object> params = new ArrayList<>();
        if (searchTerm != null && !searchTerm.isEmpty()) {
            sqlBuilder.append("WHERE (LOWER(m.name) LIKE LOWER(?) OR LOWER(m.member_id) LIKE LOWER(?) OR LOWER(b.title) LIKE LOWER(?) OR LOWER(b.isbn) LIKE LOWER(?) OR LOWER(t.status) LIKE LOWER(?)) ");
            String likeTerm = "%" + searchTerm + "%";
            for (int i = 0; i < 5; i++) params.add(likeTerm);
        }
        sqlBuilder.append("ORDER BY t.created_at DESC, t.id DESC"); 

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            if (conn == null) return transactions;
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Transaction tx = new Transaction(
                            rs.getInt("id"), rs.getInt("member_id"), rs.getInt("book_id"),
                            rs.getDate("borrow_date"), rs.getDate("return_due_date"),
                            rs.getDate("return_date"), rs.getBigDecimal("fine_amount"),
                            rs.getString("status"), rs.getTimestamp("created_at"), rs.getTimestamp("updated_at")
                    );
                    tx.setMemberName(rs.getString("member_name"));
                    tx.setMemberIdText(rs.getString("member_id_text"));
                    tx.setBookTitle(rs.getString("book_title"));
                    tx.setBookIsbn(rs.getString("book_isbn"));
                    transactions.add(tx);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat mengambil riwayat transaksi: " + e.getMessage());
        }
        return transactions;
    }
    
    public boolean isBookAvailable(int bookId) {
        String sql = "SELECT stock FROM books WHERE id = ? AND deleted_at IS NULL";
        try(Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if(conn == null) return false; pstmt.setInt(1, bookId);
            try(ResultSet rs = pstmt.executeQuery()){ if(rs.next()) return rs.getInt("stock") > 0; }
        } catch (SQLException e){ System.err.println("Error cek ketersediaan buku: " + e.getMessage()); }
        return false;
    }

    public boolean isMemberActive(int memberId) {
        String sql = "SELECT COUNT(*) FROM members WHERE id = ? AND deleted_at IS NULL";
        try(Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if(conn == null) return false; pstmt.setInt(1, memberId);
            try(ResultSet rs = pstmt.executeQuery()){ if(rs.next()) return rs.getInt(1) > 0; }
        } catch (SQLException e){ System.err.println("Error cek status member: " + e.getMessage()); }
        return false;
    }
    
    public boolean isBookAlreadyBorrowedByMember(int memberId, int bookId) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE member_id = ? AND book_id = ? AND status = 'borrowed'";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return true; 
            pstmt.setInt(1, memberId); pstmt.setInt(2, bookId);
            try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) return rs.getInt(1) > 0; }
        } catch (SQLException e) { System.err.println("Error cek buku sudah dipinjam: " + e.getMessage()); return true; }
        return false;
    }

    public int getCurrentlyBorrowedBooksCount() {
        String sql = "SELECT COUNT(*) FROM transactions WHERE status = 'borrowed'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (conn == null) return 0;
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error saat menghitung jumlah buku dipinjam: " + e.getMessage());
        }
        return 0;
    }

    // === METODE getRecentActivities DIMODIFIKASI MENGEMBALIKAN List<ActivityLogItem> ===
    public List<ActivityLogItem> getRecentActivities(int limit) {
        List<ActivityLogItem> activities = new ArrayList<>();
        
        // Mengambil data dari recycle_bin_logs
        String recycleSql = "SELECT entity_type, item_data, action_type, action_by, action_at FROM recycle_bin_logs ORDER BY action_at DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmtRecycle = conn.prepareStatement(recycleSql)) {
            if (conn != null) {
                pstmtRecycle.setInt(1, limit); // Limit awal, akan di-sort lagi nanti
                try (ResultSet rsRecycle = pstmtRecycle.executeQuery()) {
                    while (rsRecycle.next()) {
                        String activityTypeStr = "";
                        String action = rsRecycle.getString("action_type");
                        String entityType = rsRecycle.getString("entity_type");
                        String details = rsRecycle.getString("item_data");
                        Timestamp activityDate = rsRecycle.getTimestamp("action_at");
                        String actor = "Admin: " + rsRecycle.getString("action_by");
                        String status = "Selesai";

                        if ("Book".equalsIgnoreCase(entityType)) {
                            if ("deleted".equalsIgnoreCase(action)) activityTypeStr = "Buku ke Recycle Bin";
                            else if ("restored".equalsIgnoreCase(action)) activityTypeStr = "Buku Di-restore";
                            else if ("permanently_deleted".equalsIgnoreCase(action)) activityTypeStr = "Buku Dihapus Permanen";
                        } else if ("Member".equalsIgnoreCase(entityType)) {
                            if ("deleted".equalsIgnoreCase(action)) activityTypeStr = "Anggota ke Recycle Bin";
                            else if ("restored".equalsIgnoreCase(action)) activityTypeStr = "Anggota Di-restore";
                            else if ("permanently_deleted".equalsIgnoreCase(action)) activityTypeStr = "Anggota Dihapus Permanen";
                        }
                        
                        if(!activityTypeStr.isEmpty()){
                            activities.add(new ActivityLogItem(activityTypeStr, details, activityDate, actor, status));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error mengambil aktivitas dari recycle_bin_logs: " + e.getMessage());
        }

        // Mengambil data dari transactions
        String transactionSql = "SELECT m.name as member_name, b.title as book_title, t.status, t.borrow_date, t.return_date, t.updated_at " +
                                "FROM transactions t " +
                                "LEFT JOIN members m ON t.member_id = m.id " +
                                "LEFT JOIN books b ON t.book_id = b.id " +
                                "ORDER BY t.updated_at DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmtTransaction = conn.prepareStatement(transactionSql)) {
            if (conn != null) {
                pstmtTransaction.setInt(1, limit); // Limit awal
                try (ResultSet rsTransaction = pstmtTransaction.executeQuery()) {
                    while (rsTransaction.next()) {
                        String activityTypeStr = "";
                        String details = "";
                        Timestamp activityDate = rsTransaction.getTimestamp("updated_at"); // Default
                        String actor = "Anggota: " + (rsTransaction.getString("member_name") != null ? rsTransaction.getString("member_name") : "N/A");
                        String status = rsTransaction.getString("status");

                        if ("borrowed".equalsIgnoreCase(status)) {
                            activityTypeStr = "Peminjaman Buku";
                            details = "Buku: '" + (rsTransaction.getString("book_title") != null ? rsTransaction.getString("book_title") : "N/A") + "'";
                            if(rsTransaction.getTimestamp("borrow_date") != null) activityDate = rsTransaction.getTimestamp("borrow_date");
                        } else if ("returned".equalsIgnoreCase(status) || "overdue".equalsIgnoreCase(status)) {
                            activityTypeStr = "Pengembalian Buku";
                            details = "Buku: '" + (rsTransaction.getString("book_title") != null ? rsTransaction.getString("book_title") : "N/A") + "'";
                            if(rsTransaction.getTimestamp("return_date") != null) activityDate = rsTransaction.getTimestamp("return_date");
                        }
                        
                        if (!activityTypeStr.isEmpty()) {
                           activities.add(new ActivityLogItem(activityTypeStr, details, activityDate, actor, status));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error mengambil aktivitas dari transactions: " + e.getMessage());
        }
        
        // Urutkan semua aktivitas berdasarkan tanggal (terbaru dulu)
        Collections.sort(activities, Comparator.comparing(ActivityLogItem::getActivityDate, Comparator.nullsLast(Comparator.reverseOrder())));

        // Kembalikan sejumlah 'limit' dari gabungan aktivitas
        return activities.size() > limit ? activities.subList(0, limit) : activities;
    }
}