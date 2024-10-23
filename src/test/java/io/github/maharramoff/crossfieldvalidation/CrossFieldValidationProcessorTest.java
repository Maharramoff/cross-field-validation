package io.github.maharramoff.crossfieldvalidation;

import jakarta.validation.ConstraintValidatorContext;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings({"FieldCanBeLocal", "unused", "unchecked"})
class CrossFieldValidationProcessorTest
{

    private CrossFieldValidationProcessor validationProcessor;
    private ConstraintValidatorContext mockContext;

    @BeforeEach
    void setUp()
    {
        validationProcessor = new CrossFieldValidationProcessor();
        mockContext = mock(ConstraintValidatorContext.class);

        ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder = mock(
                ConstraintValidatorContext.ConstraintViolationBuilder.class);
        ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder = mock(
                ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);

        when(mockContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation()).thenReturn(mockContext);
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

        validationProcessor.isValid(testObject, mockContext);

        validationProcessor.isValid(testObject, mockContext);

        CrossFieldConstraintValidator cachedValidator = validationProcessor.getValidatorForAnnotation(MatchWith.class);
        assertNotNull(cachedValidator);
    }

    @Test
    void shouldIgnoreFieldsWithoutCrossFieldConstraintAnnotations()
    {
        TestObjectWithoutAnnotations testObject = new TestObjectWithoutAnnotations("password123", "password123");

        boolean result = validationProcessor.isValid(testObject, mockContext);

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenValidatorThrowsException() throws Exception
    {
        TestObject testObject = new TestObject("password123", "password123");

        CrossFieldConstraintValidator mockValidator = mock(CrossFieldConstraintValidator.class);
        when(mockValidator.isValid(any(), any(), any())).thenThrow(new RuntimeException("Validation Error"));

        Map<Class<? extends Annotation>, CrossFieldConstraintValidator> validatorCache = getInternalCache();
        validatorCache.put(MatchWith.class, mockValidator);

        boolean result = validationProcessor.isValid(testObject, mockContext);

        assertFalse(result);
    }

    @Test
    void shouldAddViolationsToConstraintValidatorContext()
    {
        TestObject testObject = new TestObject("password123", "password456");

        boolean result = validationProcessor.isValid(testObject, mockContext);

        assertFalse(result);
        verify(mockContext).disableDefaultConstraintViolation();
        verify(mockContext).buildConstraintViolationWithTemplate("Fields do not match.");
    }

    @Test
    void shouldReturnTrueWhenNoViolationsOccur()
    {
        TestObject testObject = new TestObject("password123", "password123");

        boolean result = validationProcessor.isValid(testObject, mockContext);

        assertTrue(result);
        verify(mockContext, never()).buildConstraintViolationWithTemplate(anyString());
    }


    @Test
    void shouldReturnTrueWhenClassHasNoFields()
    {
        EmptyTestObject emptyTestObject = new EmptyTestObject();

        boolean result = validationProcessor.isValid(emptyTestObject, mockContext);

        assertTrue(result);
        verify(mockContext, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldHandleNullValuesInFields()
    {
        TestObject testObject = new TestObject(null, "password123");
        boolean    result     = validationProcessor.isValid(testObject, mockContext);
        assertFalse(result);
        verify(mockContext).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    void shouldHandleEmptyStringsInFields()
    {
        TestObject testObject = new TestObject("", "");
        boolean    result     = validationProcessor.isValid(testObject, mockContext);
        assertTrue(result);
    }


    @Test
    void shouldReturnNullWhenAnnotationIsNotCrossFieldConstraint()
    {
        CrossFieldConstraintValidator validator = validationProcessor.getValidatorForAnnotation(NonCrossFieldConstraint.class);
        assertNull(validator);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenValidatorInstantiationFails()
    {
        Exception exception = assertThrows(IllegalStateException.class, () ->
                validationProcessor.getValidatorForAnnotation(InvalidValidatorAnnotation.class));

        assertTrue(exception.getMessage().contains("Failed to instantiate validator for"));
    }

    @Retention(RetentionPolicy.RUNTIME) @interface NonCrossFieldConstraint
    {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @CrossFieldConstraint(validatedBy = NonInstantiableValidator.class) @interface InvalidValidatorAnnotation
    {
    }

    static class NonInstantiableValidator implements CrossFieldConstraintValidator
    {
        public NonInstantiableValidator(String arg)
        {
        }

        @Override
        public boolean isValid(Object obj, Map<Class<?>, List<Field>> fields, List<CrossFieldConstraintViolation> violations)
        {
            return false;
        }
    }


    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @CrossFieldConstraint(validatedBy = MinLengthValidator.class) @interface MinLength
    {
        int value();

        String message() default "Field must be at least {value} characters long";
    }

    static class MinLengthValidator extends BaseCrossFieldValidator
    {
        @Override
        public boolean isValid(Object obj, Map<Class<?>, List<Field>> fieldMapping,
                               List<CrossFieldConstraintViolation> violations)
        {
            AtomicBoolean isValid = new AtomicBoolean(true);

            processFields(obj, fieldMapping, MinLength.class, (field, annotation) ->
            {
                String value = (String) getProperty(obj, field.getName());
                if (value == null || value.length() < annotation.value())
                {
                    violations.add(new CrossFieldConstraintViolation(
                            field.getName(),
                            annotation.message().replace("{value}", String.valueOf(annotation.value()))));
                    isValid.set(false);
                }
            });

            return isValid.get();
        }
    }

    static class EmptyTestObject
    {
    }

    private Map<Class<? extends Annotation>, CrossFieldConstraintValidator> getInternalCache() throws Exception
    {
        Field cacheField = CrossFieldValidationProcessor.class.getDeclaredField("validatorCache");
        cacheField.setAccessible(true);
        return (Map<Class<? extends Annotation>, CrossFieldConstraintValidator>) cacheField.get(validationProcessor);
    }

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
        public boolean isValid(Object obj, Map<Class<?>, List<Field>> fieldMapping,
                               List<CrossFieldConstraintViolation> violations)
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
