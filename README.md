# Cross-Field Validation Library

## Unleash the Power of Cross-Field Validation in Java

This library unlocks powerful cross-field validation for your Java applications, enabling you to define complex
constraints that span multiple fields within an object.

### Why You Need This

Jakarta Bean Validation is great for single-field validation, but it falls short when you need to enforce rules that
involve relationships between different fields. This library bridges that gap, providing a flexible and intuitive way to
define and apply cross-field validation.

This solution overcomes the limitations of `ConstraintValidatorContext`, which doesn't allow interference with the
object context when writing field-level validators. There are long-standing open issues on this topic:

- [BVAL-237](https://hibernate.atlassian.net/browse/BVAL-237)
- [BVAL-240](https://hibernate.atlassian.net/browse/BVAL-240)

**Advantages of this library:**

1. **More flexible validations:**  Define complex validation logic involving multiple fields.
2. **Improved readability:** Create custom annotations that resemble built-in constraints like `@NotNull`
   and `@NotEmpty`.
3. **Simplified validation:** Use a single `@EnableCrossFieldConstraints` annotation to enable all custom validators for
   a class.

### Getting Started

**1. Add the Dependency**

```xml

<dependency>
    <groupId>io.github.maharramoff</groupId>
    <artifactId>cross-field-validation</artifactId>
    <version>1.0.0</version>
</dependency>
```

**1. Annotate your class with `@EnableCrossFieldConstraints`**

```java
@EnableCrossFieldConstraints
public class SignupRequestDTO
{
    private String username;
    private String password;

    @MatchWith("password")
    private String confirmPassword;
}
```

**3. Implement a custom validator**

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MatchWithValidator.class)
public @interface MatchWith
{
    String field();

    String message() default "Fields do not match.";
}

public class MatchWithValidator extends AbstractCrossFieldConstraintValidator
{
    @Override
    public boolean isValid(Object obj, Map<Class<?>, List<Field>> fieldMapping, List<ConstraintViolation> violations)
    {
        processFields(obj, fieldMapping, MatchWith.class, (field, annotation) ->
        {
            Object fieldValue      = getProperty(obj, field.getName());
            Object otherFieldValue = getProperty(obj, annotation.field());
            if (fieldValue == null && otherFieldValue == null)
            {
                return; // Both null is considered valid
            }
            if (fieldValue == null || !fieldValue.equals(otherFieldValue))
            {
                violations.add(new ConstraintViolation(field.getName(), annotation.message()));
            }
        });
        return violations.isEmpty();
    }
}
```

### How It Works

This library utilizes a ConstraintValidator to manage cross-field validation. Custom validators implement the
CrossFieldConstraintValidator interface, providing the logic for your specific constraints.

**1. Annotation Processing:** When the validation framework encounters the `@EnableCrossFieldConstraints` annotation on
a class, it triggers the `CrossFieldConstraintsEnabler`.

**2. Validator Execution:** The `CrossFieldConstraintsEnabler` iterates through
registered `CrossFieldConstraintValidator` implementations.

**3. Field Analysis:** Each validator analyzes the fields of the object, looking for its corresponding annotation (
e.g., `@MatchWith`).

**4. Validation Logic:** If the annotation is present, the validator executes its custom validation logic, comparing
field values as needed.

**5. Violation Reporting:** If a constraint is violated, the validator adds a `ConstraintViolation` to the list, which
is then handled by the validation framework.

### Contributing

Contributions are welcome! Please fork the repository and submit a pull request with your changes. Ensure your code
follows the existing code style and includes appropriate unit tests.