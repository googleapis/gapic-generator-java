package com.google.api.generator.gapic.composer;

import com.google.api.generator.engine.ast.Reference;
import com.google.api.generator.engine.ast.TypeNode;
import com.google.api.generator.engine.ast.VaporReference;
import com.google.api.generator.gapic.composer.samplecode.SampleCodeWriter;
import com.google.api.generator.gapic.model.Method;
import com.google.api.generator.gapic.model.Method.Stream;
import com.google.api.generator.gapic.model.MethodArgument;
import com.google.api.generator.gapic.model.ResourceName;
import com.google.api.generator.gapic.model.Service;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PackageInfoSampleCodeComposer {

  private static final String PAGED_RESPONSE_TYPE_NAME_PATTERN = "%sPagedResponse";

  public static String composeSampleCode(Service service, Map<String, ResourceName> resourceNames) {
    Map<String, TypeNode> types = createDynamicTypes(service);
    TypeNode clientType = types.get(getClientName(service.name()));
    // Use the first pure unary RPC method's sample code as showcase, if no such method exists, use
    // the first method in the service's methods list.
    Method method =
        service.methods().stream()
            .filter(m -> m.stream() == Stream.NONE && !m.hasLro() && !m.isPaged())
            .findFirst()
            .orElse(service.methods().get(0));
    // If variant method signatures exists, use the first one.
    List<MethodArgument> arguments =
        method.methodSignatures().isEmpty()
            ? Collections.emptyList()
            : method.methodSignatures().get(0);
    if (method.stream() == Stream.NONE) {
      return SampleCodeWriter.write(
          SampleCodeHelperComposer.composeRpcMethodSampleCode(
              method, arguments, clientType, resourceNames));
    }
    TypeNode rawCallableReturnType = null;
    if (method.hasLro()) {
      rawCallableReturnType = types.get("OperationCallable");
    } else if (method.stream() == Stream.CLIENT) {
      rawCallableReturnType = types.get("ClientStreamingCallable");
    } else if (method.stream() == Stream.SERVER) {
      rawCallableReturnType = types.get("ServerStreamingCallable");
    } else if (method.stream() == Stream.BIDI) {
      rawCallableReturnType = types.get("BidiStreamingCallable");
    } else {
      rawCallableReturnType = types.get("UnaryCallable");
    }

    // Set generics.
    TypeNode returnType =
        TypeNode.withReference(
            rawCallableReturnType
                .reference()
                .copyAndSetGenerics(getGenericsForCallable(method, types)));
    return SampleCodeWriter.write(
        SampleCodeHelperComposer.composeRpcCallableMethodSampleCode(
            method, clientType, returnType, resourceNames));
  }

  private static Map<String, TypeNode> createDynamicTypes(Service service) {
    Map<String, TypeNode> types = new HashMap<>();
    // This class.
    types.put(
        getClientName(service.name()),
        TypeNode.withReference(
            VaporReference.builder()
                .setName(getClientName(service.name()))
                .setPakkage(service.pakkage())
                .build()));

    // Pagination types.
    types.putAll(
        service.methods().stream()
            .filter(m -> m.isPaged())
            .collect(
                Collectors.toMap(
                    m -> String.format(PAGED_RESPONSE_TYPE_NAME_PATTERN, m.name()),
                    m ->
                        TypeNode.withReference(
                            VaporReference.builder()
                                .setName(String.format(PAGED_RESPONSE_TYPE_NAME_PATTERN, m.name()))
                                .setPakkage(service.pakkage())
                                .setEnclosingClassNames(getClientName(service.name()))
                                .setIsStaticImport(true)
                                .build()))));
    return types;
  }

  private static List<Reference> getGenericsForCallable(
      Method method, Map<String, TypeNode> types) {
    if (method.hasLro()) {
      return Arrays.asList(
          method.inputType().reference(),
          method.lro().responseType().reference(),
          method.lro().metadataType().reference());
    }
    if (method.isPaged()) {
      return Arrays.asList(
          method.inputType().reference(),
          types.get(String.format(PAGED_RESPONSE_TYPE_NAME_PATTERN, method.name())).reference());
    }
    return Arrays.asList(method.inputType().reference(), method.outputType().reference());
  }

  private static String getClientName(String serviceName) {
    return String.format("%sClient", serviceName);
  }
}
