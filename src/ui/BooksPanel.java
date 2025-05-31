// File: src/ui/BooksPanel.java (atau src/com/perpustakaanku/ui/BooksPanel.java)
package ui; // Sesuaikan dengan nama paket Anda

import dao.BookDAO;   // Sesuaikan import jika paket berbeda
import models.Book;   // Sesuaikan import jika paket berbeda

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;

public class BooksPanel extends JPanel {
    private JTextField titleField, authorField, publisherField, yearField, isbnField, stockField;
    // ComboBox kategori dihapus
    private JButton addButton, saveButton, cancelButton, deleteButton;
    private JTable booksTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton searchButton;
    private JLabel formTitleLabel;

    private BookDAO bookDAO;
    private Book selectedBookForEdit = null;
    private String loggedInAdminUsername;

    public BooksPanel(String adminUsername) {
        this.loggedInAdminUsername = adminUsername;
        this.bookDAO = new BookDAO();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        setBackground(Color.WHITE);

        initComponents();
        layoutComponents();
        addEventListeners();

        loadBooksToTable();
        switchToAddingMode();
    }

    private void initComponents() {
        formTitleLabel = new JLabel("Add New Book");
        formTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        formTitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        titleField = new JTextField(20); authorField = new JTextField(20);
        publisherField = new JTextField(20); yearField = new JTextField(5);
        isbnField = new JTextField(15); stockField = new JTextField(5);
        Font fieldFont = new Font("SansSerif", Font.PLAIN, 13);
        titleField.setFont(fieldFont); authorField.setFont(fieldFont);
        publisherField.setFont(fieldFont); yearField.setFont(fieldFont);
        isbnField.setFont(fieldFont); stockField.setFont(fieldFont);

        addButton = new JButton("Add Book"); styleButton(addButton, new Color(40, 167, 69));
        saveButton = new JButton("Save Changes"); styleButton(saveButton, new Color(0, 123, 255));
        cancelButton = new JButton("Cancel"); styleButton(cancelButton, new Color(108, 117, 125));
        deleteButton = new JButton("Delete Book"); styleButton(deleteButton, new Color(220, 53, 69));

        searchField = new JTextField(25); searchField.setFont(fieldFont);
        searchButton = new JButton("Search"); styleButton(searchButton, new Color(0, 123, 255));

        // Kolom kategori dihapus dari tabel
        String[] columnNames = {"ID", "Title", "Author", "ISBN", "Stock", "Year"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };
        booksTable = new JTable(tableModel);
        booksTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        booksTable.setRowHeight(25);
        booksTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        booksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableColumnModel tcm = booksTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(40); // ID
        tcm.getColumn(1).setPreferredWidth(250); // Title
        tcm.getColumn(2).setPreferredWidth(150); // Author
        tcm.getColumn(3).setPreferredWidth(120); // ISBN
        tcm.getColumn(4).setPreferredWidth(60);  // Stock
        tcm.getColumn(5).setPreferredWidth(60);  // Year
    }
    
