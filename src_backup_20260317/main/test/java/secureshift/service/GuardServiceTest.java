package secureshift.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import secureshift.data.GuardRepositoryJDBC;
import secureshift.domain.Guard;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("GuardService Tests")
class GuardServiceTest {

    @Mock
    private GuardRepositoryJDBC guardRepository;

    private GuardService guardService;

    private Guard guardAlice;
    private Guard guardBob;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        guardService = new GuardService(guardRepository);

        guardAlice = new Guard("G001", "Alice", 51.5, -0.1, true, Arrays.asList("PATROL"));
        guardBob   = new Guard("G002", "Bob",   51.6, -0.2, false, Arrays.asList("CCTV"));
    }

    // ── getAllGuards ────────────────────────────────────────────────

    @Test
    @DisplayName("getAllGuards returns all guards from repository")
    void testGetAllGuards() {
        when(guardRepository.loadAllGuards()).thenReturn(Arrays.asList(guardAlice, guardBob));
        List<Guard> result = guardService.getAllGuards();
        assertEquals(2, result.size());
        verify(guardRepository).loadAllGuards();
    }

    @Test
    @DisplayName("getAllGuards returns empty list when no guards")
    void testGetAllGuardsEmpty() {
        when(guardRepository.loadAllGuards()).thenReturn(Collections.emptyList());
        List<Guard> result = guardService.getAllGuards();
        assertTrue(result.isEmpty());
    }

    // ── getGuardById ────────────────────────────────────────────────

    @Test
    @DisplayName("getGuardById returns guard when found")
    void testGetGuardByIdFound() {
        when(guardRepository.findById("G001")).thenReturn(Optional.of(guardAlice));
        Guard result = guardService.getGuardById("G001");
        assertNotNull(result);
        assertEquals("Alice", result.getName());
    }

    @Test
    @DisplayName("getGuardById returns null when not found")
    void testGetGuardByIdNotFound() {
        when(guardRepository.findById("UNKNOWN")).thenReturn(Optional.empty());
        Guard result = guardService.getGuardById("UNKNOWN");
        assertNull(result);
    }

    @Test
    @DisplayName("getGuardById throws on blank ID")
    void testGetGuardByIdBlank() {
        assertThrows(IllegalArgumentException.class, () -> guardService.getGuardById(""));
        assertThrows(IllegalArgumentException.class, () -> guardService.getGuardById(null));
    }

    // ── addGuard ────────────────────────────────────────────────────

    @Test
    @DisplayName("addGuard saves new guard successfully")
    void testAddGuardSuccess() {
        when(guardRepository.exists("G001")).thenReturn(false);
        assertDoesNotThrow(() -> guardService.addGuard(guardAlice));
        verify(guardRepository).saveGuard(guardAlice);
    }

    @Test
    @DisplayName("addGuard throws when guard already exists")
    void testAddGuardDuplicate() {
        when(guardRepository.exists("G001")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> guardService.addGuard(guardAlice));
        verify(guardRepository, never()).saveGuard(any());
    }

    @Test
    @DisplayName("addGuard throws on null guard")
    void testAddGuardNull() {
        assertThrows(IllegalArgumentException.class, () -> guardService.addGuard(null));
    }

    @Test
    @DisplayName("addGuard throws when name is blank")
    void testAddGuardBlankName() {
        Guard noName = new Guard("G003", "", 0, 0, true, null);
        assertThrows(IllegalArgumentException.class, () -> guardService.addGuard(noName));
    }

    // ── updateGuard ─────────────────────────────────────────────────

    @Test
    @DisplayName("updateGuard updates existing guard")
    void testUpdateGuardSuccess() {
        when(guardRepository.exists("G001")).thenReturn(true);
        assertDoesNotThrow(() -> guardService.updateGuard(guardAlice));
        verify(guardRepository).updateGuard(guardAlice);
    }

    @Test
    @DisplayName("updateGuard throws when guard not found")
    void testUpdateGuardNotFound() {
        when(guardRepository.exists("G001")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> guardService.updateGuard(guardAlice));
    }

    // ── deleteGuard ─────────────────────────────────────────────────

    @Test
    @DisplayName("deleteGuard removes existing guard")
    void testDeleteGuardSuccess() {
        when(guardRepository.exists("G001")).thenReturn(true);
        assertDoesNotThrow(() -> guardService.deleteGuard("G001"));
        verify(guardRepository).deleteGuard("G001");
    }

    @Test
    @DisplayName("deleteGuard throws when guard not found")
    void testDeleteGuardNotFound() {
        when(guardRepository.exists("G999")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> guardService.deleteGuard("G999"));
    }

    @Test
    @DisplayName("deleteGuard throws on blank ID")
    void testDeleteGuardBlankId() {
        assertThrows(IllegalArgumentException.class, () -> guardService.deleteGuard(""));
    }

    // ── getAvailableGuards ──────────────────────────────────────────

    @Test
    @DisplayName("getAvailableGuards returns only available guards")
    void testGetAvailableGuards() {
        when(guardRepository.loadAvailableGuards()).thenReturn(Arrays.asList(guardAlice));
        List<Guard> result = guardService.getAvailableGuards();
        assertEquals(1, result.size());
        assertTrue(result.get(0).isAvailable());
    }

    // ── getGuardsBySkill ────────────────────────────────────────────

    @Test
    @DisplayName("getGuardsBySkill returns guards with matching skill")
    void testGetGuardsBySkill() {
        when(guardRepository.loadAllGuards()).thenReturn(Arrays.asList(guardAlice, guardBob));
        List<Guard> patrolGuards = guardService.getGuardsBySkill("PATROL");
        assertEquals(1, patrolGuards.size());
        assertEquals("Alice", patrolGuards.get(0).getName());
    }

    @Test
    @DisplayName("getGuardsBySkill returns empty for unknown skill")
    void testGetGuardsBySkillNotFound() {
        when(guardRepository.loadAllGuards()).thenReturn(Arrays.asList(guardAlice, guardBob));
        List<Guard> result = guardService.getGuardsBySkill("NONEXISTENT");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getGuardsBySkill throws on blank skill")
    void testGetGuardsBySkillBlank() {
        assertThrows(IllegalArgumentException.class, () -> guardService.getGuardsBySkill(""));
    }

    // ── getTotalGuardCount ──────────────────────────────────────────

    @Test
    @DisplayName("getTotalGuardCount returns correct count")
    void testGetTotalGuardCount() {
        when(guardRepository.count()).thenReturn(5);
        assertEquals(5, guardService.getTotalGuardCount());
    }
}
