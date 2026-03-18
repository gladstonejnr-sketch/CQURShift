package secureshift.domain;

import secureshift.util.GeoUtils;

import java.util.Comparator;
import java.util.List;

public class NearestGuardStrategy implements AssignmentStrategy {

    @Override
    public Guard assignGuard(List<Guard> guards, Shift shift) {
        if (guards == null || shift == null) return null;
        if (shift.getSite() == null) return null;

        return guards.stream()
                .filter(g -> g.isAvailableFor(shift))
                .filter(g -> skillsMatch(g, shift))
                .min(Comparator.comparingDouble(g ->
                        GeoUtils.distanceKm(
                                g.getLatitude(),
                                g.getLongitude(),
                                shift.getSite().getLatitude(),
                                shift.getSite().getLongitude()
                        )
                ))
                .orElse(null);
    }

    private boolean skillsMatch(Guard guard, Shift shift) {
        if (shift.getRequiredSkills() == null) return true;
        if (guard.getSkills() == null) return false;
        for (String required : shift.getRequiredSkills()) {
            if (!guard.getSkills().contains(required)) return false;
        }
        return true;
    }
}
