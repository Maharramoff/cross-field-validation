package io.github.maharramoff.crossfieldvalidation;

import jakarta.validation.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.junit.jupiter.api.*;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings({"FieldCanBeLocal", "unused", "unchecked"})
class CrossFieldValidationProcessorTest
{

    private CrossFieldValidationProcessor validationProcessor;
    private ConstraintValidatorContext mockContext;
    private Validator standardValidator;
    private ValidatorFactory validatorFactory;

    @BeforeEach
    void setUp()
    {
        validationProcessor = new CrossFieldValidationProcessor();
        mockContext = createMockContext();
        validatorFactory = Validation.buildDefaultValidatorFactory();
        standardValidator = validatorFactory.getValidator();
    }

    private ConstraintValidatorContext createMockContext()
    {
        ConstraintValidatorContext                                                           context          = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder                                violationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder      = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        when(nodeBuilder.addConstraintViolation()).thenReturn(context);

        return context;
    }

    @Nested
    @DisplayName("Validation Success Cases")
    class ValidationSuccessTests
    {

        @Test
        void shouldReturnTrueWhenConstraintsSatisfied()
        {
            TestObject testObject = new TestObject("password123", "password123");
            boolean    result     = validationProcessor.isValid(testObject, mockContext);
            assertTrue(result);
            verifyNoInteractions(mockContext);
        }

        @Test
        void shouldHandleEmptyObjectGracefully()
        {
            EmptyTestObject emptyTestObject = new EmptyTestObject();
            boolean         result          = validationProcessor.isValid(emptyTestObject, mockContext);
            assertTrue(result);
            verifyNoInteractions(mockContext);
        }

        @Test
        void shouldIgnoreNonAnnotatedFields()
        {
            TestObjectWithoutAnnotations testObject = new TestObjectWithoutAnnotations("value1", "value2");
            boolean                      result     = validationProcessor.isValid(testObject, mockContext);
            assertTrue(result);
            verifyNoInteractions(mockContext);
        }
    }

    @Nested
    @DisplayName("Validator Caching and Instantiation")
    class ValidatorCachingTests
    {

        @Test
        void shouldCacheValidatorInstances()
        {
            TestObject testObject = new TestObject("password123", "password123");
            validationProcessor.isValid(testObject, mockContext);
            validationProcessor.isValid(testObject, mockContext); // Second call to trigger caching

            CrossFieldConstraintValidator cachedValidator = validationProcessor.getValidatorForAnnotation(MatchWith.class);
            assertNotNull(cachedValidator);
        }

        @Test
        void shouldReturnNullForNonCrossFieldConstraint()
        {
            CrossFieldConstraintValidator validator = validationProcessor.getValidatorForAnnotation(NonCrossFieldConstraint.class);
            assertNull(validator);
        }

        @Test
        void shouldThrowExceptionWhenValidatorInstantiationFails()
        {
            IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                    validationProcessor.getValidatorForAnnotation(InvalidValidatorAnnotation.class));

