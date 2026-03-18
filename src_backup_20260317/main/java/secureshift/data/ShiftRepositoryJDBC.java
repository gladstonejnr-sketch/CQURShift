package secureshift.data;

import secureshift.domain.Guard;
import secureshift.domain.Shift;
import secureshift.domain.Site;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of ShiftRepository.
 * Uses MySQL database for persistence.
 */
public class ShiftRepositoryJDBC {

    private final GuardRepositoryJDBC guardRepository = new GuardRepositoryJDBC();
    private final SiteRepositoryJDBC siteRepository = new SiteRepositoryJDBC();

    private static final String BASE_SELECT =
            "SELECT s.*, site.name as site_name, site.address, " +
                    "g.name as guard_name " +
                    "FROM shifts s " +
                    "INNER JOIN sites site ON s.site_id = site.id " +
                    "LEFT JOIN guards g ON s.guard_id = g.id ";

    // ✅ Load all shifts
    public List<Shift> loadAllShifts() {
        List<Shift> shifts = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY s.start_time";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                shifts.add(mapResultSetToShift(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error loading shifts: " + e.getMessage());
            e.printStackTrace();
        }

        return shifts;
    }

    // ✅ Find shift by ID
    public Optional<Shift> findById(int id) {
        String sql = BASE_SELECT + "WHERE s.id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToShift(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error finding shift by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    // ✅ Find shifts by guard ID
    public List<Shift> findByGuardId(String guardId) {
        List<Shift> shifts = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE g.id = ? ORDER BY s.start_time";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, guardId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                shifts.add(mapResultSetToShift(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error finding shifts by guard: " + e.getMessage());
            e.printStackTrace();
        }

        return shifts;
    }

    // ✅ Find shifts by site ID
    public List<Shift> findBySiteId(int siteId) {
        List<Shift> shifts = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE s.site_id = ? ORDER BY s.start_time";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, siteId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                shifts.add(mapResultSetToShift(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error finding shifts by site: " + e.getMessage());
            e.printStackTrace();
        }

        return shifts;
    }

    // ✅ Load all unassigned shifts
    public List<Shift> loadUnassignedShifts() {
        List<Shift> shifts = new ArrayList<>();
        String sql = "SELECT s.*, site.name as site_name, site.address " +
                "FROM shifts s " +
                "INNER JOIN sites site ON s.site_id = site.id " +
                "WHERE s.guard_id IS NULL " +
                "ORDER BY s.start_time";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                shifts.add(mapResultSetToShift(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error finding unassigned shifts: " + e.getMessage());
            e.printStackTrace();
        }

        return shifts;
    }

    // ✅ Save shift - insert or update
    public void saveShift(Shift shift) {
        if (shift.getId() == null || shift.getId().isEmpty()) {
            insertShift(shift);
        } else {
            updateShift(shift);
        }
    }

    // ✅ Insert a new shift
    private void insertShift(Shift shift) {
        String sql = "INSERT INTO shifts (guard_id, site_id, start_time, end_time, status) " +
                "VALUES (?, ?, ?, ?, 'SCHEDULED')";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            // ✅ Fixed - getAssignedGuard() not getGuard()
            if (shift.getAssignedGuard() != null) {
                stmt.setString(1, shift.getAssignedGuard().getId());
            } else {
                stmt.setNull(1, Types.VARCHAR);
            }

            stmt.setInt(2, shift.getSite().getId());
            stmt.setTimestamp(3, Timestamp.valueOf(shift.getStartTime()));
            stmt.setTimestamp(4, Timestamp.valueOf(shift.getEndTime()));
            stmt.executeUpdate();

            // ✅ Fixed - setId takes String
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                shift.setId(String.valueOf(generatedKeys.getInt(1)));
            }

            System.out.println("✅ Shift inserted: " + shift.getId());

        } catch (SQLException e) {
            System.err.println("❌ Error inserting shift: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Update an existing shift
    public void updateShift(Shift shift) {
        String sql = "UPDATE shifts SET guard_id = ?, site_id = ?, " +
                "start_time = ?, end_time = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // ✅ Fixed - getAssignedGuard() not getGuard()
            if (shift.getAssignedGuard() != null) {
                stmt.setString(1, shift.getAssignedGuard().getId());
            } else {
                stmt.setNull(1, Types.VARCHAR);
            }

            stmt.setInt(2, shift.getSite().getId());
            stmt.setTimestamp(3, Timestamp.valueOf(shift.getStartTime()));
            stmt.setTimestamp(4, Timestamp.valueOf(shift.getEndTime()));
            // ✅ Fixed - id is String
            stmt.setString(5, shift.getId());
            stmt.executeUpdate();

            System.out.println("✅ Shift updated: " + shift.getId());

        } catch (SQLException e) {
            System.err.println("❌ Error updating shift: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Update guard assignment only
    public void updateAssignment(Shift shift) {
        String sql = "UPDATE shifts SET guard_id = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // ✅ Fixed - getAssignedGuard() not getGuard()
            if (shift.getAssignedGuard() != null) {
                stmt.setString(1, shift.getAssignedGuard().getId());
            } else {
                stmt.setNull(1, Types.VARCHAR);
            }

            // ✅ Fixed - id is String
            stmt.setString(2, shift.getId());
            stmt.executeUpdate();

            System.out.println("✅ Assignment updated for shift: " + shift.getId());

        } catch (SQLException e) {
            System.err.println("❌ Error updating assignment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Delete shift by ID
    public void deleteShift(String id) {
        String sql = "DELETE FROM shifts WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            stmt.executeUpdate();
            System.out.println("✅ Shift deleted: " + id);

        } catch (SQLException e) {
            System.err.println("❌ Error deleting shift: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Check if shift exists
    public boolean exists(String id) {
        String sql = "SELECT 1 FROM shifts WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.err.println("❌ Error checking shift existence: " + e.getMessage());
            return false;
        }
    }

    // ✅ Count total shifts
    public int count() {
        String sql = "SELECT COUNT(*) FROM shifts";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error counting shifts: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    // ✅ Map ResultSet row to Shift object
    private Shift mapResultSetToShift(ResultSet rs) throws SQLException {
        Shift shift = new Shift();

        // ✅ Fixed - id is String
        shift.setId(String.valueOf(rs.getInt("id")));

        // ✅ Map site
        Site site = new Site();
        site.setId(rs.getInt("site_id"));
        site.setName(rs.getString("site_name"));
        site.setAddress(rs.getString("address"));
        shift.setSite(site);

        // ✅ Map guard if present - guard_id is VARCHAR, use getString
        String guardId = rs.getString("guard_id");
        if (guardId != null) {
            Guard guard = new Guard();
            guard.setId(guardId);
            guard.setName(rs.getString("guard_name"));
            shift.setGuard(guard);
        }

        shift.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        shift.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());

        return shift;
    }
}
