package com.freaktrader.broker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

public class AlpacaMarketData {
    private final AlpacaConnection alpacaConnection;

    public AlpacaMarketData(AlpacaConnection connection) {
        this.alpacaConnection = connection;
    }

/**Alpaca json returned example
 * {---------------latestPrice(String symbol)
  "symbol": "AAPL",                                 stock code
  "trade": {                                        Trade condition codes.
    "c": [
      "@",                                          @ means a regular sale,
      "T"                                           T marks an extended-hours trade.
    ],
    "i": 689,                                       Trade ID assigned by the exchange.
    "p": 172.6,                                     stock price
    "s": 100,                                       Number of shares traded, in this case 100 shares.
    "t": "2022-08-17T09:53:16.845580544Z",          Trade timestamp. The Z at the end indicates UTC.
    "x": "P",                                       Exchange code for where the trade occurred. P stands for NYSE Arca.
    "z": "C"                                    	Market data tape code. Tape C covers Nasdaq-listed stocks.
  }
}

   * {---------------isMarketOpen()
    "is_open": false,                               is market open or not
    "next_close": "2026-11-16T16:00:00-05:00",
    "next_open": "2026-11-16T09:30:00-05:00",
    "timestamp": "2026-11-15T18:19:25.100063492-05:00"
    }
*/

    public double latestPrice(String symbol) {
        String path = "/v2/stocks/" + symbol + "/trades/latest";
        String json = alpacaConnection.getData(path);

        JsonElement parsedJson = JsonParser.parseString(json);
        JsonObject root = parsedJson.getAsJsonObject();
        JsonObject trade = root.getAsJsonObject("trade");
        JsonElement value = trade.get("p");

        return value.getAsDouble();
    }

    public boolean isMarketOpen() {
        String path = "/v2/clock";
        String json = alpacaConnection.get(path);

        JsonElement parsedJson = JsonParser.parseString(json);
        JsonObject root = parsedJson.getAsJsonObject();
        JsonElement isOpen = root.get("is_open");

        if (isOpen == null) {
            throw new IllegalStateException("Unexpected /v2/clock response: " + json);
        }
        return isOpen.getAsBoolean();
    }
}