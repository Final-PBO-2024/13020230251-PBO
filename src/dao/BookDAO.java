// File: src/com/perpustakaanku/dao/BookDAO.java
package dao;

import config.DatabaseConnection;
import models.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// import javax.swing.JOptionPane; // Sudah kita hapus dari DAO

public class BookDAO {

    // Metode addBook DIMODIFIKASI untuk manual ID generation dan reuse
    public boolean addBook(Book book) {
        String findNextIdSQL;
        // Coba cari ID terkecil yang tidak terpakai (celah pertama)
        // Jika tidak ada celah, ambil MAX(id) + 1. Jika tabel kosong, mulai dari 1.
        findNextIdSQL = "SELECT COALESCE(MIN(t1.id + 1), 1) AS next_id " +
                        "FROM books t1 " +
                        "LEFT JOIN books t2 ON t1.id + 1 = t2.id " +
                        "WHERE t2.id IS NULL " +
                        "AND NOT EXISTS (SELECT 1 FROM books WHERE id = 1 AND t1.id IS NULL)"; // Handle kasus tabel kosong atau ID 1 belum ada

        String findMaxIdSQL = "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM books";

        String sqlInsert = "INSERT INTO books (id, title, author, publisher, year, isbn, stock) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        int nextId = 1; // Default jika tabel kosong

        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                System.err.println("BookDAO: Koneksi DB null untuk addBook.");
                return false;
            }

            // Tentukan ID berikutnya
            boolean idFoundFromGap = false;
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT id FROM books ORDER BY id ASC")) {
                List<Integer> existingIds = new ArrayList<>();
                while (rs.next()) {
                    existingIds.add(rs.getInt("id"));
                }

