package io.github.maharramoff.crossfieldvalidation;

import jakarta.validation.ConstraintValidatorContext;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrossFieldValidationProcessorTest
{

    private CrossFieldValidationProcessor validationProcessor;
    private ConstraintValidatorContext mockContext;

    @BeforeEach
    void setUp()
    {
        validationProcessor = new CrossFieldValidationProcessor();
        mockContext = mock(ConstraintValidatorContext.class);

        ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(mockContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString())).thenReturn(mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class));
    }

    @Test
    void shouldReturnTrueWhenPasswordAndConfirmPasswordMatch()
    {
        TestObject testObject = new TestObject("password123", "password123");
        boolean    result     = validationProcessor.isValid(testObject, mockContext);
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenPasswordAndConfirmPasswordDoNotMatch()
    {
        TestObject testObject = new TestObject("password123", "password456");
        boolean    result     = validationProcessor.isValid(testObject, mockContext);
        assertFalse(result);
    }

    @Test
    void shouldCacheValidatorInstancesForAnnotations()
    {
        TestObject testObject = new TestObject("password123", "password123");

        // First validation should instantiate and cache the validators
        validationProcessor.isValid(testObject, mockContext);

        // Manually clear the field mapping cache to simulate a new class
        validationProcessor.isValid(testObject, mockContext);

        // Verify that the validator is reused from the cache on subsequent calls
        CrossFieldConstraintValidator cachedValidator = validationProcessor.getValidatorForAnnotation(MatchWith.class);
        assertNotNull(cachedValidator);
    }

    @Test
    void shouldIgnoreFieldsWithoutCrossFieldConstraintAnnotations()
    {
        TestObjectWithoutAnnotations testObject = new TestObjectWithoutAnnotations("password123", "password123");

        boolean result = validationProcessor.isValid(testObject, mockContext);

        // As there are no annotated fields, the result should still be true
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenValidatorThrowsException() throws Exception
    {
        TestObject testObject = new TestObject("password123", "password123");

        // Mock the validator to throw an exception
        CrossFieldConstraintValidator mockValidator = mock(CrossFieldConstraintValidator.class);
        when(mockValidator.isValid(any(), any(), any())).thenThrow(new RuntimeException("Validation Error"));

        // Use reflection to insert mock validator into the cache
        Map<Class<? extends Annotation>, CrossFieldConstraintValidator> validatorCache = getInternalCache();
        validatorCache.put(MatchWith.class, mockValidator);

        boolean result = validationProcessor.isValid(testObject, mockContext);

        assertFalse(result);  // Ensure result is false since the validator failed
    }

    @Test
    void shouldAddViolationsToConstraintValidatorContext()
    {
        TestObject testObject = new TestObject("password123", "password456");  // These don't match, should cause a violation

        boolean result = validationProcessor.isValid(testObject, mockContext);

        // The result should be false because of mismatching fields
        assertFalse(result);

        // Verify that the violation is added to the context
        verify(mockContext).disableDefaultConstraintViolation();
        verify(mockContext).buildConstraintViolationWithTemplate("Fields do not match.");
    }

    @Test
    void shouldReturnTrueWhenNoViolationsOccur()
    {
        TestObject testObject = new TestObject("password123", "password123");

        boolean result = validationProcessor.isValid(testObject, mockContext);

        // As there are no violations, the result should be true
        assertTrue(result);

        // Verify no violations were added
        verify(mockContext, never()).buildConstraintViolationWithTemplate(anyString());
    }


    @Test
    void shouldReturnTrueWhenClassHasNoFields()
    {
        EmptyTestObject emptyTestObject = new EmptyTestObject();

        boolean result = validationProcessor.isValid(emptyTestObject, mockContext);

        // Since there are no fields, it should return true
        assertTrue(result);

        // Ensure that no violations were added to the context
        verify(mockContext, never()).buildConstraintViolationWithTemplate(anyString());
    }

    // Helper class without any fields
    static class EmptyTestObject
    {
    }

    // Reflection helper to access internal cache
    private Map<Class<? extends Annotation>, CrossFieldConstraintValidator> getInternalCache() throws Exception
    {
        Field cacheField = CrossFieldValidationProcessor.class.getDeclaredField("validatorCache");
        cacheField.setAccessible(true);
        return (Map<Class<? extends Annotation>, CrossFieldConstraintValidator>) cacheField.get(validationProcessor);
    }


    // Helper class for testing fields without annotations
    static class TestObjectWithoutAnnotations
    {
        private final String value;
        private final String other;

        TestObjectWithoutAnnotations(String value, String other)
        {
            this.value = value;
            this.other = other;
        }
    }


    // Test classes
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @CrossFieldConstraint(validatedBy = MatchWithValidator.class) @interface MatchWith
    {
        String field();

        String message() default "Fields do not match.";
    }

    static class MatchWithValidator extends BaseCrossFieldValidator
    {
        @Override
        public boolean isValid(Object obj, Map<Class<?>, List<Field>> fieldMapping, List<CrossFieldConstraintViolation> violations)
        {
            processFields(obj, fieldMapping, MatchWith.class, (field, annotation) ->
            {
                Object fieldValue      = getProperty(obj, field.getName());
                Object otherFieldValue = getProperty(obj, annotation.field());

                if (!fieldValue.equals(otherFieldValue))
                {
                    violations.add(new CrossFieldConstraintViolation(field.getName(), annotation.message()));
                }
            });
            return violations.isEmpty();
        }
    }

    @Getter
    @EnableCrossFieldConstraints
    static class TestObject
    {
        private final String value;

        @MatchWith(field = "value")
        private final String other;

        TestObject(String value, String other)
        {
            this.value = value;
            this.other = other;
        }
    }
}
