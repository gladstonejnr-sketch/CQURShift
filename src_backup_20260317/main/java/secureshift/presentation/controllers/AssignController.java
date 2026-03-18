package secureshift.presentation.controllers;

import java.time.LocalDateTime;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import secureshift.data.GuardRepositoryJDBC;
import secureshift.data.ShiftRepositoryJDBC;
import secureshift.service.SmsService;
import secureshift.domain.Guard;
import secureshift.domain.Shift;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AssignController {

    @FXML private ComboBox<Shift> shiftBox;
    @FXML private ComboBox<Guard> guardBox;
    @FXML private Label shiftInfoLabel;
    @FXML private Label guardInfoLabel;
    @FXML private Label statusLabel;
    @FXML private Button assignBtn;
    @FXML private Button unassignBtn;

    private final ShiftRepositoryJDBC shiftRepo = new ShiftRepositoryJDBC();
    private final GuardRepositoryJDBC guardRepo = new GuardRepositoryJDBC();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML
    public void initialize() {
        shiftBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Shift s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(""); return; }
                setText((s.getSite() != null ? s.getSite().getName() : "?") + " — " + (s.getStartTime() != null ? s.getStartTime().format(FMT) : "?"));
            }
        });
        shiftBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Shift s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "Select a shift..." : (s.getSite() != null ? s.getSite().getName() : "?") + " — " + (s.getStartTime() != null ? s.getStartTime().format(FMT) : "?"));
            }
        });
        guardBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Guard g, boolean empty) {
                super.updateItem(g, empty);
                setText(empty || g == null ? "" : g.getName() + (g.isAvailable() ? " (Available)" : " (Unavailable)"));
            }
        });
        guardBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Guard g, boolean empty) {
                super.updateItem(g, empty);
                setText(empty || g == null ? "Select a guard..." : g.getName() + (g.isAvailable() ? " (Available)" : " (Unavailable)"));
            }
        });
        shiftBox.setOnAction(e -> { Shift s = shiftBox.getValue(); if (shiftInfoLabel != null && s != null) shiftInfoLabel.setText(String.format("%.1f hrs • %s", s.getDurationHours(), s.getAssignedGuard() != null ? "Assigned: " + s.getAssignedGuard().getName() : "Unassigned")); });
        guardBox.setOnAction(e -> { Guard g = guardBox.getValue(); if (guardInfoLabel != null && g != null) guardInfoLabel.setText((g.isAvailable() ? "Available" : "Unavailable") + (g.getSkills() != null && !g.getSkills().isEmpty() ? " • " + String.join(", ", g.getSkills()) : "")); });
        loadData();
    }

    private void loadData() {
        try {
            shiftBox.setItems(FXCollections.observableArrayList(shiftRepo.loadAllShifts()));
            guardBox.setItems(FXCollections.observableArrayList(guardRepo.loadAllGuards()));
        } catch (Exception e) { setStatus("Error loading data: " + e.getMessage(), true); }
    }

    @FXML
    public void refresh() {
        shiftBox.getSelectionModel().clearSelection();
        guardBox.getSelectionModel().clearSelection();
        if (shiftInfoLabel != null) shiftInfoLabel.setText("");
        if (guardInfoLabel != null) guardInfoLabel.setText("");
        loadData();
    }

    @FXML
    public void assign() {
        Shift shift = shiftBox.getValue();
        Guard guard = guardBox.getValue();
        if (shift == null || guard == null) { setStatus("Select both a shift and a guard.", true); return; }
        shift.assignGuard(guard);
        try { shiftRepo.updateAssignment(shift); SmsService.getInstance().notifyShiftAssigned(guard.getName(), "SIMULATED", shift.getSite() != null ? shift.getSite().getName() : "?", shift.getStartTime(), shift.getEndTime()); refresh(); setStatus("Assigned " + guard.getName() + " to shift", false); }
        catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }

    @FXML
    public void unassign() {
        Shift shift = shiftBox.getValue();
        if (shift == null) { setStatus("Select a shift to unassign.", true); return; }
        if (shift.getAssignedGuard() == null) { setStatus("This shift has no guard assigned.", true); return; }
        try { Guard ug = shift.getAssignedGuard(); String un = ug != null ? ug.getName() : "?"; String us = shift.getSite() != null ? shift.getSite().getName() : "?"; LocalDateTime ut = shift.getStartTime(); shift.setGuard(null); shiftRepo.updateAssignment(shift); SmsService.getInstance().notifyShiftUnassigned(un, "SIMULATED", us, ut); refresh(); setStatus("Guard removed from shift", false); }
        catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }

    private void setStatus(String msg, boolean error) {
        if (statusLabel != null) { statusLabel.setText(msg); statusLabel.setTextFill(error ? Color.RED : Color.GREEN); }
    }
}
