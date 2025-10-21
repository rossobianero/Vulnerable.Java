package org.example;

import org.junit.jupiter.api.*;
import java.net.http.*;
import java.net.URI;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

public class AppIntegrationTest {

    static Thread serverThread;

    @BeforeAll
    public static void startServer() throws Exception {
        serverThread = new Thread(() -> {
            try {
                App.main(new String[]{});
            } catch (Exception e) {
                // ignore - test will fail if server not up
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        // wait a bit for server to start
        Thread.sleep(1500);
    }

    @Test
    public void testWeatherEndpoint() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8080/weather?zip=10001")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"zip\":\"10001\""));
    }
}
