package microarch.delivery.core.domain.model.order;

import libs.ddd.Aggregate;
import libs.errs.Error;
import libs.errs.GeneralErrors;
import libs.errs.Guard;
import libs.errs.Result;
import libs.errs.UnitResult;
import lombok.Getter;
import microarch.delivery.core.domain.model.kernel.Location;
import microarch.delivery.core.domain.model.kernel.Volume;

import java.util.UUID;

public class Order extends Aggregate<UUID> {

    @Getter
    private final Location location;
    @Getter
    private final Volume volume;
    private OrderStatus status;

    public static Result<Order, Error> create(UUID id, Location location, Volume volume) {
        var err = Guard.againstNullOrEmpty(id, "id");
        if (err != null) {
            return Result.failure(err);
        }
        if (location == null) {
            return Result.failure(GeneralErrors.valueIsRequired("location"));
        }
        if (volume == null) {
            return Result.failure(GeneralErrors.valueIsRequired("volume"));
        }
        if (volume.getValue() <= 0) {
            return Result.failure(GeneralErrors.valueMustBeGreaterThan("volume", volume.getValue(), 0));
        }
        return Result.success(new Order(id, location, volume));
    }

    private Order(UUID id, Location location, Volume volume) {
        super(id);
        this.location = location;
        this.volume = volume;
        this.status = OrderStatus.CREATED;
    }

    public UnitResult<Error> assignOrder() {
        return switch (status) {
            case CREATED -> {
                status = OrderStatus.ASSIGNED;
                yield UnitResult.success();
            }
            case ASSIGNED -> UnitResult.failure(Errors.orderAlreadyAssigned());
            case COMPLETED -> UnitResult.failure(Errors.orderAlreadyCompleted());
        };
    }

    public UnitResult<Error> completeOrder() {
        return switch (status) {
            case ASSIGNED -> {
                status = OrderStatus.COMPLETED;
                yield UnitResult.success();
            }
            case CREATED -> UnitResult.failure(Errors.orderShouldBeAssigned());
            case COMPLETED -> UnitResult.failure(Errors.orderAlreadyCompleted());
        };
    }

    public static class Errors {
        public static Error orderAlreadyAssigned() {
            return Error.of(
                    "order.status.is.already.assigned",
                    "Заказ уже в статусе ASSIGNED"
            );
        }

        public static Error orderAlreadyCompleted() {
            return Error.of(
                    "order.status.is.already.completed",
                    "Заказ уже в статусе COMPLETED"
            );
        }

        public static Error orderShouldBeAssigned() {
            return Error.of(
                    "order.should.be.assigned",
                    "Заказ должен быть в статусе ASSIGNED"
            );
        }
    }
}
