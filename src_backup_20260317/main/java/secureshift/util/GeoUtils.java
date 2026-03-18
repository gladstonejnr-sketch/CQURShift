package secureshift.util;

/**
 * Utility class for geographical calculations.
 */
public class GeoUtils {

    // ✅ Calculate distance between two coordinates
    public static double distance(
            double latA, double lonA,
            double latB, double lonB) {

        double latDiff = latA - latB;
        double lonDiff = lonA - lonB;
        return Math.sqrt((latDiff * latDiff) + (lonDiff * lonDiff));
    }

    // ✅ Calculate distance in kilometres using Haversine formula
    public static double distanceKm(
            double latA, double lonA,
            double latB, double lonB) {

        final int EARTH_RADIUS_KM = 6371;

        double dLat = Math.toRadians(latB - latA);
        double dLon = Math.toRadians(lonB - lonA);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(latA)) *
                        Math.cos(Math.toRadians(latB)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}