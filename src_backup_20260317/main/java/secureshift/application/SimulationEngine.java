package secureshift.application;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import secureshift.data.GuardRepositoryJDBC;
import secureshift.data.ShiftRepositoryJDBC;
import secureshift.domain.Guard;
import secureshift.domain.Location;
import secureshift.domain.Shift;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SimulationEngine {

    private final GuardRepositoryJDBC guardRepo = new GuardRepositoryJDBC();
    private final ShiftRepositoryJDBC shiftRepo = new ShiftRepositoryJDBC();
    private final Map<String, Location> guardLocations = new HashMap<>();
    private final Map<String, Location> guardTargets = new HashMap<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public void start(WebEngine engine) {
        List<Guard> guards = guardRepo.loadAllGuards();
        List<Shift> shifts = shiftRepo.loadAllShifts();

        for (Guard g : guards) {
            String id = g.getId();
            Location loc = new Location(g.getLatitude(), g.getLongitude());
            guardLocations.put(id, loc);

            Optional<Shift> assignedShift = shifts.stream()
                    .filter(s -> s.getAssignedGuard() != null &&
                            s.getAssignedGuard().getId().equals(id))
                    .findFirst();

            assignedShift.ifPresent(s -> {
                Location target = new Location(
                        s.getSite().getLatitude(),
                        s.getSite().getLongitude()
                );
                guardTargets.put(id, target);
            });
        }

        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (Guard g : guardRepo.loadAllGuards()) {
                    String id = g.getId();
                    Location current = guardLocations.getOrDefault(
                            id, new Location(g.getLatitude(), g.getLongitude()));
                    Location target = guardTargets.get(id);

                    if (target != null) {
                        double newLat = moveToward(
                                current.getLatitude(), target.getLatitude(), 0.0001);
                        double newLon = moveToward(
                                current.getLongitude(), target.getLongitude(), 0.0001);

                        guardLocations.put(id, new Location(newLat, newLon));
                        g.setLatitude(newLat);
                        g.setLongitude(newLon);

                        Platform.runLater(() ->
                                engine.executeScript(
                                        "updateGuardPosition('" + id + "', " +
                                                newLat + ", " + newLon + ");"
                                )
                        );
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Simulation error: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
        System.out.println("✅ Simulation stopped");
    }

    private double moveToward(double current, double target, double speed) {
        if (Math.abs(target - current) < speed) return target;
        return current + Math.signum(target - current) * speed;
    }
}