                if (existingIds.isEmpty()) {
                    nextId = 1;
                    idFoundFromGap = true;
                } else {
                    int expectedId = 1;
                    for (int existingId : existingIds) {
                        if (existingId > expectedId) {
                            nextId = expectedId;
                            idFoundFromGap = true;
                            break;
                        }
                        expectedId = existingId + 1;
                    }
                    if (!idFoundFromGap) {
                        nextId = existingIds.get(existingIds.size() - 1) + 1;
                    }
                }
            }
             System.out.println("DEBUG: Next ID yang akan digunakan: " + nextId);


            // Masukkan buku dengan ID yang sudah ditentukan
            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                book.setId(nextId); // Set ID pada objek Book

                pstmt.setInt(1, book.getId());
                pstmt.setString(2, book.getTitle());
                pstmt.setString(3, book.getAuthor());
                pstmt.setString(4, book.getPublisher());
                if (book.getYear() == 0) { pstmt.setNull(5, Types.INTEGER); } 
                else { pstmt.setInt(5, book.getYear()); }
                pstmt.setString(6, book.getIsbn());
                pstmt.setInt(7, book.getStock());
                
                int affectedRows = pstmt.executeUpdate();
                return affectedRows > 0;
            }

        } catch (SQLException e) {
            System.err.println("Error saat menambah buku dengan manual ID: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<Book> getAllBooks(String searchTerm) {
        List<Book> books = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder(
            "SELECT id, title, author, publisher, year, isbn, stock, " +
            "created_at, updated_at, deleted_at " +
            "FROM books " +
            "WHERE deleted_at IS NULL "
        );
        List<Object> params = new ArrayList<>();
        if (searchTerm != null && !searchTerm.isEmpty()) {
            sqlBuilder.append("AND (LOWER(title) LIKE LOWER(?) OR LOWER(author) LIKE LOWER(?) OR LOWER(isbn) LIKE LOWER(?)) ");
            String likeTerm = "%" + searchTerm + "%";
            params.add(likeTerm); params.add(likeTerm); params.add(likeTerm);
        }
        sqlBuilder.append("ORDER BY title ASC");

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                System.err.println("BookDAO: Koneksi DB null untuk getAllBooks.");
                return books;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        books.add(new Book(
                                rs.getInt("id"), rs.getString("title"), rs.getString("author"),
                                rs.getString("publisher"), rs.getInt("year"), rs.getString("isbn"),
                                rs.getInt("stock"), rs.getTimestamp("created_at"),
                                rs.getTimestamp("updated_at"), rs.getTimestamp("deleted_at")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat mengambil semua buku: " + e.getMessage());
        }
        return books;
    }
    
    public Book getBookById(int id) {
        String sql = "SELECT id, title, author, publisher, year, isbn, stock, " +
                     "created_at, updated_at, deleted_at " +
                     "FROM books WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                System.err.println("BookDAO: Koneksi DB null untuk getBookById.");
                return null;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new Book(
                                rs.getInt("id"), rs.getString("title"), rs.getString("author"),
                                rs.getString("publisher"), rs.getInt("year"), rs.getString("isbn"),
                                rs.getInt("stock"), rs.getTimestamp("created_at"),
                                rs.getTimestamp("updated_at"), rs.getTimestamp("deleted_at")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat mengambil buku by ID: " + e.getMessage());
        }
        return null;
    }

    public boolean updateBook(Book book) {
        String sql = "UPDATE books SET title = ?, author = ?, publisher = ?, year = ?, isbn = ?, " +
                     "stock = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, book.getTitle());
                pstmt.setString(2, book.getAuthor());
                pstmt.setString(3, book.getPublisher());
                if (book.getYear() == 0) { pstmt.setNull(4, Types.INTEGER); }
                else { pstmt.setInt(4, book.getYear()); }
                pstmt.setString(5, book.getIsbn());
                pstmt.setInt(6, book.getStock());
                pstmt.setInt(7, book.getId());
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error saat memperbarui buku: " + e.getMessage());
        }
        return false;
    }
    
    public boolean isbnExists(String isbn, int currentBookIdToExclude) {
        String sql = "SELECT COUNT(*) FROM books WHERE isbn = ? AND id != ? AND deleted_at IS NULL";
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                System.err.println("BookDAO: Koneksi DB null untuk isbnExists.");
                return true; 
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, isbn);
                pstmt.setInt(2, currentBookIdToExclude);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat cek ISBN: " + e.getMessage());
            return true; 
        }
        return false;
    }

    public boolean softDeleteBook(int id, String adminUsername) {
        Book bookToDelete = getBookById(id); 
        if (bookToDelete == null || bookToDelete.getDeletedAt() != null) {
            System.err.println("Buku tidak ditemukan atau sudah di soft delete sebelumnya.");
            return false; 
        }
        String sql = "UPDATE books SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL";
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    String itemData = String.format("Title: %s, Author: %s, ISBN: %s, Publisher: %s, Year: %d, Stock: %d",
                            bookToDelete.getTitle(), bookToDelete.getAuthor(), bookToDelete.getIsbn(),
                            bookToDelete.getPublisher(), bookToDelete.getYear(), bookToDelete.getStock());
                    logToRecycleBin(id, "Book", "deleted", adminUsername, itemData);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat soft delete buku: " + e.getMessage());
        }
        return false;
    }

    public boolean restoreBook(int id, String adminUsername) {
        Book bookToRestore = getBookById(id); 
        if (bookToRestore == null || bookToRestore.getDeletedAt() == null) {
            System.err.println("Buku tidak ditemukan atau tidak sedang di soft delete.");
            return false; 
        }
        String sql = "UPDATE books SET deleted_at = NULL WHERE id = ? AND deleted_at IS NOT NULL";
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return false;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    String itemData = String.format("Title: %s, Author: %s, ISBN: %s",
                            bookToRestore.getTitle(), bookToRestore.getAuthor(), bookToRestore.getIsbn());
                    logToRecycleBin(id, "Book", "restored", adminUsername, itemData);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat restore buku: " + e.getMessage());
        }
        return false;
    }

    public boolean permanentlyDeleteBook(int id, String adminUsername) {
        Book bookToDelete = getBookById(id); 
        
        String deleteTransactionsSQL = "DELETE FROM transactions WHERE book_id = ?";
        String deleteBookSQL = "DELETE FROM books WHERE id = ?";
        Connection conn = null;

        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                System.err.println("BookDAO: Koneksi DB null untuk permanentlyDeleteBook.");
                return false;
            }
            conn.setAutoCommit(false); 

            try (PreparedStatement pstmtTransactions = conn.prepareStatement(deleteTransactionsSQL)) {
                pstmtTransactions.setInt(1, id);
                int transactionsAffected = pstmtTransactions.executeUpdate();
                System.out.println("DEBUG: Jumlah transaksi terkait buku ID " + id + " yang dihapus: " + transactionsAffected);
            }
            
            int bookAffectedRows = 0;
            try (PreparedStatement pstmtBook = conn.prepareStatement(deleteBookSQL)) {
                pstmtBook.setInt(1, id);
                bookAffectedRows = pstmtBook.executeUpdate();
            }

            if (bookAffectedRows > 0) {
                conn.commit(); 
                String itemData = "Permanently deleted book and its transaction history.";
                if (bookToDelete != null) { 
                     itemData = String.format("Permanently deleted Book - Title: %s, ISBN: %s (and associated transactions)",
                        bookToDelete.getTitle(), bookToDelete.getIsbn());
                }
                logToRecycleBin(id, "Book", "permanently_deleted", adminUsername, itemData);
                return true;
            } else {
                System.err.println("Gagal menghapus permanen buku dengan ID: " + id + ". Buku mungkin sudah tidak ada.");
                conn.rollback(); 
                return false;
            }

        } catch (SQLException e) {
            System.err.println("Error SQL saat permanently delete buku beserta transaksinya: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error saat rollback: " + ex.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    System.err.println("Error saat mengembalikan auto-commit: " + ex.getMessage());
                }
            }
        }
    }

    public List<Book> getSoftDeletedBooks() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT id, title, author, publisher, year, isbn, stock, " +
                     "created_at, updated_at, deleted_at " +
                     "FROM books WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC";
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return books;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    books.add(new Book(
                            rs.getInt("id"), rs.getString("title"), rs.getString("author"),
                            rs.getString("publisher"), rs.getInt("year"), rs.getString("isbn"),
                            rs.getInt("stock"), rs.getTimestamp("created_at"),
                            rs.getTimestamp("updated_at"), rs.getTimestamp("deleted_at")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat mengambil soft deleted books: " + e.getMessage());
        }
        return books;
    }

    public void logToRecycleBin(int entityId, String entityType, String actionType, String adminUsername, String itemDataDetails) {
        String sqlLog = "INSERT INTO recycle_bin_logs (entity_id, entity_type, item_data, action_type, action_by) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return;
            try (PreparedStatement pstmtLog = conn.prepareStatement(sqlLog)) {
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

    public int getTotalActiveBooksCount() {
        String sql = "SELECT COUNT(*) FROM books WHERE deleted_at IS NULL";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                System.err.println("BookDAO: Koneksi DB null untuk getTotalActiveBooksCount.");
                return 0;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat menghitung total buku aktif: " + e.getMessage());
        }
        return 0;
    }

    public int getTotalSoftDeletedBooksCount() {
        String sql = "SELECT COUNT(*) FROM books WHERE deleted_at IS NOT NULL";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                System.err.println("BookDAO: Koneksi DB null untuk getTotalSoftDeletedBooksCount.");
                return 0;
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat menghitung total buku terhapus (soft): " + e.getMessage());
        }
        return 0;
    }
}
