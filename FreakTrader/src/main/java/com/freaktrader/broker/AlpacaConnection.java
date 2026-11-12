package com.freaktrader.broker;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Sends authenticated HTTP requests to Alpaca and returns the raw JSON response.
 * Knows nothing about orders or prices; AlpacaMarketData / AlpacaPaperTrader build on it.
 */
public class AlpacaConnection {

    private static final String DATA_URL = "https://data.alpaca.markets";

    private final String apiKey;
    private final String secretKey;
    private final String endpoint;
    private final HttpClient httpClient;

    public AlpacaConnection() {
        Dotenv env = Dotenv.load();

        this.apiKey = env.get("ALPACA_API_KEY");
        this.secretKey = env.get("ALPACA_SECRET_KEY");
        String url = env.get("ALPACA_ENDPOINT");
        this.httpClient = HttpClient.newHttpClient();

        if (apiKey == null || secretKey == null || url == null) {
            throw new IllegalStateException(
                    ".env is missing ALPACA_API_KEY / ALPACA_SECRET_KEY / ALPACA_ENDPOINT");
        }

        // remove a trailing "/" so endpoint + "/v2/account" never becomes "//v2/account"
        this.endpoint = url.endsWith("/") 
            ? url.substring(0, url.length() - 1) 
            : url;
    }

    // ===== exterior functions =====

    /** Trading API, e.g. get("/v2/account") */
    public String get(String path) {
        return send("GET", endpoint + path, null);
    }

    /** Market data API, e.g. getData("/v2/stocks/SPY/trades/latest") */
    public String getData(String path) {
        return send("GET", DATA_URL + path, null);
    }

    /** Trading API with a JSON body, e.g. placing an order */
    public String post(String path, String json) {
        return send("POST", endpoint + path, json);
    }

    /** Trading API, e.g. cancelling an order */
    public String delete(String path) {
        return send("DELETE", endpoint + path, null);
    }

    // ===== internal function =====

    private String send(String method, String url, String json) {
        // GET / DELETE have no body; POST carries JSON
        HttpRequest.BodyPublisher body = (json == null)
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("APCA-API-KEY-ID", apiKey)
                .header("APCA-API-SECRET-KEY", secretKey)
                .header("Content-Type", "application/json")
                .method(method, body)
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RuntimeException("Network error: " + method + " " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted: " + method + " " + url, e);
        }

        // 2xx = success; anything else (401 bad key, 404 bad path, 422 bad order...) output error
        int code = response.statusCode();
        if (code < 200 || code >= 300) {
            throw new RuntimeException(
                    "Alpaca rejected " + method + " " + url + " (HTTP " + code + "): " + response.body());
        }
        return response.body();
    }
}
