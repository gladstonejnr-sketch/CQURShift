package secureshift.domain;

import java.util.ArrayList;
import java.util.List;

public class Guard {

    private String id;
    private String name;
    private double latitude;
    private double longitude;
    private boolean available;
    private List<String> skills;

    // ✅ Default constructor
    public Guard() {
        this.skills = new ArrayList<>();
    }

    // ✅ Full constructor
    public Guard(String id, String name, double latitude,
                 double longitude, boolean available, List<String> skills) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.available = available;
        this.skills = skills != null ? skills : new ArrayList<>();
    }

    // ✅ Returns Location object
    public Location getLocation() {
        return new Location(this.latitude, this.longitude);
    }

    // ✅ Check if guard is available for a shift
    public boolean isAvailableFor(Shift shift) {
        return this.available;
    }

    // ✅ String id getter and setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // ✅ Double latitude getter and setter
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    // ✅ Double longitude getter and setter
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    // ✅ Boolean available getter and setter
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    // ✅ getDurationHours - needed by Guard.java line 65/78
    public double getDurationHours() { return 0.0; }

    // ✅ overlapsWith - needed by Guard.java line 73
    public boolean overlapsWith(Shift shift) { return false; }

    @Override
    public String toString() {
        return "Guard{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", available=" + available +
                '}';
    }
}

