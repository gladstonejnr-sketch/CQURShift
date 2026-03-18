package secureshift.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import secureshift.domain.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Scheduler and Strategy Tests")
class SchedulerTest {

    private Site site;
    private Shift shift;
    private Guard guardNear;
    private Guard guardFar;
    private Guard guardUnavailable;

    @BeforeEach
    void setUp() {
        site = new Site(1, "Heathrow Terminal 5", "Heathrow", "West London", null);
        site.setLatitude(51.4700);
        site.setLongitude(-0.4543);

        LocalDateTime start = LocalDateTime.now().plusHours(2);
        shift = new Shift("SH001", site, start, start.plusHours(8), null);

        guardNear        = new Guard("G001", "Alice", 51.4800, -0.4600, true,  null);
        guardFar         = new Guard("G002", "Bob",   51.5074, -0.1278, true,  null);
        guardUnavailable = new Guard("G003", "Charlie", 51.4750, -0.4550, false, null);
    }

    @Test
    @DisplayName("NearestGuardStrategy selects nearest available guard")
    void testNearestGuardSelected() {
        NearestGuardStrategy strategy = new NearestGuardStrategy();
        Guard selected = strategy.assignGuard(Arrays.asList(guardFar, guardNear), shift);
        assertNotNull(selected);
        assertEquals("G001", selected.getId());
    }

    @Test
    @DisplayName("NearestGuardStrategy skips unavailable guards")
    void testNearestGuardSkipsUnavailable() {
        NearestGuardStrategy strategy = new NearestGuardStrategy();
        Guard selected = strategy.assignGuard(Arrays.asList(guardUnavailable, guardFar), shift);
        assertNotNull(selected);
        assertEquals("G002", selected.getId());
    }

    @Test
    @DisplayName("NearestGuardStrategy returns null when no available guards")
    void testNearestGuardNoAvailable() {
        NearestGuardStrategy strategy = new NearestGuardStrategy();
        guardNear.setAvailable(false);
        guardFar.setAvailable(false);
        Guard selected = strategy.assignGuard(Arrays.asList(guardNear, guardFar), shift);
        assertNull(selected);
    }

    @Test
    @DisplayName("NearestGuardStrategy returns null for empty list")
    void testNearestGuardEmptyList() {
        NearestGuardStrategy strategy = new NearestGuardStrategy();
        assertNull(strategy.assignGuard(Collections.emptyList(), shift));
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
