package com.freaktrader.strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class creates trading signals from price changes.
 * Each grid tracks its holdings and pending signal.
 * Holdings change only after a full order fill.
 */
public class GridStrategy {

    private final GridPrice grid;
    private final int quantity;

    private double previousPrice;
    private boolean hasPreviousPrice;

    /**The possible states of each grid.*/
    public enum GridStatus {
        EMPTY,
        PENDING_BUY,
        HELD,
        PENDING_SELL
    }

    /**The current state of each grid.*/
    private final GridStatus[] gridStatus;

    /**The number of shares held by each grid.*/
    private final int[] heldQuantity;

    /**The signal waiting for a result in each grid.*/
    private final TradeSignal[] pendingSignal;


    /**
     * Creates a strategy with empty holdings.
     * @param grid contains the price levels
     * @param quantity is the number of shares for each buy order
     * @throws IllegalArgumentException if the grid or quantity is invalid
     */
    public GridStrategy(GridPrice grid, int quantity) {
        if (grid == null) {
            throw new IllegalArgumentException("The grid cannot be null.");
        }

        int levelCount = grid.getLevelCount();

        if (levelCount < 2) {
            throw new IllegalArgumentException(
                    "The grid must have at least two price levels.");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "The quantity must be greater than zero.");
        }

        /**Copy the levels so outside changes do not affect this strategy.*/
        double[] levels = new double[levelCount];

        for (int i = 0; i < levelCount; i++) {
            double price = grid.getPrice(i);

            if (!Double.isFinite(price) || price <= 0) {
                throw new IllegalArgumentException(
                        "Each price level must be finite and positive.");
            }

            if (i > 0 && price <= levels[i - 1]) {
                throw new IllegalArgumentException(
                        "Price levels must be in increasing order.");
            }

            levels[i] = price;
        }

        this.grid = new GridPrice(levels, grid.getSpacing());
        this.quantity = quantity;
        this.hasPreviousPrice = false;

        int gridCount = levelCount - 1;

        this.gridStatus = new GridStatus[gridCount];
        this.heldQuantity = new int[gridCount];
        this.pendingSignal = new TradeSignal[gridCount];

