package az.maharramoff;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.util.*;

/**
 * A {@code ConstraintValidator} implementation that enables cross-field validation for objects.
 * <h3>Key Components:</h3>
 * <ul>
 *   <li>{@code fieldValidators}: A list of all cross-field validators to be run on the object.</li>
 *   <li>{@code fieldMapping}: A mapping from each class to its list of fields.</li>
 * </ul>
 *
 * @author Shamkhal Maharramov
 * @see CrossFieldConstraintValidator
 * @see ConstraintViolation
 */
public class CrossFieldConstraintsEnabler implements ConstraintValidator<EnableCrossFieldConstraints, Object>
{

    private final List<CrossFieldConstraintValidator> fieldValidators = new ArrayList<>();
    private final Map<Class<?>, List<Field>> fieldMapping = new HashMap<>();

    public CrossFieldConstraintsEnabler()
    {
    }

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

        // Handle constraint violations
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


