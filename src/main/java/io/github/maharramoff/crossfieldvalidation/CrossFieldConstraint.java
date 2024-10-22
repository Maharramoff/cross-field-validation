package io.github.maharramoff.crossfieldvalidation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation that associates a cross-field constraint annotation with its validator implementation.
 * This annotation should be applied to custom constraint annotations to specify their validator class.
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * &#64;Target(ElementType.FIELD)
 * &#64;Retention(RetentionPolicy.RUNTIME)
 * &#64;CrossFieldConstraint(validatedBy = MatchWithValidator.class)
 * public @interface MatchWith {
 *     String field();
 *     String message() default "Fields do not match.";
 * }
 * </pre>
 *
 * @author Shamkhal Maharramov
 * @see CrossFieldConstraintValidator
 * @since 1.1.0
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CrossFieldConstraint
{
    /**
     * Interface method that returns the class type of a CrossFieldConstraintValidator.
     *
     * @return the class object representing the type of CrossFieldConstraintValidator
     * that this constraint is validated by.
     */
    Class<? extends CrossFieldConstraintValidator> validatedBy();
}
