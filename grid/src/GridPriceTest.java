import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GridPriceTest {
    /**Check all levels in a normal grid.*/
    @Test
    void correctLevels() {
        GridConfig config = new GridConfig(100, 120, 4, 10);
        GridPrice grid = new GridPrice(new double[0], 0);

        grid.PriceGrid(config);

        assertEquals(5, grid.getLevelCount());
        assertEquals(5.0, grid.getSpacing());

        assertEquals(100.0, grid.getPrice(0));
        assertEquals(105.0, grid.getPrice(1));
        assertEquals(110.0, grid.getPrice(2));
        assertEquals(115.0, grid.getPrice(3));
        assertEquals(120.0, grid.getPrice(4));
    }

    /**Check a grid with one interval and two levels.*/
    @Test
    void oneInterval() {
        GridConfig config = new GridConfig(100, 120, 1, 10);
        GridPrice grid = new GridPrice(new double[0], 0);

        grid.PriceGrid(config);

        assertEquals(2, grid.getLevelCount());
        assertEquals(20.0, grid.getSpacing());
        assertEquals(100.0, grid.getPrice(0));
        assertEquals(120.0, grid.getPrice(1));
    }

    /**Check levels with decimal spacing.*/
    @Test
    void decimalLevels() {
        GridConfig config = new GridConfig(100, 101, 3, 10);
        GridPrice grid = new GridPrice(new double[0], 0);

        grid.PriceGrid(config);

        assertEquals(4, grid.getLevelCount());
        assertEquals(0.3333333333, grid.getSpacing(), 0.000000001);

        assertEquals(100.0, grid.getPrice(0));
        assertEquals(100.3333333333, grid.getPrice(1), 0.000000001);
        assertEquals(100.6666666667, grid.getPrice(2), 0.000000001);
        assertEquals(101.0, grid.getPrice(3));
    }

    /**Check indexes outside the grid.*/
    @Test
    void invalidIndex() {
        GridConfig config = new GridConfig(100, 120, 4, 10);
        GridPrice grid = new GridPrice(new double[0], 0);

        grid.PriceGrid(config);

        assertThrows(IndexOutOfBoundsException.class,
                () -> grid.getPrice(-1));

        assertThrows(IndexOutOfBoundsException.class,
                () -> grid.getPrice(5));
    }

    /**Check that config cannot be null.*/
    @Test
    void nullConfig() {
        GridPrice grid = new GridPrice(new double[0], 0);

        assertThrows(IllegalArgumentException.class,
                () -> grid.PriceGrid(null));
    }

    /**Check that the level count cannot exceed the int limit.*/
    @Test
    void tooManyIntervals() {
        GridConfig config =
                new GridConfig(100, 120, Integer.MAX_VALUE, 10);
        GridPrice grid = new GridPrice(new double[0], 0);

        assertThrows(IllegalArgumentException.class,
                () -> grid.PriceGrid(config));
    }

    /**Check that new settings replace the old levels.*/
    @Test
    void changeGrid() {
        GridConfig firstConfig = new GridConfig(100, 120, 4, 10);
        GridConfig secondConfig = new GridConfig(50, 70, 2, 10);
        GridPrice grid = new GridPrice(new double[0], 0);

        grid.PriceGrid(firstConfig);

        assertEquals(5, grid.getLevelCount());
        assertEquals(5.0, grid.getSpacing());

        grid.PriceGrid(secondConfig);

        assertEquals(3, grid.getLevelCount());
        assertEquals(10.0, grid.getSpacing());
        assertEquals(50.0, grid.getPrice(0));
        assertEquals(60.0, grid.getPrice(1));
        assertEquals(70.0, grid.getPrice(2));

        assertThrows(IndexOutOfBoundsException.class,
                () -> grid.getPrice(3));
    }

}