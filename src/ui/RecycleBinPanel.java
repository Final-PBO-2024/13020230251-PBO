// File: src/ui/RecycleBinPanel.java
package ui;

import dao.BookDAO;
import dao.MemberDAO;
import models.Book;
import models.Member;
import models.RecycleBinItem; // Model yang baru kita buat

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date; // Untuk SimpleDateFormat
import java.util.List;
import java.util.Vector;

public class RecycleBinPanel extends JPanel {
    private JComboBox<String> itemTypeFilterComboBox;
    private JTable recycleBinTable;
    private DefaultTableModel tableModel;
    private JButton restoreButton;
    private JButton permanentDeleteButton;
    private JButton refreshButton;

    private BookDAO bookDAO;
    private MemberDAO memberDAO;
    private String loggedInAdminUsername;

    private List<RecycleBinItem> currentDisplayedItems; // Untuk menyimpan item yang ditampilkan

    public RecycleBinPanel(String adminUsername) {
        this.loggedInAdminUsername = adminUsername;
        this.bookDAO = new BookDAO();
        this.memberDAO = new MemberDAO();
        this.currentDisplayedItems = new ArrayList<>();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        setBackground(Color.WHITE);

        initComponents();
        layoutComponents();
        addEventListeners();

        loadRecycleBinItems(); // Muat data awal
    }

    private void initComponents() {
        String[] filterOptions = {"All Items", "Books Only", "Members Only"};
        itemTypeFilterComboBox = new JComboBox<>(filterOptions);
        itemTypeFilterComboBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        itemTypeFilterComboBox.setPreferredSize(new Dimension(150, 28));

        String[] columnNames = {"ID", "Type", "Name/Title", "Identifier (ISBN/MemberID)", "Deleted Date"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        recycleBinTable = new JTable(tableModel);
        recycleBinTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        recycleBinTable.setRowHeight(25);
        recycleBinTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        recycleBinTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableColumnModel tcm = recycleBinTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(50);  // Original Entity ID
        tcm.getColumn(1).setPreferredWidth(80);  // Type
        tcm.getColumn(2).setPreferredWidth(250); // Name/Title
        tcm.getColumn(3).setPreferredWidth(150); // Identifier
        tcm.getColumn(4).setPreferredWidth(180); // Deleted Date

        restoreButton = new JButton("Restore Selected");
        styleButton(restoreButton, new Color(40, 167, 69)); // Hijau
        restoreButton.setEnabled(false);

        permanentDeleteButton = new JButton("Permanently Delete Selected");
        styleButton(permanentDeleteButton, new Color(220, 53, 69)); // Merah
        permanentDeleteButton.setEnabled(false);
        
        refreshButton = new JButton("Refresh List");
        styleButton(refreshButton, new Color(0, 123, 255)); // Biru
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBackground(bgColor); button.setForeground(Color.WHITE);
        button.setFocusPainted(false); button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(Math.max(button.getPreferredSize().width, 180), 30));
    }

