package secureshift.util;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.Executors;

public class TileProxy {

    private static final int    PORT       = 7070;
    private static final String STADIA_KEY = "be3969d1-369e-4209-a8f9-3a45b2706467";
    private static HttpServer server;
    private static volatile byte[] leafletJs  = null;
    private static volatile byte[] leafletCss = null;

    // Disk cache for tiles so they load instantly on next launch
    private static final Path CACHE_DIR;
    static {
        Path p;
        try {
            p = Path.of(System.getProperty("user.home"), ".cqurshift", "tiles");
            Files.createDirectories(p);
        } catch (Exception e) {
            p = Path.of(System.getProperty("java.io.tmpdir"), "cqurshift-tiles");
        }
        CACHE_DIR = p;
    }

    public static void start() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
            server.createContext("/tile",    TileProxy::handleTile);
            server.createContext("/leaflet", TileProxy::handleLeaflet);
            server.setExecutor(Executors.newFixedThreadPool(8));
            server.start();
            System.out.println("Tile proxy started on http://localhost:" + PORT);
            // Prefetch Leaflet only — no tile warmup
            new Thread(TileProxy::prefetchLeaflet, "leaflet-prefetch").start();
        } catch (Exception e) {
            System.err.println("Tile proxy failed: " + e.getMessage());
        }
    }

    public static void stop() { if (server != null) server.stop(0); }

    private static void prefetchLeaflet() {
        try {
            leafletJs  = fetchUrl("https://unpkg.com/leaflet@1.9.4/dist/leaflet.js");
            leafletCss = fetchUrl("https://unpkg.com/leaflet@1.9.4/dist/leaflet.css");
            System.out.println("Leaflet ready");
        } catch (Exception e) {
            System.err.println("Leaflet prefetch failed: " + e.getMessage());
        }
    }

    private static void handleLeaflet(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        boolean isJs = path.endsWith(".js");
        byte[] data = isJs ? leafletJs : leafletCss;
        if (data == null) {
            try {
                data = fetchUrl(isJs
                    ? "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"
                    : "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css");
                if (isJs) leafletJs = data; else leafletCss = data;
            } catch (Exception e) {
                exchange.sendResponseHeaders(503, 0);
                exchange.getResponseBody().close();
                return;
            }
        }
        exchange.getResponseHeaders().add("Content-Type", isJs ? "application/javascript" : "text/css");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Cache-Control", "public, max-age=86400");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.getResponseBody().close();
    }

    private static void handleTile(HttpExchange exchange) throws IOException {
        String path   = exchange.getRequestURI().getPath();
        String[] parts = path.replace("/tile/", "").split("/");
        if (parts.length < 3) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().close();
            return;
        }
        int z, x, y;
        try {
            z = Integer.parseInt(parts[0]);
            x = Integer.parseInt(parts[1]);
            y = Integer.parseInt(parts[2].replace(".png",""));
        } catch (NumberFormatException e) {
            exchange.sendResponseHeaders(400, 0);
            exchange.getResponseBody().close();
            return;
        }

        Path cacheFile = CACHE_DIR.resolve(z + "_" + x + "_" + y + ".png");
        byte[] data = null;

        // Serve from disk cache instantly
        if (Files.exists(cacheFile)) {
            try { data = Files.readAllBytes(cacheFile); } catch (Exception ignored) {}
        }

        // Fetch from Stadia if not cached
        if (data == null || data.length < 100) {
            try {
                data = fetchUrl("https://tiles.stadiamaps.com/tiles/alidade_smooth_dark/"
                    + z + "/" + x + "/" + y + ".png?api_key=" + STADIA_KEY);
                final byte[] toSave = data;
                final Path   dest   = cacheFile;
                // Save to disk in background — don't block response
                Thread t = new Thread(() -> {
                    try { Files.write(dest, toSave); } catch (Exception ignored) {}
                });
                t.setDaemon(true);
                t.start();
            } catch (Exception e) {
                data = transparentPng();
            }
        }

        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Content-Type", "image/png");
        exchange.getResponseHeaders().add("Cache-Control", "public, max-age=604800");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.getResponseBody().close();
    }

    private static byte[] fetchUrl(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 C-QURShift/1.0");
        conn.setRequestProperty("Accept", "image/png,*/*");
        conn.setRequestProperty("Accept-Encoding", "identity");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        conn.setInstanceFollowRedirects(true);
        conn.connect();
        int code = conn.getResponseCode();
        if (code != 200) throw new IOException("HTTP " + code);
        try (InputStream is = conn.getInputStream();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            return bos.toByteArray();
        }
    }

    private static byte[] transparentPng() {
        return new byte[]{
            (byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,0x00,0x00,0x00,0x0D,
            0x49,0x48,0x44,0x52,0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x01,0x08,
            0x06,0x00,0x00,0x00,0x1F,0x15,(byte)0xC4,(byte)0x89,0x00,0x00,0x00,
            0x0A,0x49,0x44,0x41,0x54,0x78,(byte)0x9C,0x62,0x00,0x01,0x00,0x00,
            0x05,0x00,0x01,0x0D,0x0A,0x2D,(byte)0xB4,0x00,0x00,0x00,0x00,0x49,
            0x45,0x4E,0x44,(byte)0xAE,0x42,0x60,(byte)0x82};
    }
}
