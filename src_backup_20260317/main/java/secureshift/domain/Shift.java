package secureshift.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class Shift {

    private String id;
    private Site site;
    private Guard assignedGuard;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> requiredSkills;

    public Shift() {}

    public Shift(String id, Site site, LocalDateTime startTime,
                 LocalDateTime endTime, List<String> requiredSkills) {
        this.id = id;
        this.site = site;
        this.startTime = startTime;
        this.endTime = endTime;
        this.requiredSkills = requiredSkills;
    }

    public void assignGuard(Guard guard) {
        this.assignedGuard = guard;
    }

    public double getDurationHours() {
        if (startTime == null || endTime == null) return 0;
        return Duration.between(startTime, endTime).toMinutes() / 60.0;
    }

    public boolean overlapsWith(Shift other) {
        if (other == null || startTime == null || endTime == null) return false;
        return startTime.isBefore(other.getEndTime()) &&
                endTime.isAfter(other.getStartTime());
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }

    public Guard getAssignedGuard() { return assignedGuard; }
    public void setGuard(Guard guard) { this.assignedGuard = guard; }

    // ✅ Fixed - was incorrectly returning site id
    public String getGuardId() {
        return assignedGuard != null ? assignedGuard.getId() : null;
    }

    public void setGuardId(String guardId) {
        if (guardId == null) {
            this.assignedGuard = null;
        } else if (this.assignedGuard != null) {
            this.assignedGuard.setId(guardId);
        }
    }

    public String getSiteId() {
        return site != null ? String.valueOf(site.getId()) : null;
    }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getStart() { return startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public LocalDateTime getEnd() { return endTime; }

    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> skills) { this.requiredSkills = skills; }

    @Override
    public String toString() {
        return "Shift{" +
                "id='" + id + '\'' +
                ", site=" + (site != null ? site.getName() : "null") +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
