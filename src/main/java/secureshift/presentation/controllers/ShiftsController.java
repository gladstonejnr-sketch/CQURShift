package secureshift.presentation.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import secureshift.data.ShiftRepositoryJDBC;
import secureshift.service.SmsService;
import secureshift.data.SiteRepositoryJDBC;
import secureshift.domain.Guard;
import secureshift.domain.Shift;
import secureshift.service.ExportService;
import secureshift.domain.Site;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ShiftsController {

    @FXML private TableView<Shift> shiftsTable;
    @FXML private TableColumn<Shift, String> colId;
    @FXML private TableColumn<Shift, String> colSite;
    @FXML private TableColumn<Shift, String> colStart;
    @FXML private TableColumn<Shift, String> colEnd;
    @FXML private TableColumn<Shift, String> colDuration;
    @FXML private TableColumn<Shift, String> colGuard;
    @FXML private TableColumn<Shift, String> colStatus;
    @FXML private Label statusLabel;

    private final ShiftRepositoryJDBC shiftRepo = new ShiftRepositoryJDBC();
    private final SiteRepositoryJDBC  siteRepo  = new SiteRepositoryJDBC();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        colSite.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSite() != null ? c.getValue().getSite().getName() : "Unknown"));
        colStart.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStartTime() != null ? c.getValue().getStartTime().format(FMT) : ""));
        colEnd.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEndTime() != null ? c.getValue().getEndTime().format(FMT) : ""));
        colDuration.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("%.1f hrs", c.getValue().getDurationHours())));
        colGuard.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getAssignedGuard() != null ? c.getValue().getAssignedGuard().getName() : "Unassigned"));
        colStatus.setCellValueFactory(c -> {
            Shift s = c.getValue();
            LocalDateTime now = LocalDateTime.now();
            String status = (s.getEndTime() != null && s.getEndTime().isBefore(now)) ? "Completed"
                    : (s.getStartTime() != null && s.getStartTime().isBefore(now)) ? "Active" : "Upcoming";
            return new SimpleStringProperty(status);
        });
        refresh();
    }

    @FXML
    public void refresh() {
        try {
            List<Shift> shifts = shiftRepo.loadAllShifts();
            shiftsTable.setItems(FXCollections.observableArrayList(shifts));
            setStatus("Loaded " + shifts.size() + " shifts", false);
        } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }

    @FXML
    public void createShift() {
        List<Site> sites;
        try { sites = siteRepo.loadAllSites(); } catch (Exception e) { setStatus("Error loading sites: " + e.getMessage(), true); return; }
        if (sites.isEmpty()) { new Alert(Alert.AlertType.WARNING, "No sites found. Add a site first.", ButtonType.OK).showAndWait(); return; }

        Dialog<Shift> dialog = new Dialog<>();
        dialog.setTitle("Create Shift");
        ButtonType saveBtn = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        ComboBox<Site> siteBox = new ComboBox<>(FXCollections.observableArrayList(sites));
        siteBox.setPromptText("Select site...");
        siteBox.setPrefWidth(260);
        siteBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Site s, boolean empty) { super.updateItem(s, empty); setText(empty || s == null ? "" : s.getName()); }
        });
        siteBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Site s, boolean empty) { super.updateItem(s, empty); setText(empty || s == null ? "" : s.getName()); }
        });

        DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1));
        TextField startTime = new TextField("08:00");
        TextField endTime = new TextField("16:00");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Site:"), siteBox);
        grid.addRow(1, new Label("Date:"), datePicker);
        grid.addRow(2, new Label("Start (HH:mm):"), startTime);
        grid.addRow(3, new Label("End (HH:mm):"), endTime);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn && siteBox.getValue() != null && datePicker.getValue() != null) {
                try {
                    LocalDate d = datePicker.getValue();
                    String[] sp = startTime.getText().split(":"), ep = endTime.getText().split(":");
                    LocalDateTime start = d.atTime(Integer.parseInt(sp[0]), Integer.parseInt(sp[1]));
                    LocalDateTime end   = d.atTime(Integer.parseInt(ep[0]), Integer.parseInt(ep[1]));
                    if (!end.isAfter(start)) { new Alert(Alert.AlertType.ERROR, "End must be after start.", ButtonType.OK).showAndWait(); return null; }
                    return new Shift(null, siteBox.getValue(), start, end, siteBox.getValue().getRequiredSkills());
                } catch (Exception e) { new Alert(Alert.AlertType.ERROR, "Invalid time. Use HH:mm", ButtonType.OK).showAndWait(); }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(shift -> {
            try { shiftRepo.saveShift(shift); SmsService.getInstance().notifyShiftCreated(shift.getSite() != null ? shift.getSite().getName() : "?", shift.getStartTime(), shift.getEndTime()); refresh(); setStatus("Shift created", false); }
            catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
        });
    }

    @FXML
    public void deleteShift() {
        Shift selected = shiftsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { new Alert(Alert.AlertType.WARNING, "Select a shift to delete.", ButtonType.OK).showAndWait(); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this shift?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try { Guard dg = selected.getAssignedGuard(); if (dg != null) SmsService.getInstance().notifyShiftDeleted(dg.getName(), "SIMULATED", selected.getSite() != null ? selected.getSite().getName() : "?", selected.getStartTime()); shiftRepo.deleteShift(selected.getId()); refresh(); setStatus("Shift deleted", false); }
                catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
            }
        });
    }

    @FXML
    public void exportCsv() {
        try {
            java.nio.file.Path p = new ExportService().exportShiftsCsv(shiftsTable.getItems());
            setStatus("CSV saved: " + p.getFileName(), false);
        } catch (Exception e) { setStatus("Export failed: " + e.getMessage(), true); }
    }

    @FXML
    public void exportPdf() {
        try {
            new ExportService().exportShiftsPdf(shiftsTable.getItems());
            setStatus("PDF report opened in browser", false);
        } catch (Exception e) { setStatus("Export failed: " + e.getMessage(), true); }
    }

    private void setStatus(String msg, boolean error) {
        if (statusLabel != null) { statusLabel.setText(msg); statusLabel.setTextFill(error ? Color.RED : Color.GREEN); }
    }
}
