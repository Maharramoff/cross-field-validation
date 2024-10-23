package io.github.maharramoff.crossfieldvalidation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

/**
 * A {@code ConstraintValidator} implementation that enables cross-field validation for objects.
 * This implementation automatically discovers and caches validators for fields annotated with
 * cross-field constraint annotations.
 *
 * <h2>Key Components:</h2>
 * <ul>
 *   <li>{@code fieldMapping}: A mapping from each class to its list of fields for efficient field access.</li>
 *   <li>{@code validatorCache}: A cache of validator instances for each constraint annotation type.</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * &#64;EnableCrossFieldConstraints
 * public class UserProfileDTO {
 *     private String password;
 *
 *     &#64;MatchWith(field = "password")
 *     private String confirmPassword;
 * }
 * </pre>
 *
 * <h2>Creating Custom Constraints:</h2>
 * <ol>
 *   <li>Create a validator class that extends {@link CrossFieldConstraintValidator}</li>
 *   <li>Create a constraint annotation marked with {@link CrossFieldConstraint}</li>
 *   <li>Apply the constraint annotation to fields requiring validation</li>
 * </ol>
 *
 * @author Shamkhal Maharramov
 * @see CrossFieldConstraintValidator
 * @see CrossFieldConstraint
 * @see CrossFieldConstraintViolation
 * @since 1.0.0
 */
public class CrossFieldValidationProcessor implements ConstraintValidator<EnableCrossFieldConstraints, Object>
{
    private final Map<Class<?>, List<Field>> fieldMapping = new HashMap<>();
    private final Map<Class<? extends Annotation>, CrossFieldConstraintValidator> validatorCache = new HashMap<>();

    /**
     * Default constructor for the `CrossFieldValidationProcessor`.
     */
    public CrossFieldValidationProcessor()
    {
        // This constructor is intentionally left blank.
    }

    /**
     * Validates the given object by discovering and applying all relevant cross-field validators.
     * The method scans all fields for annotations marked with {@link CrossFieldConstraint},
     * creates or retrieves the corresponding validator instances, and applies them.
     *
     * @param obj     The object to validate
     * @param context The constraint validator context
     * @return {@code true} if all validations pass, {@code false} otherwise
     */
    @Override
    public boolean isValid(final Object obj, final ConstraintValidatorContext context)
    {
        List<CrossFieldConstraintViolation> violations = new ArrayList<>();
        boolean                             isValid    = processFields(obj, violations);

        if (!isValid)
        {
            addViolationsToContext(context, violations);
        }

        return isValid;
    }

    /**
     * Processes all fields of the given object to perform cross-field validation.
     *
     * @param obj        The object to validate
     * @param violations A list to store any constraint violations encountered during validation
     * @return {@code true} if all validations pass, {@code false} otherwise
     */
    private boolean processFields(Object obj, List<CrossFieldConstraintViolation> violations)
    {
        Class<?> clazz = obj.getClass();
        fieldMapping.computeIfAbsent(clazz, k -> Arrays.asList(k.getDeclaredFields()));

        boolean     isValid = true;
        List<Field> fields  = fieldMapping.get(clazz);
        for (Field field : fields)
        {
            for (Annotation annotation : field.getAnnotations())
            {
                CrossFieldConstraintValidator validator = getValidatorForAnnotation(annotation.annotationType());
                try
                {
                    isValid &= validator.isValid(obj, fieldMapping, violations);
                }
                catch (Exception e)
                {
                    isValid = false;
                }
            }
        }
        return isValid;
    }

    /**
     * Adds constraint violations to the provided ConstraintValidatorContext.
     * This method disables the default constraint violation and adds custom violations
     * based on the provided list of CrossFieldConstraintViolation objects.
     *
     * @param context    The ConstraintValidatorContext to which violations will be added
     * @param violations A list of CrossFieldConstraintViolation objects representing the violations to be added
     */
    private void addViolationsToContext(ConstraintValidatorContext context, List<CrossFieldConstraintViolation> violations)
    {
        context.disableDefaultConstraintViolation();
        for (CrossFieldConstraintViolation violation : violations)
        {
            context.buildConstraintViolationWithTemplate(violation.getMessage()).addPropertyNode(violation.getFieldName()).addConstraintViolation();
        }
    }

    /**
     * Retrieves a cached validator instance for the given annotation type,
     * creating a new instance if none exists.
     *
     * @param annotationType The type of the constraint annotation
     * @return The validator instance, or null if the annotation is not a cross-field constraint
     * @since 1.1.0
     */
    CrossFieldConstraintValidator getValidatorForAnnotation(Class<? extends Annotation> annotationType)
    {
        return validatorCache.computeIfAbsent(annotationType, this::registerValidator);
    }

    /**
     * Creates a new validator instance for the given annotation type.
     *
     * @param annotationType The type of the constraint annotation
     * @return A new validator instance, or null if the annotation is not marked with {@link CrossFieldConstraint}
     * @throws IllegalStateException if validator instantiation fails
     * @since 1.1.0
     */
    private CrossFieldConstraintValidator registerValidator(Class<? extends Annotation> annotationType)
    {
        CrossFieldConstraint crossFieldConstraint = annotationType.getAnnotation(CrossFieldConstraint.class);
        if (crossFieldConstraint == null)
        {
            return null;
        }

        try
        {
            return crossFieldConstraint.validatedBy().getDeclaredConstructor().newInstance();
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Failed to instantiate validator for " + annotationType, e);
        }
    }

}


