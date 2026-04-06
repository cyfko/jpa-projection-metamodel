package io.github.cyfko.jpametamodel.api;

/**
 * A reference to a specific method in a specific class.
 *
 * <p>This record is shared across the entire API surface for any concept that needs
 * to point to a concrete Java method: computation methods ({@code @Computed.computedBy},
 * {@code @Computed.then}), transformation pipes ({@code @Exposure.pipes}), and custom
 * handlers ({@code @Exposure.handler}).</p>
 *
 * <p><b>Invariants:</b></p>
 * <ul>
 *   <li>{@code owner} must not be null</li>
 *   <li>{@code methodName} must not be null</li>
 * </ul>
 *
 * @param owner      the class containing the method
 * @param methodName the method name
 * @author Frank KOSSI
 * @since 3.0.0
 */
public record MethodReference(Class<?> owner, String methodName) {

    public MethodReference {
        if (owner == null || methodName == null) {
            throw new IllegalArgumentException("neither owner nor methodName can be null.");
        }
    }
}