            assertTrue(exception.getMessage().contains("Failed to instantiate validator for"));
        }
    }

    @Nested
    @DisplayName("Error Handling and Exceptions")
    class ErrorHandlingTests
    {

        @Test
        void shouldReturnFalseWhenValidatorThrowsException() throws Exception
        {
            TestObject testObject = new TestObject("password123", "password123");

            CrossFieldConstraintValidator mockValidator = mock(CrossFieldConstraintValidator.class);
            when(mockValidator.isValid(any(), any(), any())).thenThrow(new RuntimeException("Validation Error"));

            validationProcessor.getValidatorCache().put(MatchWith.class, mockValidator);

            boolean result = validationProcessor.isValid(testObject, mockContext);
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Special Conditions")
    class EdgeCaseTests
    {

        @Test
        @DisplayName("Should fail validation when one field is null and the other is not")
        void shouldFailWhenOneFieldIsNull()
        {
            TestObject testObject = new TestObject(null, "value");
            boolean    result     = validationProcessor.isValid(testObject, mockContext);
            assertFalse(result);
            verify(mockContext).disableDefaultConstraintViolation();
            verify(mockContext).buildConstraintViolationWithTemplate("Fields do not match.");
        }

        @Test
        @DisplayName("Should handle objects with inherited fields")
        void shouldHandleInheritedFields()
        {
            SubTestObject testObject = new SubTestObject("password123", "password123", "extraField");
            boolean       result     = validationProcessor.isValid(testObject, mockContext);
            assertTrue(result);
            verifyNoInteractions(mockContext);
        }

        @Test
        @DisplayName("Should validate objects with multiple cross-field constraints")
        void shouldValidateMultipleCrossFieldConstraints()
        {
            MultiConstraintTestObject2 testObject = new MultiConstraintTestObject2("value", "value", 5, 5);
            boolean                    result     = validationProcessor.isValid(testObject, mockContext);
            assertTrue(result);
            verifyNoInteractions(mockContext);
        }
    }

    @Nested
    @DisplayName("Multiple CrossFieldConstraints Tests")
    class MultipleConstraintsTests
    {

        @Test
        void shouldCollectAllViolationsWhenMultipleConstraintsViolated()
        {
            MultiConstraintTestObject testObject = new MultiConstraintTestObject(
                    10, 100, 5, "secret123", "different");
            boolean result = validationProcessor.isValid(testObject, mockContext);
            assertFalse(result);
            verify(mockContext).disableDefaultConstraintViolation();
            verify(mockContext, times(2)).buildConstraintViolationWithTemplate(anyString());
        }
    }

    @Nested
    @DisplayName("Combined Constraints Tests")
    class CombinedConstraintsTests
    {

        private ValidatorFactory validatorFactory;
        private Validator standardValidator;

        @BeforeEach
        void setUpValidators()
        {
            validatorFactory = Validation.buildDefaultValidatorFactory();
            standardValidator = validatorFactory.getValidator();
        }

        @AfterEach
        void tearDownValidators()
        {
            validatorFactory.close();
        }
        
        @Test
        void shouldFailWhenStandardConstraintsViolated()
        {
            CombinedConstraintsTestObject testObject = new CombinedConstraintsTestObject(
                    "usr", "pass", "pass", "invalid-email");
            Set<ConstraintViolation<CombinedConstraintsTestObject>> violations = standardValidator.validate(testObject);
            assertFalse(violations.isEmpty());
        }

        @Test
        void shouldCollectViolationsFromBothStandardAndCrossFieldConstraints()
        {
            CombinedConstraintsTestObject testObject = new CombinedConstraintsTestObject(
                    "usr", "pass", "different", "invalid-email");
            Set<ConstraintViolation<CombinedConstraintsTestObject>> violations = standardValidator.validate(testObject);
            assertFalse(violations.isEmpty());

            boolean crossFieldResult = validationProcessor.isValid(testObject, mockContext);
            assertFalse(crossFieldResult);
            verify(mockContext).disableDefaultConstraintViolation();
            verify(mockContext).buildConstraintViolationWithTemplate("Fields do not match.");

            assertFalse(violations.isEmpty());
        }
    }


    // Test helper methods and classes
    private Map<Class<? extends Annotation>, CrossFieldConstraintValidator> getValidatorCache()
    {
        return validationProcessor.getValidatorCache();
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

    static class EmptyTestObject
    {
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
        public boolean isValid(Object obj, Map<Class<?>, List<Field>> fieldMapping, List<CrossFieldConstraintViolation> violations)
        {
            processFields(obj, fieldMapping, MatchWith.class, (field, annotation) ->
            {
                Object fieldValue      = getProperty(obj, field.getName());
                Object otherFieldValue = getProperty(obj, annotation.field());

                if (fieldValue == null || !fieldValue.equals(otherFieldValue))
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

    @Getter
    @EnableCrossFieldConstraints
    static class MultiConstraintTestObject
    {
        private final Integer minValue;
        private final Integer maxValue;

        @GreaterThan(field = "minValue")
        private final Integer currentValue;

        @MatchWith(field = "password")
        private final String confirmPassword;

        private final String password;

        MultiConstraintTestObject(Integer minValue, Integer maxValue, Integer currentValue, String password, String confirmPassword)
        {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.currentValue = currentValue;
            this.password = password;
            this.confirmPassword = confirmPassword;
        }
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @CrossFieldConstraint(validatedBy = GreaterThanValidator.class) @interface GreaterThan
    {
        String field();

        String message() default "Field must be greater than {field}.";
    }

    static class GreaterThanValidator extends BaseCrossFieldValidator
    {
        @Override
        public boolean isValid(Object obj, Map<Class<?>, List<Field>> fieldMapping, List<CrossFieldConstraintViolation> violations)
        {
            processFields(obj, fieldMapping, GreaterThan.class, (field, annotation) ->
            {
                Comparable<Object> fieldValue      = (Comparable<Object>) getProperty(obj, field.getName());
                Object             otherFieldValue = getProperty(obj, annotation.field());
                if (fieldValue == null || otherFieldValue == null || fieldValue.compareTo(otherFieldValue) <= 0)
                {
                    String message = annotation.message().replace("{field}", annotation.field());
                    violations.add(new CrossFieldConstraintViolation(field.getName(), message));
                }
            });
            return violations.isEmpty();
        }
    }

    @Getter
    @EnableCrossFieldConstraints
    static class CombinedConstraintsTestObject
    {
        @NotNull
        @Size(min = 5)
        private final String username;

        @NotNull
        @Size(min = 8)
        private final String password;

        @MatchWith(field = "password")
        private final String confirmPassword;

        @NotNull
        @Email
        private final String email;

        CombinedConstraintsTestObject(String username, String password, String confirmPassword, String email)
        {
            this.username = username;
            this.password = password;
            this.confirmPassword = confirmPassword;
            this.email = email;
        }
    }

    @Getter
    @EnableCrossFieldConstraints
    static class SubTestObject extends TestObject
    {
        private final String extraField;

        SubTestObject(String value, String other, String extraField)
        {
            super(value, other);
            this.extraField = extraField;
        }
    }

    @Getter
    @EnableCrossFieldConstraints
    static class MultiConstraintTestObject2
    {
        @MatchWith(field = "field2")
        private final String field1;

        private final String field2;

        @MatchWith(field = "number2")
        private final Integer number1;

        private final Integer number2;

        MultiConstraintTestObject2(String field1, String field2, Integer number1, Integer number2)
        {
            this.field1 = field1;
            this.field2 = field2;
            this.number1 = number1;
            this.number2 = number2;
        }
    }

    @Getter
    @EnableCrossFieldConstraints
    static class NumericTestObject
    {
        @MatchWith(field = "number2")
        private final Integer number1;

        private final Integer number2;

        NumericTestObject(Integer number1, Integer number2)
        {
            this.number1 = number1;
            this.number2 = number2;
        }
    }

    @Getter
    @EnableCrossFieldConstraints
    static class CollectionTestObject
    {
        @MatchWith(field = "list2")
        private final List<String> list1;

        private final List<String> list2;

        CollectionTestObject(List<String> list1, List<String> list2)
        {
            this.list1 = list1;
            this.list2 = list2;
        }
    }

    @Getter
    @EnableCrossFieldConstraints
    static class DefaultValuesTestObject
    {
        @MatchWith(field = "defaultField")
        private final String defaultField = "defaultValue";

        private final String anotherField = "defaultValue";
    }
}