package com.freaktrader.strategy;

/**
 * Risk rules consulted by {@link GridStrategy}.
 *
 * - maxPositionQty: buys are refused once shares owned + shares being bought would exceed it.
 * - stopLossPrice:  if the price falls to or below it, trading halts for good and every
 *                   held grid is sold. 0 disables the stop loss.
 */
public class RiskManager {

    private final double maxPositionQty;
    private final double stopLossPrice;
    private boolean halted;

    public RiskManager(double maxPositionQty, double stopLossPrice) {
        if (!(maxPositionQty > 0)) {
            throw new IllegalArgumentException("maxPositionQty must be positive");
        }
        if (stopLossPrice < 0) {
            throw new IllegalArgumentException("stopLossPrice must be >= 0 (0 = disabled)");
        }
        this.maxPositionQty = maxPositionQty;
        this.stopLossPrice = stopLossPrice;
    }

    /** No position limit, no stop loss. */
    public static RiskManager unlimited() {
        return new RiskManager(Double.POSITIVE_INFINITY, 0);
    }

    public double maxPositionQty() {
        return maxPositionQty;
    }

    public double stopLossPrice() {
        return stopLossPrice;
    }

    public boolean isHalted() {
        return halted;
    }

    boolean allowBuy(double currentExposure, double qty) {
        return !halted && currentExposure + qty <= maxPositionQty + 1e-9;
    }

    /** Halts trading if the stop loss is hit. Once halted, it stays halted. */
    void checkStopLoss(double price) {
        if (stopLossPrice > 0 && price <= stopLossPrice) {
            halted = true;
        }
    }
}
