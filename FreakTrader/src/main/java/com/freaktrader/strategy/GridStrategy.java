package com.freaktrader.strategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Long-only arithmetic grid strategy.
 *
 * The range [lowerPrice, upperPrice] is cut into gridCount equal grids.
 * Grid i spans levels[i] .. levels[i + 1]:
 *   - BUY  quantityPerGrid when the price crosses DOWN through levels[i] and the grid is empty
 *   - SELL that quantity    when the price reaches levels[i + 1] and the grid is held
 * so every completed grid earns roughly one grid step per share.
 *
 * The first price only sets a reference point; nothing is bought until the price
 * actually crosses a level. A gap move that crosses several levels triggers every
 * grid it crosses.
 *
 * This class does no I/O. The trading loop:
 *   1. calls {@link #onPrice(double)} with each new price,
 *   2. sends the returned signals to the broker,
 *   3. reports back with {@link #onFill} or {@link #onOrderFailed}.
 */
public class GridStrategy {

    private final String symbol;
    private final double[] levels;
    private final double quantityPerGrid;
    private final PositionState state;
    private final RiskManager riskManager;
    private Double lastPrice;

    public GridStrategy(String symbol, double lowerPrice, double upperPrice, int gridCount,
                        double quantityPerGrid, RiskManager riskManager) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        if (!(lowerPrice > 0) || !(upperPrice > lowerPrice)) {
            throw new IllegalArgumentException("need 0 < lowerPrice < upperPrice");
        }
        if (gridCount < 1) {
            throw new IllegalArgumentException("gridCount must be >= 1");
        }
        if (!(quantityPerGrid > 0)) {
            throw new IllegalArgumentException("quantityPerGrid must be positive");
        }
        if (riskManager == null) {
            throw new IllegalArgumentException("riskManager is required (use RiskManager.unlimited())");
        }
        if (riskManager.stopLossPrice() >= lowerPrice) {
            throw new IllegalArgumentException("stopLossPrice must be below lowerPrice");
        }

        this.symbol = symbol;
        this.quantityPerGrid = quantityPerGrid;
        this.riskManager = riskManager;
        this.state = new PositionState(gridCount);

        this.levels = new double[gridCount + 1];
        double step = (upperPrice - lowerPrice) / gridCount;
        for (int i = 0; i < gridCount; i++) {
            levels[i] = lowerPrice + i * step;
        }
        levels[gridCount] = upperPrice;
    }

    /** Feed a new market price; returns the orders to place (empty list = do nothing). */
    public List<TradeSignal> onPrice(double price) {
        if (!(price > 0)) {
            throw new IllegalArgumentException("price must be positive: " + price);
        }
        List<TradeSignal> signals = new ArrayList<>();

        riskManager.checkStopLoss(price);
        if (riskManager.isHalted()) {
            for (int i = 0; i < state.gridCount(); i++) {
                if (state.status(i) == PositionState.GridStatus.HELD) {
                    signals.add(sell(i, price, "stop loss hit, liquidating"));
                }
            }
            lastPrice = price;
            return signals;
        }

        for (int i = 0; i < state.gridCount(); i++) {
            PositionState.GridStatus status = state.status(i);
            if (status == PositionState.GridStatus.HELD && price >= levels[i + 1]) {
                signals.add(sell(i, price, "reached upper level " + levels[i + 1]));
            } else if (status == PositionState.GridStatus.EMPTY
                    && lastPrice != null && lastPrice > levels[i] && price <= levels[i]
                    && riskManager.allowBuy(state.exposure(), quantityPerGrid)) {
                signals.add(buy(i, price));
            }
        }
        lastPrice = price;
        return signals;
    }

    /** The broker filled the order behind this signal at fillPrice. */
    public void onFill(TradeSignal signal, double fillPrice) {
        checkOwnSignal(signal);
        if (signal.action() == TradeAction.BUY) {
            state.onBuyFilled(signal.gridIndex(), fillPrice);
        } else {
            state.onSellFilled(signal.gridIndex(), fillPrice);
        }
    }

    /** The order was rejected or cancelled; the grid goes back to what it was. */
    public void onOrderFailed(TradeSignal signal) {
        checkOwnSignal(signal);
        state.revertPending(signal.gridIndex());
    }

    public String symbol() {
        return symbol;
    }

    /** Grid boundaries, lowest first (gridCount + 1 values). */
    public double[] levels() {
        return levels.clone();
    }

    public PositionState state() {
        return state;
    }

    public RiskManager riskManager() {
        return riskManager;
    }

    private TradeSignal buy(int grid, double price) {
        state.markPendingBuy(grid, quantityPerGrid);
        return new TradeSignal(symbol, TradeAction.BUY, grid, price, quantityPerGrid,
                "crossed down through level " + levels[grid]);
    }

    private TradeSignal sell(int grid, double price, String reason) {
        state.markPendingSell(grid);
        return new TradeSignal(symbol, TradeAction.SELL, grid, price, quantityPerGrid, reason);
    }

    private void checkOwnSignal(TradeSignal signal) {
        if (!symbol.equals(signal.symbol()) || signal.gridIndex() >= state.gridCount()) {
            throw new IllegalArgumentException("signal does not belong to this strategy: " + signal);
        }
    }
}
