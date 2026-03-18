package secureshift.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import secureshift.data.GuardRepositoryJDBC;
import secureshift.data.ShiftRepositoryJDBC;
import secureshift.data.SiteRepositoryJDBC;
import secureshift.domain.Guard;
import secureshift.domain.Shift;
import secureshift.domain.Site;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ShiftService Tests")
class ShiftServiceTest {

    @Mock private ShiftRepositoryJDBC shiftRepository;
    @Mock private GuardRepositoryJDBC guardRepository;
    @Mock private SiteRepositoryJDBC  siteRepository;

    private ShiftService shiftService;

    private Site  site;
    private Guard guard;
    private Shift shiftFuture;
    private Shift shiftActive;
    private Shift shiftPast;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        shiftService = new ShiftService(shiftRepository, guardRepository, siteRepository);

        site  = new Site(1, "Heathrow", "Heathrow Airport", "West London", null);
        guard = new Guard("G001", "Alice", 51.47, -0.45, true, null);

        LocalDateTime now = LocalDateTime.now();
        shiftFuture = new Shift("SH001", site, now.plusDays(1), now.plusDays(1).plusHours(8), null);
        shiftActive = new Shift("SH002", site, now.minusHours(2), now.plusHours(6), null);
        shiftPast   = new Shift("SH003", site, now.minusDays(1), now.minusDays(1).plusHours(8), null);
        shiftFuture.setId("SH001");
        shiftActive.setId("SH002");
        shiftPast.setId("SH003");
    }

    // ── getAllShifts ────────────────────────────────────────────────

    @Test
    @DisplayName("getAllShifts returns all shifts")
    void testGetAllShifts() {
        when(shiftRepository.loadAllShifts()).thenReturn(Arrays.asList(shiftFuture, shiftActive, shiftPast));
        assertEquals(3, shiftService.getAllShifts().size());
    }

    // ── getShiftById ────────────────────────────────────────────────

    @Test
    @DisplayName("getShiftById finds shift by ID")
    void testGetShiftById() {
        when(shiftRepository.loadAllShifts()).thenReturn(Arrays.asList(shiftFuture, shiftActive));
        Shift result = shiftService.getShiftById("SH001");
        assertNotNull(result);
        assertEquals("SH001", result.getId());
    }

    @Test
    @DisplayName("getShiftById returns null when not found")
    void testGetShiftByIdNotFound() {
        when(shiftRepository.loadAllShifts()).thenReturn(Collections.emptyList());
        assertNull(shiftService.getShiftById("UNKNOWN"));
    }

    @Test
    @DisplayName("getShiftById throws on blank ID")
    void testGetShiftByIdBlank() {
        assertThrows(IllegalArgumentException.class, () -> shiftService.getShiftById(""));
    }

    // ── getUpcomingShifts ───────────────────────────────────────────

    @Test
    @DisplayName("getUpcomingShifts returns only future shifts")
    void testGetUpcomingShifts() {
        when(shiftRepository.loadAllShifts()).thenReturn(Arrays.asList(shiftFuture, shiftActive, shiftPast));
        List<Shift> upcoming = shiftService.getUpcomingShifts();
        assertEquals(1, upcoming.size());
        assertEquals("SH001", upcoming.get(0).getId());
    }

    // ── getActiveShifts ─────────────────────────────────────────────

    @Test
    @DisplayName("getActiveShifts returns only currently active shifts")
    void testGetActiveShifts() {
        when(shiftRepository.loadAllShifts()).thenReturn(Arrays.asList(shiftFuture, shiftActive, shiftPast));
        List<Shift> active = shiftService.getActiveShifts();
        assertEquals(1, active.size());
        assertEquals("SH002", active.get(0).getId());
    }

    // ── addShift ────────────────────────────────────────────────────

    @Test
    @DisplayName("addShift saves valid shift")
    void testAddShiftSuccess() {
        assertDoesNotThrow(() -> shiftService.addShift(shiftFuture));
        verify(shiftRepository).saveShift(shiftFuture);
    }

    @Test
    @DisplayName("addShift throws on null shift")
    void testAddShiftNull() {
        assertThrows(IllegalArgumentException.class, () -> shiftService.addShift(null));
    }

    @Test
    @DisplayName("addShift throws when end time is before start time")
    void testAddShiftInvalidTimes() {
        Shift bad = new Shift("SH999", site,
                LocalDateTime.now().plusHours(8),
                LocalDateTime.now(),  // end before start
                null);
        assertThrows(IllegalArgumentException.class, () -> shiftService.addShift(bad));
    }

    @Test
    @DisplayName("addShift throws when times are null")
    void testAddShiftNullTimes() {
        Shift noTimes = new Shift("SH999", site, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> shiftService.addShift(noTimes));
    }

    // ── assignGuardToShift ──────────────────────────────────────────

    @Test
    @DisplayName("assignGuardToShift assigns available guard")
    void testAssignGuardToShiftSuccess() {
        when(shiftRepository.loadAllShifts()).thenReturn(Arrays.asList(shiftFuture));
        when(guardRepository.loadAllGuards()).thenReturn(Arrays.asList(guard));

        assertDoesNotThrow(() -> shiftService.assignGuardToShift("SH001", "G001"));
        verify(shiftRepository).updateShift(shiftFuture);
    }

    @Test
    @DisplayName("assignGuardToShift throws when guard is unavailable")
    void testAssignGuardToShiftUnavailable() {
        guard.setAvailable(false);
        when(shiftRepository.loadAllShifts()).thenReturn(Arrays.asList(shiftFuture));
        when(guardRepository.loadAllGuards()).thenReturn(Arrays.asList(guard));

        assertThrows(IllegalStateException.class,
                () -> shiftService.assignGuardToShift("SH001", "G001"));
    }

    @Test
    @DisplayName("assignGuardToShift throws when shift not found")
    void testAssignGuardToShiftNotFound() {
        when(shiftRepository.loadAllShifts()).thenReturn(Collections.emptyList());
        assertThrows(IllegalArgumentException.class,
                () -> shiftService.assignGuardToShift("UNKNOWN", "G001"));
    }

    // ── deleteShift ─────────────────────────────────────────────────

    @Test
    @DisplayName("deleteShift calls repository delete")
    void testDeleteShift() {
        assertDoesNotThrow(() -> shiftService.deleteShift("SH001"));
        verify(shiftRepository).deleteShift("SH001");
    }

    @Test
    @DisplayName("deleteShift throws on blank ID")
    void testDeleteShiftBlank() {
        assertThrows(IllegalArgumentException.class, () -> shiftService.deleteShift(""));
    }

    // ── getShiftsByGuard ────────────────────────────────────────────

    @Test
    @DisplayName("getShiftsByGuard returns shifts for guard")
    void testGetShiftsByGuard() {
        shiftFuture.assignGuard(guard);
        when(shiftRepository.loadAllShifts()).thenReturn(Arrays.asList(shiftFuture, shiftActive));
        List<Shift> result = shiftService.getShiftsByGuard("G001");
        assertEquals(1, result.size());
    }
}
