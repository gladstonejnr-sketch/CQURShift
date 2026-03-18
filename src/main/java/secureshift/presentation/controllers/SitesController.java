package secureshift.presentation.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import secureshift.data.SiteRepositoryJDBC;
import secureshift.domain.Site;
import secureshift.service.ExportService;

import java.util.Arrays;
import java.util.List;

public class SitesController {

    @FXML private TableView<Site> sitesTable;
    @FXML private TableColumn<Site, String> colId;
    @FXML private TableColumn<Site, String> colName;
    @FXML private TableColumn<Site, String> colAddress;
    @FXML private TableColumn<Site, String> colRegion;
    @FXML private TableColumn<Site, String> colLatitude;
    @FXML private TableColumn<Site, String> colLongitude;
    @FXML private TableColumn<Site, String> colSkills;
    @FXML private Label statusLabel;

    private final SiteRepositoryJDBC repo = new SiteRepositoryJDBC();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colAddress.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAddress() != null ? c.getValue().getAddress() : ""));
        colRegion.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRegion() != null ? c.getValue().getRegion() : ""));
        colLatitude.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.4f", c.getValue().getLatitude())));
        colLongitude.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.4f", c.getValue().getLongitude())));
        colSkills.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getRequiredSkills() != null ? String.join(", ", c.getValue().getRequiredSkills()) : ""));
        refresh();
    }

    @FXML
    public void refresh() {
        try {
            List<Site> sites = repo.loadAllSites();
            sitesTable.setItems(FXCollections.observableArrayList(sites));
            setStatus("Loaded " + sites.size() + " sites", false);
        } catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
    }

    @FXML
    public void addSite() { showSiteDialog(null); }

    @FXML
    public void editSite() {
        Site selected = sitesTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Select a site to edit."); return; }
        showSiteDialog(selected);
    }

    @FXML
    public void deleteSite() {
        Site selected = sitesTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Select a site to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + selected.getName() + "?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try { repo.deleteSite(selected.getId()); refresh(); setStatus("Site deleted", false); }
                catch (Exception e) { setStatus("Error: " + e.getMessage(), true); }
            }
        });
    }

    private void showSiteDialog(Site existing) {
        Dialog<Site> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Site" : "Edit Site");
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField nameField    = new TextField(existing != null ? existing.getName() : "");
        TextField addressField = new TextField(existing != null && existing.getAddress() != null ? existing.getAddress() : "");
        TextField regionField  = new TextField(existing != null && existing.getRegion() != null ? existing.getRegion() : "");
        TextField latField     = new TextField(existing != null ? String.valueOf(existing.getLatitude()) : "51.5074");
        TextField lonField     = new TextField(existing != null ? String.valueOf(existing.getLongitude()) : "-0.1278");
        TextField skillsField  = new TextField(existing != null && existing.getRequiredSkills() != null ? String.join(", ", existing.getRequiredSkills()) : "");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        grid.addRow(0, new Label("Name:"), nameField);
        grid.addRow(1, new Label("Address:"), addressField);
        grid.addRow(2, new Label("Region:"), regionField);
        grid.addRow(3, new Label("Latitude:"), latField);
        grid.addRow(4, new Label("Longitude:"), lonField);
        grid.addRow(5, new Label("Skills:"), skillsField);
        nameField.setPrefWidth(260);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn && !nameField.getText().isBlank()) {
                Site s = existing != null ? existing : new Site();
                s.setName(nameField.getText().trim());
                s.setAddress(addressField.getText().trim());
                s.setRegion(regionField.getText().trim());
                s.setLatitude(parseDouble(latField.getText(), 0.0));
                s.setLongitude(parseDouble(lonField.getText(), 0.0));
                s.setRequiredSkills(skillsField.getText().isBlank() ? List.of()
                        : Arrays.stream(skillsField.getText().split(",")).map(String::trim).filter(t -> !t.isEmpty()).toList());
                return s;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(site -> {
            try {
                if (existing == null) repo.saveSite(site); else repo.updateSite(site);
                refresh(); setStatus("Site saved", false);
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
            java.nio.file.Path p = new ExportService().exportSitesCsv(sitesTable.getItems());
            setStatus("CSV saved: " + p.getFileName(), false);
        } catch (Exception e) { setStatus("Export failed: " + e.getMessage(), true); }
    }

    @FXML
    public void exportPdf() {
        try {
            new ExportService().exportSitesPdf(sitesTable.getItems());
            setStatus("PDF report opened in browser", false);
        } catch (Exception e) { setStatus("Export failed: " + e.getMessage(), true); }
    }
}
