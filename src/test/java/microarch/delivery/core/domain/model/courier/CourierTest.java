package microarch.delivery.core.domain.model.courier;

import libs.errs.Error;
import libs.errs.GeneralErrors;
import microarch.delivery.core.domain.model.kernel.AssignmentStatus;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Volume;
import microarch.delivery.core.domain.model.order.Order;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourierTest {

    @Nested
    class Creation {

        @Test
        void createsCourier() {
            var name = "Courier";
            var location = Location.mustCreate(5, 5);

            var result = Courier.create(name, location);

            assertTrue(result.isSuccess());
            var courier = result.getValue();
            assertNotNull(courier.getId());
            assertEquals(name, courier.getName());
            assertEquals(location, courier.getLocation());
            assertEquals(Volume.ZERO_VOLUME, courier.getVolume());
            assertTrue(courier.getAssignments().isEmpty());
        }

        @ParameterizedTest
        @MethodSource("invalidArguments")
        void rejectsInvalidArguments(String name, Location location, Error expectedError) {
            var result = Courier.create(name, location);

            assertTrue(result.isFailure());
            assertEquals(expectedError, result.getError());
        }

        private static Stream<Arguments> invalidArguments() {
            var location = Location.mustCreate(5, 5);
            return Stream.of(
                    Arguments.of(null, location, GeneralErrors.valueIsRequired("name")),
                    Arguments.of("", location, GeneralErrors.valueIsRequired("name")),
                    Arguments.of("   ", location, GeneralErrors.valueIsRequired("name")),
                    Arguments.of("Courier", null, GeneralErrors.valueIsRequired("location"))
            );
        }
    }

    @Nested
    class Assignment {

        @ParameterizedTest
        @MethodSource("successfulCases")
        void assignsOrder(Courier courier, Order order, int expectedVolume) {
            var result = courier.assignOrder(order);

            assertTrue(result.isSuccess());
            assertEquals(Volume.create(expectedVolume).getValueOrThrow(), courier.getVolume());
            assertEquals(Order.Errors.orderAlreadyAssigned(), order.assignOrder().getError());
        }

        private static Stream<Arguments> successfulCases() {
            var location = Location.mustCreate(5, 5);
            return Stream.of(
                    Arguments.of(courierWithVolume(0), createOrder(1, location), 1),
                    Arguments.of(courierWithVolume(0), createOrder(20, location), 20),
                    Arguments.of(courierWithVolume(5), createOrder(15, location), 20)
            );
        }

        @ParameterizedTest
        @MethodSource("failureCases")
        void rejectsAssignmentWithoutChangingState(Courier courier, Order order, Error expectedError) {
            var originalVolume = courier.getVolume();

            var result = courier.assignOrder(order);

            assertTrue(result.isFailure());
            assertEquals(expectedError, result.getError());
            assertEquals(originalVolume, courier.getVolume());
            if (order != null) {
                assertTrue(order.assignOrder().isSuccess());
            }
        }

        private static Stream<Arguments> failureCases() {
            var location = Location.mustCreate(5, 5);
            var tooMuchVolume = Courier.Errors.tooMuchVolume();

            var assignedCourier = courierWithVolume(0);
            var assignedOrder = createOrder(5, location);
            assignedCourier.assignOrder(assignedOrder).getOrElseThrow();
            var duplicateOrder = Order.create(assignedOrder.getId(), location, Volume.create(1).getValueOrThrow())
                    .getValueOrThrow();

            var completedCourier = courierWithVolume(0);
            var completedOrder = createOrder(5, location);
            completedCourier.assignOrder(completedOrder).getOrElseThrow();
            completedCourier.closeAssignment(completedOrder.getId()).getOrElseThrow();
            var completedDuplicate = Order.create(completedOrder.getId(), location, Volume.create(1).getValueOrThrow())
                    .getValueOrThrow();

            return Stream.of(
                    Arguments.of(courierWithVolume(0), null, GeneralErrors.valueIsRequired("order")),
                    Arguments.of(courierWithVolume(0), createOrder(21, location), tooMuchVolume),
                    Arguments.of(courierWithVolume(5), createOrder(16, location), tooMuchVolume),
                    Arguments.of(courierWithVolume(20), createOrder(1, location), tooMuchVolume),
                    Arguments.of(courierWithVolume(1), createOrder(Integer.MAX_VALUE, location), tooMuchVolume),
                    Arguments.of(assignedCourier, duplicateOrder, Order.Errors.orderAlreadyAssigned()),
                    Arguments.of(completedCourier, completedDuplicate, Order.Errors.orderAlreadyAssigned())
            );
        }
    }

    @Nested
    class Completion {

        @ParameterizedTest
        @MethodSource("successfulCases")
        void completesAssignment(Courier courier, UUID orderId, int expectedVolume) {
            var result = courier.closeAssignment(orderId);

            assertTrue(result.isSuccess());
            assertEquals(Volume.create(expectedVolume).getValueOrThrow(), courier.getVolume());
        }

        private static Stream<Arguments> successfulCases() {
            var fullCourier = courierWithVolume(0);
            var fullOrder = createOrder(20, Location.mustCreate(5, 6));
            fullCourier.assignOrder(fullOrder).getOrElseThrow();

            var partialCourier = courierWithVolume(15);
            var partialOrder = createOrder(5, partialCourier.getLocation());
            partialCourier.assignOrder(partialOrder).getOrElseThrow();

            var remainingCourier = courierWithVolume(0);
            var firstOrder = createOrder(5, remainingCourier.getLocation());
            var remainingOrder = createOrder(15, remainingCourier.getLocation());
            remainingCourier.assignOrder(firstOrder).getOrElseThrow();
            remainingCourier.assignOrder(remainingOrder).getOrElseThrow();
            remainingCourier.closeAssignment(firstOrder.getId()).getOrElseThrow();

            return Stream.of(
                    Arguments.of(fullCourier, fullOrder.getId(), 0),
                    Arguments.of(partialCourier, partialOrder.getId(), 15),
                    Arguments.of(remainingCourier, remainingOrder.getId(), 0)
            );
        }

        @ParameterizedTest
        @MethodSource("failureCases")
        void rejectsCompletionWithoutChangingState(Courier courier, UUID orderId, Error expectedError) {
            var originalVolume = courier.getVolume();

            var result = courier.closeAssignment(orderId);

            assertTrue(result.isFailure());
            assertEquals(expectedError, result.getError());
            assertEquals(originalVolume, courier.getVolume());
            assertEquals(expectedError, courier.closeAssignment(orderId).getError());
            assertEquals(originalVolume, courier.getVolume());
        }

        private static Stream<Arguments> failureCases() {
            var nonOwner = courierWithVolume(20);
            var owner = courierWithVolume(0);
            var foreignOrder = createOrder(5, owner.getLocation());
            owner.assignOrder(foreignOrder).getOrElseThrow();

            var completedCourier = courierWithVolume(0);
            var completedOrder = createOrder(20, completedCourier.getLocation());
            completedCourier.assignOrder(completedOrder).getOrElseThrow();
            completedCourier.closeAssignment(completedOrder.getId()).getOrElseThrow();

            var partiallyCompletedCourier = courierWithVolume(15);
            var partialOrder = createOrder(5, partiallyCompletedCourier.getLocation());
            partiallyCompletedCourier.assignOrder(partialOrder).getOrElseThrow();
            partiallyCompletedCourier.closeAssignment(partialOrder.getId()).getOrElseThrow();

            var distantCourier = courierWithVolume(0);
            var distantOrder = createOrder(20, Location.mustCreate(5, 7));
            distantCourier.assignOrder(distantOrder).getOrElseThrow();

            return Stream.of(
                    Arguments.of(courierWithVolume(0), null, GeneralErrors.valueIsRequired("orderId")),
                    Arguments.of(courierWithVolume(0), new UUID(0, 0), GeneralErrors.valueIsRequired("orderId")),
                    Arguments.of(nonOwner, foreignOrder.getId(),
                            GeneralErrors.notFound("assignment", foreignOrder.getId())),
                    Arguments.of(completedCourier, completedOrder.getId(), Courier.Errors.assignmentAlreadyCompleted()),
                    Arguments.of(partiallyCompletedCourier, partialOrder.getId(), Courier.Errors.assignmentAlreadyCompleted()),
                    Arguments.of(distantCourier, distantOrder.getId(), Courier.Errors.distanceFarFromAssignmentLocation())
            );
        }
    }

    @Nested
    class Movement {

        @ParameterizedTest
        @MethodSource("successfulCases")
        void movesCourier(Courier courier, Location destination) {
            var originalVolume = courier.getVolume();
            var originalAssignments = courier.getAssignments();

            var result = courier.move(destination);

            assertTrue(result.isSuccess());
            assertEquals(destination, courier.getLocation());
            assertEquals(originalVolume, courier.getVolume());
            assertEquals(originalAssignments, courier.getAssignments());
        }

        private static Stream<Arguments> successfulCases() {
            return Stream.of(
                    Arguments.of(courierWithVolume(0), Location.mustCreate(4, 5)),
                    Arguments.of(courierWithVolume(5), Location.mustCreate(6, 5)),
                    Arguments.of(courierWithVolume(5), Location.mustCreate(5, 4)),
                    Arguments.of(courierWithVolume(20), Location.mustCreate(5, 6)),
                    Arguments.of(courierWithVolume(5), Location.mustCreate(5, 5))
            );
        }

        @ParameterizedTest
        @MethodSource("failureCases")
        void rejectsMovementWithoutChangingState(Courier courier, Location destination, Error expectedError) {
            var originalLocation = courier.getLocation();
            var originalVolume = courier.getVolume();
            var originalAssignments = courier.getAssignments();

            var result = courier.move(destination);

            assertTrue(result.isFailure());
            assertEquals(expectedError, result.getError());
            assertEquals(originalLocation, courier.getLocation());
            assertEquals(originalVolume, courier.getVolume());
            assertEquals(originalAssignments, courier.getAssignments());
        }

        private static Stream<Arguments> failureCases() {
            var tooFar = GeneralErrors.valueMustBeLessOrEqual("distance", 2, 1);
            return Stream.of(
                    Arguments.of(courierWithVolume(5), null, GeneralErrors.valueIsRequired("location")),
                    Arguments.of(courierWithVolume(5), Location.mustCreate(5, 7), tooFar),
                    Arguments.of(courierWithVolume(5), Location.mustCreate(6, 6), tooFar),
                    Arguments.of(courierWithVolume(5), Location.mustCreate(10, 10),
                            GeneralErrors.valueMustBeLessOrEqual("distance", 10, 1))
            );
        }
    }

    @Nested
    class AssignmentsAccess {

        @Test
        void returnsUnmodifiableAssignments() {
            var courier = courierWithVolume(0);
            var order = createOrder(5, courier.getLocation());
            courier.assignOrder(order).getOrElseThrow();

            var assignments = courier.getAssignments();

            assertEquals(Set.of(order.getId()), assignments.keySet());
            var snapshot = assignments.get(order.getId());
            assertNotNull(snapshot);
            assertNotNull(snapshot.id());
            assertEquals(order.getId(), snapshot.orderId());
            assertEquals(order.getVolume(), snapshot.volume());
            assertEquals(order.getLocation(), snapshot.location());
            assertEquals(AssignmentStatus.ASSIGNED, snapshot.status());
            assertThrows(UnsupportedOperationException.class, () -> assignments.remove(order.getId()));
            assertThrows(UnsupportedOperationException.class, () -> assignments.put(UUID.randomUUID(), snapshot));
            assertEquals(Set.of(order.getId()), courier.getAssignments().keySet());
        }

        @Test
        void keepsSnapshotsIndependentOfCourierChanges() {
            var courier = courierWithVolume(0);
            var order = createOrder(5, courier.getLocation());
            courier.assignOrder(order).getOrElseThrow();
            var assignments = courier.getAssignments();
            var snapshot = assignments.get(order.getId());

            var additionalOrder = createOrder(1, courier.getLocation());
            courier.assignOrder(additionalOrder).getOrElseThrow();
            courier.closeAssignment(order.getId()).getOrElseThrow();

            assertEquals(Set.of(order.getId()), assignments.keySet());
            assertEquals(AssignmentStatus.ASSIGNED, snapshot.status());
            var updatedAssignments = courier.getAssignments();
            assertEquals(Set.of(order.getId(), additionalOrder.getId()), updatedAssignments.keySet());
            var updatedSnapshot = updatedAssignments.get(order.getId());
            assertEquals(AssignmentStatus.COMPLETED, updatedSnapshot.status());
            assertEquals(snapshot.id(), updatedSnapshot.id());
        }
    }

    private static Courier courierWithVolume(int initialVolume) {
        var courier = Courier.create("Courier", Location.mustCreate(5, 5)).getValueOrThrow();
        if (initialVolume > 0) {
            courier.assignOrder(createOrder(initialVolume, courier.getLocation())).getOrElseThrow();
        }
        return courier;
    }

    private static Order createOrder(int volume, Location location) {
        return Order.create(UUID.randomUUID(), location, Volume.create(volume).getValueOrThrow()).getValueOrThrow();
    }
}