    private void layoutComponents() {
        JPanel topPanel = new JPanel(new BorderLayout(10,10));
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Recycle Bin");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0,0,15,0));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setOpaque(false);
        filterPanel.add(new JLabel("Show:"));
        filterPanel.add(itemTypeFilterComboBox);
        filterPanel.add(Box.createHorizontalStrut(20));
        filterPanel.add(refreshButton);
        topPanel.add(filterPanel, BorderLayout.CENTER);
        
        add(topPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(recycleBinTable);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.add(restoreButton);
        bottomPanel.add(permanentDeleteButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void addEventListeners() {
        itemTypeFilterComboBox.addActionListener(e -> loadRecycleBinItems());
        refreshButton.addActionListener(e -> loadRecycleBinItems());

        recycleBinTable.getSelectionModel().addListSelectionListener(e -> {
            boolean rowSelected = recycleBinTable.getSelectedRow() != -1;
            restoreButton.setEnabled(rowSelected);
            permanentDeleteButton.setEnabled(rowSelected);
        });

        restoreButton.addActionListener(e -> handleRestoreItem());
        permanentDeleteButton.addActionListener(e -> handlePermanentDeleteItem());
    }

    private void loadRecycleBinItems() {
        currentDisplayedItems.clear();
        tableModel.setRowCount(0);
        String filter = (String) itemTypeFilterComboBox.getSelectedItem();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss");

        if ("All Items".equals(filter) || "Books Only".equals(filter)) {
            List<Book> deletedBooks = bookDAO.getSoftDeletedBooks();
            for (Book book : deletedBooks) {
                String itemData = String.format("Title: %s, Author: %s, ISBN: %s", 
                                                book.getTitle(), book.getAuthor(), book.getIsbn());
                currentDisplayedItems.add(new RecycleBinItem(
                        book.getId(), "Book", book.getTitle(), book.getIsbn(), book.getDeletedAt(), itemData));
            }
        }

        if ("All Items".equals(filter) || "Members Only".equals(filter)) {
            List<Member> deletedMembers = memberDAO.getSoftDeletedMembers();
            for (Member member : deletedMembers) {
                 String itemData = String.format("MemberID: %s, Name: %s, Contact: %s", 
                                                member.getMemberIdText(), member.getName(), member.getContact());
                currentDisplayedItems.add(new RecycleBinItem(
                        member.getId(), "Member", member.getName(), member.getMemberIdText(), member.getDeletedAt(), itemData));
            }
        }

        // Urutkan berdasarkan tanggal dihapus (terbaru dulu)
        currentDisplayedItems.sort(Comparator.comparing(RecycleBinItem::getDeletedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        for (RecycleBinItem item : currentDisplayedItems) {
            Vector<Object> row = new Vector<>();
            row.add(item.getOriginalEntityId());
            row.add(item.getEntityType());
            row.add(item.getDisplayName());
            row.add(item.getIdentifier());
            row.add(item.getDeletedAt() != null ? sdf.format(new Date(item.getDeletedAt().getTime())) : "N/A");
            tableModel.addRow(row);
        }
        restoreButton.setEnabled(false);
        permanentDeleteButton.setEnabled(false);
    }

    private void handleRestoreItem() {
        int selectedRow = recycleBinTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to restore.", "No Item Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        RecycleBinItem selectedItem = currentDisplayedItems.get(recycleBinTable.convertRowIndexToModel(selectedRow));
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to restore this " + selectedItem.getEntityType() + ": '" + selectedItem.getDisplayName() + "'?",
            "Confirm Restore", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = false;
            if ("Book".equals(selectedItem.getEntityType())) {
                success = bookDAO.restoreBook(selectedItem.getOriginalEntityId(), loggedInAdminUsername);
            } else if ("Member".equals(selectedItem.getEntityType())) {
                success = memberDAO.restoreMember(selectedItem.getOriginalEntityId(), loggedInAdminUsername);
            }

            if (success) {
                JOptionPane.showMessageDialog(this, selectedItem.getEntityType() + " restored successfully.", "Restore Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to restore " + selectedItem.getEntityType() + ".", "Restore Failed", JOptionPane.ERROR_MESSAGE);
            }
            loadRecycleBinItems(); // Refresh list
        }
    }

    private void handlePermanentDeleteItem() {
        int selectedRow = recycleBinTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to permanently delete.", "No Item Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        RecycleBinItem selectedItem = currentDisplayedItems.get(recycleBinTable.convertRowIndexToModel(selectedRow));
        int confirm = JOptionPane.showConfirmDialog(this, 
            "PERMANENTLY DELETE this " + selectedItem.getEntityType() + ": '" + selectedItem.getDisplayName() + "'?\nThis action CANNOT be undone.",
            "Confirm Permanent Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = false;
            if ("Book".equals(selectedItem.getEntityType())) {
                success = bookDAO.permanentlyDeleteBook(selectedItem.getOriginalEntityId(), loggedInAdminUsername);
            } else if ("Member".equals(selectedItem.getEntityType())) {
                success = memberDAO.permanentlyDeleteMember(selectedItem.getOriginalEntityId(), loggedInAdminUsername);
            }

            if (success) {
                JOptionPane.showMessageDialog(this, selectedItem.getEntityType() + " permanently deleted.", "Deletion Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to permanently delete " + selectedItem.getEntityType() + ".\nIt might be referenced in active transactions.", "Deletion Failed", JOptionPane.ERROR_MESSAGE);
            }
            loadRecycleBinItems(); // Refresh list
        }
    }
}