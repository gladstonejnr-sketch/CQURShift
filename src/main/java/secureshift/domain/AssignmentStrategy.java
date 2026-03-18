package secureshift.domain;

import java.util.List;

public interface AssignmentStrategy {
    Guard assignGuard(List<Guard> guards, Shift shift);
}

