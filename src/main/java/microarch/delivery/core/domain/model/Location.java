package microarch.delivery.core.domain.model;

import libs.ddd.ValueObject;
import libs.errs.Error;
import libs.errs.Guard;
import libs.errs.Result;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Location extends ValueObject<Location> {

    public static final int MIN_COORDINATE = 1;
    public static final int MAX_COORDINATE = 10;

    private final int horizontalCoordinate;
    private final int verticalCoordinate;

    public static Result<Location, Error> create(int horizontalCoordinate, int verticalCoordinate) {
        var result = validateCoordinate(horizontalCoordinate, "horizontalCoordinate");
        if (result.isFailure()) {
            return Result.failure(result.getError());
        }
        result = validateCoordinate(verticalCoordinate, "verticalCoordinate");
        if (result.isFailure()) {
            return Result.failure(result.getError());
        }
        return Result.success(new Location(horizontalCoordinate, verticalCoordinate));
    }

    public static Location mustCreate(int horizontalCoordinate, int verticalCoordinate) {
        return create(horizontalCoordinate, verticalCoordinate).getValueOrThrow();
    }

    private static Result<Void, Error> validateCoordinate(int coordinate, String name) {
        var err = Guard.againstOutOfRange(coordinate, Location.MIN_COORDINATE, Location.MAX_COORDINATE, name);
        if (err != null) {
            return Result.failure(err);
        }
        return Result.success();
    }

    public Location goLeft() {
        return this.horizontalCoordinate - 1 >= Location.MIN_COORDINATE
                ? mustCreate(this.horizontalCoordinate - 1, this.verticalCoordinate)
                : this;
    }

    public Location goRight() {
        return this.horizontalCoordinate + 1 <= Location.MAX_COORDINATE
                ? mustCreate(this.horizontalCoordinate + 1, this.verticalCoordinate)
                : this;
    }

    public Location goUp() {
        return this.verticalCoordinate - 1 >= Location.MIN_COORDINATE
                ? mustCreate(this.horizontalCoordinate, this.verticalCoordinate - 1)
                : this;
    }

    public Location goDown() {
        return this.verticalCoordinate + 1 <= Location.MAX_COORDINATE
                ? mustCreate(this.horizontalCoordinate, this.verticalCoordinate + 1)
                : this;
    }

    @Override
    protected Iterable<Object> equalityComponents() {
        return List.of(horizontalCoordinate, verticalCoordinate);
    }
}
