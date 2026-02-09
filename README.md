# JPA Projection Metamodel

[![Maven Central](https://img.shields.io/maven-central/v/io.github.cyfko/jpa-projection-metamodel)](https://search.maven.org/artifact/io.github.cyfko/jpa-projection-metamodel)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

**JPA Projection Metamodel** provides interfaces, annotations, and runtime APIs for defining and leveraging type-safe DTO projections based on the [Projection Specification](https://github.com/cyfko/projection-spec/tree/main).

## 🎯 Goals

This module provides:

- **Projection annotations**: `@Projection`, `@Projected`, `@Computed`, `@Provider`, `@MethodReference`
- **Runtime registry APIs**: `PersistenceRegistry` and `ProjectionRegistry`
- **Metadata interfaces**: Type-safe access to entity and projection metadata
- **Computation support**: Integration of computation providers for derived fields

## 📋 Prerequisites

- **Java 21+**
- **Jakarta Persistence API 3.1.0+**

## 🚀 Installation

### Maven

```xml
<dependency>
    <groupId>io.github.cyfko</groupId>
    <artifactId>jpa-projection-metamodel</artifactId>
    <version>1.0.0</version>
</dependency>
```

> **Note:** To automatically generate metadata at compile time, also add the `jpa-metamodel-processor` module.

## 📖 Usage Guide

### 1. Define Your JPA Entities

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate birthDate;
    
    @Embedded
    private Address address;
    
    @ManyToOne
    private Department department;
    
    @OneToMany(mappedBy = "user")
    private List<Order> orders;
    
    // Getters and setters...
}
```

### 2. Create DTO Projections

Use the `@Projection`, `@Projected`, and `@Computed` annotations:

```java
@Projection(
    from = User.class,
    providers = {
        @Provider(value = UserComputations.class)
    }
)
public class UserDTO {
    // Direct mapping with field renaming
    @Projected(from = "email")
    private String userEmail;
    
    // Nested path to an embeddable field
    @Projected(from = "address.city")
    private String city;
    
    // Nested path to a relationship
    @Projected(from = "department.name")
    private String departmentName;
    
    // Collection
    @Projected(from = "orders")
    private List<OrderDTO> orders;
    
    // Computed field depending on multiple fields
    @Computed(dependsOn = {"firstName", "lastName"})
    private String fullName;
    
    // Computed field depending on a single field
    @Computed(dependsOn = {"birthDate"})
    private Integer age;
    
    // Getters and setters...
}
```

**Note:**
- Only entities referenced in the `from` attribute of `@Projection` are scanned for projection purposes.
- All fields in a class annotated with `@Projection` are implicitly considered as if annotated with `@Projected`, unless explicitly annotated otherwise.
- **Empty projections:** A `@Projection` with no fields is valid and still registers the target entity in `PersistenceRegistry`. This can be useful as a base class for inheritance or to force registration of specific entities.

### 3. Define Computation Providers and External Methods

Create classes containing computation methods for your `@Computed` fields. Methods can be:

- **In a declared provider** (via `@Provider`)
- **Or in any external class** accessible via `@MethodReference(type = ...)`, even if this class is not listed in the providers

Standard provider example:
```java
public class UserComputations {
    // Static method for fullName
    public static String getFullName(String firstName, String lastName) {
        return firstName + " " + lastName;
    }
    
    // Instance method for age (can be a Spring bean)
    public Integer getAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
```

Advanced example: external static method not declared as a provider
```java
public class ExternalComputer {
    public static String joinNames(String first, String last) {
        return first + ":" + last;
    }
}

@Projection(from = User.class)
public class UserDTO {
    @Computed(
        dependsOn = {"firstName", "lastName"}, 
        computedBy = @MethodReference(type = ExternalComputer.class, method = "joinNames")
    )
    private String displayName;
}
```

**Resolution behavior:**
- If `@MethodReference(type = ...)` is used, the method is searched in the specified class, even if it is not a provider.
- Compilation fails if the method does not exist or the signature does not match.
- Project tests validate this behavior to guarantee the expected flexibility.

**Naming convention:** Methods in providers should follow the pattern `get[FieldName]` (DTO field name with the first letter capitalized), unless an explicit method is referenced via `@MethodReference`.

### 4. Use Registries at Runtime

#### Access Entity Metadata

```java
import io.github.cyfko.jpametamodel.PersistenceRegistry;

// Check if an entity is registered
boolean isRegistered = PersistenceRegistry.isEntityRegistered(User.class);

// Get metadata for an entity
Map<String, PersistenceMetadata> metadata = PersistenceRegistry.getMetadataFor(User.class);

// Get metadata for a specific field
PersistenceMetadata fieldMeta = PersistenceRegistry.getFieldMetadata(User.class, "email");

// Get ID fields of an entity
List<String> idFields = PersistenceRegistry.getIdFields(User.class);
```

#### Access Projection Metadata

```java
import io.github.cyfko.jpametamodel.ProjectionRegistry;

// Get metadata for a projection
ProjectionMetadata projectionMeta = ProjectionRegistry.getMetadataFor(UserDTO.class);

// Check if a projection exists
boolean hasProjection = ProjectionRegistry.hasProjection(UserDTO.class);

// Get required entity fields for a projection
List<String> requiredFields = ProjectionRegistry.getRequiredEntityFields(UserDTO.class);

// Convert a DTO path to an entity path
String entityPath = ProjectionRegistry.toEntityPath("userEmail", UserDTO.class, false);
// Returns: "email"

String nestedPath = ProjectionRegistry.toEntityPath("city", UserDTO.class, false);
// Returns: "address.city"
```

## 💡 Use Cases

- **REST API with Projections:** Dynamically build JPA queries based on fields requested by the client.
- **Filter Schema Validation:** Validate that fields used in filters exist in entities.
- **API Documentation Generation:** Automatically generate OpenAPI/Swagger documentation from metadata.
- **Query Optimization:** Build JPA queries that only load fields necessary for a given projection.

## 🔧 Advanced Features

### Collection Support

The processor automatically detects collections and extracts their metadata:

```java
@Projection(from = User.class)
public class UserDTO {
    
    @Projected(from = "orders")
    private List<OrderDTO> orders;  // Entity collection
    
    @Projected(from = "tags")
    private Set<String> tags;  // Element collection
}
```

### Computation Providers with Dependency Injection

You can use Spring beans for computation providers:

```java
@Projection(
    from = User.class,
    providers = {
        @Provider(value = DateFormatter.class, bean = "isoDateFormatter")
    }
)
public class UserDTO {
    @Computed(dependsOn = {"createdAt"})
    private String formattedDate;
}

@Service("isoDateFormatter")
public class DateFormatter {
    public String getFormattedDate(LocalDateTime createdAt) {
        return createdAt.format(DateTimeFormatter.ISO_DATE);
    }
}
```

### Complex Nested Paths

Support for deep paths in entity hierarchy:

```java
@Projected(from = "department.manager.address.city")
private String managerCity;
```

## 📝 Annotations

Annotations described here are a reminder of the [Projection Specification](https://github.com/cyfko/projection-spec/tree/main)

### `@Projection`

Class-level annotation that declares a DTO projection.

**Parameters:**
- `from`: The source JPA entity class (required)
- `providers`: Array of computation providers (optional)

### `@Projected`

Field-level annotation to map a DTO field to an entity field.

**Parameters:**
- `from`: The path to the entity field (optional, uses DTO field name by default)

### `@Computed`

Field-level annotation to declare a computed field.

**Parameters:**
- `dependsOn`: Array of paths to entity fields required for computation
- `reducers`: Array of reducer names for collection dependencies (e.g., `"SUM"`, `"AVG"`, `"COUNT"`, `"MIN"`, `"MAX"`)
- `computedBy`: Optional `@MethodReference` to specify the computation method

**Reducers for Collection Dependencies:**

When a dependency traverses a collection (e.g., `orders.total`), a **reducer is mandatory** to specify how to aggregate the values:

```java
@Projection(from = Company.class, providers = @Provider(CompanyComputers.class))
public class CompanyDTO {
    // Single collection dependency with SUM reducer
    @Computed(dependsOn = {"orders.amount"}, reducers = {"SUM"})
    private BigDecimal totalRevenue;
    
    // Multiple collection dependencies with different reducers
    @Computed(
        dependsOn = {"orders.amount", "orders.items.quantity"},
        reducers = {"SUM", "COUNT"}
    )
    private Object orderStats;
    
    // Mixed: scalar + collection (only collection needs reducer)
    @Computed(dependsOn = {"name", "orders.amount"}, reducers = {"AVG"})
    private String summary;
}
```

**Available Reducers:** `SUM`, `AVG`, `COUNT`, `COUNT_DISTINCT`, `MIN`, `MAX`

**Runtime API:**

```java
ComputedField field = ...;
for (ComputedField.ReducerMapping rm : field.reducers()) {
    String dependency = field.dependencies()[rm.dependencyIndex()];
    String reducer = rm.reducer();
    // "orders.amount" → "SUM"
}
```

### `@Provider`

Annotation to declare a computation provider.

**Parameters:**
- `value`: The provider class (required)
- `bean`: The bean name for dependency injection (optional)

## 📦 Java Modules (JPMS) Configuration

If your project uses the Java Platform Module System (i.e., you have a `module-info.java` file), you must follow these specific steps to allow the library to discover the generated metamodel code.

> **Note:** If you are not using Java Modules (e.g., a standard Spring Boot application without `module-info.java`), you can **skip this section**.

### 1. Update `module-info.java`

You need to authorize the library to access the generated implementation via reflection. Add the following directives to your module descriptor:

```java
module com.mycompany.myproject {
    // 1. Require the library
    requires io.github.cyfko.jpametamodel;

    // 2. Open the generated package to the library
    // This allows the PersistenceRegistry to instantiate the generated provider via reflection.
    opens io.github.cyfko.jpametamodel.providers.impl to io.github.cyfko.jpametamodel;
}
```

### 2. Create a "Placeholder" Package File (Crucial)

Since the `io.github.cyfko.jpametamodel.providers.impl` package is populated only after the annotation processor runs, the Java compiler might throw a "package does not exist" error when processing the `opens` directive in step 1.

To fix this, you must explicitly create the package structure in your source tree with a placeholder file.

**Create the following file:**
`src/main/java/io/github/cyfko/jpametamodel/providers/impl/package-info.java`

With this content:
```java
/**
 * Placeholder package to ensure the package exists at compile-time 
 * for JPMS 'opens' directive compatibility.
 */
package io.github.cyfko.jpametamodel.providers.impl;
```

This ensures the package technically exists before compilation starts, satisfying the strict module checks.

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## 👤 Author

**Frank KOSSI**

- Email: frank.kossi@kunrin.com, frank.kossi@sprint-pay.com
- Organization: [Kunrin SA](https://www.kunrin.com), [Sprint-Pay SA](https://www.sprint-pay.com)

## 🔗 Links

- [GitHub Repository](https://github.com/cyfko/jpa-projection-metamodel)
- [Maven Central](https://search.maven.org/artifact/io.github.cyfko/jpa-projection-metamodel)
- [Projection Specification](https://github.com/cyfko/projection-spec)
- [jpa-metamodel-processor](https://github.com/cyfko/jpa-metamodel-processor)