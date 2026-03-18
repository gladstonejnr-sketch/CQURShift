package secureshift.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.Arrays;
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
        assertEquals("East London", site.getRegion());
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
    @DisplayName("Site required skills stored correctly")
    void testRequiredSkills() {
        assertEquals(2, site.getRequiredSkills().size());
        assertTrue(site.getRequiredSkills().contains("PATROL"));
    }

    @Test
    @DisplayName("Site coordinates can be updated")
    void testCoordinates() {
        site.setLatitude(52.0);
        site.setLongitude(1.0);
        assertEquals(52.0, site.getLatitude(), 0.0001);
        assertEquals(1.0, site.getLongitude(), 0.0001);
    }

    @Test
    @DisplayName("Site toString contains key info")
    void testToString() {
        assertTrue(site.toString().contains("Canary Wharf"));
        assertTrue(site.toString().contains("East London"));
    }
}
