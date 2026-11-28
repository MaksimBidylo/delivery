package microarch.delivery.core.domain.model.order;

import libs.errs.Error;
import libs.errs.GeneralErrors;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Volume;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    @Nested
    class Creation {

        @ParameterizedTest
        @ValueSource(ints = {1, Integer.MAX_VALUE})
        void createsOrderWithPositiveVolume(int value) {
            var id = UUID.randomUUID();
            var location = Location.mustCreate(5, 5);
            var volume = Volume.create(value).getValueOrThrow();

            var result = Order.create(id, location, volume);

            assertTrue(result.isSuccess());
            var order = result.getValue();
            assertEquals(id, order.getId());
            assertEquals(location, order.getLocation());
            assertEquals(volume, order.getVolume());
        }

        @ParameterizedTest
        @MethodSource("invalidArguments")
        void rejectsInvalidArguments(UUID id, Location location, Volume volume, Error expectedError) {
            var result = Order.create(id, location, volume);

            assertTrue(result.isFailure());
            assertEquals(expectedError, result.getError());
        }

        private static Stream<Arguments> invalidArguments() {
            var id = UUID.randomUUID();
            var location = Location.mustCreate(5, 5);
            var volume = Volume.create(1).getValueOrThrow();
            return Stream.of(
                    Arguments.of(null, location, volume, GeneralErrors.valueIsRequired("id")),
                    Arguments.of(new UUID(0, 0), location, volume, GeneralErrors.valueIsRequired("id")),
                    Arguments.of(id, null, volume, GeneralErrors.valueIsRequired("location")),
                    Arguments.of(id, location, null, GeneralErrors.valueIsRequired("volume")),
                    Arguments.of(id, location, Volume.ZERO_VOLUME,
                            GeneralErrors.valueMustBeGreaterThan("volume", 0, 0))
            );
        }
    }

    @Nested
    class Assignment {

        @Test
        void assignsCreatedOrder() {
            var order = createOrder();

            var result = order.assignOrder();

            assertTrue(result.isSuccess());
            assertEquals(Order.Errors.orderAlreadyAssigned(), order.assignOrder().getError());
        }

        @ParameterizedTest
        @MethodSource("failureCases")
        void rejectsAssignmentWithoutChangingStatus(Order order, Error expectedError) {
            var result = order.assignOrder();

            assertTrue(result.isFailure());
            assertEquals(expectedError, result.getError());
            assertEquals(expectedError, order.assignOrder().getError());
        }

        private static Stream<Arguments> failureCases() {
            return Stream.of(
                    Arguments.of(assignedOrder(), Order.Errors.orderAlreadyAssigned()),
                    Arguments.of(completedOrder(), Order.Errors.orderAlreadyCompleted())
            );
        }
    }

    @Nested
    class Completion {

        @Test
        void completesAssignedOrder() {
            var order = assignedOrder();

            var result = order.completeOrder();

            assertTrue(result.isSuccess());
            assertEquals(Order.Errors.orderAlreadyCompleted(), order.assignOrder().getError());
        }

        @ParameterizedTest
        @MethodSource("failureCases")
        void rejectsCompletionWithoutChangingStatus(Order order, Error expectedError) {
            var result = order.completeOrder();

            assertTrue(result.isFailure());
            assertEquals(expectedError, result.getError());
            assertEquals(expectedError, order.completeOrder().getError());
        }

        private static Stream<Arguments> failureCases() {
            return Stream.of(
                    Arguments.of(createOrder(), Order.Errors.orderShouldBeAssigned()),
                    Arguments.of(completedOrder(), Order.Errors.orderAlreadyCompleted())
            );
        }
    }

    private static Order createOrder() {
        return Order.create(UUID.randomUUID(), Location.mustCreate(5, 5), Volume.create(1).getValueOrThrow())
                .getValueOrThrow();
    }

    private static Order assignedOrder() {
        var order = createOrder();
        order.assignOrder().getOrElseThrow();
        return order;
    }

    private static Order completedOrder() {
        var order = assignedOrder();
        order.completeOrder().getOrElseThrow();
        return order;
    }
}
