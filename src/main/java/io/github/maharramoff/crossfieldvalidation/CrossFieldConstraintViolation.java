package io.github.maharramoff.crossfieldvalidation;

import lombok.Getter;

/**
 * Represents a constraint violation encountered during cross-field validation.
 *
 * @author Shamkhal Maharramov
 * @since 1.0.0
 */
@Getter
public class CrossFieldConstraintViolation
{

    private final String fieldName;
    private final String message;

    /**
     * Constructs a new `ConstraintViolation` with the specified field name and message.
     *
     * @param fieldName The name of the field where the violation occurred.
     * @param message   The violation message.
     */
    public CrossFieldConstraintViolation(String fieldName, String message)
    {
        this.fieldName = fieldName;
        this.message = message;
    }
}
