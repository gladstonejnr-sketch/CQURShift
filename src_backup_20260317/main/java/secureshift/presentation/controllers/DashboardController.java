package secureshift.presentation.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;

// ✅ Correct domain imports - fixes all 9 errors
import secureshift.data.GuardRepositoryJDBC;
import secureshift.data.ShiftRepositoryJDBC;
import secureshift.data.SiteRepositoryJDBC;
import secureshift.domain.Guard;
import secureshift.domain.Shift;
import secureshift.domain.Site;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private PieChart guardUtilChart;
    @FXML private BarChart<String, Number> siteShiftChart;
    @FXML private LineChart<String, Number> shiftTrendChart;
    @FXML private DatePicker startDate;
    @FXML private DatePicker endDate;
    @FXML private ComboBox<String> regionBox;
    @FXML private ComboBox<String> skillBox;

    private final GuardRepositoryJDBC guardRepo = new GuardRepositoryJDBC();
    private final ShiftRepositoryJDBC shiftRepo = new ShiftRepositoryJDBC();
    private final SiteRepositoryJDBC siteRepo = new SiteRepositoryJDBC();

    // ✅ Single initialize() method
    @FXML
    public void initialize() {
        loadFilters();
        List<Shift> shifts = shiftRepo.loadAllShifts();
        updateCharts(shifts);
    }

    // ✅ Load filter dropdowns
    private void loadFilters() {
        try {
            regionBox.getItems().add("All Regions");
            regionBox.getItems().addAll(siteRepo.loadAllRegions());
            regionBox.getSelectionModel().select("All Regions");

            skillBox.getItems().add("All Skills");
            skillBox.getItems().addAll(siteRepo.loadAllSkills());
            skillBox.getSelectionModel().select("All Skills");

        } catch (Exception e) {
            System.err.println("❌ Error loading filters: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Apply filters
    @FXML
    public void applyFilters() {
        try {
            List<Shift> shifts = shiftRepo.loadAllShifts();

            if (startDate.getValue() != null)
                shifts = shifts.stream()
                        .filter(s -> s.getStartTime() != null &&
                                !s.getStartTime().toLocalDate()
                                        .isBefore(startDate.getValue()))
                        .toList();

            if (endDate.getValue() != null)
                shifts = shifts.stream()
                        .filter(s -> s.getStartTime() != null &&
                                !s.getStartTime().toLocalDate()
                                        .isAfter(endDate.getValue()))
                        .toList();

            if (regionBox.getValue() != null &&
                    !regionBox.getValue().equals("All Regions"))
                shifts = shifts.stream()
                        .filter(s -> s.getSite() != null &&
                                s.getSite().getRegion() != null &&
                                s.getSite().getRegion()
                                        .equals(regionBox.getValue()))
                        .toList();

            if (skillBox.getValue() != null &&
                    !skillBox.getValue().equals("All Skills"))
                shifts = shifts.stream()
                        .filter(s -> s.getRequiredSkills() != null &&
                                s.getRequiredSkills()
                                        .contains(skillBox.getValue()))
                        .toList();

            updateCharts(shifts);

        } catch (Exception e) {
            System.err.println("❌ Error applying filters: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Update all charts
    private void updateCharts(List<Shift> shifts) {
        guardUtilChart.getData().clear();
        siteShiftChart.getData().clear();
        shiftTrendChart.getData().clear();

        updateGuardUtilisation(shifts);
        updateSiteShiftCounts(shifts);
        updateShiftTrend(shifts);
    }

    // ✅ Pie chart - guard utilisation
    private void updateGuardUtilisation(List<Shift> shifts) {
        try {
            long assigned = shifts.stream()
                    .filter(s -> s.getAssignedGuard() != null)
                    .count();
            long totalGuards = guardRepo.loadAllGuards().size();
            long unassigned = Math.max(totalGuards - assigned, 0);

            guardUtilChart.getData().add(
                    new PieChart.Data("Assigned", assigned));
            guardUtilChart.getData().add(
                    new PieChart.Data("Unassigned", unassigned));

        } catch (Exception e) {
            System.err.println("❌ Error updating guard utilisation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Bar chart - shifts per site
    private void updateSiteShiftCounts(List<Shift> shifts) {
        try {
            Map<String, Long> counts = shifts.stream()
                    .filter(s -> s.getSite() != null)
                    .collect(Collectors.groupingBy(
                            s -> s.getSite().getName(),
                            Collectors.counting()
                    ));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Shifts per Site");

            siteRepo.loadAllSites().forEach(site ->
                    series.getData().add(new XYChart.Data<>(
                            site.getName(),
                            counts.getOrDefault(site.getName(), 0L)))
            );

            siteShiftChart.getData().add(series);

        } catch (Exception e) {
            System.err.println("❌ Error updating site shift counts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ Line chart - shifts over time
    private void updateShiftTrend(List<Shift> shifts) {
        try {
            Map<String, Long> trend = shifts.stream()
                    .filter(s -> s.getStartTime() != null)
                    .collect(Collectors.groupingBy(
                            s -> s.getStartTime().toLocalDate()
                                    .format(DateTimeFormatter.ISO_DATE),
                            Collectors.counting()
                    ));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Shifts Over Time");

            trend.forEach((date, count) ->
                    series.getData().add(new XYChart.Data<>(date, count))
            );

            shiftTrendChart.getData().add(series);

        } catch (Exception e) {
            System.err.println("❌ Error updating shift trend: " + e.getMessage());
            e.printStackTrace();
        }
    }
}



