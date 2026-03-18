package secureshift.service;

import secureshift.domain.Guard;
import secureshift.domain.Shift;
import secureshift.domain.Site;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportService {

    private static final DateTimeFormatter FMT      = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public Path exportGuardsCsv(List<Guard> guards) throws IOException {
        Path out = getOutputPath("Guards", "csv");
        try (PrintWriter w = new PrintWriter(new FileWriter(out.toFile()))) {
            w.println("ID,Name,Latitude,Longitude,Available,Skills");
            for (Guard g : guards)
                w.printf("%s,%s,%.4f,%.4f,%s,%s%n", csv(g.getId()), csv(g.getName()),
                        g.getLatitude(), g.getLongitude(), g.isAvailable() ? "Yes" : "No",
                        csv(g.getSkills() != null ? String.join("; ", g.getSkills()) : ""));
        }
        System.out.println("✅ Guards CSV: " + out); return out;
    }

    public Path exportSitesCsv(List<Site> sites) throws IOException {
        Path out = getOutputPath("Sites", "csv");
        try (PrintWriter w = new PrintWriter(new FileWriter(out.toFile()))) {
            w.println("ID,Name,Address,Region,Latitude,Longitude,Required Skills");
            for (Site s : sites)
                w.printf("%d,%s,%s,%s,%.4f,%.4f,%s%n", s.getId(), csv(s.getName()),
                        csv(s.getAddress()), csv(s.getRegion()), s.getLatitude(), s.getLongitude(),
                        csv(s.getRequiredSkills() != null ? String.join("; ", s.getRequiredSkills()) : ""));
        }
        System.out.println("✅ Sites CSV: " + out); return out;
    }

    public Path exportShiftsCsv(List<Shift> shifts) throws IOException {
        Path out = getOutputPath("Shifts", "csv");
        try (PrintWriter w = new PrintWriter(new FileWriter(out.toFile()))) {
            w.println("ID,Site,Guard,Start,End,Duration (hrs),Status");
            for (Shift s : shifts)
                w.printf("%s,%s,%s,%s,%s,%.1f,%s%n", csv(s.getId()),
                        csv(s.getSite() != null ? s.getSite().getName() : ""),
                        csv(s.getAssignedGuard() != null ? s.getAssignedGuard().getName() : "Unassigned"),
                        s.getStartTime() != null ? s.getStartTime().format(FMT) : "",
                        s.getEndTime()   != null ? s.getEndTime().format(FMT)   : "",
                        s.getDurationHours(), getShiftStatus(s));
        }
        System.out.println("✅ Shifts CSV: " + out); return out;
    }

    public Path exportGuardsPdf(List<Guard> guards) throws IOException {
        Path out = getOutputPath("Guards", "html");
        StringBuilder rows = new StringBuilder();
        for (Guard g : guards)
            rows.append("<tr>").append(td(g.getId())).append(td(g.getName()))
                .append(td(String.format("%.4f", g.getLatitude()))).append(td(String.format("%.4f", g.getLongitude())))
                .append(td(g.isAvailable() ? "<span class='badge green'>Available</span>" : "<span class='badge red'>Unavailable</span>"))
                .append(td(g.getSkills() != null ? String.join(", ", g.getSkills()) : "—")).append("</tr>\n");
        writeAndOpen(out, reportHtml("Guards Report", "<th>ID</th><th>Name</th><th>Latitude</th><th>Longitude</th><th>Available</th><th>Skills</th>", rows.toString(), guards.size() + " guards"));
        return out;
    }

    public Path exportSitesPdf(List<Site> sites) throws IOException {
        Path out = getOutputPath("Sites", "html");
        StringBuilder rows = new StringBuilder();
        for (Site s : sites)
            rows.append("<tr>").append(td(String.valueOf(s.getId()))).append(td(s.getName()))
                .append(td(s.getAddress() != null ? s.getAddress() : "—")).append(td(s.getRegion() != null ? s.getRegion() : "—"))
                .append(td(String.format("%.4f", s.getLatitude()))).append(td(String.format("%.4f", s.getLongitude())))
                .append(td(s.getRequiredSkills() != null ? String.join(", ", s.getRequiredSkills()) : "—")).append("</tr>\n");
        writeAndOpen(out, reportHtml("Sites Report", "<th>ID</th><th>Name</th><th>Address</th><th>Region</th><th>Lat</th><th>Lon</th><th>Skills</th>", rows.toString(), sites.size() + " sites"));
        return out;
    }

    public Path exportShiftsPdf(List<Shift> shifts) throws IOException {
        Path out = getOutputPath("Shifts", "html");
        long assigned = shifts.stream().filter(s -> s.getAssignedGuard() != null).count();
        StringBuilder rows = new StringBuilder();
        for (Shift s : shifts) {
            String status = getShiftStatus(s);
            String cls = switch (status) { case "Active" -> "blue"; case "Completed" -> "grey"; default -> "orange"; };
            rows.append("<tr>").append(td(s.getId())).append(td(s.getSite() != null ? s.getSite().getName() : "—"))
                .append(td(s.getAssignedGuard() != null ? s.getAssignedGuard().getName() : "<em>Unassigned</em>"))
                .append(td(s.getStartTime() != null ? s.getStartTime().format(FMT) : "—"))
                .append(td(s.getEndTime()   != null ? s.getEndTime().format(FMT)   : "—"))
                .append(td(String.format("%.1f hrs", s.getDurationHours())))
                .append(td("<span class='badge " + cls + "'>" + status + "</span>")).append("</tr>\n");
        }
        writeAndOpen(out, reportHtml("Shifts Report", "<th>ID</th><th>Site</th><th>Guard</th><th>Start</th><th>End</th><th>Duration</th><th>Status</th>",
                rows.toString(), shifts.size() + " shifts  •  " + assigned + " assigned  •  " + (shifts.size() - assigned) + " unassigned"));
        return out;
    }

    private String reportHtml(String title, String headers, String rows, String summary) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>" + title + "</title><style>" +
            "* { box-sizing:border-box; margin:0; padding:0; } body { font-family:'Segoe UI',Arial,sans-serif; background:#f0f2f5; padding:32px; color:#1a1a2e; } " +
            ".card { background:white; border-radius:12px; box-shadow:0 2px 12px rgba(0,0,0,0.08); overflow:hidden; } " +
            ".header { background:linear-gradient(135deg,#1e1b4b,#312e81); color:white; padding:24px 32px; } " +
            ".header h1 { font-size:24px; font-weight:700; } .header p { font-size:13px; opacity:0.7; margin-top:4px; } " +
            ".summary { padding:16px 32px; background:#f8fafc; border-bottom:1px solid #e2e8f0; font-size:13px; color:#64748b; } " +
            "table { width:100%; border-collapse:collapse; } " +
            "th { background:#f1f5f9; padding:12px 16px; text-align:left; font-size:11px; text-transform:uppercase; letter-spacing:0.05em; color:#64748b; border-bottom:1px solid #e2e8f0; } " +
            "td { padding:11px 16px; font-size:13px; border-bottom:1px solid #f1f5f9; } tr:last-child td { border-bottom:none; } tr:hover td { background:#f8fafc; } " +
            ".badge { padding:3px 10px; border-radius:20px; font-size:11px; font-weight:600; } " +
            ".badge.green{background:#dcfce7;color:#166534} .badge.red{background:#fee2e2;color:#991b1b} " +
            ".badge.blue{background:#dbeafe;color:#1e40af} .badge.orange{background:#fef3c7;color:#92400e} .badge.grey{background:#f1f5f9;color:#475569} " +
            ".footer { padding:16px 32px; font-size:11px; color:#94a3b8; border-top:1px solid #e2e8f0; } " +
            "@media print { body{background:white;padding:0} .card{box-shadow:none} }" +
            "</style></head><body><div class='card'>" +
            "<div class='header'><h1>C-QURShift — " + title + "</h1><p>Generated: " + ts + "</p></div>" +
            "<div class='summary'>" + summary + "</div>" +
            "<table><thead><tr>" + headers + "</tr></thead><tbody>" + rows + "</tbody></table>" +
            "<div class='footer'>C-QURShift Guard Scheduling Platform — Confidential</div>" +
            "</div><script>window.onload=function(){setTimeout(function(){window.print();},800);}</script>" +
            "</body></html>";
    }

    private void writeAndOpen(Path path, String html) throws IOException {
        try (PrintWriter w = new PrintWriter(new FileWriter(path.toFile()))) { w.print(html); }
        System.out.println("✅ PDF report: " + path);
        try { java.awt.Desktop.getDesktop().browse(path.toUri()); } catch (Exception e) { System.err.println("⚠ Could not open browser: " + e.getMessage()); }
    }

    private String td(String v) { return "<td>" + (v != null ? v : "—") + "</td>"; }
    private String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) return "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }
    private String getShiftStatus(Shift s) {
        LocalDateTime now = LocalDateTime.now();
        if (s.getEndTime()   != null && s.getEndTime().isBefore(now))   return "Completed";
        if (s.getStartTime() != null && s.getStartTime().isBefore(now)) return "Active";
        return "Upcoming";
    }
    private Path getOutputPath(String name, String ext) {
        return Path.of(System.getProperty("user.home"), "Downloads",
                "CQURShift_" + name + "_" + LocalDateTime.now().format(FILE_FMT) + "." + ext);
    }
}
