package io.github.maharramoff.validator;

import io.github.maharramoff.model.ConstraintViolation;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AbstractCrossFieldConstraintValidatorTest
{

    private AbstractCrossFieldConstraintValidator validator;

    @BeforeEach
    void setUp()
    {
        validator = new AbstractCrossFieldConstraintValidator()
        {
            @Override
            public boolean isValid(Object obj, Map<Class<?>, List<Field>> fields, List<ConstraintViolation> violations)
            {
                return false;
            }
        };
    }

    @Test
    void testGetProperty_ReturnsCorrectValue()
    {
        TestObject testObject = new TestObject("testValue");
        Object     value      = validator.getProperty(testObject, "property");
        assertEquals("testValue", value);
    }

    @Test
    void testGetProperty_ReturnsNullForInvalidProperty()
    {
        TestObject testObject = new TestObject("testValue");
        Object     value      = validator.getProperty(testObject, "nonExistentProperty");
        assertNull(value);
    }

    @Getter
    private static class TestObject
    {
        private final String property;

        public TestObject(String property)
        {
            this.property = property;
        }
    }
}
