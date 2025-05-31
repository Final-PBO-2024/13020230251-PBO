// File: src/ui/TransactionsPanel.java
package ui;

import dao.TransactionDAO;
import dao.BookDAO; // Untuk mencari buku berdasarkan ISBN/ID dan cek stok
import dao.MemberDAO; // Untuk mencari anggota berdasarkan ID
import models.Transaction;
import models.Book;
import models.Member;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.Date; // Penting: java.sql.Date
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

public class TransactionsPanel extends JPanel {
    // Komponen untuk Form Peminjaman
    private JTextField memberIdTextField, bookIdentifierField; // bookIdentifierField bisa ISBN atau ID Buku Internal
    private JButton borrowButton, findMemberButton, findBookButton;
    private JLabel memberNameLabel, bookTitleLabel, bookStockLabel;
    private JSpinner borrowDateSpinner, dueDateSpinner; // Menggunakan JSpinner untuk tanggal

    // Komponen untuk Riwayat Transaksi dan Pengembalian
    private JTable transactionsTable;
    private DefaultTableModel tableModel;
    private JTextField searchTransactionField;
    private JButton searchTransactionButton, returnBookButton;

    // DAO
    private TransactionDAO transactionDAO;
    private MemberDAO memberDAO;
    private BookDAO bookDAO;

    private Member currentSelectedMember = null;
    private Book currentSelectedBook = null;
    private final String loggedInAdminUsername; // Disimpan untuk logging aksi
    private final int DEFAULT_BORROW_DURATION_DAYS = 7; // Durasi peminjaman default 7 hari
    private final BigDecimal FINE_PER_DAY = new BigDecimal("1000"); // Denda Rp 1.000 per hari


    public TransactionsPanel(String adminUsername) {
        this.loggedInAdminUsername = adminUsername;
        this.transactionDAO = new TransactionDAO();
        this.memberDAO = new MemberDAO();
        this.bookDAO = new BookDAO();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        setBackground(Color.WHITE);

        initComponents();
        layoutComponents();
        addEventListeners();

        loadTransactionHistory();
        resetBorrowForm();
    }

    private void initComponents() {
        Font fieldFont = new Font("SansSerif", Font.PLAIN, 13);
        Dimension textFieldSize = new Dimension(150, 28);

        // Form Peminjaman
        memberIdTextField = new JTextField();
        memberIdTextField.setPreferredSize(textFieldSize);
        memberIdTextField.setFont(fieldFont);

        bookIdentifierField = new JTextField(); // Bisa ISBN atau ID Buku
        bookIdentifierField.setPreferredSize(textFieldSize);
        bookIdentifierField.setFont(fieldFont);

        findMemberButton = new JButton("Find");
        styleButtonMini(findMemberButton);
        findBookButton = new JButton("Find");
        styleButtonMini(findBookButton);

        borrowButton = new JButton("Process Borrowing");
        styleButton(borrowButton, new Color(0, 123, 255));

        memberNameLabel = new JLabel("Member Name: -");
        memberNameLabel.setFont(fieldFont.deriveFont(Font.ITALIC));
        bookTitleLabel = new JLabel("Book Title: -");
        bookTitleLabel.setFont(fieldFont.deriveFont(Font.ITALIC));
        bookStockLabel = new JLabel("Stock: -");
        bookStockLabel.setFont(fieldFont.deriveFont(Font.ITALIC));

        // Date Spinners
        SpinnerDateModel borrowDateModel = new SpinnerDateModel(new java.util.Date(), null, null, Calendar.DAY_OF_MONTH);
        borrowDateSpinner = new JSpinner(borrowDateModel);
        JSpinner.DateEditor borrowDateEditor = new JSpinner.DateEditor(borrowDateSpinner, "dd/MM/yyyy");
        borrowDateSpinner.setEditor(borrowDateEditor);
        borrowDateSpinner.setFont(fieldFont);
        borrowDateSpinner.setPreferredSize(new Dimension(120, 28));

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, DEFAULT_BORROW_DURATION_DAYS);
        SpinnerDateModel dueDateModel = new SpinnerDateModel(cal.getTime(), null, null, Calendar.DAY_OF_MONTH);
        dueDateSpinner = new JSpinner(dueDateModel);
        JSpinner.DateEditor dueDateEditor = new JSpinner.DateEditor(dueDateSpinner, "dd/MM/yyyy");
        dueDateSpinner.setEditor(dueDateEditor);
        dueDateSpinner.setFont(fieldFont);
        dueDateSpinner.setPreferredSize(new Dimension(120, 28));


