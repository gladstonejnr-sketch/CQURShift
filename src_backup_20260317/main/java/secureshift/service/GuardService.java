package secureshift.service;

// ✅ Add these imports - fixes all 7 errors
import secureshift.data.GuardRepositoryJDBC;
import secureshift.domain.Guard;

import java.util.List;
import java.util.Optional;
/**
 * Service class for Guard business logic.
 * Delegates persistence to GuardRepositoryJDBC.
 */
public class GuardService {

    private final GuardRepositoryJDBC guardRepository;

    // ✅ Constructor
    public GuardService(GuardRepositoryJDBC guardRepository) {
        this.guardRepository = guardRepository;
    }

    // ✅ Get all guards
    public List<Guard> getAllGuards() {
        return guardRepository.loadAllGuards();
    }

    // ✅ Get guard by ID using Optional for safety
    public Guard getGuardById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Guard ID cannot be empty");
        }
        return guardRepository.findById(id)
                .orElse(null);
    }

    // ✅ Add a new guard
    public void addGuard(Guard guard) {
        if (guard == null) {
            throw new IllegalArgumentException("Guard cannot be null");
        }
        if (guard.getName() == null || guard.getName().isBlank()) {
            throw new IllegalArgumentException("Guard name cannot be empty");
        }
        if (guardExists(guard.getId())) {
            throw new IllegalArgumentException("Guard already exists: " + guard.getId());
        }
        guardRepository.saveGuard(guard);
        System.out.println("✅ Guard added: " + guard.getName());
    }

    // ✅ Update an existing guard
    public void updateGuard(Guard guard) {
        if (guard == null) {
            throw new IllegalArgumentException("Guard cannot be null");
        }
        if (guard.getName() == null || guard.getName().isBlank()) {
            throw new IllegalArgumentException("Guard name cannot be empty");
        }
        if (!guardExists(guard.getId())) {
            throw new IllegalArgumentException("Guard not found: " + guard.getId());
        }
        guardRepository.updateGuard(guard);
        System.out.println("✅ Guard updated: " + guard.getName());
    }

    // ✅ Delete a guard by ID
    public void deleteGuard(String guardId) {
        if (guardId == null || guardId.isBlank()) {
            throw new IllegalArgumentException("Guard ID cannot be empty");
        }
        if (!guardExists(guardId)) {
            throw new IllegalArgumentException("Guard not found: " + guardId);
        }
        guardRepository.deleteGuard(guardId);
        System.out.println("✅ Guard deleted: " + guardId);
    }

    // ✅ Check if a guard exists
    public boolean guardExists(String guardId) {
        if (guardId == null || guardId.isBlank()) {
            return false;
        }
        return guardRepository.exists(guardId);
    }

    // ✅ Get guards by skill
    public List<Guard> getGuardsBySkill(String skill) {
        if (skill == null || skill.isBlank()) {
            throw new IllegalArgumentException("Skill cannot be empty");
        }
        return guardRepository.loadAllGuards()
                .stream()
                .filter(g -> g.getSkills() != null &&
                        g.getSkills().contains(skill))
                .toList();
    }

    // ✅ Get available guards only
    public List<Guard> getAvailableGuards() {
        return guardRepository.loadAvailableGuards();
    }

    // ✅ Set guard availability
    public void setGuardAvailability(String guardId, boolean available) {
        if (guardId == null || guardId.isBlank()) {
            throw new IllegalArgumentException("Guard ID cannot be empty");
        }
        Guard guard = getGuardById(guardId);
        if (guard == null) {
            throw new IllegalArgumentException("Guard not found: " + guardId);
        }
        guard.setAvailable(available);
        guardRepository.updateGuard(guard);
        System.out.println("✅ Guard availability updated: " + guardId + " → " + available);
    }

    // ✅ Get total guard count
    public int getTotalGuardCount() {
        return guardRepository.count();
    }
}

