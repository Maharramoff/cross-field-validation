package io.github.maharramoff.validator;

import io.github.maharramoff.annotation.EnableCrossFieldConstraints;
import io.github.maharramoff.model.ConstraintViolation;
import lombok.Getter;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.*;

/**
 * A {@code ConstraintValidator} implementation that enables cross-field validation for objects.
 * <h2>Key Components:</h2>
 * <ul>
 *   <li>{@code fieldValidators}: A list of all cross-field validators to be run on the object.</li>
 *   <li>{@code fieldMapping}: A mapping from each class to its list of fields.</li>
 * </ul>
 *
 * @author Shamkhal Maharramov
 * @see CrossFieldConstraintValidator
 * @see ConstraintViolation
 * @since 1.0.0
 */
public class CrossFieldConstraintsEnabler implements ConstraintValidator<EnableCrossFieldConstraints, Object>
{

    @Getter
    private final List<CrossFieldConstraintValidator> fieldValidators = new ArrayList<>();
    private final Map<Class<?>, List<Field>> fieldMapping = new HashMap<>();

    /**
     * Default constructor for the `CrossFieldConstraintsEnabler`.
     */
    public CrossFieldConstraintsEnabler()
    {
        // This constructor is intentionally left blank.
    }

    /**
     * Validates the given object using the registered cross-field validators.
     *
     * @param obj     The object to validate.
     * @param context The constraint validator context.
     * @return {@code true} if the object is valid, {@code false} otherwise.
     */
    @Override
    public boolean isValid(final Object obj, final ConstraintValidatorContext context)
    {
        boolean                   isValid    = true;
        List<ConstraintViolation> violations = new ArrayList<>();

        Class<?> clazz = obj.getClass();
        fieldMapping.computeIfAbsent(clazz, k -> Arrays.asList(k.getDeclaredFields()));

        for (CrossFieldConstraintValidator fieldValidator : fieldValidators)
        {
            try
            {
                isValid &= fieldValidator.isValid(obj, fieldMapping, violations);


            }
            catch (Exception e)
            {
                isValid = false;
            }
        }

        if (!isValid)
        {
            context.disableDefaultConstraintViolation();
            for (ConstraintViolation violation : violations)
            {
                context.buildConstraintViolationWithTemplate(violation.getMessage())
                        .addPropertyNode(violation.getFieldName())
                        .addConstraintViolation();
            }
        }

        return isValid;
    }

}


