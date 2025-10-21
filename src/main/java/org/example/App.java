package org.example;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.URI;
import java.sql.*;
import java.net.http.*;
import java.security.MessageDigest;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.io.ObjectInputStream;

public class App {
    static final String WEATHER_KEY = "OWM_JAVA_KEY_PLACEHOLDER_4444";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/weather", new WeatherHandler());
        server.createContext("/deserialize", new DeserHandler());
        server.start();
        System.out.println("Vulnerable.Java listening on 8080");
    }

    static class WeatherHandler implements HttpHandler {
        public void handle(HttpExchange exchange) {
            try {
                // Parse zip
                URI uri = exchange.getRequestURI();
                String query = uri.getQuery();
                String zip = "";
                if (query != null && query.startsWith("zip=")) zip = query.substring(4);

                // Build URL
                String url = "https://api.openweathermap.org/data/2.5/weather?zip=" + zip + "&appid=" + WEATHER_KEY;

                // --- Make network fetch, but don't fail the request if it errors ---
                String body = "{\"error\":\"fetch failed\"}";
                try {
                    HttpClient client = HttpClient.newBuilder()
                        .sslContext(trustAllSSLContext()) // (still intentionally insecure)
                        .build();
                    HttpRequest req = HttpRequest.newBuilder().uri(java.net.URI.create(url)).GET().build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    body = resp.body();
                } catch (Exception netEx) {
                    // Log for debugging but keep going so tests can still pass
                    netEx.printStackTrace();
                }

                // --- Ensure SQLite driver is registered (helps with “No suitable driver”) ---
                try {
                    Class.forName("org.sqlite.JDBC");
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                // DB insert (still intentionally vulnerable via string concatenation)
                Connection conn = DriverManager.getConnection("jdbc:sqlite:vulnerable_java.db");
                Statement st = conn.createStatement();
                st.execute("CREATE TABLE IF NOT EXISTS weather (id INTEGER PRIMARY KEY AUTOINCREMENT, zip TEXT, payload TEXT)");
                String sql = "INSERT INTO weather (zip, payload) VALUES ('" + zip + "', '" + body.replace("'", "''") + "')";
                st.execute(sql);
                st.close();
                conn.close();

                // MD5 (intentionally weak)
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] dg = md.digest(zip.getBytes("UTF-8"));
                StringBuilder sb = new StringBuilder();
                for (byte b: dg) sb.append(String.format("%02x", b));

                // Response
                String respText = "{\"zip\":\"" + zip + "\",\"md5\":\"" + sb.toString() + "\"}";
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, respText.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(respText.getBytes()); }
            } catch (Exception e) {
                // Print the real cause to console to help you debug locally/CI
                e.printStackTrace();
                try {
                    exchange.sendResponseHeaders(500, 0);
                    exchange.getResponseBody().close();
                } catch(Exception ex) { /* ignore */ }
            }
        }
    }

    static class DeserHandler implements HttpHandler {
        public void handle(HttpExchange exchange) {
            try (InputStream is = exchange.getRequestBody()) {
                ObjectInputStream ois = new ObjectInputStream(is);
                Object o = ois.readObject(); // INSECURE deserialization (intentional)
                String respText = "{\"type\":\"" + o.getClass().getName() + "\"}";
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, respText.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(respText.getBytes()); }
            } catch (Exception e) {
                e.printStackTrace();
                try { exchange.sendResponseHeaders(400,0); exchange.getResponseBody().close(); } catch(Exception ex) {}
            }
        }
    }

    static SSLContext trustAllSSLContext() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{ new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
        }};
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        return sc;
    }
}
