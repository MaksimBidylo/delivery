package microarch.delivery.core.domain.model.kernel;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationTest {

    @Nested
    class Creation {

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
    }

    @Nested
    class Movement {

        @ParameterizedTest
        @MethodSource("movementCases")
        void movesWithinMapWithoutChangingOriginalLocation(
                Location location,
                UnaryOperator<Location> movement,
                Location expectedLocation
        ) {
            var originalX = location.getHorizontalCoordinate();
            var originalY = location.getVerticalCoordinate();

            var result = movement.apply(location);

            assertEquals(expectedLocation, result);
            assertEquals(originalX, location.getHorizontalCoordinate());
            assertEquals(originalY, location.getVerticalCoordinate());
        }

        private static Stream<Arguments> movementCases() {
            var center = Location.mustCreate(5, 5);
            var minimum = Location.mustCreate(1, 1);
            var maximum = Location.mustCreate(10, 10);
            UnaryOperator<Location> left = Location::goLeft;
            UnaryOperator<Location> right = Location::goRight;
            UnaryOperator<Location> up = Location::goUp;
            UnaryOperator<Location> down = Location::goDown;

            return Stream.of(
                    Arguments.of(center, left, Location.mustCreate(4, 5)),
                    Arguments.of(center, right, Location.mustCreate(6, 5)),
                    Arguments.of(center, up, Location.mustCreate(5, 4)),
                    Arguments.of(center, down, Location.mustCreate(5, 6)),
                    Arguments.of(minimum, left, minimum),
                    Arguments.of(minimum, up, minimum),
                    Arguments.of(maximum, right, maximum),
                    Arguments.of(maximum, down, maximum)
            );
        }
    }

    @Nested
    class Distance {

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
    }

    @Nested
    class Equality {

        @Test
        void comparesLocationsByBothCoordinates() {
            var location = Location.mustCreate(4, 5);
            var sameLocation = Location.mustCreate(4, 5);
            var anotherLocation = Location.mustCreate(4, 6);

            assertEquals(location, sameLocation);
            assertNotEquals(location, anotherLocation);
        }
    }
}
