package secureshift.presentation.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import secureshift.application.Scheduler;
import secureshift.data.GuardRepositoryJDBC;
import secureshift.data.ShiftRepositoryJDBC;
import secureshift.data.SiteRepositoryJDBC;
import secureshift.domain.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class SimulationController {

    @FXML private WebView simMapView;
    @FXML private Button  btnStartMap;
    @FXML private Button  btnStopMap;
    @FXML private Label   mapStatusLabel;
    @FXML private Slider  speedSlider;
    @FXML private Label   speedLabel;

    @FXML private Button      btnAutoAssign;
    @FXML private Button      btnClearAssignments;
    @FXML private TextArea    assignLog;
    @FXML private Label       assignSummaryLabel;
    @FXML private ProgressBar assignProgress;

    @FXML private TableView<ShiftStatusRow> statusTable;
    @FXML private TableColumn<ShiftStatusRow, String> colShiftId;
    @FXML private TableColumn<ShiftStatusRow, String> colSiteName;
    @FXML private TableColumn<ShiftStatusRow, String> colGuardName;
    @FXML private TableColumn<ShiftStatusRow, String> colStartTime;
    @FXML private TableColumn<ShiftStatusRow, String> colEndTime;
    @FXML private TableColumn<ShiftStatusRow, String> colShiftStatus;
    @FXML private Button btnStartStatusMonitor;
    @FXML private Button btnStopStatusMonitor;
    @FXML private Label  statusMonitorLabel;

    @FXML private PieChart                utilChart;
    @FXML private BarChart<String,Number> siteChart;
    @FXML private LineChart<String,Number> trendChart;
    @FXML private Button btnRefreshDash;
    @FXML private Button btnStartLiveDash;
    @FXML private Button btnStopLiveDash;
    @FXML private Label  dashStatusLabel;
    @FXML private Label  statGuards;
    @FXML private Label  statSites;
    @FXML private Label  statShifts;
    @FXML private Label  statAssigned;

    private final GuardRepositoryJDBC guardRepo = new GuardRepositoryJDBC();
    private final ShiftRepositoryJDBC shiftRepo = new ShiftRepositoryJDBC();
    private final SiteRepositoryJDBC  siteRepo  = new SiteRepositoryJDBC();

    private ScheduledExecutorService mapExecutor;
    private ScheduledExecutorService statusExecutor;
    private ScheduledExecutorService dashExecutor;

    private final Map<String, double[]> guardPos    = new ConcurrentHashMap<>();
    private final Map<String, double[]> guardTarget = new ConcurrentHashMap<>();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM HH:mm");

    @FXML
    public void initialize() {
        if (speedSlider != null)
            speedSlider.valueProperty().addListener((o, ov, nv) ->
                    speedLabel.setText(String.format("%.1fx", nv.doubleValue())));

        if (statusTable != null) {
            colShiftId.setCellValueFactory(c     -> c.getValue().id);
            colSiteName.setCellValueFactory(c    -> c.getValue().site);
            colGuardName.setCellValueFactory(c   -> c.getValue().guard);
            colStartTime.setCellValueFactory(c   -> c.getValue().start);
            colEndTime.setCellValueFactory(c     -> c.getValue().end);
            colShiftStatus.setCellValueFactory(c -> c.getValue().status);

            statusTable.setRowFactory(tv -> new TableRow<>() {
                @Override protected void updateItem(ShiftStatusRow r, boolean empty) {
                    super.updateItem(r, empty);
                    if (r == null || empty) { setStyle(""); return; }
                    switch (r.status.get()) {
                        case "🟢 Active"    -> { setStyle("-fx-background-color:#e3f2fd;"); setTextFill(javafx.scene.paint.Color.BLACK); }
                        case "✅ Completed" -> setStyle("-fx-background-color:#f5f5f5;");
                        default             -> setStyle("-fx-background-color:#e8f5e9;");
                    }
                }
            });
        }

        if (simMapView != null) {
            java.net.URL url = getClass().getResource("/secureshift/presentation/views/map.html");
            if (url != null) {
                WebEngine eng = simMapView.getEngine();
                eng.load(url.toExternalForm());
                eng.documentProperty().addListener((obs, o, n) -> {
                    if (n != null) Platform.runLater(() -> loadMapMarkers(eng));
                });
            }
        }
        refreshDashboard();
    }

    @FXML
    private void startMapSimulation() {
        if (mapExecutor != null && !mapExecutor.isShutdown()) return;
        List<Guard> guards;
        try { guards = guardRepo.loadAllGuards(); }
        catch (Exception e) { setMapStatus("Error: " + e.getMessage(), true); return; }

        for (Guard g : guards) {
            guardPos.put(g.getId(),    new double[]{g.getLatitude(), g.getLongitude()});
            guardTarget.put(g.getId(), pickTarget(g));
        }
        loadMapMarkers(simMapView.getEngine());

        double multiplier = speedSlider != null ? speedSlider.getValue() : 1.0;
        long intervalMs = (long)(3000 / multiplier);
        mapExecutor = Executors.newSingleThreadScheduledExecutor();
        mapExecutor.scheduleAtFixedRate(this::tickMapSimulation, 0, intervalMs, TimeUnit.MILLISECONDS);
        btnStartMap.setDisable(true);
        btnStopMap.setDisable(false);
        setMapStatus("Simulation running", false);
    }

    @FXML
    private void stopMapSimulation() {
        shutdownExecutor(mapExecutor); mapExecutor = null;
        btnStartMap.setDisable(false); btnStopMap.setDisable(true);
        setMapStatus("Simulation stopped", false);
    }

    private void tickMapSimulation() {
        try {
            List<Shift> shifts = shiftRepo.loadAllShifts();
            List<Guard> guards = guardRepo.loadAllGuards();
            WebEngine eng = simMapView.getEngine();
            double speed = 0.0008;
            for (Guard g : guards) {
                double[] pos = guardPos.computeIfAbsent(g.getId(),
                        k -> new double[]{g.getLatitude(), g.getLongitude()});
                double[] tgt = guardTarget.computeIfAbsent(g.getId(), k -> pickTarget(g));
                double dLat = tgt[0] - pos[0], dLon = tgt[1] - pos[1];
                double dist = Math.sqrt(dLat*dLat + dLon*dLon);
                if (dist < 0.001) {
                    guardTarget.put(g.getId(), pickNextTarget(g, shifts));
                } else {
                    pos[0] += (dLat/dist) * Math.min(speed, dist);
                    pos[1] += (dLon/dist) * Math.min(speed, dist);
                }
                final double lat = pos[0], lon = pos[1];
                final boolean assigned = shifts.stream().anyMatch(s ->
                    s.getAssignedGuard() != null && s.getAssignedGuard().getId().equals(g.getId()));
                Platform.runLater(() ->
                    eng.executeScript("if(typeof updateGuardPosition==='function')" +
                        "updateGuardPosition('" + g.getId() + "'," + lat + "," + lon + "," + assigned + ");"));
            }
        } catch (Exception e) { System.err.println("Map tick: " + e.getMessage()); }
    }

    private double[] pickTarget(Guard g) {
        return new double[]{
            g.getLatitude()  + (Math.random()-0.5)*0.05,
            g.getLongitude() + (Math.random()-0.5)*0.05};
    }

    private double[] pickNextTarget(Guard g, List<Shift> shifts) {
        return shifts.stream()
            .filter(s -> s.getAssignedGuard() != null &&
                         s.getAssignedGuard().getId().equals(g.getId()) && s.getSite() != null)
            .findFirst()
            .map(s -> new double[]{s.getSite().getLatitude(), s.getSite().getLongitude()})
            .orElse(new double[]{51.45+Math.random()*0.15, -0.25+Math.random()*0.30});
    }

    private void loadMapMarkers(WebEngine eng) {
        try {
            eng.executeScript("if(typeof clearMap==='function') clearMap();");
            for (Site s : siteRepo.loadAllSites())
                eng.executeScript("if(typeof addSiteMarker==='function')" +
                    "addSiteMarker(" + s.getLatitude() + "," + s.getLongitude() + ",'" + esc(s.getName()) + "');");
            List<Shift> shifts = shiftRepo.loadAllShifts();
            for (Guard g : guardRepo.loadAllGuards()) {
                boolean assigned = shifts.stream().anyMatch(s ->
                    s.getAssignedGuard() != null && s.getAssignedGuard().getId().equals(g.getId()));
                eng.executeScript("if(typeof addGuardMarker==='function')" +
                    "addGuardMarker(" + g.getLatitude() + "," + g.getLongitude() +
                    ",'" + esc(g.getName()) + "'," + assigned + ");");
            }
        } catch (Exception e) { System.err.println("loadMarkers: " + e.getMessage()); }
    }

    private void setMapStatus(String msg, boolean err) {
        Platform.runLater(() -> {
            mapStatusLabel.setText(msg);
            mapStatusLabel.setTextFill(err ? Color.RED : Color.GREEN);
        });
    }

    @FXML
    public void runAutoAssign() {
        btnAutoAssign.setDisable(true);
        assignLog.clear();
        assignProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        new Thread(() -> {
            try {
                List<Shift> unassigned = shiftRepo.loadUnassignedShifts();
                List<Guard> guards     = guardRepo.loadAvailableGuards();
                log("Found " + unassigned.size() + " unassigned shifts");
                log("Found " + guards.size() + " available guards");
                log("-----------------------------------------");
                int assigned = 0, skipped = 0;
                for (int i = 0; i < unassigned.size(); i++) {
                    Shift shift = unassigned.get(i);
                    final int idx = i;
                    Platform.runLater(() -> assignProgress.setProgress((double)idx/unassigned.size()));
                    Guard best = new NearestGuardStrategy().assignGuard(guards, shift);
                    if (best != null) {
                        shift.assignGuard(best);
                        shiftRepo.updateAssignment(shift);
                        log("ASSIGNED: " + shift.getId() + " -> " + best.getName() +
                            " (" + (shift.getSite()!=null?shift.getSite().getName():"?") + ")");
                        assigned++;
                        final String bid = best.getId();
                        guards = guards.stream().filter(g -> !g.getId().equals(bid)).collect(Collectors.toList());
                    } else {
                        log("SKIPPED:  " + shift.getId() + " -> No suitable guard");
                        skipped++;
                    }
                    Thread.sleep(120);
                }
                final int a=assigned, s=skipped;
                Platform.runLater(() -> {
                    assignProgress.setProgress(1.0);
                    assignSummaryLabel.setText(a + " assigned   " + s + " skipped");
                    assignSummaryLabel.setTextFill(s==0 ? Color.GREEN : Color.ORANGE);
                    btnAutoAssign.setDisable(false);
                    log("-----------------------------------------");
                    log("Done: " + a + " assigned, " + s + " skipped.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> { log("ERROR: " + e.getMessage()); assignProgress.setProgress(0); btnAutoAssign.setDisable(false); });
            }
        }, "auto-assign").start();
    }

    @FXML
    public void clearAllAssignments() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remove all guard assignments?", ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    List<Shift> shifts = shiftRepo.loadAllShifts();
                    int count = 0;
                    for (Shift s : shifts) {
                        if (s.getAssignedGuard() != null) { s.setGuard(null); shiftRepo.updateAssignment(s); count++; }
                    }
                    assignLog.clear(); log("Cleared " + count + " assignments");
                    assignProgress.setProgress(0); assignSummaryLabel.setText("");
                } catch (Exception e) { log("ERROR: " + e.getMessage()); }
            }
        });
    }

    private void log(String msg) { Platform.runLater(() -> assignLog.appendText(msg + "\n")); }

    @FXML public void startStatusMonitor() {
        if (statusExecutor != null && !statusExecutor.isShutdown()) return;
        refreshStatusTable();
        statusExecutor = Executors.newSingleThreadScheduledExecutor();
        statusExecutor.scheduleAtFixedRate(() -> Platform.runLater(this::refreshStatusTable), 10, 10, TimeUnit.SECONDS);
        btnStartStatusMonitor.setDisable(true); btnStopStatusMonitor.setDisable(false);
        statusMonitorLabel.setText("Monitoring — refreshes every 10 seconds");
        statusMonitorLabel.setTextFill(Color.GREEN);
    }

    @FXML public void stopStatusMonitor() {
        shutdownExecutor(statusExecutor); statusExecutor = null;
        btnStartStatusMonitor.setDisable(false); btnStopStatusMonitor.setDisable(true);
        statusMonitorLabel.setText("Monitor stopped"); statusMonitorLabel.setTextFill(Color.GRAY);
    }

    @FXML public void refreshStatusNow() { refreshStatusTable(); }

    private void refreshStatusTable() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<ShiftStatusRow> rows = shiftRepo.loadAllShifts().stream().map(s -> {
                String st;
                if (s.getEndTime()!=null && s.getEndTime().isBefore(now))        st = "Completed";
                else if (s.getStartTime()!=null && s.getStartTime().isBefore(now)) st = "Active";
                else                                                               st = "Upcoming";
                return new ShiftStatusRow(s.getId(),
                    s.getSite()!=null?s.getSite().getName():"Unknown",
                    s.getAssignedGuard()!=null?s.getAssignedGuard().getName():"Unassigned",
                    s.getStartTime()!=null?s.getStartTime().format(FMT):"",
                    s.getEndTime()  !=null?s.getEndTime().format(FMT):"", st);
            }).sorted(Comparator.comparing(r -> r.status.get())).toList();

            Platform.runLater(() -> {
                statusTable.setItems(FXCollections.observableArrayList(rows));
                long active=rows.stream().filter(r->r.status.get().contains("Active")).count();
                long upcoming=rows.stream().filter(r->r.status.get().contains("Upcoming")).count();
                long completed=rows.stream().filter(r->r.status.get().contains("Completed")).count();
                statusMonitorLabel.setText(active+" Active  "+upcoming+" Upcoming  "+completed+
                    " Completed  Last: "+LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            });
        } catch (Exception e) { System.err.println("Status refresh: " + e.getMessage()); }
    }

    @FXML
    private void refreshDashboard() {
        new Thread(() -> {
            try {
                List<Guard> guards = guardRepo.loadAllGuards();
                List<Shift> shifts = shiftRepo.loadAllShifts();
                List<Site>  sites  = siteRepo.loadAllSites();
                long assigned  = shifts.stream().filter(s->s.getAssignedGuard()!=null).count();
                long unassigned= shifts.stream().filter(s->s.getAssignedGuard()==null).count();
                Set<String> busyIds = shifts.stream()
                    .filter(s->s.getAssignedGuard()!=null && s.getEndTime()!=null && s.getEndTime().isAfter(LocalDateTime.now()))
                    .map(s->s.getAssignedGuard().getId()).collect(Collectors.toSet());
                long busy=busyIds.size(), idle=guards.size()-busy;
                Map<String,Long> siteCounts = shifts.stream().filter(s->s.getSite()!=null)
                    .collect(Collectors.groupingBy(s->s.getSite().getName(),Collectors.counting()));
                Map<String,Long> trendMap = new TreeMap<>(shifts.stream().filter(s->s.getStartTime()!=null)
                    .collect(Collectors.groupingBy(
                        s->s.getStartTime().toLocalDate().format(DateTimeFormatter.ofPattern("MM/dd")),
                        Collectors.counting())));
                Platform.runLater(() -> {
                    if(statGuards!=null)   statGuards.setText(String.valueOf(guards.size()));
                    if(statSites!=null)    statSites.setText(String.valueOf(sites.size()));
                    if(statShifts!=null)   statShifts.setText(String.valueOf(shifts.size()));
                    if(statAssigned!=null) statAssigned.setText(assigned+"/"+shifts.size());
                    utilChart.getData().clear();
                    utilChart.getData().addAll(
                        new PieChart.Data("Active Duty ("+busy+")", busy),
                        new PieChart.Data("Idle ("+Math.max(idle,0)+")", Math.max(idle,0)),
                        new PieChart.Data("Unassigned Shifts ("+unassigned+")", unassigned));
                    siteChart.getData().clear();
                    XYChart.Series<String,Number> bar = new XYChart.Series<>(); bar.setName("Shifts");
                    siteCounts.forEach((n,c)->bar.getData().add(new XYChart.Data<>(n.length()>12?n.substring(0,12)+"...":n,c)));
                    siteChart.getData().add(bar);
                    trendChart.getData().clear();
                    XYChart.Series<String,Number> line = new XYChart.Series<>(); line.setName("Shifts/Day");
                    trendMap.forEach((d,c)->line.getData().add(new XYChart.Data<>(d,c)));
                    trendChart.getData().add(line);
                    dashStatusLabel.setText("Updated: "+LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    dashStatusLabel.setTextFill(Color.GREEN);
                });
            } catch (Exception e) {
                Platform.runLater(() -> { dashStatusLabel.setText("Error: "+e.getMessage()); dashStatusLabel.setTextFill(Color.RED); });
            }
        },"dash-refresh").start();
    }

    @FXML private void startLiveDashboard() {
        if(dashExecutor!=null && !dashExecutor.isShutdown()) return;
        dashExecutor = Executors.newSingleThreadScheduledExecutor();
        dashExecutor.scheduleAtFixedRate(this::refreshDashboard, 15, 15, TimeUnit.SECONDS);
        btnStartLiveDash.setDisable(true); btnStopLiveDash.setDisable(false);
        dashStatusLabel.setText("Live — auto-refreshes every 15s"); dashStatusLabel.setTextFill(Color.GREEN);
    }

    @FXML private void stopLiveDashboard() {
        shutdownExecutor(dashExecutor); dashExecutor=null;
        btnStartLiveDash.setDisable(false); btnStopLiveDash.setDisable(true);
        dashStatusLabel.setText("Live updates stopped"); dashStatusLabel.setTextFill(Color.GRAY);
    }

    public void shutdown() {
        shutdownExecutor(mapExecutor); shutdownExecutor(statusExecutor); shutdownExecutor(dashExecutor);
    }

    private void shutdownExecutor(ScheduledExecutorService ex) { if(ex!=null && !ex.isShutdown()) ex.shutdownNow(); }
    private String esc(String s) { return s==null?"":s.replace("'","\\'"); }

    public static class ShiftStatusRow {
        public final javafx.beans.property.SimpleStringProperty id,site,guard,start,end,status;
        ShiftStatusRow(String id,String site,String guard,String start,String end,String status){
            this.id=new javafx.beans.property.SimpleStringProperty(id);
            this.site=new javafx.beans.property.SimpleStringProperty(site);
            this.guard=new javafx.beans.property.SimpleStringProperty(guard);
            this.start=new javafx.beans.property.SimpleStringProperty(start);
            this.end=new javafx.beans.property.SimpleStringProperty(end);
            this.status=new javafx.beans.property.SimpleStringProperty(status);
        }
    }
}
