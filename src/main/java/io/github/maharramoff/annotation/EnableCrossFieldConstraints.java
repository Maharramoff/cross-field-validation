package io.github.maharramoff.annotation;

import io.github.maharramoff.validator.CrossFieldConstraintsEnabler;
import jakarta.validation.Constraint;

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
 * @see CrossFieldConstraintsEnabler
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CrossFieldConstraintsEnabler.class)
public @interface EnableCrossFieldConstraints
{
}

