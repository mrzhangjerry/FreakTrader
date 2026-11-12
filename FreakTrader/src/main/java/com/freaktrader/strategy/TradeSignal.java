package com.freaktrader.strategy;

/**
 * An order request produced by {@link GridStrategy}.
 *
 * @param symbol    ticker, e.g. "SPY"
 * @param action    BUY or SELL
 * @param gridIndex which grid this order belongs to (0 = lowest grid)
 * @param price     market price when the signal was generated
 * @param quantity  shares to trade
 * @param reason    human readable explanation, for logging
 */
public record TradeSignal(
        String symbol,
        TradeAction action,
        int gridIndex,
        double price,
        double quantity,
        String reason) {

    public TradeSignal {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (action == null) {
            throw new IllegalArgumentException("action is required");
        }
        if (gridIndex < 0) {
            throw new IllegalArgumentException("gridIndex must be >= 0");
        }
        if (!(price > 0) || !(quantity > 0)) {
            throw new IllegalArgumentException("price and quantity must be positive");
        }
    }
}
