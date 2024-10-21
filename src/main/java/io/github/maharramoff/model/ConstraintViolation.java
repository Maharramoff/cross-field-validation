package io.github.maharramoff.model;

import lombok.Getter;

/**
 * Represents a constraint violation encountered during cross-field validation.
 *
 * @author Shamkhal Maharramov
 * @since 1.0.0
 */
@Getter
public class ConstraintViolation
{

    private final String fieldName;
    private final String message;

    /**
     * Constructs a new `ConstraintViolation` with the specified field name and message.
     *
     * @param fieldName The name of the field where the violation occurred.
     * @param message   The violation message.
     */
    public ConstraintViolation(String fieldName, String message)
    {
        this.fieldName = fieldName;
        this.message = message;
    }
}
