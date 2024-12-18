package io.github.maharramoff.crossfieldvalidation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private static final Logger logger = LoggerFactory.getLogger(CrossFieldValidationProcessor.class);
    private final Map<Class<?>, List<Field>> fieldMapping = new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<Field, Annotation[]>> fieldAnnotationsCache = new ConcurrentHashMap<>();
    private final Map<Class<? extends Annotation>, CrossFieldConstraintValidator> validatorCache = new ConcurrentHashMap<>();

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
        logger.debug("Starting validation for object: {}", obj.getClass().getName());

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
        Class<?>                 clazz            = obj.getClass();
        List<Field>              fields           = fieldMapping.computeIfAbsent(clazz, this::getAllDeclaredFields);
        Map<Field, Annotation[]> fieldAnnotations = fieldAnnotationsCache.computeIfAbsent(clazz, this::getFieldAnnotations);

        boolean isValid = true;

        logger.debug("Processing {} fields for class: {}", fields.size(), clazz.getName());

        for (Field field : fields)
        {
            Annotation[] annotations = fieldAnnotations.get(field);
            for (Annotation annotation : annotations)
            {
                logger.debug("Processing annotation: {}", annotation.annotationType().getName());

                CrossFieldConstraintValidator validator = getValidatorForAnnotation(annotation.annotationType());

                logger.debug("Validator obtained: {}", (validator != null ? validator.getClass().getName() : "null"));

                if (validator != null)
                {
                    isValid = applyValidator(obj, violations, isValid, validator);
                }
            }
        }
        return isValid;
    }

    /**
     * Retrieves all declared fields of the specified class.
     *
     * @param clazz The class whose declared fields are to be retrieved.
     * @return A list containing all declared fields of the class.
     */
    private List<Field> getAllDeclaredFields(Class<?> clazz)
    {
        return Arrays.asList(clazz.getDeclaredFields());
    }

    /**
     * Retrieves annotations for all fields of the specified class and maps them accordingly.
     *
     * @param clazz The class whose field annotations are to be retrieved.
     * @return A map where each field is associated with its array of annotations.
     */
    private Map<Field, Annotation[]> getFieldAnnotations(Class<?> clazz)
    {
        List<Field> fields = fieldMapping.get(clazz);
        return fields.stream()
                .collect(Collectors.toMap(Function.identity(), Field::getAnnotations));
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
        logger.debug("Getting validator for annotation type: {}", annotationType.getName());

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
        logger.debug("Attempting to register validator for: {}", annotationType.getName());

        CrossFieldConstraint crossFieldConstraint = annotationType.getAnnotation(CrossFieldConstraint.class);

        logger.debug("CrossFieldConstraint annotation present: {}", (crossFieldConstraint != null));

        if (crossFieldConstraint == null)
        {
            logger.debug("No @CrossFieldConstraint found on {}", annotationType.getName());

            return null;
        }

        try
        {
            Class<? extends CrossFieldConstraintValidator> validatorClass = crossFieldConstraint.validatedBy();

            logger.debug("Validator class to instantiate: {}", validatorClass.getName());

            CrossFieldConstraintValidator validator = validatorClass.getDeclaredConstructor().newInstance();

            logger.debug("Successfully created validator instance: {}", validator.getClass().getName());

            return validator;
        }
        catch (Exception e)
        {
            logger.debug("Failed to instantiate validator: {}", e.getMessage());

            throw new IllegalStateException("Failed to instantiate validator for " + annotationType, e);
        }
    }

    /**
     * Applies the given validator to the object and updates the list of violations.
     *
     * @param obj        The object to validate.
     * @param violations The list to store any constraint violations encountered during validation.
     * @param isValid    The current validity status before applying the validator.
     * @param validator  The validator to apply to the object.
     * @return {@code true} if the object passes validation after applying the validator; {@code false} otherwise.
     */
    private boolean applyValidator(Object obj, List<CrossFieldConstraintViolation> violations, boolean isValid, CrossFieldConstraintValidator validator)
    {
        try
        {
            isValid &= validator.isValid(obj, fieldMapping, violations);
            logger.debug("Validation result: {}", isValid);
        }
        catch (Exception e)
        {
            logger.debug("Validation error: {}", e.getMessage());
            isValid = false;
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
}


