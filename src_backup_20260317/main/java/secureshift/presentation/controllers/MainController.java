package secureshift.presentation.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import secureshift.UserSession;
import secureshift.service.SmsService;
import secureshift.data.GuardRepositoryJDBC;
import secureshift.data.ShiftRepositoryJDBC;
import secureshift.data.SiteRepositoryJDBC;
import secureshift.domain.Guard;
import secureshift.domain.Shift;
import secureshift.domain.Site;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainController {

    @FXML private WebView   mapView;
    @FXML private Button    simulationBtn;
    @FXML private Label     userLabel;
    @FXML private StackPane notificationOverlay;

    private final GuardRepositoryJDBC guardRepo = new GuardRepositoryJDBC();
    private final ShiftRepositoryJDBC shiftRepo = new ShiftRepositoryJDBC();
    private final SiteRepositoryJDBC  siteRepo  = new SiteRepositoryJDBC();

    private final Map<String, Double> currentSpeeds = new HashMap<>();
    private final Map<String, Double> targetSpeeds  = new HashMap<>();
    private final double acceleration = 0.00002;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "map-updater"); t.setDaemon(true); return t;
            });
    private final NotificationManager notifications = new NotificationManager();
    private static final String FXML_BASE = "/secureshift/presentation/views/";

    @FXML
    public void initialize() {
        applyRoleRestrictions();
        loadMap();
        Platform.runLater(() -> {
            if (notificationOverlay != null) { notifications.startMonitoring(notificationOverlay); SmsService.getInstance().init(notifications); }
        });
    }

    private void applyRoleRestrictions() {
        UserSession session = UserSession.getInstance();
        if (userLabel != null) userLabel.setText("👤 " + session.getUsername() + "  [" + (session.isAdmin() ? "Admin" : "Manager") + "]");
        if (simulationBtn != null && !session.isAdmin()) { simulationBtn.setVisible(false); simulationBtn.setManaged(false); }
    }

    @FXML public void openGuards()    throws Exception { loadWindow("guards.fxml",    "Manage Guards"); }
    @FXML public void openSites()     throws Exception { loadWindow("sites.fxml",     "Manage Sites"); }
    @FXML public void openShifts()    throws Exception { loadWindow("shifts.fxml",    "Manage Shifts"); }
    @FXML public void openAssign()    throws Exception { loadWindow("assign.fxml",    "Assign Shift"); }
    @FXML public void openDashboard() throws Exception { loadWindow("dashboard.fxml", "Dashboard"); }
    @FXML public void openMap()       throws Exception { loadWindow("map.fxml",       "Guard Locations"); }
    @FXML public void openSimulation() throws Exception {
        if (!UserSession.getInstance().isAdmin()) return;
        loadWindow("simulation.fxml", "⚡ Simulation Centre");
    }

    @FXML
    public void logout() {
        scheduler.shutdownNow();
        notifications.shutdown();
        UserSession.getInstance().logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_BASE + "login.fxml"));
            Scene scene = new Scene(loader.load(), 480, 560);
            scene.getStylesheets().add(getClass().getResource("/secureshift/presentation/css/liquid-glass.css").toExternalForm());
            Stage loginStage = new Stage();
            loginStage.setTitle("C-QURShift — Sign In");
            loginStage.setResizable(false);
            loginStage.setScene(scene);
            loginStage.show();
            ((Stage) mapView.getScene().getWindow()).close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void refreshMap() {
        WebEngine engine = mapView.getEngine();
        engine.executeScript("if(typeof clearMap==='function') clearMap();");
        loadMarkers(engine);
        drawCurvedRoutes(engine);
    }

    private void loadWindow(String fxml, String title) throws Exception {
        java.net.URL url = getClass().getResource(FXML_BASE + fxml);
        if (url == null) { System.err.println("❌ Cannot find FXML: " + fxml); return; }
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(FXMLLoader.load(url)));
        stage.show();
    }

    private void loadMap() {
        WebEngine engine = mapView.getEngine();
        java.net.URL mapUrl = getClass().getResource(FXML_BASE + "map.html");
        if (mapUrl != null) {
            engine.load(mapUrl.toExternalForm());
            engine.documentProperty().addListener((obs, oldDoc, newDoc) -> {
                if (newDoc != null) { initGuardSpeeds(guardRepo.loadAllGuards()); loadMarkers(engine); drawCurvedRoutes(engine); startLiveUpdates(engine); }
            });
        }
    }

    private void loadMarkers(WebEngine engine) {
        try {
            List<Guard> guards = guardRepo.loadAllGuards();
            List<Shift> shifts = shiftRepo.loadAllShifts();
            List<Site>  sites  = siteRepo.loadAllSites();
            for (Guard g : guards) {
                boolean assigned = shifts.stream().anyMatch(s -> s.getAssignedGuard() != null && s.getAssignedGuard().getId().equals(g.getId()));
                engine.executeScript("if(typeof addGuardMarker==='function') addGuardMarker(" + g.getLatitude() + "," + g.getLongitude() + ",'" + g.getName() + "'," + assigned + ");");
            }
            for (Site site : sites) {
                engine.executeScript("if(typeof addSiteMarker==='function') addSiteMarker(" + site.getLatitude() + "," + site.getLongitude() + ",'" + site.getName() + "');");
            }
        } catch (Exception e) { System.err.println("❌ Error loading markers: " + e.getMessage()); }
    }

    private void drawCurvedRoutes(WebEngine engine) {
        try {
            for (Shift shift : shiftRepo.loadAllShifts()) {
                Guard g = shift.getAssignedGuard();
                if (g == null || shift.getSite() == null) continue;
                engine.executeScript("if(typeof drawCurvedRoute==='function') drawCurvedRoute(" + g.getLatitude() + "," + g.getLongitude() + "," + shift.getSite().getLatitude() + "," + shift.getSite().getLongitude() + ");");
            }
        } catch (Exception e) { System.err.println("❌ Error drawing routes: " + e.getMessage()); }
    }

    private void initGuardSpeeds(List<Guard> guards) {
        for (Guard g : guards) { currentSpeeds.putIfAbsent(g.getId(), 0.0); targetSpeeds.putIfAbsent(g.getId(), randomTargetSpeed()); }
    }

    private void startLiveUpdates(WebEngine engine) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (Shift shift : shiftRepo.loadAllShifts()) {
                    Guard g = shift.getAssignedGuard();
                    if (g == null || shift.getSite() == null) continue;
                    double speed = updateSpeed(g.getId());
                    double newLat = moveToward(g.getLatitude(), shift.getSite().getLatitude(), speed);
                    double newLon = moveToward(g.getLongitude(), shift.getSite().getLongitude(), speed);
                    g.setLatitude(newLat); g.setLongitude(newLon);
                    Platform.runLater(() -> engine.executeScript("if(typeof updateGuardPosition==='function') updateGuardPosition('" + g.getId() + "'," + newLat + "," + newLon + ");"));
                }
            } catch (Exception e) { System.err.println("❌ Live update error: " + e.getMessage()); }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private double moveToward(double current, double target, double speed) {
        if (Math.abs(target - current) < speed) return target;
        return current + Math.signum(target - current) * speed;
    }

    private double updateSpeed(String guardId) {
        double current = currentSpeeds.getOrDefault(guardId, 0.0);
        double target  = targetSpeeds.getOrDefault(guardId, randomTargetSpeed());
        if (Math.abs(target - current) < acceleration) { current = target; targetSpeeds.put(guardId, randomTargetSpeed()); }
        else { current += Math.signum(target - current) * acceleration; }
        currentSpeeds.put(guardId, current);
        return current;
    }

    private double randomTargetSpeed() { return 0.00005 + Math.random() * 0.00015; }
    public void shutdown() { scheduler.shutdownNow(); notifications.shutdown(); }
}
