/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package config;

/**
 *
 * @author andi.ikhlass
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;

public class DatabaseConnection {
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3308";
    private static final String DB_NAME = "perpustakaan"; // Pastikan database ini sudah Anda buat
    private static final String DB_USER = "root";      // Ganti dengan user Anda
    private static final String DB_PASSWORD = ""; // GANTI DENGAN PASSWORD ANDA

    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME +
                                         "?serverTimezone=Asia/Makassar&useSSL=false&allowPublicKeyRetrieval=true";

    private static Connection staticConnection;

    public static Connection getConnection() {
        try {
            if (staticConnection == null || staticConnection.isClosed()) {
                System.out.println("Mencoba membuat koneksi baru ke: " + DB_URL);
                staticConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                System.out.println("Koneksi ke MySQL (database " + DB_NAME + ") berhasil.");
                // Panggil inisialisasi admin jika diperlukan (misalnya, melalui AdminDAO)
                // ensureDefaultAdminExists(staticConnection); 
            }
        } catch (SQLException e) {
            System.err.println("Error koneksi database MySQL: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Tidak dapat terhubung ke database MySQL.\nPastikan server MySQL berjalan dan konfigurasi sudah benar.\nDetail: " + e.getMessage(),
                    "Error Database Kritis", JOptionPane.ERROR_MESSAGE);
            staticConnection = null;
        }
        return staticConnection;
    }

    public static void closeConnection() {
        if (staticConnection != null) {
            try {
                if (!staticConnection.isClosed()) {
                    staticConnection.close();
                    System.out.println("Koneksi database MySQL ditutup.");
                }
                staticConnection = null;
            } catch (SQLException e) {
                System.err.println("Error saat menutup koneksi MySQL: " + e.getMessage());
            }
        }
    }
}