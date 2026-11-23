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
        var err = Guard.againstOutOfRange(coordinate, MIN_COORDINATE, MAX_COORDINATE, name);
        if (err != null) {
            return Result.failure(err);
        }
        return Result.success();
    }

    public Location goLeft() {
        return horizontalCoordinate - 1 >= MIN_COORDINATE
                ? mustCreate(horizontalCoordinate - 1, verticalCoordinate)
                : this;
    }

    public Location goRight() {
        return horizontalCoordinate + 1 <= MAX_COORDINATE
                ? mustCreate(horizontalCoordinate + 1, verticalCoordinate)
                : this;
    }

    public Location goUp() {
        return verticalCoordinate - 1 >= MIN_COORDINATE
                ? mustCreate(horizontalCoordinate, verticalCoordinate - 1)
                : this;
    }

    public Location goDown() {
        return verticalCoordinate + 1 <= MAX_COORDINATE
                ? mustCreate(horizontalCoordinate, verticalCoordinate + 1)
                : this;
    }

    public int distanceTo(Location other) {
        return Math.abs(horizontalCoordinate - other.horizontalCoordinate)
                + Math.abs(verticalCoordinate - other.verticalCoordinate);
    }

    @Override
    protected Iterable<Object> equalityComponents() {
        return List.of(horizontalCoordinate, verticalCoordinate);
    }
}
