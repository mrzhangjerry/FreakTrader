package com.freaktrader;

import com.freaktrader.broker.AlpacaConnection;

public class Main {
    public static void main(String[] args) {
        AlpacaConnection alpaca = new AlpacaConnection();
        String json = alpaca.get("/account");
        System.out.println(json.replace(",", ",\n"));
    }
}
