package com.freaktrader.strategy;

import static org.junit.jupiter.api.Assertions.*;

import com.freaktrader.strategy.PositionState.GridStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Grid used throughout: 90..110 in 10 grids => levels every 2.0, 1 share per grid. */
class GridStrategyTest {

    private static final double EPS = 1e-9;

    private GridStrategy grid(RiskManager risk) {
        return new GridStrategy("SPY", 90, 110, 10, 1, risk);
    }

    private GridStrategy grid() {
        return grid(RiskManager.unlimited());
    }

    @Test
    void levelsAreEvenlySpaced() {
        double[] levels = grid().levels();
        assertEquals(11, levels.length);
        assertEquals(90, levels[0], EPS);
        assertEquals(100, levels[5], EPS);
        assertEquals(110, levels[10], EPS);
    }

    @Test
    void firstPriceOnlySetsReference() {
        assertTrue(grid().onPrice(95).isEmpty());
    }

    @Test
    void buysWhenPriceCrossesDownThroughLevel() {
        GridStrategy g = grid();
        g.onPrice(101);
        List<TradeSignal> signals = g.onPrice(99.5);

        assertEquals(1, signals.size());
        TradeSignal s = signals.get(0);
        assertEquals(TradeAction.BUY, s.action());
        assertEquals(5, s.gridIndex());      // grid 100..102
        assertEquals(1, s.quantity(), EPS);
        assertEquals(GridStatus.PENDING_BUY, g.state().status(5));
    }

    @Test
    void gapDownBuysEveryCrossedGrid() {
        GridStrategy g = grid();
        g.onPrice(105);
        List<TradeSignal> signals = g.onPrice(95.5);   // crosses 104, 102, 100, 98, 96

        assertEquals(List.of(3, 4, 5, 6, 7),
                signals.stream().map(TradeSignal::gridIndex).toList());
    }

    @Test
    void doesNotRebuyWhileOrderIsPending() {
        GridStrategy g = grid();
        g.onPrice(101);
        g.onPrice(99.5);                      // buy grid 5, not filled yet
        g.onPrice(101);
        assertTrue(g.onPrice(99.5).isEmpty());
    }

    @Test
    void sellsAtUpperLevelAndBooksProfit() {
        GridStrategy g = grid();
        g.onPrice(101);
        TradeSignal buy = g.onPrice(99.5).get(0);
        g.onFill(buy, 99.5);
        assertEquals(1, g.state().heldQuantity(), EPS);

        assertTrue(g.onPrice(101.9).isEmpty());   // below grid 5 upper level (102)
        List<TradeSignal> signals = g.onPrice(102.1);
        assertEquals(1, signals.size());
        TradeSignal sell = signals.get(0);
        assertEquals(TradeAction.SELL, sell.action());
        assertEquals(5, sell.gridIndex());

        g.onFill(sell, 102.1);
        assertEquals(2.6, g.state().realizedPnl(), EPS);
        assertEquals(1, g.state().completedRoundTrips());
        assertEquals(GridStatus.EMPTY, g.state().status(5));
        assertEquals(0, g.state().heldQuantity(), EPS);
    }

    @Test
    void failedBuyFreesGridForNextCross() {
        GridStrategy g = grid();
        g.onPrice(101);
        TradeSignal buy = g.onPrice(99.5).get(0);
        g.onOrderFailed(buy);
        assertEquals(GridStatus.EMPTY, g.state().status(5));

        g.onPrice(101);
        assertEquals(1, g.onPrice(99.5).size());
    }

    @Test
    void failedSellReturnsGridToHeldAndRetries() {
        GridStrategy g = grid();
        g.onPrice(101);
        g.onFill(g.onPrice(99.5).get(0), 99.5);
        TradeSignal sell = g.onPrice(102.5).get(0);
        g.onOrderFailed(sell);
        assertEquals(GridStatus.HELD, g.state().status(5));
        assertEquals(1, g.onPrice(102.5).size());
    }

    @Test
    void maxPositionLimitsBuys() {
        GridStrategy g = grid(new RiskManager(2, 0));
        g.onPrice(105);
        List<TradeSignal> signals = g.onPrice(95.5);   // would cross 5 grids
        assertEquals(2, signals.size());
        assertEquals(2, g.state().exposure(), EPS);
    }

    @Test
    void stopLossLiquidatesAndHalts() {
        GridStrategy g = grid(new RiskManager(Double.POSITIVE_INFINITY, 85));
        g.onPrice(101);
        g.onFill(g.onPrice(99.5).get(0), 99.5);   // grid 5
        g.onFill(g.onPrice(97.5).get(0), 97.5);   // grid 4

        List<TradeSignal> signals = g.onPrice(84);
        assertTrue(g.riskManager().isHalted());
        assertEquals(2, signals.size());
        assertTrue(signals.stream().allMatch(s -> s.action() == TradeAction.SELL));

        signals.forEach(s -> g.onFill(s, 84));
        assertEquals(0, g.state().heldQuantity(), EPS);
        assertEquals((84 - 99.5) + (84 - 97.5), g.state().realizedPnl(), EPS);

        // halted for good: no new buys even on a fresh down-cross
        g.onPrice(101);
        assertTrue(g.onPrice(99.5).isEmpty());
    }

    @Test
    void unrealizedPnlTracksHeldGrids() {
        GridStrategy g = grid();
        g.onPrice(101);
        g.onFill(g.onPrice(99.5).get(0), 99.5);
        assertEquals(1.5, g.state().unrealizedPnl(101), EPS);
    }

    @Test
    void rejectsBadConfig() {
        RiskManager none = RiskManager.unlimited();
        assertThrows(IllegalArgumentException.class, () -> new GridStrategy("SPY", 110, 90, 10, 1, none));
        assertThrows(IllegalArgumentException.class, () -> new GridStrategy("SPY", 90, 110, 0, 1, none));
        assertThrows(IllegalArgumentException.class, () -> new GridStrategy("SPY", 90, 110, 10, 0, none));
        assertThrows(IllegalArgumentException.class,
                () -> new GridStrategy("SPY", 90, 110, 10, 1, new RiskManager(10, 95)));
    }

    @Test
    void rejectsFillForUnknownSignal() {
        GridStrategy g = grid();
        TradeSignal foreign = new TradeSignal("QQQ", TradeAction.BUY, 0, 100, 1, "x");
        assertThrows(IllegalArgumentException.class, () -> g.onFill(foreign, 100));
    }
}
