package secureshift.application;

import secureshift.domain.Location;

import java.util.List;

public class RouteService {

    public List<Location> generateCurve(Location guardLoc, Location siteLoc) {
        if (guardLoc == null || siteLoc == null) {
            throw new IllegalArgumentException("Guard and site locations cannot be null");
        }
        double midLat = (guardLoc.getLatitude() + siteLoc.getLatitude()) / 2 + 0.2;
        double midLon = (guardLoc.getLongitude() + siteLoc.getLongitude()) / 2 + 0.2;
        Location mid = new Location(midLat, midLon);
        return List.of(guardLoc, mid, siteLoc);
    }

    public double calculateDistance(Location locationA, Location locationB) {
        if (locationA == null || locationB == null) {
            throw new IllegalArgumentException("Locations cannot be null");
        }
        double latDiff = locationA.getLatitude() - locationB.getLatitude();
        double lonDiff = locationA.getLongitude() - locationB.getLongitude();
        return Math.sqrt((latDiff * latDiff) + (lonDiff * lonDiff));
    }

    public List<Location> generateCurveWithOffset(
            Location guardLoc, Location siteLoc, double offset) {
        if (guardLoc == null || siteLoc == null) {
            throw new IllegalArgumentException("Guard and site locations cannot be null");
        }
        double midLat = (guardLoc.getLatitude() + siteLoc.getLatitude()) / 2 + offset;
        double midLon = (guardLoc.getLongitude() + siteLoc.getLongitude()) / 2 + offset;
        Location mid = new Location(midLat, midLon);
        return List.of(guardLoc, mid, siteLoc);
    }
}
