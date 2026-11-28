package microarch.delivery.core.domain.model.courier;

import libs.ddd.Aggregate;
import libs.errs.Error;
import libs.errs.GeneralErrors;
import libs.errs.Guard;
import libs.errs.Result;
import libs.errs.UnitResult;
import lombok.Getter;
import microarch.delivery.core.domain.model.kernel.AssignmentStatus;
import microarch.delivery.core.domain.model.kernel.Assignment;
import microarch.delivery.core.domain.model.kernel.Assignment.AssignmentSnapshot;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Volume;
import microarch.delivery.core.domain.model.order.Order;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Courier extends Aggregate<UUID> {

    private static final int MAX_VOLUME = 20;

    @Getter
    private String name;
    @Getter
    private Location location;
    @Getter
    private Volume volume;
    private final Map<UUID, Assignment> assignments = new HashMap<>();

    private Courier(String name, Location location) {
        super(UUID.randomUUID());
        this.name = name;
        this.location = location;
        this.volume = Volume.create(0).getValueOrThrow();
    }

    public static Result<Courier, Error> create(String name, Location location) {
        var err = Guard.againstNullOrEmpty(name, "name");
        if (err != null) {
            return Result.failure(err);
        }
        if (location == null) {
            return Result.failure(GeneralErrors.valueIsRequired("location"));
        }
        return Result.success(new Courier(name, location));
    }

    public Map<UUID, AssignmentSnapshot> getAssignments() {
        var snapshots = new HashMap<UUID, AssignmentSnapshot>();
        assignments.forEach((orderId, assignment) -> snapshots.put(orderId, assignment.toSnapshot()));
        return Map.copyOf(snapshots);
    }

    private boolean isOrderWithinMaxVolume(Volume orderVolume) {
        return orderVolume != null && MAX_VOLUME - volume.getValue() >= orderVolume.getValue();
    }

    public UnitResult<Error> assignOrder(Order order) {
        if (order == null) {
            return UnitResult.failure(GeneralErrors.valueIsRequired("order"));
        }
        if (!isOrderWithinMaxVolume(order.getVolume())) {
            return UnitResult.failure(Errors.tooMuchVolume());
        }
        if (assignments.containsKey(order.getId())) {
            return UnitResult.failure(Order.Errors.orderAlreadyAssigned());
        }
        var volumeResult = this.volume.increase(order.getVolume());
        if (volumeResult.isFailure()) {
            return UnitResult.failure(volumeResult.getError());
        }
        var assignmentResult = Assignment.create(order);
        if (assignmentResult.isFailure()) {
            return UnitResult.failure(assignmentResult.getError());
        }
        this.assignments.put(order.getId(), assignmentResult.getValue());
        this.volume = volumeResult.getValue();
        return UnitResult.success();
    }

    public UnitResult<Error> closeAssignment(UUID orderId) {
        var err = Guard.againstNullOrEmpty(orderId, "orderId");
        if (err != null) {
            return UnitResult.failure(err);
        }
        var assignment = assignments.get(orderId);
        if (assignment == null) {
            return UnitResult.failure(GeneralErrors.notFound("assignment", orderId));
        }
        if (assignment.getStatus() == AssignmentStatus.COMPLETED) {
            return UnitResult.failure(Errors.assignmentAlreadyCompleted());
        }
        var volumeResult = volume.decrease(assignment.getVolume());
        if (volumeResult.isFailure()) {
            return UnitResult.failure(volumeResult.getError());
        }
        var assignmentResult = assignment.completeAssignment(location);
        if (assignmentResult.isFailure()) {
            return UnitResult.failure(Errors.distanceFarFromAssignmentLocation());
        }
        volume = volumeResult.getValue();
        return UnitResult.success();
    }

    public UnitResult<Error> move(Location location) {
        if (location == null) {
            return UnitResult.failure(GeneralErrors.valueIsRequired("location"));
        }
        var distance = this.location.distanceTo(location);
        if (distance > 1) {
            return UnitResult.failure(GeneralErrors.valueMustBeLessOrEqual("distance", distance, 1));
        }
        this.location = location;
        return UnitResult.success();
    }

    public static class Errors {
        public static Error assignmentAlreadyCompleted() {
            return Error.of(
                    "assignment.status.is.already.completed",
                    "Назначение уже в статусе COMPLETED"
            );
        }

        public static Error tooMuchVolume() {
            return Error.of(
                    "order.volume.is.too.much",
                    "Объем заказа слишком большой для курьера"
            );
        }

        public static Error distanceFarFromAssignmentLocation() {
            return Error.of(
                    "assignment.location.is.too.far",
                    "Расстояние до пункта назначения не позволяет закрыть заказ"
            );
        }
    }


}
