package microarch.delivery.core.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
    void comparesVolumesByValue() {
        var volume = Volume.create(1).getValueOrThrow();
        var sameVolume = Volume.create(1).getValueOrThrow();
        var largerVolume = Volume.create(Integer.MAX_VALUE).getValueOrThrow();

        assertEquals(volume, sameVolume);
        assertEquals(volume.hashCode(), sameVolume.hashCode());
        assertNotEquals(volume, largerVolume);
    }
}
