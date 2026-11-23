package microarch.delivery.core.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationTest {

    @ParameterizedTest
    @CsvSource({"1, 1", "1, 10", "10, 1", "10, 10", "5, 5"})
    void createsLocationWithinMapBoundaries(int horizontalCoordinate, int verticalCoordinate) {
        var result = Location.create(horizontalCoordinate, verticalCoordinate);

        assertTrue(result.isSuccess());
        assertEquals(horizontalCoordinate, result.getValue().getHorizontalCoordinate());
        assertEquals(verticalCoordinate, result.getValue().getVerticalCoordinate());
    }

    @ParameterizedTest
    @CsvSource({"0, 5", "11, 5", "5, 0", "5, 11"})
    void rejectsCoordinatesOutsideMap(int horizontalCoordinate, int verticalCoordinate) {
        assertTrue(Location.create(horizontalCoordinate, verticalCoordinate).isFailure());
    }

    @Test
    void movesOneCellInEachDirection() {
        var location = Location.mustCreate(5, 5);

        assertEquals(Location.mustCreate(4, 5), location.goLeft());
        assertEquals(Location.mustCreate(6, 5), location.goRight());
        assertEquals(Location.mustCreate(5, 4), location.goUp());
        assertEquals(Location.mustCreate(5, 6), location.goDown());
        assertEquals(5, location.getHorizontalCoordinate());
        assertEquals(5, location.getVerticalCoordinate());
    }

    @Test
    void staysWithinMapAtBoundaries() {
        var minimum = Location.mustCreate(1, 1);
        var maximum = Location.mustCreate(10, 10);

        assertEquals(minimum, minimum.goLeft());
        assertEquals(minimum, minimum.goUp());
        assertEquals(maximum, maximum.goRight());
        assertEquals(maximum, maximum.goDown());
    }

    @ParameterizedTest
    @CsvSource({
            "5, 5, 5, 5, 0",
            "2, 3, 4, 3, 2",
            "2, 3, 2, 6, 3",
            "2, 3, 4, 6, 5",
            "4, 6, 2, 3, 5",
            "2, 6, 4, 3, 5",
            "1, 1, 10, 10, 18"
    })
    void calculatesDistance(int fromX, int fromY, int toX, int toY, int expectedDistance) {
        var from = Location.mustCreate(fromX, fromY);
        var to = Location.mustCreate(toX, toY);

        assertEquals(expectedDistance, from.distanceTo(to));
    }

    @Test
    void comparesLocationsByBothCoordinates() {
        var location = Location.mustCreate(4, 5);
        var sameLocation = Location.mustCreate(4, 5);
        var anotherLocation = Location.mustCreate(4, 6);

        assertEquals(location, sameLocation);
        assertNotEquals(location, anotherLocation);
    }
}
