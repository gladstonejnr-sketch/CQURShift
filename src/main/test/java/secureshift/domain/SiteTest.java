package secureshift.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Site Domain Tests")
class SiteTest {

    private Site site;

    @BeforeEach
    void setUp() {
        site = new Site(1, "Canary Wharf", "One Canada Square, London", "East London",
                Arrays.asList("PATROL", "CCTV"));
        site.setLatitude(51.5054);
        site.setLongitude(-0.0235);
    }

    @Test
    @DisplayName("Site is created with correct fields")
    void testSiteCreation() {
        assertEquals(1, site.getId());
        assertEquals("Canary Wharf", site.getName());
        assertEquals("One Canada Square, London", site.getAddress());
        assertEquals("East London", site.getRegion());
    }

    @Test
    @DisplayName("Site default constructor works")
    void testDefaultConstructor() {
        Site s = new Site();
        assertEquals(0, s.getId());
        assertNull(s.getName());
    }

    @Test
    @DisplayName("Site getLocation returns correct lat/lon")
    void testGetLocation() {
        Location loc = site.getLocation();
        assertNotNull(loc);
        assertEquals(51.5054, loc.getLatitude(), 0.0001);
        assertEquals(-0.0235, loc.getLongitude(), 0.0001);
    }

    @Test
    @DisplayName("Site required skills are stored correctly")
    void testRequiredSkills() {
        List<String> skills = site.getRequiredSkills();
        assertEquals(2, skills.size());
        assertTrue(skills.contains("PATROL"));
        assertTrue(skills.contains("CCTV"));
    }

    @Test
    @DisplayName("Site skills can be updated")
    void testUpdateSkills() {
        site.setRequiredSkills(Arrays.asList("FIRST_AID"));
        assertEquals(1, site.getRequiredSkills().size());
        assertTrue(site.getRequiredSkills().contains("FIRST_AID"));
    }

    @Test
    @DisplayName("Site toString contains key info")
    void testToString() {
        String str = site.toString();
        assertTrue(str.contains("Canary Wharf"));
        assertTrue(str.contains("East London"));
    }

    @Test
    @DisplayName("Site latitude and longitude setters work")
    void testCoordinates() {
        site.setLatitude(52.0);
        site.setLongitude(1.0);
        assertEquals(52.0, site.getLatitude(), 0.0001);
        assertEquals(1.0, site.getLongitude(), 0.0001);
    }
}
