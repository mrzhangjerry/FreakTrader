package com.freaktrader.strategy;

/**
 * This class stores one buy or sell signal.
 * It records the grid index, quantity, and trigger price.
 * The trigger price is not the actual fill price.
 */
public class TradeSignal {

    private final TradeState.state action;
    private final int gridIndex;
    private final int quantity;
    private final double triggerPrice;


    /**
     * Creates a buy or sell signal.
     * @param action is BUY or SELL
     * @param gridIndex is the grid that created this signal
     * @param quantity is the number of shares to trade
     * @param triggerPrice is the price when the signal was created
     * @throws IllegalArgumentException if any input is invalid
     */
    TradeSignal(TradeState.state action, int gridIndex,
                int quantity, double triggerPrice) {

        if (action != TradeState.state.BUY
                && action != TradeState.state.SELL) {
            throw new IllegalArgumentException(
                    "The action must be BUY or SELL.");
        }

        if (gridIndex < 0) {
            throw new IllegalArgumentException(
                    "The grid index cannot be negative.");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "The quantity must be greater than zero.");
        }

        if (!Double.isFinite(triggerPrice) || triggerPrice <= 0) {
            throw new IllegalArgumentException(
                    "The trigger price must be finite and positive.");
        }

        this.action = action;
        this.gridIndex = gridIndex;
        this.quantity = quantity;
        this.triggerPrice = triggerPrice;
    }


    /**Returns BUY or SELL.*/
    public TradeState.state getAction() {
        return this.action;
    }


    /**Returns the grid index.*/
    public int getGridIndex() {
        return this.gridIndex;
    }


    /**Returns the number of shares to trade.*/
    public int getQuantity() {
        return this.quantity;
    }


    /**Returns the price when the signal was created.*/
    public double getTriggerPrice() {
        return this.triggerPrice;
    }
}