        // Riwayat Transaksi
        String[] columnNames = {"ID", "Member ID", "Member Name", "Book ISBN", "Book Title", "Borrow Date", "Due Date", "Return Date", "Status", "Fine"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        transactionsTable = new JTable(tableModel);
        transactionsTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        transactionsTable.setRowHeight(25);
        transactionsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        transactionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Atur lebar kolom
        TableColumnModel tcm = transactionsTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(40);  // TX ID
        tcm.getColumn(1).setPreferredWidth(80);  // Member ID Text
        tcm.getColumn(2).setPreferredWidth(150); // Member Name
        tcm.getColumn(3).setPreferredWidth(100); // Book ISBN
        tcm.getColumn(4).setPreferredWidth(200); // Book Title
        tcm.getColumn(5).setPreferredWidth(90);  // Borrow Date
        tcm.getColumn(6).setPreferredWidth(90);  // Due Date
        tcm.getColumn(7).setPreferredWidth(90);  // Return Date
        tcm.getColumn(8).setPreferredWidth(80);  // Status
        tcm.getColumn(9).setPreferredWidth(70);  // Fine

        searchTransactionField = new JTextField(20);
        searchTransactionField.setFont(fieldFont);
        searchTransactionButton = new JButton("Search Tx");
        styleButton(searchTransactionButton, new Color(108, 117, 125));
        returnBookButton = new JButton("Return Book");
        styleButton(returnBookButton, new Color(40, 167, 69));
        returnBookButton.setEnabled(false); // Aktif jika transaksi "borrowed" dipilih
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBackground(bgColor); button.setForeground(Color.WHITE);
        button.setFocusPainted(false); button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(Math.max(button.getPreferredSize().width, 150), 30));
    }
    private void styleButtonMini(JButton button) {
        button.setFont(new Font("SansSerif", Font.PLAIN, 11));
        button.setMargin(new Insets(2,5,2,5));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void layoutComponents() {
        // Panel Atas: Form Peminjaman
        JPanel borrowFormPanel = new JPanel(new GridBagLayout());
        borrowFormPanel.setBackground(Color.WHITE);
        borrowFormPanel.setBorder(BorderFactory.createTitledBorder("New Borrowing Transaction"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,8,5,8);
        gbc.anchor = GridBagConstraints.WEST;

        // Baris 1: Member ID
        gbc.gridx = 0; gbc.gridy = 0; borrowFormPanel.add(new JLabel("Member ID:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; borrowFormPanel.add(memberIdTextField, gbc);
        gbc.gridx = 2; gbc.gridy = 0; borrowFormPanel.add(findMemberButton, gbc);
        gbc.gridx = 3; gbc.gridy = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        borrowFormPanel.add(memberNameLabel, gbc); gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;

        // Baris 2: Book Identifier (ISBN/ID)
        gbc.gridx = 0; gbc.gridy = 1; borrowFormPanel.add(new JLabel("Book (ISBN/ID):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; borrowFormPanel.add(bookIdentifierField, gbc);
        gbc.gridx = 2; gbc.gridy = 1; borrowFormPanel.add(findBookButton, gbc);
        gbc.gridx = 3; gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel bookInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5,0));
        bookInfoPanel.setOpaque(false);
        bookInfoPanel.add(bookTitleLabel); bookInfoPanel.add(new JLabel("|")); bookInfoPanel.add(bookStockLabel);
        borrowFormPanel.add(bookInfoPanel, gbc); gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;


        // Baris 3: Tanggal Pinjam & Jatuh Tempo
        gbc.gridx = 0; gbc.gridy = 2; borrowFormPanel.add(new JLabel("Borrow Date:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; borrowFormPanel.add(borrowDateSpinner, gbc);
        gbc.gridx = 3; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST; borrowFormPanel.add(new JLabel("Due Date:"), gbc); gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 4; gbc.gridy = 2; borrowFormPanel.add(dueDateSpinner, gbc);

        // Baris 4: Tombol Borrow
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 5; gbc.anchor = GridBagConstraints.CENTER;
        borrowFormPanel.add(borrowButton, gbc);
        gbc.anchor = GridBagConstraints.WEST; gbc.gridwidth = 1;


        // Panel Bawah: Riwayat Transaksi
        JPanel historyPanel = new JPanel(new BorderLayout(10,10));
        historyPanel.setBackground(Color.WHITE);
        historyPanel.setBorder(BorderFactory.createTitledBorder("Transaction History"));

        JPanel historyControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        historyControlsPanel.setOpaque(false);
        historyControlsPanel.add(new JLabel("Search History:"));
        historyControlsPanel.add(searchTransactionField);
        historyControlsPanel.add(searchTransactionButton);
        historyControlsPanel.add(Box.createHorizontalStrut(50)); // Spasi
        historyControlsPanel.add(returnBookButton);

        historyPanel.add(historyControlsPanel, BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(transactionsTable), BorderLayout.CENTER);

        // Gabungkan form dan history
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, borrowFormPanel, historyPanel);
        mainSplitPane.setDividerLocation(200); // Sesuaikan tinggi form peminjaman
        mainSplitPane.setResizeWeight(0.1); // Beri sedikit ruang lebih ke history saat resize
        
        add(mainSplitPane, BorderLayout.CENTER);
    }

    private void addEventListeners() {
        findMemberButton.addActionListener(e -> findMember());
        findBookButton.addActionListener(e -> findBook());
        borrowButton.addActionListener(e -> processBorrowing());

        searchTransactionButton.addActionListener(e -> loadTransactionHistory());
        searchTransactionField.addActionListener(e -> loadTransactionHistory());
        returnBookButton.addActionListener(e -> processReturn());

        transactionsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && transactionsTable.getSelectedRow() != -1) {
                // Cek apakah transaksi yang dipilih berstatus "borrowed" atau "overdue"
                String status = (String) tableModel.getValueAt(transactionsTable.getSelectedRow(), 8); // Kolom status
                returnBookButton.setEnabled("borrowed".equalsIgnoreCase(status) || "overdue".equalsIgnoreCase(status));
            } else if (transactionsTable.getSelectedRow() == -1) {
                returnBookButton.setEnabled(false);
            }
        });

        // Auto-calculate due date when borrow date changes
        borrowDateSpinner.addChangeListener(e -> {
            java.util.Date selectedBorrowDate = (java.util.Date) borrowDateSpinner.getValue();
            Calendar cal = Calendar.getInstance();
            cal.setTime(selectedBorrowDate);
            cal.add(Calendar.DAY_OF_MONTH, DEFAULT_BORROW_DURATION_DAYS);
            dueDateSpinner.setValue(cal.getTime());
        });
    }

    private void findMember() {
        String memberIdText = memberIdTextField.getText().trim();
        if (memberIdText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Member ID to find.", "Input Needed", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        currentSelectedMember = memberDAO.getMemberByMemberIdText(memberIdText); // Cari berdasarkan member_id (teks)
        if (currentSelectedMember != null && currentSelectedMember.getDeletedAt() == null) {
            memberNameLabel.setText("Member Name: " + currentSelectedMember.getName());
        } else {
            memberNameLabel.setText("Member Name: Not Found / Inactive");
            currentSelectedMember = null;
        }
    }

    private void findBook() {
        String bookIdentifier = bookIdentifierField.getText().trim();
        if (bookIdentifier.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Book ISBN or Internal ID to find.", "Input Needed", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Coba cari berdasarkan ISBN dulu, lalu ID internal jika tidak ketemu atau jika input adalah angka
        List<Book> books = bookDAO.getAllBooks(bookIdentifier); // DAO search bisa cari berdasarkan title/author/isbn
        
        currentSelectedBook = null;
        if (!books.isEmpty()) {
            // Jika ada lebih dari satu hasil (misal search by title), idealnya ada dialog pemilihan.
            // Untuk sederhana, ambil yang pertama jika ISBN cocok persis, atau jika hanya satu hasil.
            for(Book book : books){
                if(book.getIsbn().equalsIgnoreCase(bookIdentifier)){
                    currentSelectedBook = book;
                    break;
                }
            }
            if(currentSelectedBook == null && books.size() == 1){
                currentSelectedBook = books.get(0);
            } else if (currentSelectedBook == null && books.size() > 1) {
                 JOptionPane.showMessageDialog(this, books.size() + " books found matching '"+bookIdentifier+"'. Please use a more specific ISBN or ID.", "Multiple Books Found", JOptionPane.INFORMATION_MESSAGE);
            }
        }
        // Jika tidak ketemu via search, coba sebagai ID internal (jika numerik)
        if (currentSelectedBook == null && bookIdentifier.matches("\\d+")) {
            currentSelectedBook = bookDAO.getBookById(Integer.parseInt(bookIdentifier));
        }


        if (currentSelectedBook != null && currentSelectedBook.getDeletedAt() == null) {
            bookTitleLabel.setText("Book Title: " + currentSelectedBook.getTitle());
            bookStockLabel.setText("Stock: " + currentSelectedBook.getStock());
            if (currentSelectedBook.getStock() <= 0) {
                bookStockLabel.setForeground(Color.RED);
            } else {
                bookStockLabel.setForeground(Color.DARK_GRAY);
            }
        } else {
            bookTitleLabel.setText("Book Title: Not Found / Unavailable");
            bookStockLabel.setText("Stock: -");
            currentSelectedBook = null;
        }
    }

    private void processBorrowing() {
        if (currentSelectedMember == null) {
            JOptionPane.showMessageDialog(this, "Please find and select a valid member.", "Member Not Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentSelectedBook == null) {
            JOptionPane.showMessageDialog(this, "Please find and select a valid book.", "Book Not Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!transactionDAO.isBookAvailable(currentSelectedBook.getId())) {
            JOptionPane.showMessageDialog(this, "Book '" + currentSelectedBook.getTitle() + "' is out of stock or unavailable.", "Book Unavailable", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (transactionDAO.isBookAlreadyBorrowedByMember(currentSelectedMember.getId(), currentSelectedBook.getId())) {
            JOptionPane.showMessageDialog(this, "Member '" + currentSelectedMember.getName() + "' has already borrowed this book ('" + currentSelectedBook.getTitle() + "') and not returned it yet.", "Book Already Borrowed", JOptionPane.WARNING_MESSAGE);
            return;
        }


        java.util.Date utilBorrowDate = (java.util.Date) borrowDateSpinner.getValue();
        java.util.Date utilDueDate = (java.util.Date) dueDateSpinner.getValue();

        Date sqlBorrowDate = new Date(utilBorrowDate.getTime());
        Date sqlDueDate = new Date(utilDueDate.getTime());
        
        // Validasi tanggal
        if (sqlDueDate.before(sqlBorrowDate)) {
            JOptionPane.showMessageDialog(this, "Due date cannot be before borrow date.", "Date Error", JOptionPane.ERROR_MESSAGE);
            return;
        }


        if (transactionDAO.borrowBook(currentSelectedMember.getId(), currentSelectedBook.getId(), sqlBorrowDate, sqlDueDate)) {
            JOptionPane.showMessageDialog(this, "Book successfully borrowed by " + currentSelectedMember.getName() + ".", "Borrowing Success", JOptionPane.INFORMATION_MESSAGE);
            // Log aksi peminjaman
            String details = "Member: " + currentSelectedMember.getName() + " (ID: " + currentSelectedMember.getMemberIdText() + ") " +
                             "borrowed Book: " + currentSelectedBook.getTitle() + " (ISBN: " + currentSelectedBook.getIsbn() + ")";
            // Kita bisa log ini ke tabel log umum atau tabel khusus transaksi jika diperlukan audit lebih.
            // Untuk sekarang, tabel transactions sudah mencatat.
            // Jika fitur recycle_bin_logs mau dipakai untuk audit umum, bisa panggil:
            // bookDAO.logToRecycleBin(currentSelectedBook.getId(), "BookBorrow", "borrowed", loggedInAdminUsername, details);
            
            loadTransactionHistory();
            resetBorrowForm();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to process borrowing. Check stock or member status.", "Borrowing Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void processReturn() {
        int selectedRow = transactionsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a transaction to process return.", "No Transaction Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int transactionId = (int) tableModel.getValueAt(selectedRow, 0);
        String bookTitle = (String) tableModel.getValueAt(selectedRow, 4);
        String status = (String) tableModel.getValueAt(selectedRow, 8);

        if (!("borrowed".equalsIgnoreCase(status) || "overdue".equalsIgnoreCase(status))) {
            JOptionPane.showMessageDialog(this, "This book has already been returned or transaction status is invalid.", "Invalid Action", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Minta tanggal pengembalian aktual
        // Untuk sederhana, kita gunakan tanggal hari ini
        Date actualReturnDate = new Date(System.currentTimeMillis()); // Tanggal hari ini

        // Dialog konfirmasi dengan input tanggal jika ingin manual
        // String returnDateStr = JOptionPane.showInputDialog(this, "Enter actual return date (yyyy-MM-dd):", LocalDate.now().toString());
        // if (returnDateStr == null || returnDateStr.trim().isEmpty()) return; // User cancel
        // try {
        //     actualReturnDate = Date.valueOf(returnDateStr.trim());
        // } catch (IllegalArgumentException e) {
        //     JOptionPane.showMessageDialog(this, "Invalid date format. Please use yyyy-MM-dd.", "Date Error", JOptionPane.ERROR_MESSAGE);
        //     return;
        // }


        int confirm = JOptionPane.showConfirmDialog(this, 
            "Process return for book: '" + bookTitle + "'?\nActual Return Date: " + new SimpleDateFormat("dd MMM yyyy").format(actualReturnDate),
            "Confirm Return", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (transactionDAO.returnBook(transactionId, actualReturnDate, FINE_PER_DAY)) {
                JOptionPane.showMessageDialog(this, "Book returned successfully.", "Return Success", JOptionPane.INFORMATION_MESSAGE);
                loadTransactionHistory();
                returnBookButton.setEnabled(false); // Nonaktifkan tombol setelah aksi
            } else {
                JOptionPane.showMessageDialog(this, "Failed to process return.", "Return Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void loadTransactionHistory() {
        String searchTerm = searchTransactionField.getText().trim();
        tableModel.setRowCount(0);
        List<Transaction> transactions = transactionDAO.getAllTransactionHistory(searchTerm);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");

        for (Transaction tx : transactions) {
            Vector<Object> row = new Vector<>();
            row.add(tx.getId());
            row.add(tx.getMemberIdText() != null ? tx.getMemberIdText() : "ID: " + tx.getMemberId());
            row.add(tx.getMemberName() != null ? tx.getMemberName() : "-");
            row.add(tx.getBookIsbn() != null ? tx.getBookIsbn() : "-");
            row.add(tx.getBookTitle() != null ? tx.getBookTitle() : "Book ID: " + tx.getBookId());
            row.add(tx.getBorrowDate() != null ? sdf.format(tx.getBorrowDate()) : "N/A");
            row.add(tx.getReturnDueDate() != null ? sdf.format(tx.getReturnDueDate()) : "N/A");
            row.add(tx.getReturnDate() != null ? sdf.format(tx.getReturnDate()) : "-");
            row.add(tx.getStatus());
            row.add(tx.getFineAmount().compareTo(BigDecimal.ZERO) > 0 ? String.format("Rp %,.0f", tx.getFineAmount()) : "-");
            tableModel.addRow(row);
        }
        returnBookButton.setEnabled(false); // Reset setelah refresh
    }
    
    private void resetBorrowForm() {
        memberIdTextField.setText("");
        bookIdentifierField.setText("");
        memberNameLabel.setText("Member Name: -");
        bookTitleLabel.setText("Book Title: -");
        bookStockLabel.setText("Stock: -");
        bookStockLabel.setForeground(Color.DARK_GRAY);
        currentSelectedMember = null;
        currentSelectedBook = null;

        // Set borrow date ke hari ini dan due date ke default durasi dari hari ini
        java.util.Date today = new java.util.Date();
        borrowDateSpinner.setValue(today);
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.add(Calendar.DAY_OF_MONTH, DEFAULT_BORROW_DURATION_DAYS);
        dueDateSpinner.setValue(cal.getTime());
    }
}