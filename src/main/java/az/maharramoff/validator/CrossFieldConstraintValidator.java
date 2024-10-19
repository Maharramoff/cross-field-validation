package az.maharramoff.validator;

import az.maharramoff.model.ConstraintViolation;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Interface for performing cross-field validation.
 * <p>
 * Implementations of this interface can perform validation checks that depend on multiple fields within an object.
 * </p>
 *
 * @author Shamkhal Maharramov
 * @see ConstraintViolation
 */
public interface CrossFieldConstraintValidator
{
    /**
     * Validates the object based on custom cross-field constraints.
     *
     * @param obj        The object to validate.
     * @param fields     A mapping from each class to its list of fields.
     * @param violations The list to populate with any constraint violations.
     * @return True if the object is valid according to custom cross-field constraints; false otherwise.
     * @throws Exception If an error occurs during validation.
     */
    boolean isValid(Object obj, Map<Class<?>, List<Field>> fields, List<ConstraintViolation> violations) throws Exception;
}

