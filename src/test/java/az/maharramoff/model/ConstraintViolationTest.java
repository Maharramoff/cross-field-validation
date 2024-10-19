package az.maharramoff.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstraintViolationTest
{

    @Test
    void shouldCreateConstraintViolation()
    {
        ConstraintViolation violation = new ConstraintViolation("fieldName", "Error message");
        assertEquals("fieldName", violation.getFieldName());
        assertEquals("Error message", violation.getMessage());
    }
}