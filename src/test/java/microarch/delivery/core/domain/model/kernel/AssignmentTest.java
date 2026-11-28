package microarch.delivery.core.domain.model.kernel;

import libs.errs.GeneralErrors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentTest {

    @Test
    void initializesUniqueEntityIds() {
        var volume = Volume.create(1).getValueOrThrow();
        var location = Location.mustCreate(5, 5);
        var first = Assignment.create(UUID.randomUUID(), volume, location).getValueOrThrow();
        var second = Assignment.create(UUID.randomUUID(), volume, location).getValueOrThrow();

        assertNotNull(first.getId());
        assertNotNull(second.getId());
        assertNotEquals(first.getId(), second.getId());
        assertNotEquals(first, second);
    }

    @Test
    void rejectsMissingCourierLocation() {
        var assignment = Assignment.create(UUID.randomUUID(), Volume.create(1).getValueOrThrow(),
                Location.mustCreate(5, 5)).getValueOrThrow();

        var result = assignment.completeAssignment(null);

        assertTrue(result.isFailure());
        assertEquals(GeneralErrors.valueIsRequired("couriersLocation"), result.getError());
    }

    @ParameterizedTest
    @CsvSource({
            "5, 5, true",
            "4, 5, true",
            "6, 5, true",
            "5, 4, true",
            "5, 6, true",
            "5, 7, false",
            "6, 6, false"
    })
    void completesOnlyWithinOneCell(int courierX, int courierY, boolean expectedSuccess) {
        var assignment = Assignment.create(UUID.randomUUID(), Volume.create(1).getValueOrThrow(),
                Location.mustCreate(5, 5)).getValueOrThrow();

        var result = assignment.completeAssignment(Location.mustCreate(courierX, courierY));

        assertEquals(expectedSuccess, result.isSuccess());
        if (!expectedSuccess) {
            assertEquals("value.must.be.less.or.equal", result.getError().getCode());
        }
        if (expectedSuccess) {
            assertEquals(AssignmentStatus.COMPLETED, assignment.getStatus());
        }
    }
}
