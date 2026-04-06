package io.github.cyfko.jpametamodel.providers;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * SPI for compile-time validation of method references in {@code @Exposure} annotations.
 *
 * <p>Both pipes and handler have implementation-defined method signatures: the query context
 * type, the handler return type, and whether methods are static or IoC-managed are all
 * determined by the implementation. The annotation processor cannot validate these
 * signatures alone.</p>
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader} by the annotation
 * processor at compile time. If no implementation is found, the processor performs minimal
 * validation only (class exists + method name is non-empty).</p>
 *
 * @author Frank KOSSI
 * @since 3.0.0
 */
public interface MethodSignatureValidator {

    /**
     * Validates that a pipe method reference is valid for the given implementation.
     *
     * @param methodType the type element of the class containing the method, or {@code null} if no explicit type was specified
     * @param methodName the pipe method name
     * @param elements   the Elements utility from the processing environment
     * @param types      the Types utility from the processing environment
     * @return {@code null} if the method reference is valid, or an error message describing the issue
     */
    String validatePipeMethod(TypeElement methodType, String methodName, Elements elements, Types types);

    /**
     * Validates that a handler method reference is valid for the given implementation.
     *
     * @param methodType the type element of the class containing the method, or {@code null} if no explicit type was specified
     * @param methodName the handler method name
     * @param elements   the Elements utility from the processing environment
     * @param types      the Types utility from the processing environment
     * @return {@code null} if the method reference is valid, or an error message describing the issue
     */
    String validateHandlerMethod(TypeElement methodType, String methodName, Elements elements, Types types);
}
