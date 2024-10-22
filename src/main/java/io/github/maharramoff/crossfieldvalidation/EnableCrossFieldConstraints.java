package io.github.maharramoff.crossfieldvalidation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables cross-field constraints validation on the annotated class type.
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * &#64;EnableCrossFieldConstraints
 * public class UserProfileDTO {
 *     private String username;
 *     private String password;
 *
 *     &#64;MatchWith("password")
 *     private String confirmPassword;
 * }
 * </pre>
 *
 * @author Shamkhal Maharramov
 * @see CrossFieldValidationProcessor
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CrossFieldValidationProcessor.class)
public @interface EnableCrossFieldConstraints
{
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

