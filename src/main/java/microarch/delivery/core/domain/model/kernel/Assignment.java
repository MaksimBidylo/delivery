package microarch.delivery.core.domain.model.kernel;

import libs.ddd.BaseEntity;
import libs.errs.Error;
import libs.errs.GeneralErrors;
import libs.errs.Guard;
import libs.errs.Result;
import libs.errs.UnitResult;
import lombok.Getter;
import microarch.delivery.core.domain.model.order.Order;

import java.util.UUID;

@Getter
public class Assignment extends BaseEntity<UUID> {

    private final UUID orderId;
    private final Volume volume;
    private final Location location;
    private AssignmentStatus status;

    private Assignment(UUID orderId, Volume volume, Location location) {
        super(UUID.randomUUID());
        this.orderId = orderId;
        this.volume = volume;
        this.location = location;
        this.status = AssignmentStatus.ASSIGNED;
    }

    public static Result<Assignment, Error> create(Order order) {
        var result = create(order.getId(), order.getVolume(), order.getLocation());
        if (result.isFailure()) {
            return result;
        }
        var orderResult = order.assignOrder();
        if (orderResult.isFailure()) {
            return Result.failure(orderResult.getError());
        }
        return result;
    }

    public static Result<Assignment, Error> create(UUID orderId, Volume volume, Location location) {
        var err = Guard.againstNullOrEmpty(orderId, "orderId");
        if (err != null) {
            return Result.failure(err);
        }
        if (volume == null) {
            return Result.failure(GeneralErrors.valueIsRequired("volume"));
        }
        if (location == null) {
            return Result.failure(GeneralErrors.valueIsRequired("location"));
        }
        return Result.success(new Assignment(orderId, volume, location));
    }

    public AssignmentSnapshot toSnapshot() {
        return new AssignmentSnapshot(getId(), orderId, volume, location, status);
    }

    public UnitResult<Error> completeAssignment(Location couriersLocation) {
        if (couriersLocation == null) {
            return UnitResult.failure(GeneralErrors.valueIsRequired("couriersLocation"));
        }
        var distance = couriersLocation.distanceTo(this.location);
        if (distance > 1) {
            return UnitResult.failure(GeneralErrors.valueMustBeLessOrEqual("distance", distance, 1));
        }
        this.status = AssignmentStatus.COMPLETED;
        return UnitResult.success();
    }

    public record AssignmentSnapshot(UUID id, UUID orderId, Volume volume, Location location, AssignmentStatus status) {
    }
}
