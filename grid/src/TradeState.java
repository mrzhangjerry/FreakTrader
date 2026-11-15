/**
 * Define the possible decisions a trading strategy can return
 * * */

public class TradeState {
    /**represents the action after evaluating a price*/
    public enum state{
        BUY,
        SELL,
        NO_ACTION
    }
}
