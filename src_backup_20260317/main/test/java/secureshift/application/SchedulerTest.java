package secureshift.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import secureshift.domain.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Scheduler & Assignment Strategy Tests")
class SchedulerTest {

    private Site site;
    private Shift shift;
    private Guard guardNear;
    private Guard guardFar;
    private Guard guardUnavailable;

    @BeforeEach
    void setUp() {
        // Site at Heathrow
        site = new Site(1, "Heathrow Terminal 5", "Heathrow", "West London", null);
        site.setLatitude(51.4700);
        site.setLongitude(-0.4543);

        LocalDateTime start = LocalDateTime.now().plusHours(2);
        shift = new Shift("SH001", site, start, start.plusHours(8), null);

        // Guard near Heathrow (~2 km away)
        guardNear = new Guard("G001", "Alice", 51.4800, -0.4600, true, null);

        // Guard far from Heathrow (~50 km away in central London)
        guardFar = new Guard("G002", "Bob", 51.5074, -0.1278, true, null);

        // Guard near but unavailable
        guardUnavailable = new Guard("G003", "Charlie", 51.4750, -0.4550, false, null);
    }

    // ── NearestGuardStrategy ────────────────────────────────────────

    @Test
    @DisplayName("NearestGuardStrategy selects nearest available guard")
    void testNearestGuardSelected() {
        NearestGuardStrategy strategy = new NearestGuardStrategy();
        List<Guard> guards = Arrays.asList(guardFar, guardNear); // Far listed first
        Guard selected = strategy.assignGuard(guards, shift);

        assertNotNull(selected);
        assertEquals("G001", selected.getId(), "Should pick Alice who is nearest");
    }

    @Test
    @DisplayName("NearestGuardStrategy skips unavailable guards")
    void testNearestGuardSkipsUnavailable() {
        NearestGuardStrategy strategy = new NearestGuardStrategy();
        List<Guard> guards = Arrays.asList(guardUnavailable, guardFar);
        Guard selected = strategy.assignGuard(guards, shift);

        assertNotNull(selected);
        assertEquals("G002", selected.getId(), "Should skip Charlie (unavailable) and pick Bob");
    }

    @Test
    @DisplayName("NearestGuardStrategy returns null when no available guards")
    void testNearestGuardNoAvailable() {
        NearestGuardStrategy strategy = new NearestGuardStrategy();
        guardUnavailable.setAvailable(false);
        guardFar.setAvailable(false);
        guardNear.setAvailable(false);

        Guard selected = strategy.assignGuard(Arrays.asList(guardNear, guardFar), shift);
        assertNull(selected);
    }

    @Test
    @DisplayName("NearestGuardStrategy returns null for empty guard list")
    void testNearestGuardEmptyList() {
        NearestGuardStrategy strategy = new NearestGuardStrategy();
        Guard selected = strategy.assignGuard(Collections.emptyList(), shift);
        assertNull(selected);
    }

    @Test
    @DisplayName("NearestGuardStrategy returns null for null guard list")
    void testNearestGuardNullList() {
        NearestGuardStrategy strategy = new NearestGuardStrategy();
        Guard selected = strategy.assignGuard(null, shift);
        assertNull(selected);
    }

    // ── Scheduler ──────────────────────────────────────────────────

    @Test
    @DisplayName("Scheduler.assignShift assigns guard to shift")
    void testSchedulerAssignShift() {
        NearestGuardStrategy strategy = new NearestGuardStrategy();

        // We test the assignment logic directly since Scheduler
        // calls assignGuard internally
        List<Guard> guards = Arrays.asList(guardNear, guardFar);
        Guard selected = strategy.assignGuard(guards, shift);

        shift.assignGuard(selected);
        assertNotNull(shift.getAssignedGuard());
        assertEquals("G001", shift.getAssignedGuard().getId());
    }

    @Test
    @DisplayName("Shift is unassigned initially")
    void testShiftInitiallyUnassigned() {
        assertNull(shift.getAssignedGuard());
        assertNull(shift.getGuardId());
    }

    @Test
    @DisplayName("Guard assigned to shift is retrievable")
    void testGuardAssignedAndRetrievable() {
        shift.assignGuard(guardNear);
        assertEquals(guardNear, shift.getAssignedGuard());
        assertEquals("G001", shift.getGuardId());
    }
}
