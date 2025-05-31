// File: src/models/Transaction.java
package models;

import java.sql.Date; // Menggunakan java.sql.Date untuk kompatibilitas dengan kolom DATE SQL
import java.sql.Timestamp;
import java.math.BigDecimal; // Untuk fine_amount

public class Transaction {
    private int id;
    private int memberId;         // FK ke tabel members (id internal)
    private int bookId;           // FK ke tabel books (id internal)
    private Date borrowDate;
    private Date returnDueDate;
    private Date returnDate;      // Tanggal aktual kembali, bisa null
    private BigDecimal fineAmount;
    private String status;        // "borrowed", "returned", "overdue"

    // Informasi tambahan untuk tampilan (bukan kolom langsung di tabel transactions)
    private String memberName;
    private String memberIdText;  // ID Anggota yang terlihat
    private String bookTitle;
    private String bookIsbn;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Konstruktor default
    public Transaction() {
    }

    // Konstruktor untuk membuat transaksi baru (saat peminjaman)
    public Transaction(int memberId, int bookId, Date borrowDate, Date returnDueDate) {
        this.memberId = memberId;
        this.bookId = bookId;
        this.borrowDate = borrowDate;
        this.returnDueDate = returnDueDate;
        this.status = "borrowed"; // Status awal
        this.fineAmount = BigDecimal.ZERO; // Denda awal nol
    }

    // Konstruktor lengkap (misalnya saat mengambil dari database)
    public Transaction(int id, int memberId, int bookId, Date borrowDate, Date returnDueDate,
                       Date returnDate, BigDecimal fineAmount, String status,
                       Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.memberId = memberId;
        this.bookId = bookId;
        this.borrowDate = borrowDate;
        this.returnDueDate = returnDueDate;
        this.returnDate = returnDate;
        this.fineAmount = fineAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public Date getBorrowDate() { return borrowDate; }
    public void setBorrowDate(Date borrowDate) { this.borrowDate = borrowDate; }

    public Date getReturnDueDate() { return returnDueDate; }
    public void setReturnDueDate(Date returnDueDate) { this.returnDueDate = returnDueDate; }

    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }

    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getMemberIdText() { return memberIdText; }
    public void setMemberIdText(String memberIdText) { this.memberIdText = memberIdText; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    
    public String getBookIsbn() { return bookIsbn; }
    public void setBookIsbn(String bookIsbn) { this.bookIsbn = bookIsbn; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Transaction{" +
               "id=" + id +
               ", memberId=" + memberId +
               ", bookId=" + bookId +
               ", borrowDate=" + borrowDate +
               ", status='" + status + '\'' +
               '}';
    }
}