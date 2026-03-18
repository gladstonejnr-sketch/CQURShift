package secureshift.service;

import secureshift.presentation.controllers.NotificationManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SmsService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM HH:mm");
    private static SmsService instance;
    private NotificationManager notificationManager;

    private SmsService() {}

    public static SmsService getInstance() {
        if (instance == null) instance = new SmsService();
        return instance;
    }

    public void init(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    public void notifyShiftAssigned(String guardName, String guardPhone, String siteName, LocalDateTime start, LocalDateTime end) {
        String msg = String.format("📋 Shift assigned to %s at %s — %s to %s", guardName, siteName,
                start != null ? start.format(FMT) : "?", end != null ? end.format(FMT) : "?");
        sendSms(guardPhone, msg);
        toast("📱 SMS → " + guardName + ": shift assigned at " + siteName, NotificationManager.Level.INFO);
    }

    public void notifyShiftUnassigned(String guardName, String guardPhone, String siteName, LocalDateTime start) {
        String msg = String.format("⚠ Your shift at %s on %s has been unassigned. Contact your manager.",
                siteName, start != null ? start.format(FMT) : "?");
        sendSms(guardPhone, msg);
        toast("📱 SMS → " + guardName + ": removed from shift at " + siteName, NotificationManager.Level.WARN);
    }

    public void notifyShiftCreated(String siteName, LocalDateTime start, LocalDateTime end) {
        String msg = String.format("🆕 New shift at %s — %s to %s. Log in to C-QURShift.",
                siteName, start != null ? start.format(FMT) : "?", end != null ? end.format(FMT) : "?");
        sendSms("BROADCAST", msg);
        toast("📱 SMS broadcast: new shift created at " + siteName, NotificationManager.Level.INFO);
    }

    public void notifyShiftDeleted(String guardName, String guardPhone, String siteName, LocalDateTime start) {
        String msg = String.format("❌ Your shift at %s on %s has been cancelled. Contact your manager.",
                siteName, start != null ? start.format(FMT) : "?");
        sendSms(guardPhone, msg);
        toast("📱 SMS → " + guardName + ": shift at " + siteName + " cancelled", NotificationManager.Level.WARN);
    }

    private void sendSms(String to, String body) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("─".repeat(60));
        System.out.println("📱 SMS [" + timestamp + "]");
        System.out.println("   TO  : " + (to.equals("BROADCAST") ? "All available guards" : to));
        System.out.println("   MSG : " + body);
        System.out.println("─".repeat(60));
        /* ── REAL TWILIO (uncomment when ready) ──
        Twilio.init(System.getenv("TWILIO_SID"), System.getenv("TWILIO_TOKEN"));
        Message.creator(new PhoneNumber(to), new PhoneNumber(System.getenv("TWILIO_FROM")), body).create();
        ─────────────────────────────────────────── */
    }

    private void toast(String message, NotificationManager.Level level) {
        if (notificationManager != null) notificationManager.toast(message, level);
    }
}
