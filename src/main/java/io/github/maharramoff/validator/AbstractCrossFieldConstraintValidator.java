package io.github.maharramoff.validator;

import io.github.maharramoff.model.ConstraintViolation;
import org.springframework.beans.BeanUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * An abstract base class for implementing cross-field validation logic.
 * <h2>Example Usage:</h2>
 * <pre>
 * <code>
 * public class ExampleValidator extends AbstractCrossFieldConstraintValidator {
 *     {@literal @}Override
 *     public boolean isValid(Object obj, Map&lt;Class&lt;?&gt;, List&lt;Field&gt;&gt; fieldMapping, List&lt;ConstraintViolation&gt; violations) {
 *         processFields(obj, fieldMapping, Example.class, (field, annotation) -&gt; {
 *             // ... your validation logic ...
 *         });
 *         return violations.isEmpty();
 *     }
 * }
 * </code>
 * </pre>
 *
 * @author Shamkhal Maharramov
 * @see CrossFieldConstraintValidator
 * @see ConstraintViolation
 * @since 1.0.0
 */
public abstract class AbstractCrossFieldConstraintValidator implements CrossFieldConstraintValidator
{
    /**
     * Default constructor for the `AbstractCrossFieldConstraintValidator`.
     */
    public AbstractCrossFieldConstraintValidator()
    {
        // This constructor is intentionally left blank.
    }

    /**
     * Retrieves the value of a given property field from an object.
     *
     * @param object    The object instance from which to fetch the property value.
     * @param fieldName The name of the field whose value needs to be fetched.
     * @return The value of the field if it exists, or null otherwise.
     */
    protected Object getProperty(Object object, String fieldName)
    {
        java.beans.PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(object.getClass(), fieldName);
        if (pd == null)
        {
            return null;
        }

        try
        {
            return pd.getReadMethod().invoke(object);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Processes each annotated field of a given object, applying the specified {@code fieldProcessor} function.
     *
     * <p>This method iterates through all fields in the object that are annotated with the given annotation type.
     * For each annotated field, the {@code fieldProcessor} function is invoked with the field and its annotation as arguments.</p>
     *
     * @param <T>             The type of annotation to look for on the fields.
     * @param obj             The object instance containing the fields to be processed.
     * @param fieldMapping    A map containing field information for each class type, used to look up fields in the object.
     * @param annotationClass The class type of the annotation to look for on the fields.
     * @param fieldProcessor  A {@code BiConsumer} that takes a field and its annotation as arguments and processes them.
     *                        This consumer function is responsible for the actual work to be done on each field.
     */
    protected <T extends Annotation> void processFields(Object obj,
                                                        Map<Class<?>, List<Field>> fieldMapping,
                                                        Class<T> annotationClass,
                                                        BiConsumer<Field, T> fieldProcessor)
    {
        List<Field> fields = fieldMapping.getOrDefault(obj.getClass(), Collections.emptyList());
        for (Field field : fields)
        {
            T annotation = field.getAnnotation(annotationClass);
            if (annotation != null)
            {
                fieldProcessor.accept(field, annotation);
            }
        }
    }
}
