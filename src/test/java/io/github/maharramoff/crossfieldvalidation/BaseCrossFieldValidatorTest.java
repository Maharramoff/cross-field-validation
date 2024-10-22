package io.github.maharramoff.crossfieldvalidation;

import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BaseCrossFieldValidatorTest
{

    private BaseCrossFieldValidator validator;

    @BeforeEach
    void setUp()
    {
        validator = new BaseCrossFieldValidator()
        {
            @Override
            public boolean isValid(Object obj, Map<Class<?>, List<Field>> fields, List<CrossFieldConstraintViolation> violations)
            {
                return false;
            }
        };
    }

    @Test
    void shouldReturnCorrectValue()
    {
        TestObject testObject = new TestObject("testValue");
        Object     value      = validator.getProperty(testObject, "property");
        assertEquals("testValue", value);
    }

    @Test
    void shouldReturnNullForInvalidProperty()
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
