package az.maharramoff;

import lombok.Getter;

@Getter
public class ConstraintViolation
{
    private final String fieldName;
    private final String message;

    public ConstraintViolation(String fieldName, String message)
    {
        this.fieldName = fieldName;
        this.message = message;
    }
}
