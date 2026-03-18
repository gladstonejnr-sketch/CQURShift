package secureshift.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Shift Domain Tests")
class ShiftTest {

    private Site site;
    private Guard guard;
    private Shift shift;
    private LocalDateTime start;
    private LocalDateTime end;

    @BeforeEach
    void setUp() {
        site  = new Site(1, "Heathrow Terminal 5", "Heathrow", "West London", null);
        guard = new Guard("G001", "Alice Brown", 51.47, -0.45, true, Arrays.asList("PATROL"));
        start = LocalDateTime.of(2026, 3, 10, 8, 0);
        end   = LocalDateTime.of(2026, 3, 10, 16, 0);
        shift = new Shift("SH001", site, start, end, Arrays.asList("PATROL"));
    }

    @Test
    @DisplayName("getDurationHours calculates correctly")
    void testDurationHours() {
        assertEquals(8.0, shift.getDurationHours(), 0.001);
    }

    @Test
    @DisplayName("getDurationHours returns 0 when times are null")
    void testDurationHoursNullTimes() {
        Shift s = new Shift();
        assertEquals(0.0, s.getDurationHours(), 0.001);
    }

    @Test
    @DisplayName("assignGuard sets guard correctly")
    void testAssignGuard() {
        shift.assignGuard(guard);
        assertEquals(guard, shift.getAssignedGuard());
        assertEquals("G001", shift.getGuardId());
    }

    @Test
    @DisplayName("getGuardId returns null when no guard assigned")
    void testGetGuardIdNoGuard() {
        assertNull(shift.getGuardId());
    }

    @Test
    @DisplayName("getSiteId returns correct site ID as String")
    void testGetSiteId() {
        assertEquals("1", shift.getSiteId());
    }

    @Test
    @DisplayName("overlapsWith detects overlapping shifts")
    void testOverlapsWithOverlap() {
        Shift shiftB = new Shift("SH002", site,
                LocalDateTime.of(2026, 3, 10, 12, 0),
                LocalDateTime.of(2026, 3, 10, 20, 0), null);
        assertTrue(shift.overlapsWith(shiftB));
    }

    @Test
    @DisplayName("overlapsWith returns false for non-overlapping shifts")
    void testOverlapsWithNoOverlap() {
        Shift shiftB = new Shift("SH002", site,
                LocalDateTime.of(2026, 3, 10, 16, 0),
                LocalDateTime.of(2026, 3, 10, 23, 0), null);
        assertFalse(shift.overlapsWith(shiftB));
    }

    @Test
    @DisplayName("overlapsWith returns false for null")
    void testOverlapsWithNull() {
        assertFalse(shift.overlapsWith(null));
    }
}