        /**Start every grid with no holdings.*/
        Arrays.fill(this.gridStatus, GridStatus.EMPTY);
    }


    /**
     * Checks a new price and creates trading signals.
     * @param currentPrice is the latest price
     * @return the signals, or an empty list if no action is needed
     * @throws IllegalArgumentException if the price is not finite or positive
     */
    public List<TradeSignal> evaluatePrice(double currentPrice) {
        if (!Double.isFinite(currentPrice) || currentPrice <= 0) {
            throw new IllegalArgumentException(
                    "The current price must be finite and positive.");
        }

        List<TradeSignal> signals = new ArrayList<>();

        /**Save the first price without creating a signal.*/
        if (!hasPreviousPrice) {
            previousPrice = currentPrice;
            hasPreviousPrice = true;
            return signals;
        }

        double lowerPrice = grid.getPrice(0);
        double upperPrice = grid.getPrice(grid.getLevelCount() - 1);

        boolean insideRange =
                currentPrice >= lowerPrice && currentPrice <= upperPrice;

        for (int i = 0; i < gridStatus.length; i++) {

            /**Wait until the pending signal has a result.*/
            if (pendingSignal[i] != null) {
                continue;
            }

            double buyLevel = grid.getPrice(i);
            double sellLevel = grid.getPrice(i + 1);

            TradeState.state action = TradeState.state.NO_ACTION;

            /**Sell held shares when the price reaches the upper level.*/
            if (gridStatus[i] == GridStatus.HELD) {
                if (currentPrice >= sellLevel) {
                    action = TradeState.state.SELL;
                }
            }

            /**Buy when the price crosses down through the lower level.*/
            else if (gridStatus[i] == GridStatus.EMPTY && insideRange) {
                boolean crossedDown =
                        previousPrice > buyLevel && currentPrice <= buyLevel;

                if (crossedDown) {
                    action = TradeState.state.BUY;
                }
            }

            if (action != TradeState.state.NO_ACTION) {
                TradeSignal signal = createSignal(action, i, currentPrice);
                signals.add(signal);
            }
        }

        previousPrice = currentPrice;

        return signals;
    }


    /**
     * Creates a signal and marks its grid as pending.
     * @param action is BUY or SELL
     * @param index is the grid index
     * @param price is the current price
     * @return the pending signal
     */
    private TradeSignal createSignal(TradeState.state action,
                                     int index, double price) {
        boolean isBuy = action == TradeState.state.BUY;

        /**Sell the shares held by this grid.*/
        int orderQuantity = isBuy ? quantity : heldQuantity[index];

        TradeSignal signal =
                new TradeSignal(action, index, orderQuantity, price);

        gridStatus[index] =
                isBuy ? GridStatus.PENDING_BUY : GridStatus.PENDING_SELL;

        pendingSignal[index] = signal;

        return signal;
    }


    /**
     * Updates a grid after its order is fully filled.
     * @param signal is the original pending signal object
     * @param filledQuantity is the total number of shares filled
     * @throws IllegalArgumentException if the signal or filled quantity is invalid
     * @throws IllegalStateException if the signal is no longer pending
     */
    public void onOrderFilled(TradeSignal signal, int filledQuantity) {
        checkPendingSignal(signal);

        if (filledQuantity != signal.getQuantity()) {
            throw new IllegalArgumentException(
                    "The order must be fully filled before this update.");
        }

        int index = signal.getGridIndex();
        boolean isBuy = signal.getAction() == TradeState.state.BUY;

        heldQuantity[index] = isBuy ? filledQuantity : 0;
        gridStatus[index] = isBuy ? GridStatus.HELD : GridStatus.EMPTY;

        pendingSignal[index] = null;
    }


    /**
     * Restores a grid after rejection or cancellation with no fills.
     * @param signal is the original pending signal object
     * @throws IllegalArgumentException if the signal is invalid
     * @throws IllegalStateException if the signal is no longer pending
     */
    public void onOrderRejected(TradeSignal signal) {
        checkPendingSignal(signal);

        int index = signal.getGridIndex();
        boolean isBuy = signal.getAction() == TradeState.state.BUY;

        /**A rejected sell leaves the shares in this grid.*/
        gridStatus[index] = isBuy ? GridStatus.EMPTY : GridStatus.HELD;

        pendingSignal[index] = null;
    }


    /**Returns the number of grids.*/
    public int getGridCount() {
        return this.gridStatus.length;
    }


    /**
     * Returns the state of one grid.
     * @param index is the grid index
     * @return the current state
     * @throws IllegalArgumentException if the index is outside the grid
     */
    public GridStatus getGridStatus(int index) {
        checkIndex(index);
        return this.gridStatus[index];
    }


    /**
     * Returns the shares held by one grid.
     * @param index is the grid index
     * @return the number of shares held
     * @throws IllegalArgumentException if the index is outside the grid
     */
    public int getHeldQuantity(int index) {
        checkIndex(index);
        return this.heldQuantity[index];
    }


    /**Checks that the index belongs to this strategy.*/
    private void checkIndex(int index) {
        if (index < 0 || index >= gridStatus.length) {
            throw new IllegalArgumentException(
                    "The grid index is out of range.");
        }
    }


    /**Checks that this is the original signal still waiting for a result.*/
    private void checkPendingSignal(TradeSignal signal) {
        if (signal == null) {
            throw new IllegalArgumentException("The signal cannot be null.");
        }

        int index = signal.getGridIndex();
        checkIndex(index);

        if (pendingSignal[index] != signal) {
            throw new IllegalStateException(
                    "This signal is not pending in this grid.");
        }
    }
}