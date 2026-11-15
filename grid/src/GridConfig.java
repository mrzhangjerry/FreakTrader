import static java.lang.Double.isFinite;

/**
 * This class stores the settings used by a grid trading strategy.
 * The settings include the price range, number of intervals,
 * and number of shares for each order.
 *
 * For example, a range from 100 to 120 with 4 intervals
 * has a spacing of 5.
 *
 * The settings can be read using getters and cannot be
 * changed after the configuration is created.
 */
public class GridConfig {


    private final double lowerPrice;
    private final double upperPrice;


     /**The number of equal intervals in the price range.*/
    private final int gridCount;


    /** The number of whole shares for each order.*/
    private final int quantity;


    /**
     * Creates a grid configuration with the given settings.
     *
     * @param lowerPrice is the lowest price in the grid
     * @param upperPrice is the highest price in the grid
     * @param gridCount is the number of girds set
     * @param quantity is the number of BUYs or SELLs for each trade
     * @throws IllegalArgumentException if a price is not finite or positive,
     *                                  upperPrice is not greater than lowerPrice,
     *                                  or gridCount or quantity is smaller than or equal to zero
     */
    public GridConfig(double lowerPrice, double upperPrice, int gridCount, int quantity) {

        if (!isFinite(lowerPrice) || lowerPrice <= 0) {
            throw new IllegalArgumentException(
                    "lowerPrice must be finite and greater than zero.");
        }

        if (!isFinite(upperPrice) || upperPrice <= 0) {
            throw new IllegalArgumentException(
                    "upperPrice must be finite and greater than zero.");
        }

        if (upperPrice <= lowerPrice) {
            throw new IllegalArgumentException(
                    "upperPrice must be greater than lowerPrice.");
        }

        if (gridCount <= 0) {
            throw new IllegalArgumentException(
                    "gridCount must be greater than zero.");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "quantity must be greater than zero.");
        }

        this.lowerPrice = lowerPrice;
        this.upperPrice = upperPrice;
        this.gridCount = gridCount;
        this.quantity = quantity;
    }


    /**
     * Returns the lowest price in the grid.
     *
     * @return the lower price
     */
    public double getLowerPrice() {
        return this.lowerPrice;
    }


    /**
     * Returns the highest price in the grid
     *
     * @return the upper price
     */
    public double getUpperPrice() {
        return this.upperPrice;
    }


    /**
     * Returns the number of intervals in the grid.
     *
     * @return the grid interval count
     */
    public int getGridCount() {
        return this.gridCount;
    }


    /**
     * Returns the number of whole shares for each order
     *
     * @return the order quantity
     */
    public int getQuantity() {
        return this.quantity;
    }


    /**
     * Calculates the spacing between two price levels
     *
     * @return the grid spacing
     * */
    public double getSpacing(){
        return (this.upperPrice - this.lowerPrice) / this.gridCount;
    }
}