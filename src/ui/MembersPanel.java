/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ui;

/**
 *
 * @author andi.ikhlass
 */
import dao.MemberDAO;
import models.Member;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date; // Untuk format Timestamp
import java.util.List;
import java.util.Vector;

public class MembersPanel extends JPanel {
    // Komponen UI
    private JTextField memberIdField, nameField, contactField, addressField, membershipTypeField;
    private JButton addButton, saveButton, cancelButton, deleteButton; // Delete = soft delete
    private JTable membersTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton searchButton;
    private JLabel formTitleLabel;

    // DAO
    private MemberDAO memberDAO;
    private Member selectedMemberForEdit = null;
    private String loggedInAdminUsername;

    public MembersPanel(String adminUsername) {
        this.loggedInAdminUsername = adminUsername;
        this.memberDAO = new MemberDAO();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        setBackground(Color.WHITE);

        initComponents();
        layoutComponents();
        addEventListeners();

        loadMembersToTable();
        switchToAddingMode();
    }

    private void initComponents() {
        formTitleLabel = new JLabel("Add New Member");
        formTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        formTitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        memberIdField = new JTextField(15);
        nameField = new JTextField(20);
        contactField = new JTextField(15); // Untuk email atau phone
        addressField = new JTextField(25); // Untuk alamat
        membershipTypeField = new JTextField(15); // Untuk tipe keanggotaan

        Font fieldFont = new Font("SansSerif", Font.PLAIN, 13);
        memberIdField.setFont(fieldFont);
        nameField.setFont(fieldFont);
        contactField.setFont(fieldFont);
        addressField.setFont(fieldFont);
        membershipTypeField.setFont(fieldFont);

        addButton = new JButton("Add Member");
        styleButton(addButton, new Color(40, 167, 69));
        saveButton = new JButton("Save Changes");
        styleButton(saveButton, new Color(0, 123, 255));
        cancelButton = new JButton("Cancel");
        styleButton(cancelButton, new Color(108, 117, 125));
        deleteButton = new JButton("Delete Member");
        styleButton(deleteButton, new Color(220, 53, 69));

        searchField = new JTextField(25);
        searchField.setFont(fieldFont);
        searchButton = new JButton("Search");
        styleButton(searchButton, new Color(0, 123, 255));

        String[] columnNames = {"ID DB", "Member ID", "Name", "Contact", "Address", "Membership", "Registered"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        membersTable = new JTable(tableModel);
        membersTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        membersTable.setRowHeight(25);
        membersTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        membersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableColumnModel tcm = membersTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(50);  // ID Internal DB
        tcm.getColumn(1).setPreferredWidth(100); // Member ID Text
        tcm.getColumn(2).setPreferredWidth(200); // Name
        tcm.getColumn(3).setPreferredWidth(150); // Contact
        tcm.getColumn(4).setPreferredWidth(250); // Address
        tcm.getColumn(5).setPreferredWidth(100); // Membership Type
        tcm.getColumn(6).setPreferredWidth(150); // Created At
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(Math.max(button.getPreferredSize().width, 130), 30));
    }

    private void layoutComponents() {
        // Panel Form (Kiri)
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Member Details"),
            BorderFactory.createEmptyBorder(10,10,10,10)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; formPanel.add(formTitleLabel, gbc); gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Member ID:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; formPanel.add(memberIdField, gbc); gbc.weightx = 0.0;

        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; formPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; formPanel.add(new JLabel("Contact (Email/Phone):"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; formPanel.add(contactField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; formPanel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; formPanel.add(addressField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5; formPanel.add(new JLabel("Membership Type:"), gbc);
        gbc.gridx = 1; gbc.gridy = 5; formPanel.add(membershipTypeField, gbc);


        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionButtonPanel.setOpaque(false);
        actionButtonPanel.add(addButton);
        actionButtonPanel.add(saveButton);
        actionButtonPanel.add(cancelButton);
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(actionButtonPanel, gbc);
        gbc.anchor = GridBagConstraints.WEST;

        // Panel Kanan (Search dan Tabel)
        JPanel rightPanel = new JPanel(new BorderLayout(10,10));
        rightPanel.setOpaque(false);

        JPanel searchAreaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchAreaPanel.setOpaque(false);
        searchAreaPanel.add(new JLabel("Search Member:"));
        searchAreaPanel.add(searchField);
        searchAreaPanel.add(searchButton);
        searchAreaPanel.add(Box.createHorizontalStrut(20));
        searchAreaPanel.add(deleteButton);
        
        rightPanel.add(searchAreaPanel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(membersTable), BorderLayout.CENTER);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, formPanel, rightPanel);
        splitPane.setDividerLocation(450); // Lebar form panel
        splitPane.setOpaque(false);
        splitPane.setBackground(Color.WHITE);
        
        add(splitPane, BorderLayout.CENTER);
    }

    private void addEventListeners() {
        addButton.addActionListener(e -> handleAddMember());
        saveButton.addActionListener(e -> handleSaveMember());
        cancelButton.addActionListener(e -> switchToAddingMode());
        deleteButton.addActionListener(e -> handleDeleteMember());
        searchButton.addActionListener(e -> loadMembersToTable());
        searchField.addActionListener(e -> loadMembersToTable()); // Search on Enter

        membersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && membersTable.getSelectedRow() != -1) {
                populateFormFromSelectedRow();
            } else if (membersTable.getSelectedRow() == -1){
                deleteButton.setEnabled(false);
            }
        });
    }

