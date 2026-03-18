package secureshift.presentation.controllers;

import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import secureshift.data.GuardRepositoryJDBC;
import secureshift.domain.Guard;

import java.net.URL;
import java.util.List;

public class MapController {

    @FXML private WebView mapView;

    private final GuardRepositoryJDBC guardRepo = new GuardRepositoryJDBC();

    @FXML
    public void initialize() {
        WebEngine engine = mapView.getEngine();

        // ✅ Fixed path
        URL mapUrl = getClass().getResource(
                "/secureshift/presentation/views/map.html");

        if (mapUrl != null) {
            engine.load(mapUrl.toExternalForm());
            engine.documentProperty().addListener((obs, oldDoc, newDoc) -> {
                if (newDoc != null) loadGuardMarkers(engine);
            });
        } else {
            engine.loadContent(
                "<html><body style='background:#1a1a2e;color:white'>" +
                "<h2>Map loading...</h2></body></html>"
            );
        }
    }

    private void loadGuardMarkers(WebEngine engine) {
        try {
            List<Guard> guards = guardRepo.loadAllGuards();
            for (Guard g : guards) {
                engine.executeScript(
                    "if(typeof addGuardMarker==='function') " +
                    "addGuardMarker(" + g.getLatitude() + "," + g.getLongitude() +
                    ",'" + g.getName() + "',false);"
                );
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading guard markers: " + e.getMessage());
        }
    }
}
