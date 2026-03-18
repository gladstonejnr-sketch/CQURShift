package secureshift.application;

import secureshift.data.GuardRepositoryJDBC;
import secureshift.data.ShiftRepositoryJDBC;
import secureshift.domain.AssignmentStrategy;
import secureshift.domain.Guard;
import secureshift.domain.Shift;

import java.util.List;

public class Scheduler {

    private final GuardRepositoryJDBC guardRepo;
    private final ShiftRepositoryJDBC shiftRepo;
    private final AssignmentStrategy strategy;

    public Scheduler(AssignmentStrategy strategy) {
        this.guardRepo = new GuardRepositoryJDBC();
        this.shiftRepo = new ShiftRepositoryJDBC();
        this.strategy = strategy;
    }

    public void assignShift(Shift shift, List<Guard> guards) {
        if (shift == null || guards == null || guards.isEmpty()) {
            System.out.println("❌ Invalid shift or empty guard list");
            return;
        }
        Guard selected = strategy.assignGuard(guards, shift);
        if (selected != null) {
            shift.assignGuard(selected);
            System.out.println("✅ Assigned guard " + selected.getName() + " to shift " + shift.getId());
        } else {
            System.out.println("⚠️ No suitable guard found for shift " + shift.getId());
        }
    }

    public void autoAssignAll() {
        List<Shift> unassigned = shiftRepo.loadUnassignedShifts();
        List<Guard> guards = guardRepo.loadAllGuards();
        if (unassigned.isEmpty()) {
            System.out.println("✅ No unassigned shifts found");
            return;
        }
        for (Shift shift : unassigned) {
            assignShift(shift, guards);
        }
    }
}
