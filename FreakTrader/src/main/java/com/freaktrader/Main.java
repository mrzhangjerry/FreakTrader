package com.freaktrader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.freaktrader.broker.AlpacaConnection;
import com.freaktrader.broker.AlpacaMarketData;

public class Main {
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().create();

    /** Re-format a JSON string with line breaks and indentation. */
    private static String pretty(String json) {
        return PRETTY.toJson(JsonParser.parseString(json));
    }
    public static void main(String[] args) {
        AlpacaConnection alpaca = new AlpacaConnection();
        AlpacaMarketData market = new AlpacaMarketData(alpaca);

        // 1. Is the connection + API key working?
        System.out.println("Account: " + pretty(alpaca.get("/v2/account")));

        // 2. isMarketOpen()
        System.out.println("Market open? " + market.isMarketOpen());

        // 3. latestPrice()
        System.out.println("SPY price: " + market.latestPrice("SPY"));
    }
}