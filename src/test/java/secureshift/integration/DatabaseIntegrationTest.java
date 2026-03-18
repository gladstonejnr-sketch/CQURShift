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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Database Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseIntegrationTest {

    private static GuardRepositoryJDBC guardRepo;
    private static SiteRepositoryJDBC  siteRepo;
    private static ShiftRepositoryJDBC shiftRepo;
    private static final String TEST_GUARD_ID = "TEST_G_INTEGRATION";

    @BeforeAll
    static void setUpAll() {
        guardRepo = new GuardRepositoryJDBC();
        siteRepo  = new SiteRepositoryJDBC();
        shiftRepo = new ShiftRepositoryJDBC();
    }

    @Test @Order(1) @DisplayName("Database connection is successful")
    void testDatabaseConnection() throws Exception {
        try (Connection conn = DatabaseConfig.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
            System.out.println("Connected to: " + conn.getMetaData().getURL());
        }
    }

    @Test @Order(2) @DisplayName("Can save a guard to the database")
    void testSaveGuard() {
        Guard guard = new Guard(TEST_GUARD_ID, "Test Guard Integration",
                51.5074, -0.1278, true, Arrays.asList("PATROL"));
        assertDoesNotThrow(() -> guardRepo.saveGuard(guard));
        System.out.println("Guard saved: " + TEST_GUARD_ID);
    }

    @Test @Order(3) @DisplayName("Can find guard by ID")
    void testFindGuardById() {
        var found = guardRepo.findById(TEST_GUARD_ID);
        assertTrue(found.isPresent());
        assertEquals("Test Guard Integration", found.get().getName());
        System.out.println("Guard found: " + found.get().getName());
    }

    @Test @Order(4) @DisplayName("Can load all guards")
    void testLoadAllGuards() {
        List<Guard> guards = guardRepo.loadAllGuards();
        assertNotNull(guards);
        assertTrue(guards.stream().anyMatch(g -> g.getId().equals(TEST_GUARD_ID)));
        System.out.println("Total guards in DB: " + guards.size());
    }

    @Test @Order(5) @DisplayName("Can update a guard")
    void testUpdateGuard() {
        Guard guard = guardRepo.findById(TEST_GUARD_ID).orElseThrow();
        guard.setName("Updated Guard Name");
        assertDoesNotThrow(() -> guardRepo.updateGuard(guard));
        assertEquals("Updated Guard Name", guardRepo.findById(TEST_GUARD_ID).orElseThrow().getName());
        System.out.println("Guard updated successfully");
    }

    @Test @Order(6) @DisplayName("guardExists returns correct values")
    void testGuardExists() {
        assertTrue(guardRepo.exists(TEST_GUARD_ID));
        assertFalse(guardRepo.exists("DEFINITELY_NOT_EXISTING_XYZ"));
        System.out.println("exists() works correctly");
    }

    @Test @Order(7) @DisplayName("Can load all sites")
    void testLoadAllSites() {
        List<Site> sites = siteRepo.loadAllSites();
        assertNotNull(sites);
        System.out.println("Total sites in DB: " + sites.size());
    }

    @Test @Order(8) @DisplayName("Can load all shifts")
    void testLoadAllShifts() {
        List<Shift> shifts = shiftRepo.loadAllShifts();
        assertNotNull(shifts);
        System.out.println("Total shifts in DB: " + shifts.size());
    }

    @Test @Order(99) @DisplayName("Cleanup: delete test guard")
    void testDeleteGuard() {
        assertDoesNotThrow(() -> guardRepo.deleteGuard(TEST_GUARD_ID));
        assertFalse(guardRepo.exists(TEST_GUARD_ID));
        System.out.println("Test guard cleaned up");
    }
}
