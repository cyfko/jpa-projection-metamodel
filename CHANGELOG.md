# Changelog

## [3.0.0] — Exposure Layer & Composed Criterion Inheritance

### Breaking Changes

#### `DirectMapping` record — new `logicalPrefix` and `cycleBreak` components
The `DirectMapping` record now has **6 components** instead of 4:

```java
// Before
record DirectMapping(String dtoField, String entityField, Class<?> dtoFieldType, Optional<CollectionMetadata> collection)

// After
record DirectMapping(String dtoField, String entityField, Class<?> dtoFieldType, Optional<CollectionMetadata> collection, Optional<String> logicalPrefix, boolean cycleBreak)
```

- `logicalPrefix` (`Optional<String>`) — captures `@Projected(as)` for composed criterion inheritance. Present when the DTO field returns another `@Projection` type. Validated: must not contain `__`, nor start/end with `_`.
- `cycleBreak` (`boolean`) — if `true`, excludes this mapping from criterion inheritance to break bidirectional cycles.
- A **convenience constructor** with the old 4-argument signature is provided, defaulting to `Optional.empty()` and `false`.
- New method: `isProjectionType()` — returns `true` if `logicalPrefix` is present.

#### `ProjectionMetadata` record — new `exposedCriteria` and `exposure` components
The `ProjectionMetadata` record now has **6 components** instead of 4:

```java
// Before
record ProjectionMetadata(Class<?> entityClass, DirectMapping[] directMappings, ComputedField[] computedFields, ComputationProvider[] computers)

// After
record ProjectionMetadata(Class<?> entityClass, DirectMapping[] directMappings, ComputedField[] computedFields, ComputationProvider[] computers, ExposedCriterion[] exposedCriteria, ExposureMetadata exposure)
```

- `exposedCriteria` (`ExposedCriterion[]`) — queryable criteria (both direct and composed via inheritance). Cannot be null.
- `exposure` (`ExposureMetadata`) — resource exposure declaration from `@Exposure`. Nullable (null = no `@Exposure`).
- A **convenience constructor** with the old 4-argument signature is provided, defaulting to `new ExposedCriterion[]{}` and `null`.

---

### Added

#### New record: `ExposedCriterion`
Runtime metadata for a queryable criterion, either declared directly via `@ExposedAs` or inherited through composed criterion inheritance from a nested `@Projection` type.

```java
public record ExposedCriterion(String ref, String sourcePath, String[] operators, boolean exposed, boolean composed)
```

Key methods: `isComposed()`, `compositionDepth()`, `refSegments()`, `supportsOperator(String)`.

#### New record: `ExposureMetadata`
Runtime metadata for a resource exposure declaration (`@Exposure` annotation).

```java
public record ExposureMetadata(String value, String namespace, String strategy)
```

Key methods: `hasNamespace()`, `isWindowed()`, `isFull()`, `isCustom()`.

#### New methods on `ProjectionMetadata`
- `getComposedCriteria()` — returns only composed (inherited) criteria
- `getProjectionMappings()` — returns direct mappings whose type is a `@Projection`
- `getCriterion(String ref, boolean ignoreCase)` — finds a criterion by ref
- `isExposed()` — returns `true` if `@Exposure` metadata is present

---

### Migration Guide

#### 1. `DirectMapping` — existing 4-arg constructor still works
```java
// This still compiles thanks to the convenience constructor:
new DirectMapping("email", "email", String.class, Optional.empty());
// Equivalent to:
new DirectMapping("email", "email", String.class, Optional.empty(), Optional.empty(), false);
```

#### 2. `ProjectionMetadata` — existing 4-arg constructor still works
```java
// This still compiles thanks to the convenience constructor:
new ProjectionMetadata(entityClass, mappings, computeds, providers);
// Equivalent to:
new ProjectionMetadata(entityClass, mappings, computeds, providers, new ExposedCriterion[]{}, null);
```

#### 3. Code using canonical constructors must be updated
Any code that uses the canonical constructor directly (e.g., generated code in the processor) must add the two new arguments.

---

## [2.0.1] — Fixed documentation

### Documentation
- Improved accuracy of Javadoc to reflect true specification behavior since projection specification 2.0.0+.
- Enhanced README.md and new sections added to describe first citizen facade classes: ProjectionRegistry and
  PersistenceRegistry

## [2.0.0] — ComputedField API

### Breaking Changes

#### `ComputedField` record — new `transformer` component
The `ComputedField` record now has **5 components** instead of 4:

```java
// Before
record ComputedField(String dtoField, String[] dependencies, ReducerMapping[] reducers, MethodReference methodReference)

// After
record ComputedField(String dtoField, String[] dependencies, ReducerMapping[] reducers, MethodReference computedBy, MethodReference transformer)
```

- The `methodReference` component is **renamed** to `computedBy`.
- A new optional `transformer` component (of type `MethodReference`) is added to describe a post-computation transformation step, separate from the computation itself.
- Any code using the canonical constructor or calling `methodReference()` must be updated.

---

#### `MethodReference` — stricter nullability contract
The inner `MethodReference` record moves from an **"at least one non-null"** policy to **"both required"**:

```java
// Before — either field could be null
record MethodReference(Class<?> targetClass, String methodName)
// → IllegalArgumentException only if BOTH are null

// After — neither field can be null
record MethodReference(Class<?> owner, String methodName)
// → IllegalArgumentException if EITHER is null
```

- The `targetClass` field is **renamed** to `owner`.
- It is no longer possible to create a `MethodReference` with only the class or only the method name — both are now required.
- Convenience constructors on `ComputedField` that previously built a partial `MethodReference` (e.g. `new MethodReference(clazz, null)`) **have been removed**.

---

### Removed

The following `ComputedField` convenience constructors have been **removed** because they created partial `MethodReference` instances, which are now invalid:

| Removed constructor | Reason |
|---|---|
| `ComputedField(String, String[], Class<?>)` | was creating `new MethodReference(clazz, null)` |
| `ComputedField(String, String[], String)` | was creating `new MethodReference(null, methodName)` |
| `ComputedField(String, String[], Class<?>, String)` | replaced — see migration guide |
| `ComputedField(String, String[], ReducerMapping[], Class<?>, String)` | replaced — see migration guide |

---

### Changed

- **Parameter rename**: `methodReferenceClass` → `compBy` in constructor signatures.
- **Javadoc**: `@param computedBy` replaces `@param methodReference` on the main record.
- **`MethodReference` invariant updated**: both `owner` and `methodName` are now mandatory (see above).

---

### Migration Guide

#### 1. Accessing the `methodReference` component
```java
// Before
MethodReference ref = computedField.methodReference();

// After
MethodReference ref = computedField.computedBy();
```

#### 2. Accessing `targetClass` on `MethodReference`
```java
// Before
Class<?> cls = ref.targetClass();

// After
Class<?> cls = ref.owner();
```

#### 3. Constructing with class-only or method-only
These cases are no longer supported by `MethodReference`. Both must always be provided:
```java
// Before
new ComputedField("field", deps, MyResolver.class);
new ComputedField("field", deps, "computeValue");

// After — both class AND method name are required
new ComputedField("field", deps, new ReducerMapping[0],
    new MethodReference(MyResolver.class, "computeValue"), null);
```

#### 4. Canonical constructor
```java
// Before (4 args)
new ComputedField(dtoField, deps, reducers, methodRef);

// After (5 args)
new ComputedField(dtoField, deps, reducers, computedBy, transformer);
// transformer may be null if no transformation step is needed
```


## [1.0.0] — First release