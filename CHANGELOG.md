# Changelog

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