    private void styleButton(JButton button, Color bgColor) {
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBackground(bgColor); button.setForeground(Color.WHITE);
        button.setFocusPainted(false); button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(Math.max(button.getPreferredSize().width, 120), 30));
    }

    private void layoutComponents() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Book Details"),
            BorderFactory.createEmptyBorder(10,10,10,10)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx=0; gbc.gridy=0; gbc.gridwidth=2; formPanel.add(formTitleLabel, gbc); gbc.gridwidth=1;
        gbc.gridx=0; gbc.gridy=1; formPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx=1; gbc.gridy=1; gbc.weightx=1.0; formPanel.add(titleField, gbc); gbc.weightx=0.0;
        gbc.gridx=0; gbc.gridy=2; formPanel.add(new JLabel("Author:"), gbc);
        gbc.gridx=1; gbc.gridy=2; formPanel.add(authorField, gbc);
        gbc.gridx=0; gbc.gridy=3; formPanel.add(new JLabel("Publisher:"), gbc);
        gbc.gridx=1; gbc.gridy=3; formPanel.add(publisherField, gbc);
        
        JPanel yearIsbnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0)); yearIsbnPanel.setOpaque(false);
        yearIsbnPanel.add(new JLabel("Year:")); yearIsbnPanel.add(Box.createHorizontalStrut(5)); yearIsbnPanel.add(yearField);
        yearIsbnPanel.add(Box.createHorizontalStrut(15));
        yearIsbnPanel.add(new JLabel("ISBN:")); yearIsbnPanel.add(Box.createHorizontalStrut(5)); yearIsbnPanel.add(isbnField);
        gbc.gridx=0; gbc.gridy=4; gbc.gridwidth=2; formPanel.add(yearIsbnPanel, gbc); gbc.gridwidth=1;
        
        // Baris untuk Stock (tanpa kategori)
        gbc.gridx = 0; gbc.gridy = 5; formPanel.add(new JLabel("Stock:"), gbc);
        gbc.gridx = 1; gbc.gridy = 5; formPanel.add(stockField, gbc);

        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); actionButtonPanel.setOpaque(false);
        actionButtonPanel.add(addButton); actionButtonPanel.add(saveButton); actionButtonPanel.add(cancelButton);
        gbc.gridx=0; gbc.gridy=6; gbc.gridwidth=2; gbc.anchor=GridBagConstraints.CENTER; formPanel.add(actionButtonPanel, gbc);
        
        JPanel rightPanel = new JPanel(new BorderLayout(10,10)); rightPanel.setOpaque(false);
        JPanel searchAreaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); searchAreaPanel.setOpaque(false);
        searchAreaPanel.add(new JLabel("Search Book:")); searchAreaPanel.add(searchField);
        searchAreaPanel.add(searchButton); searchAreaPanel.add(Box.createHorizontalStrut(20));
        searchAreaPanel.add(deleteButton);
        rightPanel.add(searchAreaPanel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(booksTable), BorderLayout.CENTER);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formPanel, rightPanel);
        splitPane.setDividerLocation(420); splitPane.setOpaque(false); splitPane.setBackground(Color.WHITE);
        add(splitPane, BorderLayout.CENTER);
    }

    private void addEventListeners() {
        addButton.addActionListener(e -> handleAddBook());
        saveButton.addActionListener(e -> handleSaveBook());
        cancelButton.addActionListener(e -> switchToAddingMode());
        deleteButton.addActionListener(e -> handleDeleteBook());
        searchButton.addActionListener(e -> loadBooksToTable());
        searchField.addActionListener(e -> loadBooksToTable());
        booksTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && booksTable.getSelectedRow() != -1) populateFormFromSelectedRow();
            else if (booksTable.getSelectedRow() == -1) deleteButton.setEnabled(false);
        });
    }

    private void loadBooksToTable() {
        String searchTerm = searchField.getText().trim();
        tableModel.setRowCount(0);
        List<Book> books = bookDAO.getAllBooks(searchTerm);
        for (Book book : books) {
            Vector<Object> row = new Vector<>();
            row.add(book.getId()); row.add(book.getTitle()); row.add(book.getAuthor());
            row.add(book.getIsbn()); 
            // Kolom kategori dihapus dari tampilan tabel
            row.add(book.getStock()); 
            row.add(book.getYear() != 0 ? book.getYear() : "N/A");
            tableModel.addRow(row);
        }
        if (booksTable.getSelectedRow() == -1) deleteButton.setEnabled(false);
    }

    private void clearForm() {
        titleField.setText(""); authorField.setText(""); publisherField.setText("");
        yearField.setText(""); isbnField.setText(""); stockField.setText("");
        // ComboBox kategori dihapus
        selectedBookForEdit = null; booksTable.clearSelection();
    }

    private void switchToAddingMode() {
        formTitleLabel.setText("Add New Book"); clearForm();
        addButton.setVisible(true); saveButton.setVisible(false); cancelButton.setVisible(false);
        deleteButton.setEnabled(false); isbnField.setEditable(true);
    }

    private void switchToEditingMode(Book book) {
        selectedBookForEdit = book;
        formTitleLabel.setText("Edit Book: " + book.getTitle());
        titleField.setText(book.getTitle()); authorField.setText(book.getAuthor());
        publisherField.setText(book.getPublisher());
        yearField.setText(book.getYear() != 0 ? String.valueOf(book.getYear()) : "");
        isbnField.setText(book.getIsbn()); isbnField.setEditable(false);
        stockField.setText(String.valueOf(book.getStock()));
        // Logika ComboBox kategori dihapus
        addButton.setVisible(false); saveButton.setVisible(true); cancelButton.setVisible(true);
        deleteButton.setEnabled(true);
    }

    private void populateFormFromSelectedRow() {
        int selectedRow = booksTable.getSelectedRow();
        if (selectedRow != -1) {
            int bookId = (int) tableModel.getValueAt(selectedRow, 0);
            Book book = bookDAO.getBookById(bookId);
            if (book != null && book.getDeletedAt() == null) switchToEditingMode(book);
            else if (book == null) {
                JOptionPane.showMessageDialog(this, "Book details not found.", "Error", JOptionPane.ERROR_MESSAGE);
                loadBooksToTable();
            } else {
                 JOptionPane.showMessageDialog(this, "This book is in Recycle Bin.", "Info", JOptionPane.INFORMATION_MESSAGE);
                 clearForm(); switchToAddingMode();
            }
        }
    }

    private boolean validateInput() {
        if (titleField.getText().trim().isEmpty()){ JOptionPane.showMessageDialog(this, "Title cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE); return false; }
        if (authorField.getText().trim().isEmpty()){ JOptionPane.showMessageDialog(this, "Author cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE); return false; }
        if (isbnField.getText().trim().isEmpty()){ JOptionPane.showMessageDialog(this, "ISBN cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE); return false; }
        try {
            if (!yearField.getText().trim().isEmpty()) Integer.parseInt(yearField.getText().trim());
            if (stockField.getText().trim().isEmpty()){ JOptionPane.showMessageDialog(this, "Stock cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE); return false; }
            Integer.parseInt(stockField.getText().trim());
        } catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Year and Stock must be valid numbers.", "Validation Error", JOptionPane.ERROR_MESSAGE); return false; }
        return true;
    }

    private void handleAddBook() {
        if (!validateInput()) return;
        String title = titleField.getText().trim(); String author = authorField.getText().trim();
        String publisher = publisherField.getText().trim();
        int year = yearField.getText().trim().isEmpty() ? 0 : Integer.parseInt(yearField.getText().trim());
        String isbn = isbnField.getText().trim();
        int stock = Integer.parseInt(stockField.getText().trim());
        // Logika categoryId dihapus
        if (bookDAO.isbnExists(isbn, 0)) {
            JOptionPane.showMessageDialog(this, "ISBN '" + isbn + "' already exists.", "Duplicate ISBN", JOptionPane.ERROR_MESSAGE); return;
        }
        Book newBook = new Book(title, author, publisher, year, isbn, stock); // Konstruktor disesuaikan
        if (bookDAO.addBook(newBook)) {
            JOptionPane.showMessageDialog(this, "Book added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadBooksToTable(); clearForm();
        } else JOptionPane.showMessageDialog(this, "Failed to add book.", "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    private void handleSaveBook() {
        if (selectedBookForEdit == null) { JOptionPane.showMessageDialog(this, "No book selected for update.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        if (!validateInput()) return;
        selectedBookForEdit.setTitle(titleField.getText().trim());
        selectedBookForEdit.setAuthor(authorField.getText().trim());
        selectedBookForEdit.setPublisher(publisherField.getText().trim());
        selectedBookForEdit.setYear(yearField.getText().trim().isEmpty() ? 0 : Integer.parseInt(yearField.getText().trim()));
        selectedBookForEdit.setStock(Integer.parseInt(stockField.getText().trim()));
        // Logika categoryId dihapus
        if (bookDAO.updateBook(selectedBookForEdit)) {
            JOptionPane.showMessageDialog(this, "Book updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadBooksToTable(); switchToAddingMode();
        } else JOptionPane.showMessageDialog(this, "Failed to update book.", "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    private void handleDeleteBook() {
        int selectedRow = booksTable.getSelectedRow();
        if (selectedRow == -1) { JOptionPane.showMessageDialog(this, "Please select a book to delete.", "Selection Error", JOptionPane.WARNING_MESSAGE); return; }
        int bookId = (int) tableModel.getValueAt(selectedRow, 0);
        String bookTitle = (String) tableModel.getValueAt(selectedRow, 1);
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to move book '" + bookTitle + "' to Recycle Bin?", 
            "Confirm Soft Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            if (bookDAO.softDeleteBook(bookId, loggedInAdminUsername)) {
                JOptionPane.showMessageDialog(this, "Book '" + bookTitle + "' moved to Recycle Bin.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadBooksToTable(); switchToAddingMode();
            } else JOptionPane.showMessageDialog(this, "Failed to move book to Recycle Bin.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}