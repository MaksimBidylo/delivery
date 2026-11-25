package microarch.delivery.core.domain.model;

import libs.ddd.BaseEntity;
import libs.errs.Error;
import libs.errs.GeneralErrors;
import libs.errs.Guard;
import libs.errs.Result;
import libs.errs.UnitResult;
import lombok.Getter;

import java.util.UUID;

@Getter
public class Assignment extends BaseEntity<UUID> {

    private UUID orderId;
    private Volume volume;
    private Location location;
    private AssigmentStatus status;

    private Assignment(UUID orderId, Volume volume, Location location) {
        super(UUID.randomUUID());
        this.orderId = orderId;
        this.volume = volume;
        this.location = location;
        this.status = AssigmentStatus.ASSIGNED;
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

    public UnitResult<Error> completeAssignment(Location couriersLocation) {
        if (couriersLocation == null) {
            return UnitResult.failure(GeneralErrors.valueIsRequired("couriersLocation"));
        }
        var distance = couriersLocation.distanceTo(this.location);
        if (distance > 1) {
            return UnitResult.failure(GeneralErrors.valueMustBeLessOrEqual("distance", distance, 1));
        }
        this.status = AssigmentStatus.COMPLETED;
        return UnitResult.success();
    }
}
