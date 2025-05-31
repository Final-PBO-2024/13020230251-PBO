// File: src/ui/MainAppView.java
package ui;

import config.DatabaseConnection; 
import dao.BookDAO;
import dao.MemberDAO;
import dao.TransactionDAO;
import models.ActivityLogItem; 

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.text.SimpleDateFormat; 
import java.util.Date; 
import java.util.HashMap;
import java.util.List; 
import java.util.Map;
import java.util.Vector; 

public class MainAppView extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // Panel untuk setiap fitur
    private JPanel dashboardPanel;
    private BooksPanel actualBooksPanel;
    private MembersPanel actualMembersPanel;
    private TransactionsPanel actualTransactionsPanel;
    private RecycleBinPanel actualRecycleBinPanel;
    // HAPUS: private JPanel categoriesPanelPlaceholder; 

    private JLabel currentAdminLabel;
    private String loggedInAdminUsername;
    private int loggedInAdminId;
    
    private Map<String, JButton> navigationButtons = new HashMap<>();

    public MainAppView(String adminUsername, int adminId) {
        this.loggedInAdminUsername = adminUsername;
        this.loggedInAdminId = adminId; 

        setTitle("Library Management System - Admin: " + loggedInAdminUsername);
        setSize(1200, 800); 
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                handleLogout();
            }
        });

        setupTopBar();

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); 
        mainPanel.setBackground(Color.WHITE);

        // Inisialisasi panel-panel
        dashboardPanel = createDashboardPanel(); 
        actualBooksPanel = new BooksPanel(this.loggedInAdminUsername); 
        actualMembersPanel = new MembersPanel(this.loggedInAdminUsername);
        actualTransactionsPanel = new TransactionsPanel(this.loggedInAdminUsername);
        actualRecycleBinPanel = new RecycleBinPanel(this.loggedInAdminUsername); 
        
        // HAPUS: categoriesPanelPlaceholder = createPlaceholderPanel("Manajemen Kategori", "Fitur ini telah dihapus.", "image_fd33eb.png");
        
        mainPanel.add(dashboardPanel, "Dashboard");
        mainPanel.add(actualBooksPanel, "Books"); 
        mainPanel.add(actualMembersPanel, "Members"); 
        mainPanel.add(actualTransactionsPanel, "Transactions"); 
        mainPanel.add(actualRecycleBinPanel, "Recycle Bin"); 
        // HAPUS: mainPanel.add(categoriesPanelPlaceholder, "Categories"); 

        add(mainPanel, BorderLayout.CENTER);

        cardLayout.show(mainPanel, "Dashboard");
        if (navigationButtons.containsKey("Dashboard")) {
            setActiveButton(navigationButtons.get("Dashboard"));
        }
    }

    private void setupTopBar() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(70, 130, 180)); 
        headerPanel.setPreferredSize(new Dimension(getWidth(), 60));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20)); 

        JLabel headerTitle = new JLabel("Library Management System");
        headerTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        headerTitle.setForeground(Color.WHITE);
        headerPanel.add(headerTitle, BorderLayout.WEST);

        JPanel rightMenuPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightMenuPanel.setOpaque(false); 
        rightMenuPanel.setBorder(new EmptyBorder(15,0,0,0)); 

        currentAdminLabel = new JLabel("Admin: " + loggedInAdminUsername);
        currentAdminLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        currentAdminLabel.setForeground(Color.WHITE);

        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        logoutButton.setBackground(new Color(220, 53, 69)); 
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.setPreferredSize(new Dimension(90, 30));
        logoutButton.addActionListener(e -> handleLogout());

        rightMenuPanel.add(currentAdminLabel);
        rightMenuPanel.add(logoutButton);
        headerPanel.add(rightMenuPanel, BorderLayout.EAST);

        JPanel navigationButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        navigationButtonPanel.setBackground(new Color(248, 249, 250)); 
        navigationButtonPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(222, 226, 230))); 

        // ***** PERUBAHAN DI SINI: "Categories" dihapus dari menuItems *****
        String[] menuItems = {"Dashboard", "Books", "Members", "Transactions", "Recycle Bin"};
        navigationButtons.clear(); 

        for (String itemName : menuItems) {
            JButton menuButton = new JButton(itemName);
            styleMenuButton(menuButton, itemName.equals("Dashboard"));             
            navigationButtons.put(itemName, menuButton);

            menuButton.addActionListener(e -> {
                setActiveButton(menuButton);
                if ("Dashboard".equals(itemName) && dashboardPanel != null) { 
                    System.out.println("DEBUG: Refreshing Dashboard Panel..."); 
                    mainPanel.remove(dashboardPanel); 
                    dashboardPanel = createDashboardPanel(); 
                    mainPanel.add(dashboardPanel, "Dashboard"); 
                    mainPanel.revalidate(); 
                    mainPanel.repaint();    
                }
                cardLayout.show(mainPanel, itemName); 
            });
            navigationButtonPanel.add(menuButton);
        }

        JPanel topBarPanel = new JPanel(new BorderLayout());
        topBarPanel.add(headerPanel, BorderLayout.NORTH);
        topBarPanel.add(navigationButtonPanel, BorderLayout.CENTER);

        add(topBarPanel, BorderLayout.NORTH);
    }
    
    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Apakah Anda yakin ingin logout dan keluar dari aplikasi?",
                "Konfirmasi Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            DatabaseConnection.closeConnection(); 
            this.dispose(); 
            new LoginView().setVisible(true); 
        }
    }

    private void styleMenuButton(JButton button, boolean isActive) {
        button.setFont(new Font("SansSerif", Font.PLAIN, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20)); 
        button.setContentAreaFilled(false);
        button.setOpaque(true); 
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (isActive) {
            button.setBackground(new Color(220, 220, 240)); 
            button.setForeground(Color.BLACK);
        } else {
            button.setBackground(new Color(248, 249, 250)); 
            button.setForeground(new Color(70, 70, 70)); 
        }

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!button.getBackground().equals(new Color(220, 220, 240))) { 
                     button.setBackground(new Color(230, 235, 240)); 
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                 if (!button.getBackground().equals(new Color(220, 220, 240))) { 
                    button.setBackground(new Color(248, 249, 250)); 
                 }
            }
        });
    }

    private void setActiveButton(JButton activeButton) {
        for (JButton btn : navigationButtons.values()) {
            if (btn == activeButton) {
                btn.setBackground(new Color(220, 220, 240)); 
                btn.setForeground(Color.BLACK);
            } else {
                btn.setBackground(new Color(248, 249, 250)); 
                btn.setForeground(new Color(70, 70, 70));
            }
        }
    }

    private JPanel createPlaceholderPanel(String title, String description, String imageRef) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        panel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title, SwingConstants.LEFT);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0)); 
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JTextArea descriptionArea = new JTextArea(description + "\n\n(UI berdasarkan referensi: " + imageRef + ")");
        descriptionArea.setFont(new Font("SansSerif", Font.PLAIN, 16));
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setOpaque(false);
        descriptionArea.setEditable(false);
        descriptionArea.setFocusable(false);
        panel.add(new JScrollPane(descriptionArea), BorderLayout.CENTER); 
        
        return panel;
    }
    
    private JPanel createDashboardPanel() {
        System.out.println("DEBUG: createDashboardPanel() dipanggil."); 
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        panel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("Selamat Datang, " + loggedInAdminUsername + "!", SwingConstants.LEFT);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0,0,20,0));
        panel.add(titleLabel, BorderLayout.NORTH);
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS)); 
        contentPanel.setBackground(Color.WHITE);

        BookDAO bookDAO_dash = new BookDAO(); 
        MemberDAO memberDAO_dash = new MemberDAO();
        TransactionDAO transactionDAO_dash = new TransactionDAO();

        System.out.println("DEBUG: Mengambil statistik..."); 
        int totalBooks = bookDAO_dash.getTotalActiveBooksCount();
        int borrowedBooks = transactionDAO_dash.getCurrentlyBorrowedBooksCount();
        int deletedItems = bookDAO_dash.getTotalSoftDeletedBooksCount() + memberDAO_dash.getTotalSoftDeletedMembersCount();
        System.out.println("DEBUG: Statistik: Books=" + totalBooks + ", Borrowed=" + borrowedBooks + ", Deleted=" + deletedItems); 

        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 0)); 
        summaryPanel.setBackground(Color.WHITE);
        summaryPanel.add(createSummaryBox("Jumlah Buku", String.valueOf(totalBooks), "\uD83D\uDCDA", new Color(23, 162, 184)));  
        summaryPanel.add(createSummaryBox("Buku Dipinjam", String.valueOf(borrowedBooks), "\uD83D\uDCD6", new Color(255, 193, 7)));  
        summaryPanel.add(createSummaryBox("Item Dihapus", String.valueOf(deletedItems), "\uD83D\uDDD1\uFE0F", new Color(108, 117, 125))); 
        
        contentPanel.add(summaryPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0,25))); 
        JLabel activityLabel = new JLabel("Aktivitas Terkini");
        activityLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        contentPanel.add(activityLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0,10))); 
        
        String[] columnNamesActivity = {"Aktivitas", "Detail", "Tanggal", "Pengguna", "Status"};
        DefaultTableModel activityTableModel = new DefaultTableModel(null, columnNamesActivity) {
            @Override public boolean isCellEditable(int r, int c){ return false; }
        };
        JTable activityTable = new JTable(activityTableModel); 
        
        activityTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        activityTable.setRowHeight(22);
        activityTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        TableColumnModel activityTCM = activityTable.getColumnModel();
        activityTCM.getColumn(0).setPreferredWidth(200); 
        activityTCM.getColumn(1).setPreferredWidth(300); 
        activityTCM.getColumn(2).setPreferredWidth(150); 
        activityTCM.getColumn(3).setPreferredWidth(150); 
        activityTCM.getColumn(4).setPreferredWidth(100); 

        System.out.println("DEBUG: Mengambil aktivitas terkini..."); 
        List<ActivityLogItem> recentActivities = transactionDAO_dash.getRecentActivities(5); 
        System.out.println("DEBUG: Jumlah aktivitas terkini: " + recentActivities.size()); 
        SimpleDateFormat sdfDashboard = new SimpleDateFormat("dd MMM yy, HH:mm");

        if (recentActivities.isEmpty()) {
            Object[] placeholderRowData = new Object[]{"Tidak ada aktivitas terkini.", "", "", "", ""}; 
            System.out.println("DEBUG: Menambah placeholder row ke tabel aktivitas: " + java.util.Arrays.toString(placeholderRowData));
            activityTableModel.addRow(placeholderRowData);
        } else {
            for (ActivityLogItem item : recentActivities) { 
                Vector<Object> rowData = new Vector<>();
                rowData.add(item.getActivityType());    
                rowData.add(item.getDetails());         
                rowData.add(item.getActivityDate() != null ? sdfDashboard.format(new Date(item.getActivityDate().getTime())) : "N/A"); 
                rowData.add(item.getActor());           
                rowData.add(item.getStatus());          
                System.out.println("DEBUG: Menambah row ke tabel aktivitas: " + rowData + " (size: " + rowData.size() + ")");
                if(rowData.size() == activityTableModel.getColumnCount()){ 
                    activityTableModel.addRow(rowData); 
                } else {
                     System.err.println("WARNING: Ukuran rowData dari ActivityLogItem tidak cocok! Row: " + rowData);
                }
            }
        }
        System.out.println("DEBUG: activityTableModel row count: " + activityTableModel.getRowCount()); 
        System.out.println("DEBUG: activityTableModel column count: " + activityTableModel.getColumnCount()); 
        
        JScrollPane scrollPane = new JScrollPane(activityTable);
        scrollPane.setPreferredSize(new Dimension(getWidth() - 60, 150));
        contentPanel.add(scrollPane);
        panel.add(contentPanel, BorderLayout.CENTER);
        System.out.println("DEBUG: createDashboardPanel() selesai."); 
        return panel;
    }    
    
    private JPanel createSummaryBox(String title, String value, String iconText, Color borderColor) {
        JPanel box = new JPanel(new BorderLayout(10,0)); 
        box.setPreferredSize(new Dimension(220, 100)); 
        box.setBackground(Color.WHITE);
        Border line = BorderFactory.createMatteBorder(0, 5, 0, 0, borderColor); 
        Border padding = BorderFactory.createEmptyBorder(15, 20, 15, 20); 
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 226, 230)), 
            BorderFactory.createCompoundBorder(line, padding)
        ));
        JLabel valueLabel = new JLabel(value, SwingConstants.LEFT);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        JLabel titleLabelForBox = new JLabel(title.toUpperCase(), SwingConstants.LEFT);
        titleLabelForBox.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabelForBox.setForeground(Color.GRAY);
        JPanel textPanel = new JPanel(new GridLayout(2,1));
        textPanel.setOpaque(false);
        textPanel.add(valueLabel); textPanel.add(titleLabelForBox);
        JLabel iconLabel = new JLabel(iconText);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36)); 
        iconLabel.setForeground(borderColor); 
        box.add(textPanel, BorderLayout.CENTER); box.add(iconLabel, BorderLayout.EAST);
        return box;
    }
}
