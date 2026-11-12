package com.freaktrader.strategy;

import java.util.Arrays;

/**
 * Tracks what each grid currently holds.
 *
 * Every grid moves through: EMPTY -> PENDING_BUY -> HELD -> PENDING_SELL -> EMPTY.
 * The PENDING states stop the strategy from sending the same order twice while
 * the broker has not confirmed it yet. Mutators are package-private: only
 * {@link GridStrategy} changes the state; everyone else can read it.
 */
public class PositionState {

    public enum GridStatus { EMPTY, PENDING_BUY, HELD, PENDING_SELL }

    private final GridStatus[] status;
    private final double[] quantity;
    private final double[] entryPrice;
    private double realizedPnl;
    private int completedRoundTrips;

    PositionState(int gridCount) {
        if (gridCount <= 0) {
            throw new IllegalArgumentException("gridCount must be positive");
        }
        status = new GridStatus[gridCount];
        Arrays.fill(status, GridStatus.EMPTY);
        quantity = new double[gridCount];
        entryPrice = new double[gridCount];
    }

    // ---------- read-only view ----------

    public int gridCount() {
        return status.length;
    }

    public GridStatus status(int grid) {
        return status[grid];
    }

    /** Fill price of the buy for this grid; 0 if the grid holds nothing. */
    public double entryPrice(int grid) {
        return entryPrice[grid];
    }

    /** Shares actually owned (bought and not yet sold). */
    public double heldQuantity() {
        double total = 0;
        for (int i = 0; i < status.length; i++) {
            if (status[i] == GridStatus.HELD || status[i] == GridStatus.PENDING_SELL) {
                total += quantity[i];
            }
        }
        return total;
    }

    /** Shares owned plus shares with a buy order in flight; used for risk limits. */
    public double exposure() {
        double total = 0;
        for (int i = 0; i < status.length; i++) {
            if (status[i] != GridStatus.EMPTY) {
                total += quantity[i];
            }
        }
        return total;
    }

    public double unrealizedPnl(double currentPrice) {
        double pnl = 0;
        for (int i = 0; i < status.length; i++) {
            if (status[i] == GridStatus.HELD || status[i] == GridStatus.PENDING_SELL) {
                pnl += (currentPrice - entryPrice[i]) * quantity[i];
            }
        }
        return pnl;
    }

    public double realizedPnl() {
        return realizedPnl;
    }

    /** Number of buy-then-sell cycles that have completed. */
    public int completedRoundTrips() {
        return completedRoundTrips;
    }

    // ---------- transitions (strategy only) ----------

    void markPendingBuy(int grid, double qty) {
        require(grid, GridStatus.EMPTY);
        status[grid] = GridStatus.PENDING_BUY;
        quantity[grid] = qty;
    }

    void markPendingSell(int grid) {
        require(grid, GridStatus.HELD);
        status[grid] = GridStatus.PENDING_SELL;
    }

    void onBuyFilled(int grid, double fillPrice) {
        require(grid, GridStatus.PENDING_BUY);
        status[grid] = GridStatus.HELD;
        entryPrice[grid] = fillPrice;
    }

    void onSellFilled(int grid, double fillPrice) {
        require(grid, GridStatus.PENDING_SELL);
        realizedPnl += (fillPrice - entryPrice[grid]) * quantity[grid];
        completedRoundTrips++;
        status[grid] = GridStatus.EMPTY;
        quantity[grid] = 0;
        entryPrice[grid] = 0;
    }

    /** Order was rejected or cancelled: go back to the state before it was sent. */
    void revertPending(int grid) {
        switch (status[grid]) {
            case PENDING_BUY -> {
                status[grid] = GridStatus.EMPTY;
                quantity[grid] = 0;
            }
            case PENDING_SELL -> status[grid] = GridStatus.HELD;
            default -> throw new IllegalStateException(
                    "grid " + grid + " has no pending order (status " + status[grid] + ")");
        }
    }

    private void require(int grid, GridStatus expected) {
        if (status[grid] != expected) {
            throw new IllegalStateException(
                    "grid " + grid + " is " + status[grid] + ", expected " + expected);
        }
    }
}
