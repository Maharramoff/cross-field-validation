package az.maharramoff.annotation;

import az.maharramoff.validator.CrossFieldConstraintsEnabler;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * Enables cross-field constraints validation on the annotated class type.
 * <p>
 *
 * <h3>Usage Example:</h3>
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
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CrossFieldConstraintsEnabler.class)
@Documented
public @interface EnableCrossFieldConstraints
{
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

