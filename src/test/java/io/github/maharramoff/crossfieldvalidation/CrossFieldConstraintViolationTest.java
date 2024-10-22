package io.github.maharramoff.crossfieldvalidation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrossFieldConstraintViolationTest
{

    @Test
    void shouldCreateConstraintViolation()
    {
        CrossFieldConstraintViolation violation = new CrossFieldConstraintViolation("fieldName", "Error message");
        assertEquals("fieldName", violation.getFieldName());
        assertEquals("Error message", violation.getMessage());
    }
}