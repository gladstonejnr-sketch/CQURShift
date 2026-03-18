package secureshift.service;

import secureshift.data.GuardRepositoryJDBC;
import secureshift.data.ShiftRepositoryJDBC;
import secureshift.data.SiteRepositoryJDBC;
import secureshift.domain.Guard;
import secureshift.domain.Shift;
import secureshift.domain.Site;

import java.time.LocalDateTime;
import java.util.List;

public class ShiftService {

    private final ShiftRepositoryJDBC shiftRepository;
    private final GuardRepositoryJDBC guardRepository;
    private final SiteRepositoryJDBC siteRepository;

    public ShiftService(ShiftRepositoryJDBC shiftRepository,
                        GuardRepositoryJDBC guardRepository,
                        SiteRepositoryJDBC siteRepository) {
        this.shiftRepository = shiftRepository;
        this.guardRepository = guardRepository;
        this.siteRepository = siteRepository;
    }

    public List<Shift> getAllShifts() {
        return shiftRepository.loadAllShifts();
    }

    public Shift getShiftById(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Shift ID cannot be empty");
        return shiftRepository.loadAllShifts().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst().orElse(null);
    }

    public List<Shift> getShiftsByGuard(String guardId) {
        if (guardId == null || guardId.isBlank()) throw new IllegalArgumentException("Guard ID cannot be empty");
        return shiftRepository.loadAllShifts().stream()
                .filter(s -> s.getGuardId() != null && s.getGuardId().equals(guardId))
                .toList();
    }

    public List<Shift> getShiftsBySite(String siteId) {
        if (siteId == null || siteId.isBlank()) throw new IllegalArgumentException("Site ID cannot be empty");
        return shiftRepository.loadAllShifts().stream()
                .filter(s -> s.getSiteId() != null && s.getSiteId().equals(siteId))
                .toList();
    }

    public List<Shift> getUpcomingShifts() {
        LocalDateTime now = LocalDateTime.now();
        return shiftRepository.loadAllShifts().stream()
                .filter(s -> s.getStartTime() != null && s.getStartTime().isAfter(now))
                .toList();
    }

    public List<Shift> getActiveShifts() {
        LocalDateTime now = LocalDateTime.now();
        return shiftRepository.loadAllShifts().stream()
                .filter(s -> s.getStartTime() != null && s.getEndTime() != null &&
                        s.getStartTime().isBefore(now) && s.getEndTime().isAfter(now))
                .toList();
    }

    public void addShift(Shift shift) {
        if (shift == null) throw new IllegalArgumentException("Shift cannot be null");
        if (shift.getStartTime() == null || shift.getEndTime() == null)
            throw new IllegalArgumentException("Shift must have start and end times");
        if (shift.getEndTime().isBefore(shift.getStartTime()))
            throw new IllegalArgumentException("End time cannot be before start time");
        shiftRepository.saveShift(shift);
        System.out.println("✅ Shift added: " + shift.getId());
    }

    public void updateShift(Shift shift) {
        if (shift == null) throw new IllegalArgumentException("Shift cannot be null");
        if (shift.getStartTime() == null || shift.getEndTime() == null)
            throw new IllegalArgumentException("Shift must have start and end times");
        if (shift.getEndTime().isBefore(shift.getStartTime()))
            throw new IllegalArgumentException("End time cannot be before start time");
        shiftRepository.updateShift(shift);
        System.out.println("✅ Shift updated: " + shift.getId());
    }

    public void deleteShift(String shiftId) {
        if (shiftId == null || shiftId.isBlank()) throw new IllegalArgumentException("Shift ID cannot be empty");
        shiftRepository.deleteShift(shiftId);
        System.out.println("✅ Shift deleted: " + shiftId);
    }

    public void assignGuardToShift(String shiftId, String guardId) {
        Shift shift = getShiftById(shiftId);
        if (shift == null) throw new IllegalArgumentException("Shift not found: " + shiftId);

        Guard guard = guardRepository.loadAllGuards().stream()
                .filter(g -> g.getId().equals(guardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Guard not found: " + guardId));

        if (!guard.isAvailable())
            throw new IllegalStateException("Guard is not available: " + guard.getName());

        shift.assignGuard(guard);
        shiftRepository.updateShift(shift);
        System.out.println("✅ Guard " + guard.getName() + " assigned to shift " + shiftId);
    }

    public void unassignGuardFromShift(String shiftId) {
        Shift shift = getShiftById(shiftId);
        if (shift == null) throw new IllegalArgumentException("Shift not found: " + shiftId);
        shift.setGuard(null);
        shiftRepository.updateShift(shift);
        System.out.println("✅ Guard unassigned from shift " + shiftId);
    }

    public boolean shiftExists(String shiftId) {
        if (shiftId == null || shiftId.isBlank()) return false;
        return shiftRepository.loadAllShifts().stream().anyMatch(s -> s.getId().equals(shiftId));
    }
}
