/**
 * This class creates price levels using a GridConfig.
 * Each interval has the same spacing.
 * The first level is the lower price.
 * The last level is the upper price.
 */



public class GridPrice {
    /**Stores the price of each level.*/
    private double[] levels;

    /**The spacing between two levels.*/
    private double spacing;


    public GridPrice(double[] levels, double spacing) {
        this.levels = levels;
        this.spacing = spacing;
    }


    /**
     * Creates the grid price levels using the given settings.
     *
     * @param config is the grid configuration
     * @throws IllegalArgumentException if config is null
     *                                  or the number of levels is too large for an array
     */
    public void PriceGrid(GridConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null.");
        }

        double lowerPrice = config.getLowerPrice();
        double upperPrice = config.getUpperPrice();
        int gridCount = config.getGridCount();

        /**Make sure adding one does not exceed the int limit.*/
        if (gridCount == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("gridCount is too large.");
        }

        this.spacing = config.getSpacing();
        this.levels = new double[gridCount + 1];

        /**Start from the lower price and add spacing for each level.*/
        for (int i = 0; i < gridCount; i++) {
            levels[i] = lowerPrice + i * spacing;
        }

        /**Use the exact upper price for the last level.*/
        levels[gridCount] = upperPrice;
    }


    /**
     * Returns the price at the given index.
     * The first level has index 0.
     *
     * @param index is the position of the level
     * @return the price at this level
     * @throws IndexOutOfBoundsException if the index is outside the array
     */
    public double getPrice(int index) {
        if (index < 0 || index >= levels.length) {
            throw new IndexOutOfBoundsException("Level index is out of range.");
        }

        return this.levels[index];
    }


    /**
     * Returns the number of price levels.
     *
     * @return the number of levels
     */
    public int getLevelCount() {
        return this.levels.length;
    }


    /**
     * Returns the spacing between two levels.
     *
     * @return the grid spacing
     */
    public double getSpacing() {
        return this.spacing;
    }









}