    private void loadMembersToTable() {
        String searchTerm = searchField.getText().trim();
        tableModel.setRowCount(0);
        List<Member> members = memberDAO.getAllMembers(searchTerm); // DAO sudah menangani filter
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");

        for (Member member : members) {
            Vector<Object> row = new Vector<>();
            row.add(member.getId());
            row.add(member.getMemberIdText());
            row.add(member.getName());
            row.add(member.getContact());
            row.add(member.getAddress());
            row.add(member.getMembershipType());
            row.add(member.getCreatedAt() != null ? sdf.format(new Date(member.getCreatedAt().getTime())) : "N/A");
            tableModel.addRow(row);
        }
        if (membersTable.getSelectedRow() == -1) {
             deleteButton.setEnabled(false);
        }
    }

    private void clearForm() {
        memberIdField.setText("");
        nameField.setText("");
        contactField.setText("");
        addressField.setText("");
        membershipTypeField.setText("");
        selectedMemberForEdit = null;
        membersTable.clearSelection();
    }

    private void switchToAddingMode() {
        formTitleLabel.setText("Add New Member");
        clearForm();
        addButton.setVisible(true);
        saveButton.setVisible(false);
        cancelButton.setVisible(false);
        deleteButton.setEnabled(false);
        memberIdField.setEditable(true); // Member ID bisa diisi saat menambah
    }

    private void switchToEditingMode(Member member) {
        selectedMemberForEdit = member;
        formTitleLabel.setText("Edit Member: " + member.getName());
        memberIdField.setText(member.getMemberIdText());
        memberIdField.setEditable(false); // Member ID tidak boleh diubah
        nameField.setText(member.getName());
        contactField.setText(member.getContact());
        addressField.setText(member.getAddress());
        membershipTypeField.setText(member.getMembershipType());

        addButton.setVisible(false);
        saveButton.setVisible(true);
        cancelButton.setVisible(true);
        deleteButton.setEnabled(true);
    }

    private void populateFormFromSelectedRow() {
        int selectedRow = membersTable.getSelectedRow();
        if (selectedRow != -1) {
            int memberInternalId = (int) tableModel.getValueAt(selectedRow, 0); // Ambil ID internal dari tabel
            Member member = memberDAO.getMemberByInternalId(memberInternalId); 
            if (member != null && member.getDeletedAt() == null) {
                switchToEditingMode(member);
            } else if (member == null){
                 JOptionPane.showMessageDialog(this, "Member details not found.", "Error", JOptionPane.ERROR_MESSAGE);
                 loadMembersToTable();
            } else {
                 JOptionPane.showMessageDialog(this, "This member is in Recycle Bin.", "Info", JOptionPane.INFORMATION_MESSAGE);
                 clearForm();
                 switchToAddingMode();
            }
        }
    }

    private boolean validateInput() {
        if (memberIdField.getText().trim().isEmpty() && addButton.isVisible()) { // Hanya validasi saat tambah baru
            JOptionPane.showMessageDialog(this, "Member ID cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // Validasi lain bisa ditambahkan (format email/telepon, dll.)
        return true;
    }

    private void handleAddMember() {
        if (!validateInput()) return;

        String memberIdText = memberIdField.getText().trim();
        String name = nameField.getText().trim();
        String contact = contactField.getText().trim();
        String address = addressField.getText().trim();
        String membershipType = membershipTypeField.getText().trim();

        if (memberDAO.memberIdTextExists(memberIdText)) {
            JOptionPane.showMessageDialog(this, "Member ID '" + memberIdText + "' already exists.", "Duplicate Member ID", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Member newMember = new Member(memberIdText, name, contact, address, membershipType);
        if (memberDAO.addMember(newMember)) {
            JOptionPane.showMessageDialog(this, "Member added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadMembersToTable();
            clearForm(); // Tetap di mode tambah
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add member.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSaveMember() {
        if (selectedMemberForEdit == null) {
            JOptionPane.showMessageDialog(this, "No member selected for update.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!validateInput()) return; // Validasi nama, kontak dll. (Member ID tidak divalidasi karena tidak diedit)

        selectedMemberForEdit.setName(nameField.getText().trim());
        selectedMemberForEdit.setContact(contactField.getText().trim());
        selectedMemberForEdit.setAddress(addressField.getText().trim());
        selectedMemberForEdit.setMembershipType(membershipTypeField.getText().trim());
        // memberIdText dan ID internal tidak diubah

        if (memberDAO.updateMember(selectedMemberForEdit)) {
            JOptionPane.showMessageDialog(this, "Member updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadMembersToTable();
            switchToAddingMode();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update member.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteMember() { // Soft Delete
        int selectedRow = membersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a member to delete.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int memberInternalId = (int) tableModel.getValueAt(selectedRow, 0);
        String memberName = (String) tableModel.getValueAt(selectedRow, 2);

        // Perlu cek apakah anggota masih punya buku yang dipinjam (dari tabel transactions)
        // Untuk sekarang, kita sederhanakan dulu. Fitur validasi transaksi nanti.
        // boolean hasActiveTransactions = transactionDAO.hasActiveTransactions(memberInternalId);
        // if (hasActiveTransactions) {
        //     JOptionPane.showMessageDialog(this, "Member '" + memberName + "' cannot be deleted, has active borrowings.", "Deletion Denied", JOptionPane.ERROR_MESSAGE);
        //     return;
        // }

        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to move member '" + memberName + "' to Recycle Bin?", 
            "Confirm Soft Deletion", 
            JOptionPane.YES_NO_OPTION, 
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (memberDAO.softDeleteMember(memberInternalId, loggedInAdminUsername)) {
                JOptionPane.showMessageDialog(this, "Member '" + memberName + "' moved to Recycle Bin.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadMembersToTable();
                switchToAddingMode();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to move member to Recycle Bin.", "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}