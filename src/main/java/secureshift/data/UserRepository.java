package secureshift.data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Optional;

public class UserRepository {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE, password VARCHAR(64) NOT NULL, role VARCHAR(20) NOT NULL DEFAULT 'MANAGER', created TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

    public void initialise() {
        try (Connection c = DatabaseConfig.getConnection(); Statement s = c.createStatement()) {
            s.execute(CREATE_TABLE);
            seedDefaults(c);
        } catch (Exception e) { System.err.println("❌ UserRepository init failed: " + e.getMessage()); }
    }

    public Optional<String> authenticate(String username, String password) {
        String hashed = sha256(password);
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT role FROM users WHERE username = ? AND password = ?")) {
            ps.setString(1, username.trim().toLowerCase());
            ps.setString(2, hashed);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(rs.getString("role"));
        } catch (Exception e) { System.err.println("❌ Auth error: " + e.getMessage()); }
        return Optional.empty();
    }

    private void seedDefaults(Connection c) throws SQLException {
        insertIfAbsent(c, "admin", "admin123", "ADMIN");
        insertIfAbsent(c, "manager", "manager123", "MANAGER");
    }

    private void insertIfAbsent(Connection c, String username, String password, String role) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            ps.setString(1, username);
            if (ps.executeQuery().next()) return;
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO users (username, password, role) VALUES (?, ?, ?)")) {
            ps.setString(1, username); ps.setString(2, sha256(password)); ps.setString(3, role);
            ps.executeUpdate();
            System.out.println("✅ Default user created: " + username + " [" + role + "]");
        }
    }

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException("SHA-256 not available", e); }
    }
}
