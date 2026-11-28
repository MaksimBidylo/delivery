package microarch.delivery.core.domain.model;

import libs.errs.GeneralErrors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VolumeTest {

    @ParameterizedTest
    @ValueSource(ints = {1, Integer.MAX_VALUE})
    void createsPositiveVolume(int value) {
        var result = Volume.create(value);

        assertTrue(result.isSuccess());
        assertEquals(value, result.getValue().getValue());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsNonPositiveVolume(int value) {
        assertTrue(Volume.create(value).isFailure());
    }

    @Test
    void increasesByOneWithoutChangingOriginalVolume() {
        var volume = Volume.create(5).getValueOrThrow();

        var increased = volume.increase(1).getValueOrThrow();

        assertEquals(6, increased.getValue());
        assertEquals(5, volume.getValue());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsNonPositiveIncrease(int value) {
        var volume = Volume.create(5).getValueOrThrow();

        var result = volume.increase(value);

        assertTrue(result.isFailure());
        assertEquals(GeneralErrors.valueMustBeGreaterThan("value", value, 0), result.getError());
    }

    @ParameterizedTest
    @CsvSource({"5, 1, 4", "5, 4, 1", "2147483647, 2147483646, 1"})
    void decreasesWithoutChangingOriginalVolume(int initialValue, int decrease, int expectedValue) {
        var volume = Volume.create(initialValue).getValueOrThrow();

        var decreased = volume.decrease(decrease).getValueOrThrow();

        assertEquals(expectedValue, decreased.getValue());
        assertEquals(initialValue, volume.getValue());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsNonPositiveDecrease(int value) {
        var volume = Volume.create(5).getValueOrThrow();

        var result = volume.decrease(value);

        assertTrue(result.isFailure());
        assertEquals(GeneralErrors.valueMustBeGreaterThan("volume", value, 0), result.getError());
        assertEquals(5, volume.getValue());
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 6, Integer.MAX_VALUE})
    void rejectsDecreaseToZeroOrBelow(int value) {
        var volume = Volume.create(5).getValueOrThrow();

        var result = volume.decrease(value);

        assertTrue(result.isFailure());
        assertEquals(GeneralErrors.valueMustBeLessThan("value", value, 5), result.getError());
        assertEquals(5, volume.getValue());
    }

    @Test
    void comparesVolumesByValue() {
        var volume = Volume.create(1).getValueOrThrow();
        var sameVolume = Volume.create(1).getValueOrThrow();
        var largerVolume = Volume.create(Integer.MAX_VALUE).getValueOrThrow();

        assertEquals(volume, sameVolume);
        assertEquals(volume.hashCode(), sameVolume.hashCode());
        assertNotEquals(volume, largerVolume);
    }
}
