package secureshift.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GeoUtils Tests")
class GeoUtilsTest {

    @Test
    @DisplayName("distance() returns 0 for same coordinates")
    void testDistanceSamePoint() {
        double d = GeoUtils.distance(51.5074, -0.1278, 51.5074, -0.1278);
        assertEquals(0.0, d, 0.0001);
    }

    @Test
    @DisplayName("distance() is positive for different points")
    void testDistanceDifferentPoints() {
        double d = GeoUtils.distance(51.5074, -0.1278, 53.4808, -2.2426);
        assertTrue(d > 0);
    }

    @Test
    @DisplayName("distance() is symmetric")
    void testDistanceSymmetric() {
        double d1 = GeoUtils.distance(51.5074, -0.1278, 53.4808, -2.2426);
        double d2 = GeoUtils.distance(53.4808, -2.2426, 51.5074, -0.1278);
        assertEquals(d1, d2, 0.0001);
    }

    @Test
    @DisplayName("distanceKm() returns 0 for same coordinates")
    void testDistanceKmSamePoint() {
        double d = GeoUtils.distanceKm(51.5074, -0.1278, 51.5074, -0.1278);
        assertEquals(0.0, d, 0.001);
    }

    @Test
    @DisplayName("distanceKm() London to Manchester is approximately 262 km")
    void testDistanceKmLondonManchester() {
        double d = GeoUtils.distanceKm(51.5074, -0.1278, 53.4808, -2.2426);
        assertEquals(262.0, d, 10.0);
    }

    @Test
    @DisplayName("distanceKm() is symmetric")
    void testDistanceKmSymmetric() {
        double d1 = GeoUtils.distanceKm(51.5074, -0.1278, 53.4808, -2.2426);
        double d2 = GeoUtils.distanceKm(53.4808, -2.2426, 51.5074, -0.1278);
        assertEquals(d1, d2, 0.001);
    }
}
