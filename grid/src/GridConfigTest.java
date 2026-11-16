import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GridConfigTest {

    @Test
    void correctSettings() {
        GridConfig config = new GridConfig(100, 120, 4, 10);

        assertEquals(100.0, config.getLowerPrice());
        assertEquals(120.0, config.getUpperPrice());
        assertEquals(4, config.getGridCount());
        assertEquals(10, config.getQuantity());
        assertEquals(5.0, config.getSpacing());
    }

    @Test
    void oneInterval() {
        GridConfig config = new GridConfig(100, 120, 1, 1);

        assertEquals(1, config.getGridCount());
        assertEquals(1, config.getQuantity());
        assertEquals(20.0, config.getSpacing());
    }




    @Test
    void decimalSpacing() {
        GridConfig config = new GridConfig(100, 101, 3, 10);

        /**Double calculation have errors, allow |expected-actual|<= 0.000000001*/
        assertEquals(0.3333333333, config.getSpacing(), 0.000000001);
    }

    @Test
    void invalidPrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(0, 120, 4, 10));

        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(-100, 120, 4, 10));

        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(100, 0, 4, 10));

        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(100, -120, 4, 10));
    }

    @Test
    void invalidRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(100, 100, 4, 10));

        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(120, 100, 4, 10));
    }

    @Test
    void invalidGridCount() {
        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(100, 120, 0, 10));

        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(100, 120, -4, 10));
    }


    @Test
    void invalidQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(100, 120, 4, 0));

        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(100, 120, 4, -10));
    }

    @Test
    void nonFinitePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(Double.NaN, 120, 4, 10));

        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(100, Double.NaN, 4, 10));

        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(Double.POSITIVE_INFINITY, 120, 4, 10));

        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(100, Double.POSITIVE_INFINITY, 4, 10));

        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(Double.NEGATIVE_INFINITY, 120, 4, 10));

        assertThrows(IllegalArgumentException.class,
                () -> new GridConfig(100, Double.NEGATIVE_INFINITY, 4, 10));
    }


}