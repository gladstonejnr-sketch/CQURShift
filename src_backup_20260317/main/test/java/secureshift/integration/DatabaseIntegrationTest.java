package secureshift.integration;

import org.junit.jupiter.api.*;
import secureshift.data.DatabaseConfig;
import secureshift.data.GuardRepositoryJDBC;
import secureshift.data.SiteRepositoryJDBC;
import secureshift.data.ShiftRepositoryJDBC;
import secureshift.domain.Guard;
import secureshift.domain.Site;
import secureshift.domain.Shift;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that hit the real database.
 * Requires MySQL to be running with secureshift_db configured.
 * Run with: mvn test -Dtest=DatabaseIntegrationTest
 */
@DisplayName("Database Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseIntegrationTest {

    private static GuardRepositoryJDBC guardRepo;
    private static SiteRepositoryJDBC  siteRepo;
    private static ShiftRepositoryJDBC shiftRepo;

    // Use unique test IDs to avoid conflicts
    private static final String TEST_GUARD_ID = "TEST_G_" + System.currentTimeMillis();
    private static final String TEST_GUARD_ID_2 = "TEST_G2_" + System.currentTimeMillis();

    @BeforeAll
    static void setUpAll() {
        guardRepo = new GuardRepositoryJDBC();
        siteRepo  = new SiteRepositoryJDBC();
        shiftRepo = new ShiftRepositoryJDBC();
    }

    // ── Database Connection ─────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Database connection is successful")
    void testDatabaseConnection() {
        assertDoesNotThrow(() -> {
            try (Connection conn = DatabaseConfig.getConnection()) {
                assertNotNull(conn);
                assertFalse(conn.isClosed());
                System.out.println("✅ DB connection OK: " + conn.getMetaData().getURL());
            }
        });
    }

    // ── Guard CRUD ──────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("Can save a guard to the database")
    void testSaveGuard() {
        Guard guard = new Guard(TEST_GUARD_ID, "Test Guard Integration",
                51.5074, -0.1278, true, Arrays.asList("PATROL"));
        assertDoesNotThrow(() -> guardRepo.saveGuard(guard));
        System.out.println("✅ Guard saved: " + TEST_GUARD_ID);
    }

    @Test
    @Order(3)
    @DisplayName("Can load guard by ID")
    void testFindGuardById() {
        var found = guardRepo.findById(TEST_GUARD_ID);
        assertTrue(found.isPresent(), "Guard should exist in DB");
        assertEquals("Test Guard Integration", found.get().getName());
    }

    @Test
    @Order(4)
    @DisplayName("Can load all guards")
    void testLoadAllGuards() {
        List<Guard> guards = guardRepo.loadAllGuards();
        assertNotNull(guards);
        assertTrue(guards.size() >= 1, "Should have at least our test guard");
        boolean found = guards.stream().anyMatch(g -> g.getId().equals(TEST_GUARD_ID));
        assertTrue(found, "Test guard should appear in all guards list");
    }

    @Test
    @Order(5)
    @DisplayName("Can update a guard")
    void testUpdateGuard() {
        var found = guardRepo.findById(TEST_GUARD_ID);
        assertTrue(found.isPresent());

        Guard guard = found.get();
        guard.setName("Updated Guard Name");
        guard.setLatitude(52.0);
        assertDoesNotThrow(() -> guardRepo.updateGuard(guard));

        var updated = guardRepo.findById(TEST_GUARD_ID);
        assertTrue(updated.isPresent());
        assertEquals("Updated Guard Name", updated.get().getName());
    }

    @Test
    @Order(6)
    @DisplayName("guardExists returns true for existing guard")
    void testGuardExists() {
        assertTrue(guardRepo.exists(TEST_GUARD_ID));
        assertFalse(guardRepo.exists("DEFINITELY_NOT_EXISTING_ID_XYZ"));
    }

    @Test
    @Order(7)
    @DisplayName("Can load available guards only")
    void testLoadAvailableGuards() {
        List<Guard> available = guardRepo.loadAvailableGuards();
        assertNotNull(available);
        assertTrue(available.stream().allMatch(Guard::isAvailable));
    }

    // ── Site CRUD ───────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("Can load all sites")
    void testLoadAllSites() {
        List<Site> sites = siteRepo.loadAllSites();
        assertNotNull(sites);
        System.out.println("✅ Sites in DB: " + sites.size());
    }

    // ── Shift CRUD ──────────────────────────────────────────────────

    @Test
    @Order(20)
    @DisplayName("Can load all shifts")
    void testLoadAllShifts() {
        List<Shift> shifts = shiftRepo.loadAllShifts();
        assertNotNull(shifts);
        System.out.println("✅ Shifts in DB: " + shifts.size());
    }

    // ── Cleanup ─────────────────────────────────────────────────────

    @Test
    @Order(99)
    @DisplayName("Cleanup: delete test guard")
    void testDeleteGuard() {
        assertDoesNotThrow(() -> guardRepo.deleteGuard(TEST_GUARD_ID));
        assertFalse(guardRepo.exists(TEST_GUARD_ID), "Guard should be deleted");
        System.out.println("✅ Test guard cleaned up: " + TEST_GUARD_ID);
    }
}
