package io.github.maharramoff.crossfieldvalidation;

import jakarta.validation.ConstraintValidatorContext;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class CrossFieldConstraintsEnablerTest
{

    private CrossFieldConstraintsEnabler enabler;
    private ConstraintValidatorContext mockContext;
    private CrossFieldConstraintValidator mockValidator;
    private Object mockObject;

    @BeforeEach
    void setUp()
    {
        enabler = new CrossFieldConstraintsEnabler();
        mockContext = mock(ConstraintValidatorContext.class);
        mockValidator = mock(CrossFieldConstraintValidator.class);
        mockObject = new Object();
    }

    @Test
    void testIsValid_WhenNoViolations_ReturnsTrue() throws Exception
    {
        when(mockValidator.isValid(any(), any(), any())).thenReturn(true);
        enabler.getFieldValidators().add(mockValidator);
        boolean result = enabler.isValid(mockObject, mockContext);
        assertTrue(result);
    }

    @Test
    void testIsValid_WithViolations_ReturnsFalse() throws Exception
    {
        when(mockValidator.isValid(any(), any(), any())).thenReturn(false);
        enabler.getFieldValidators().add(mockValidator);
        boolean result = enabler.isValid(mockObject, mockContext);
        assertFalse(result);
    }

    @Test
    void testIsValid_WithException_ReturnsFalse() throws Exception
    {
        when(mockValidator.isValid(any(), any(), any())).thenThrow(new RuntimeException());
        enabler.getFieldValidators().add(mockValidator);
        boolean result = enabler.isValid(mockObject, mockContext);
        assertFalse(result);
    }

    @Test
    void shouldReturnTrue_WhenPasswordAndConfirmPasswordMatch() throws Exception
    {
        TestObject testObject = new TestObject("password123", "password123");
        when(mockValidator.isValid(any(), any(), any())).thenReturn(true);
        enabler.getFieldValidators().add(mockValidator);
        boolean result = enabler.isValid(testObject, mockContext);
        assertTrue(result);
    }

    @Test
    void shouldReturnFalse_WhenPasswordAndConfirmPasswordDoNotMatch() throws Exception
    {
        TestObject testObject = new TestObject("password123", "password456");
        when(mockValidator.isValid(any(), any(), any())).thenReturn(false);
        enabler.getFieldValidators().add(mockValidator);
        boolean result = enabler.isValid(testObject, mockContext);
        assertFalse(result);
    }

    @Test
    void shouldReturnFalse_WhenExceptionIsThrownInValidator() throws Exception
    {
        TestObject testObject = new TestObject("password123", "password123");
        when(mockValidator.isValid(any(), any(), any())).thenThrow(new RuntimeException());
        enabler.getFieldValidators().add(mockValidator);
        boolean result = enabler.isValid(testObject, mockContext);
        assertFalse(result);
    }

    @Getter @EnableCrossFieldConstraints static class TestObject
    {
        private final String password;
        private final String confirmPassword;

        TestObject(String password, String confirmPassword)
        {
            this.password = password;
            this.confirmPassword = confirmPassword;
        }
    }
}
