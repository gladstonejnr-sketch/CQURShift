package secureshift.presentation.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import secureshift.data.GuardRepositoryJDBC;
import secureshift.domain.Guard;
import secureshift.service.ExportService;

import java.util.Arrays;
import java.util.List;

public class GuardsController {

    @FXML private TableView<Guard> guardsTable;
    @FXML private TableColumn<Guard, String> colId;
    @FXML private TableColumn<Guard, String> colName;
    @FXML private TableColumn<Guard, String> colLatitude;
    @FXML private TableColumn<Guard, String> colLongitude;
    @FXML private TableColumn<Guard, String> colAvailable;
    @FXML private TableColumn<Guard, String> colSkills;
    @FXML private Label statusLabel;

    private final GuardRepositoryJDBC repo = new GuardRepositoryJDBC();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colLatitude.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.4f", c.getValue().getLatitude())));
        colLongitude.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.4f", c.getValue().getLongitude())));
        colAvailable.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isAvailable() ? "Yes" : "No"));
        colSkills.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSkills() != null ? String.join(", ", c.getValue().getSkills()) : ""));
        refresh();
    }

    @FXML
    public void refresh() {
        try {
            List<Guard> guards = repo.loadAllGuards();
            guardsTable.setItems(FXCollections.observableArrayList(guards));
            setStatus("Loaded " + guards.size() + " guards", false);
        } catch (Exception e) {
            setStatus("Error loading guards: " + e.getMessage(), true);
        }
    }

    @FXML
    public void addGuard() { showGuardDialog(null); }

    @FXML
    public void editGuard() {
        Guard selected = guardsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Please select a guard to edit."); return; }
        showGuardDialog(selected);
    }

    @FXML
    public void deleteGuard() {
        Guard selected = guardsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Please select a guard to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Guard");
        confirm.setHeaderText("Delete " + selected.getName() + "?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try { repo.deleteGuard(selected.getId()); refresh(); setStatus("Guard deleted", false); }
                catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
            }
        });
    }

    @FXML
    public void toggleAvailability() {
        Guard selected = guardsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Please select a guard."); return; }
        selected.setAvailable(!selected.isAvailable());
        try { repo.updateGuard(selected); refresh(); }
        catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }

    private void showGuardDialog(Guard existing) {
        Dialog<Guard> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Guard" : "Edit Guard");
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField nameField  = new TextField(existing != null ? existing.getName() : "");
        TextField latField   = new TextField(existing != null ? String.valueOf(existing.getLatitude()) : "51.5074");
        TextField lonField   = new TextField(existing != null ? String.valueOf(existing.getLongitude()) : "-0.1278");
        TextField skillsField = new TextField(existing != null && existing.getSkills() != null
                ? String.join(", ", existing.getSkills()) : "");
        CheckBox availBox = new CheckBox("Available");
        availBox.setSelected(existing == null || existing.isAvailable());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Name:"), nameField);
        grid.addRow(1, new Label("Latitude:"), latField);
        grid.addRow(2, new Label("Longitude:"), lonField);
        grid.addRow(3, new Label("Skills:"), skillsField);
        grid.addRow(4, new Label(""), availBox);
        nameField.setPrefWidth(260);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn && !nameField.getText().isBlank()) {
                String id = existing != null ? existing.getId() : "G" + System.currentTimeMillis();
                double lat = parseDouble(latField.getText(), 0.0);
                double lon = parseDouble(lonField.getText(), 0.0);
                List<String> skills = skillsField.getText().isBlank() ? List.of()
                        : Arrays.stream(skillsField.getText().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
                return new Guard(id, nameField.getText().trim(), lat, lon, availBox.isSelected(), skills);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(guard -> {
            try {
                if (existing == null) repo.saveGuard(guard); else repo.updateGuard(guard);
                refresh(); setStatus("Guard saved: " + guard.getName(), false);
            } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
        });
    }

    private double parseDouble(String s, double fallback) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return fallback; }
    }
    private void setStatus(String msg, boolean error) {
        if (statusLabel != null) { statusLabel.setText(msg); statusLabel.setTextFill(error ? Color.RED : Color.GREEN); }
    }
    private void showAlert(String msg) { new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait(); }

    @FXML
    public void exportCsv() {
        try {
            java.nio.file.Path p = new ExportService().exportGuardsCsv(guardsTable.getItems());
            setStatus("CSV saved: " + p.getFileName(), false);
        } catch (Exception e) { setStatus("Export failed: " + e.getMessage(), true); }
    }

    @FXML
    public void exportPdf() {
        try {
            new ExportService().exportGuardsPdf(guardsTable.getItems());
            setStatus("PDF report opened in browser", false);
        } catch (Exception e) { setStatus("Export failed: " + e.getMessage(), true); }
    }
}
