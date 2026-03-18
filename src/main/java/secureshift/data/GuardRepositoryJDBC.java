package secureshift.data;

// ✅ Add this import - fixes all 10 errors
import secureshift.domain.Guard;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of GuardRepository.
 * Uses MySQL database for persistence.
 */
public class GuardRepositoryJDBC {

    // ✅ Reusable base SQL query
    private static final String BASE_SELECT = "SELECT * FROM guards ";

    // ✅ Load all guards
    public List<Guard> loadAllGuards() {
        List<Guard> guards = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY name";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                guards.add(mapResultSetToGuard(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error loading guards: " + e.getMessage());
            e.printStackTrace();
        }

        return guards;
    }

    // ✅ Find guard by ID
    public Optional<Guard> findById(String id) {
        String sql = BASE_SELECT + "WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToGuard(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error finding guard by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    // ✅ Load only available guards
    public List<Guard> loadAvailableGuards() {
        List<Guard> guards = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE available = true ORDER BY name";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                guards.add(mapResultSetToGuard(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error loading available guards: " + e.getMessage());
            e.printStackTrace();
        }

        return guards;
    }

    // ✅ Save a new guard
    public void saveGuard(Guard guard) {
        String sql = "INSERT INTO guards (id, name, latitude, longitude, available) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, guard.getId());
            stmt.setString(2, guard.getName());
            stmt.setDouble(3, guard.getLatitude());
            stmt.setDouble(4, guard.getLongitude());
            stmt.setBoolean(5, guard.isAvailable());
            stmt.executeUpdate();

            System.out.println("✅ Guard saved: " + guard.getName());

        } catch (SQLException e) {
            System.err.println("❌ Error saving guard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Update an existing guard
    public void updateGuard(Guard guard) {
        String sql = "UPDATE guards SET name = ?, latitude = ?, " +
                "longitude = ?, available = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, guard.getName());
            stmt.setDouble(2, guard.getLatitude());
            stmt.setDouble(3, guard.getLongitude());
            stmt.setBoolean(4, guard.isAvailable());
            stmt.setString(5, guard.getId());
            stmt.executeUpdate();

            System.out.println("✅ Guard updated: " + guard.getName());

        } catch (SQLException e) {
            System.err.println("❌ Error updating guard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Delete a guard by ID
    public void deleteGuard(String id) {
        String sql = "DELETE FROM guards WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            stmt.executeUpdate();

            System.out.println("✅ Guard deleted: " + id);

        } catch (SQLException e) {
            System.err.println("❌ Error deleting guard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Check if a guard exists
    public boolean exists(String id) {
        String sql = "SELECT 1 FROM guards WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.err.println("❌ Error checking guard existence: " + e.getMessage());
            return false;
        }
    }

    // ✅ Count total guards
    public int count() {
        String sql = "SELECT COUNT(*) FROM guards";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error counting guards: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    // ✅ Map ResultSet row to Guard object
    private Guard mapResultSetToGuard(ResultSet rs) throws SQLException {
        Guard guard = new Guard();
        guard.setId(rs.getString("id"));
        guard.setName(rs.getString("name"));
        guard.setLatitude(rs.getDouble("latitude"));
        guard.setLongitude(rs.getDouble("longitude"));
        guard.setAvailable(rs.getBoolean("available"));
        return guard;
    }
}
