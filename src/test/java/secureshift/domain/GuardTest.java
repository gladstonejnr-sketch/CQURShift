package secureshift.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Guard Domain Tests")
class GuardTest {

    private Guard guard;

    @BeforeEach
    void setUp() {
        guard = new Guard("G001", "John Smith", 51.5074, -0.1278, true, Arrays.asList("PATROL", "CCTV"));
    }

    @Test
    @DisplayName("Guard is created with correct fields")
    void testGuardCreation() {
        assertEquals("G001", guard.getId());
        assertEquals("John Smith", guard.getName());
        assertEquals(51.5074, guard.getLatitude(), 0.0001);
        assertTrue(guard.isAvailable());
    }

    @Test
    @DisplayName("Guard default constructor creates empty skills list")
    void testDefaultConstructor() {
        Guard g = new Guard();
        assertNotNull(g.getSkills());
        assertTrue(g.getSkills().isEmpty());
    }

    @Test
    @DisplayName("Guard getLocation returns correct Location")
    void testGetLocation() {
        Location loc = guard.getLocation();
        assertNotNull(loc);
        assertEquals(51.5074, loc.getLatitude(), 0.0001);
        assertEquals(-0.1278, loc.getLongitude(), 0.0001);
    }

    @Test
    @DisplayName("Guard skills can be updated")
    void testSkillsUpdate() {
        guard.setSkills(Arrays.asList("PATROL", "CCTV", "FIRST_AID"));
        assertEquals(3, guard.getSkills().size());
        assertTrue(guard.getSkills().contains("FIRST_AID"));
    }

    @Test
    @DisplayName("Guard availability can be toggled")
    void testAvailabilityToggle() {
        assertTrue(guard.isAvailable());
        guard.setAvailable(false);
        assertFalse(guard.isAvailable());
        guard.setAvailable(true);
        assertTrue(guard.isAvailable());
    }

    @Test
    @DisplayName("Guard toString contains key info")
    void testToString() {
        String str = guard.toString();
        assertTrue(str.contains("G001"));
        assertTrue(str.contains("John Smith"));
    }
}
