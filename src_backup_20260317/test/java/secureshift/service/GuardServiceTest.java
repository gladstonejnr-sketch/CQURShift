package secureshift.service;

import org.junit.jupiter.api.*;
import org.mockito.*;
import secureshift.data.GuardRepositoryJDBC;
import secureshift.domain.Guard;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("GuardService Tests")
class GuardServiceTest {

    @Mock private GuardRepositoryJDBC guardRepository;
    private GuardService guardService;
    private Guard guardAlice, guardBob;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        guardService = new GuardService(guardRepository);
        guardAlice = new Guard("G001", "Alice", 51.5, -0.1, true,  Arrays.asList("PATROL"));
        guardBob   = new Guard("G002", "Bob",   51.6, -0.2, false, Arrays.asList("CCTV"));
    }

    @Test @DisplayName("getAllGuards returns all guards")
    void testGetAllGuards() {
        when(guardRepository.loadAllGuards()).thenReturn(Arrays.asList(guardAlice, guardBob));
        assertEquals(2, guardService.getAllGuards().size());
    }

    @Test @DisplayName("getGuardById returns guard when found")
    void testGetGuardByIdFound() {
        when(guardRepository.findById("G001")).thenReturn(Optional.of(guardAlice));
        assertEquals("Alice", guardService.getGuardById("G001").getName());
    }

    @Test @DisplayName("getGuardById returns null when not found")
    void testGetGuardByIdNotFound() {
        when(guardRepository.findById("UNKNOWN")).thenReturn(Optional.empty());
        assertNull(guardService.getGuardById("UNKNOWN"));
    }

    @Test @DisplayName("getGuardById throws on blank ID")
    void testGetGuardByIdBlank() {
        assertThrows(IllegalArgumentException.class, () -> guardService.getGuardById(""));
    }

    @Test @DisplayName("addGuard saves new guard")
    void testAddGuardSuccess() {
        when(guardRepository.exists("G001")).thenReturn(false);
        assertDoesNotThrow(() -> guardService.addGuard(guardAlice));
        verify(guardRepository).saveGuard(guardAlice);
    }

    @Test @DisplayName("addGuard throws when guard already exists")
    void testAddGuardDuplicate() {
        when(guardRepository.exists("G001")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> guardService.addGuard(guardAlice));
        verify(guardRepository, never()).saveGuard(any());
    }

    @Test @DisplayName("addGuard throws on null")
    void testAddGuardNull() {
        assertThrows(IllegalArgumentException.class, () -> guardService.addGuard(null));
    }

    @Test @DisplayName("deleteGuard removes guard")
    void testDeleteGuard() {
        when(guardRepository.exists("G001")).thenReturn(true);
        assertDoesNotThrow(() -> guardService.deleteGuard("G001"));
        verify(guardRepository).deleteGuard("G001");
    }

    @Test @DisplayName("deleteGuard throws when not found")
    void testDeleteGuardNotFound() {
        when(guardRepository.exists("G999")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> guardService.deleteGuard("G999"));
    }

    @Test @DisplayName("getGuardsBySkill returns matching guards")
    void testGetGuardsBySkill() {
        when(guardRepository.loadAllGuards()).thenReturn(Arrays.asList(guardAlice, guardBob));
        List<Guard> result = guardService.getGuardsBySkill("PATROL");
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getName());
    }

    @Test @DisplayName("getAvailableGuards returns only available")
    void testGetAvailableGuards() {
        when(guardRepository.loadAvailableGuards()).thenReturn(Arrays.asList(guardAlice));
        assertEquals(1, guardService.getAvailableGuards().size());
    }

    @Test @DisplayName("getTotalGuardCount returns count")
    void testGetTotalGuardCount() {
        when(guardRepository.count()).thenReturn(5);
        assertEquals(5, guardService.getTotalGuardCount());
    }
}
