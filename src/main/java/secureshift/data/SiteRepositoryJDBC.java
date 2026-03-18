package secureshift.data;

import secureshift.domain.Site;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of SiteRepository.
 * Uses MySQL database for persistence.
 */
public class SiteRepositoryJDBC {

    // ✅ Reusable base SQL query
    private static final String BASE_SELECT = "SELECT * FROM sites ";

    // ✅ Load all sites
    public List<Site> loadAllSites() {
        List<Site> sites = new ArrayList<>();
        String sql = BASE_SELECT + "ORDER BY name";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                sites.add(mapResultSetToSite(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error loading sites: " + e.getMessage());
            e.printStackTrace();
        }

        return sites;
    }

    // ✅ Find site by ID
    public Optional<Site> findById(int id) {
        String sql = BASE_SELECT + "WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToSite(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error finding site by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    // ✅ Find site by name
    public Optional<Site> findByName(String name) {
        String sql = BASE_SELECT + "WHERE name = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToSite(rs));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error finding site by name: " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    // ✅ Load all regions
    public List<String> loadAllRegions() {
        List<String> regions = new ArrayList<>();
        String sql = "SELECT DISTINCT region FROM sites WHERE region IS NOT NULL ORDER BY region";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                regions.add(rs.getString("region"));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error loading regions: " + e.getMessage());
            e.printStackTrace();
        }

        return regions;
    }

    // ✅ Load all skills required across all sites
    public List<String> loadAllSkills() {
        List<String> skills = new ArrayList<>();
        String sql = "SELECT DISTINCT required_skill FROM site_skills ORDER BY required_skill";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                skills.add(rs.getString("required_skill"));
            }

        } catch (SQLException e) {
            System.err.println("❌ Error loading skills: " + e.getMessage());
            e.printStackTrace();
        }

        return skills;
    }

    // ✅ Save site - insert or update
    public void saveSite(Site site) {
        if (site.getId() == 0) {
            insertSite(site);
        } else {
            updateSite(site);
        }
    }

    // ✅ Insert a new site
    private void insertSite(Site site) {
        String sql = "INSERT INTO sites (name, address, region, latitude, longitude) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, site.getName());
            stmt.setString(2, site.getAddress());
            stmt.setString(3, site.getRegion());
            stmt.setDouble(4, site.getLatitude());
            stmt.setDouble(5, site.getLongitude());
            stmt.executeUpdate();

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                site.setId(generatedKeys.getInt(1));
            }

            System.out.println("✅ Site inserted: " + site.getName());

        } catch (SQLException e) {
            System.err.println("❌ Error inserting site: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Update an existing site
    public void updateSite(Site site) {
        String sql = "UPDATE sites SET name = ?, address = ?, " +
                "region = ?, latitude = ?, longitude = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, site.getName());
            stmt.setString(2, site.getAddress());
            stmt.setString(3, site.getRegion());
            stmt.setDouble(4, site.getLatitude());
            stmt.setDouble(5, site.getLongitude());
            stmt.setInt(6, site.getId());
            stmt.executeUpdate();

            System.out.println("✅ Site updated: " + site.getName());

        } catch (SQLException e) {
            System.err.println("❌ Error updating site: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Delete site by ID
    public void deleteSite(int id) {
        String sql = "DELETE FROM sites WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
            System.out.println("✅ Site deleted: " + id);

        } catch (SQLException e) {
            System.err.println("❌ Error deleting site: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Check if site exists
    public boolean exists(int id) {
        String sql = "SELECT 1 FROM sites WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.err.println("❌ Error checking site existence: " + e.getMessage());
            return false;
        }
    }

    // ✅ Count total sites
    public int count() {
        String sql = "SELECT COUNT(*) FROM sites";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error counting sites: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    // ✅ Map ResultSet row to Site object
    private Site mapResultSetToSite(ResultSet rs) throws SQLException {
        Site site = new Site();
        site.setId(rs.getInt("id"));
        site.setName(rs.getString("name"));
        site.setAddress(rs.getString("address"));
        site.setRegion(rs.getString("region"));
        site.setLatitude(rs.getDouble("latitude"));
        site.setLongitude(rs.getDouble("longitude"));
        return site;
    }
}