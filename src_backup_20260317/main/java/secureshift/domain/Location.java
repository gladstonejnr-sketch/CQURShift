package secureshift.domain;

/**
 * Represents a geographical location with latitude and longitude.
 */
public class Location {

    private String address;
    private double latitude;
    private double longitude;

    // Default constructor
    //public Location() {}


    // ✅ fixes error constructor
    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = "";
    }

    // ✅ Constructor with all fields
    public Location(String address, double latitude, double longitude) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // ✅ Constructor with address only
    public Location(String address) {
        this.address = address;
        this.latitude = 0.0;
        this.longitude = 0.0;
    }

    // ✅ Getters
    public String getAddress() {
        return address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    // ✅ Setters
    public void setAddress(String address) {
        this.address = address;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    // ✅ Useful for debugging
    @Override
    public String toString() {
        return "Location{" +
                "address='" + address + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    // ✅ Equality check
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location)) return false;
        Location location = (Location) o;
        return Double.compare(location.latitude, latitude) == 0 &&
                Double.compare(location.longitude, longitude) == 0 &&
                address.equals(location.address);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(address, latitude, longitude);
    }
}



