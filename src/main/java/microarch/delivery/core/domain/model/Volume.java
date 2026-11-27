package microarch.delivery.core.domain.model;

import libs.ddd.ValueObject;
import libs.errs.Error;
import libs.errs.GeneralErrors;
import libs.errs.Guard;
import libs.errs.Result;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Volume extends ValueObject<Volume> {

    private final int value;

    public static Result<Volume, Error> create(int value) {
        var err = Guard.againstOutOfRange(value, 1, Integer.MAX_VALUE, "value");
        if (err != null) {
            return Result.failure(err);
        }
        return Result.success(new Volume(value));
    }

    public Result<Volume, Error> increase(int value) {
        if (value <= 0) {
            return Result.failure(GeneralErrors.valueMustBeGreaterThan("value", value, 0));
        }
        int maxIncrease = Integer.MAX_VALUE - this.value;
        if (value > maxIncrease) {
            return Result.failure(
                    GeneralErrors.valueMustBeLessOrEqual("value", value, maxIncrease));
        }
        return Result.success(new Volume(this.value + value));
    }

    public Result<Volume, Error> decrease(int value) {
        if (value <= 0) {
            return Result.failure(GeneralErrors.valueMustBeGreaterThan("volume", value, 0));
        }
        if (this.value <= value) {
            return Result.failure(GeneralErrors.valueMustBeLessThan("value", value, this.value));
        }
        return Result.success(new Volume(this.value - value));
    }

    @Override
    protected Iterable<Object> equalityComponents() {
        return List.of(value);
    }
}
