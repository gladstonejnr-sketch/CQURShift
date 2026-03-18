package secureshift.presentation.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import secureshift.data.GuardRepositoryJDBC;
import secureshift.data.ShiftRepositoryJDBC;
import secureshift.domain.Guard;
import secureshift.domain.Shift;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationManager {

    public enum Level { INFO, WARN, DANGER }

    private VBox toastContainer;
    private ScheduledExecutorService scheduler;

    private final GuardRepositoryJDBC guardRepo = new GuardRepositoryJDBC();
    private final ShiftRepositoryJDBC shiftRepo = new ShiftRepositoryJDBC();

    public void startMonitoring(StackPane overlay) {
        toastContainer = new VBox(8);
        toastContainer.setAlignment(Pos.TOP_RIGHT);
        toastContainer.setPickOnBounds(false);
        toastContainer.setPadding(new Insets(70, 16, 0, 0));
        toastContainer.setMaxWidth(340);
        toastContainer.setMaxHeight(Double.MAX_VALUE);
        StackPane.setAlignment(toastContainer, Pos.TOP_RIGHT);
        overlay.getChildren().add(toastContainer);

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "notification-monitor");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::runChecks, 3, 60, TimeUnit.SECONDS);
    }

    public void shutdown() { if (scheduler != null) scheduler.shutdownNow(); }

    public void toast(String message, Level level) {
        Platform.runLater(() -> showToast(message, level));
    }

    private void runChecks() {
        List<String> alerts = new ArrayList<>();
        List<Level>  levels = new ArrayList<>();
        try {
            List<Shift> shifts = shiftRepo.loadAllShifts();
            List<Guard> guards = guardRepo.loadAllGuards();
            LocalDateTime now  = LocalDateTime.now();

            long unassigned = shifts.stream()
                    .filter(s -> s.getAssignedGuard() == null)
                    .filter(s -> s.getStartTime() != null && s.getStartTime().isAfter(now))
                    .count();
            if (unassigned > 0) {
                alerts.add("⚠ " + unassigned + " upcoming shift" + (unassigned > 1 ? "s are" : " is") + " unassigned");
                levels.add(Level.WARN);
            }

            long soonUnassigned = shifts.stream()
                    .filter(s -> s.getAssignedGuard() == null)
                    .filter(s -> s.getStartTime() != null)
                    .filter(s -> { long mins = java.time.Duration.between(now, s.getStartTime()).toMinutes(); return mins >= 0 && mins <= 120; })
                    .count();
            if (soonUnassigned > 0) {
                alerts.add("🚨 " + soonUnassigned + " shift" + (soonUnassigned > 1 ? "s start" : " starts") + " within 2 hrs — no guard!");
                levels.add(Level.DANGER);
            }

            long unavailableAssigned = shifts.stream()
                    .filter(s -> s.getAssignedGuard() != null && !s.getAssignedGuard().isAvailable())
                    .filter(s -> s.getStartTime() != null && s.getStartTime().isAfter(now))
                    .count();
            if (unavailableAssigned > 0) {
                alerts.add("⚠ " + unavailableAssigned + " shift" + (unavailableAssigned > 1 ? "s assigned to" : " assigned to an") + " unavailable guard");
                levels.add(Level.WARN);
            }

            long available = guards.stream().filter(Guard::isAvailable).count();
            if (available == 0 && !guards.isEmpty()) {
                alerts.add("🚨 No guards are currently available");
                levels.add(Level.DANGER);
            }

        } catch (Exception e) {
            alerts.add("ℹ Could not check shift status");
            levels.add(Level.INFO);
        }

        for (int i = 0; i < alerts.size(); i++) {
            final String msg = alerts.get(i);
            final Level level = levels.get(i);
            final long delay = i * 600L;
            scheduler.schedule(() -> Platform.runLater(() -> showToast(msg, level)), delay, TimeUnit.MILLISECONDS);
        }
    }

    private void showToast(String message, Level level) {
        HBox toast = buildToast(message, level);
        toast.setOpacity(0);
        toast.setTranslateX(60);
        toastContainer.getChildren().add(toast);

        ParallelTransition enter = new ParallelTransition(fade(toast, 0, 1, 300), slide(toast, 60, 0, 300));
        PauseTransition hold = new PauseTransition(Duration.seconds(5));
        ParallelTransition exit = new ParallelTransition(fade(toast, 1, 0, 400), slide(toast, 0, 60, 400));
        exit.setOnFinished(e -> toastContainer.getChildren().remove(toast));
        SequentialTransition seq = new SequentialTransition(enter, hold, exit);
        seq.play();

        toast.setOnMouseClicked(e -> {
            seq.stop();
            ParallelTransition quickExit = new ParallelTransition(fade(toast, toast.getOpacity(), 0, 200), slide(toast, toast.getTranslateX(), 60, 200));
            quickExit.setOnFinished(ev -> toastContainer.getChildren().remove(toast));
            quickExit.play();
        });
    }

    private HBox buildToast(String message, Level level) {
        String bg     = switch (level) { case DANGER -> "rgba(220,38,38,0.92)"; case WARN -> "rgba(161,98,7,0.92)";   case INFO -> "rgba(30,64,175,0.92)"; };
        String border = switch (level) { case DANGER -> "#f87171";              case WARN -> "#fbbf24";               case INFO -> "#60a5fa"; };
        Label text = new Label(message);
        text.setTextFill(Color.WHITE);
        text.setWrapText(true);
        text.setMaxWidth(280);
        text.setStyle("-fx-font-size: 13px; -fx-font-weight: 500;");
        HBox box = new HBox(text);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 16, 12, 16));
        box.setMaxWidth(320);
        box.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:10;-fx-border-color:" + border + ";-fx-border-radius:10;-fx-border-width:1;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),12,0,0,4);-fx-cursor:hand;");
        return box;
    }

    private FadeTransition fade(javafx.scene.Node n, double from, double to, int ms) { FadeTransition ft = new FadeTransition(Duration.millis(ms), n); ft.setFromValue(from); ft.setToValue(to); return ft; }
    private TranslateTransition slide(javafx.scene.Node n, double fx, double tx, int ms) { TranslateTransition tt = new TranslateTransition(Duration.millis(ms), n); tt.setFromX(fx); tt.setToX(tx); return tt; }
}
