package com.freaktrader.broker;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Talks to Alpaca: sends a request, gives back the JSON text.
 * It does not know about prices or orders, other classes handle that.
 */
public class AlpacaConnection {

    private static final String DATA_URL = "https://data.alpaca.markets";
    private static final MediaType JSON = MediaType.get("application/json");

    private final String apiKey;
    private final String secretKey;
    private final String endpoint;
    private final OkHttpClient httpClient;

    public AlpacaConnection() {
        // read the keys and address from the .env file
        Dotenv env = Dotenv.load();

        this.apiKey = env.get("ALPACA_API_KEY");
        this.secretKey = env.get("ALPACA_SECRET_KEY");
        String url = env.get("ALPACA_ENDPOINT");
        this.httpClient = new OkHttpClient();

        if (apiKey == null || secretKey == null || url == null) {
            throw new IllegalStateException(
                    ".env is missing ALPACA_API_KEY / ALPACA_SECRET_KEY / ALPACA_ENDPOINT");
        }

        // drop a "/" at the end, so we never get "//" in the address
        this.endpoint = url.endsWith("/")
            ? url.substring(0, url.length() - 1)
            : url;
    }

    // ===== use these =====

    /** Get account or order info, e.g. get("/v2/account") */
    public String get(String path) {
        return send("GET", endpoint + path, null);
    }

    /** Get prices, e.g. getData("/v2/stocks/SPY/trades/latest") */
    public String getData(String path) {
        return send("GET", DATA_URL + path, null);
    }

    /** Send data, e.g. place an order */
    public String post(String path, String json) {
        return send("POST", endpoint + path, json);
    }

    /** Remove something, e.g. cancel an order */
    public String delete(String path) {
        return send("DELETE", endpoint + path, null);
    }

    // ===== does the real work =====

    private String send(String method, String url, String json) {
        // only POST sends data; GET and DELETE send nothing
        RequestBody body = (json == null) ? null : RequestBody.create(json, JSON);

        // the address plus our keys, so Alpaca knows who we are
        Request request = new Request.Builder()
            .url(url)
            .header("APCA-API-KEY-ID", apiKey)
            .header("APCA-API-SECRET-KEY", secretKey)
            .method(method, body)
            .build();

        // send it and wait for the answer (closed automatically when done)
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String text = (responseBody != null) ? responseBody.string() : "";

            // Alpaca said no (wrong key, wrong path, bad order...): stop with the reason
            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        "Alpaca rejected " + method + " " + url + " (HTTP " + response.code() + "): " + text);
            }
            return text;
        } catch (IOException e) {
            // if not reach Alpaca (no internet, timeout...)
            throw new RuntimeException("Network error: " + method + " " + url, e);
        }
    }
}
