# Changelog

## 1.2.1 (2024-10-27)

### Bug Fixes

- Fixed HV000033 validation exception when using standard Jakarta validation annotations (@NotEmpty, @Email, etc.)
  alongside cross-field constraints

### Upgrading from 1.2.0

This is a patch release that fixes a bug in validation handling. No changes to your code are required.

To upgrade, update your dependency version:

Maven:

```xml

<dependency>
    <groupId>io.github.maharramoff</groupId>
    <artifactId>cross-field-validation</artifactId>
    <version>1.2.1</version>
</dependency>
```

Gradle:

```groovy
implementation 'io.github.maharramoff:cross-field-validation:1.2.1'
```

## 1.2.0 (2024-10-22)

### Changes

* Replace BeanUtils with Java Introspector for accessing properties in BaseCrossFieldValidator.

## 1.1.0 (2024-10-22)

### Bug Fixes

* Resolve issue with manual validator registration (#1)

### Changes

* Migrate from `javax` to `jakarta` packages

## 1.0.0 (2024-10-21)

### Features

* Initial release