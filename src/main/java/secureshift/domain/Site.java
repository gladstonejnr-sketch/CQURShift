package secureshift.domain;

import java.util.List;

public class Site {

    private int id;
    private String name;
    private String address;
    private String region;
    private double latitude;
    private double longitude;
    private List<String> requiredSkills;

    // ✅ Default constructor
    public Site() {}

    // ✅ Full constructor
    public Site(int id, String name, String address, String region, List<String> requiredSkills) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.region = region;
        this.requiredSkills = requiredSkills;
    }

    // ✅ Returns a Location object from lat/lon
    public Location getLocation() {
        return new Location(this.latitude, this.longitude);
    }

    // ✅ Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    // ✅ toString() moved INSIDE the class
    @Override
    public String toString() {
        return "Site{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", region='" + region + '\'' +
                ", requiredSkills=" + requiredSkills +
                '}';
    }

} // ✅ Single closing brace for the class




