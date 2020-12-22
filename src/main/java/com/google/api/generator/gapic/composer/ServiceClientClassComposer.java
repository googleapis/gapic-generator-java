// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.api.generator.gapic.composer;

import com.google.api.core.ApiFunction;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.core.BetaApi;
import com.google.api.gax.core.BackgroundResource;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.AbstractFixedSizeCollection;
import com.google.api.gax.paging.AbstractPage;
import com.google.api.gax.paging.AbstractPagedListResponse;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.api.gax.rpc.ClientStreamingCallable;
import com.google.api.gax.rpc.OperationCallable;
import com.google.api.gax.rpc.PageContext;
import com.google.api.gax.rpc.ServerStreamingCallable;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.api.generator.engine.ast.AnnotationNode;
import com.google.api.generator.engine.ast.AnonymousClassExpr;
import com.google.api.generator.engine.ast.AssignmentExpr;
import com.google.api.generator.engine.ast.CastExpr;
import com.google.api.generator.engine.ast.ClassDefinition;
import com.google.api.generator.engine.ast.ConcreteReference;
import com.google.api.generator.engine.ast.Expr;
import com.google.api.generator.engine.ast.ExprStatement;
import com.google.api.generator.engine.ast.MethodDefinition;
import com.google.api.generator.engine.ast.MethodInvocationExpr;
import com.google.api.generator.engine.ast.NewObjectExpr;
import com.google.api.generator.engine.ast.PrimitiveValue;
import com.google.api.generator.engine.ast.Reference;
import com.google.api.generator.engine.ast.ReferenceConstructorExpr;
import com.google.api.generator.engine.ast.RelationalOperationExpr;
import com.google.api.generator.engine.ast.ScopeNode;
import com.google.api.generator.engine.ast.Statement;
import com.google.api.generator.engine.ast.SuperObjectValue;
import com.google.api.generator.engine.ast.TernaryExpr;
import com.google.api.generator.engine.ast.ThisObjectValue;
import com.google.api.generator.engine.ast.TypeNode;
import com.google.api.generator.engine.ast.ValueExpr;
import com.google.api.generator.engine.ast.VaporReference;
import com.google.api.generator.engine.ast.Variable;
import com.google.api.generator.engine.ast.VariableExpr;
import com.google.api.generator.gapic.model.Field;
import com.google.api.generator.gapic.model.GapicClass;
import com.google.api.generator.gapic.model.GapicClass.Kind;
import com.google.api.generator.gapic.model.LongrunningOperation;
import com.google.api.generator.gapic.model.Message;
import com.google.api.generator.gapic.model.Method;
import com.google.api.generator.gapic.model.Method.Stream;
import com.google.api.generator.gapic.model.MethodArgument;
import com.google.api.generator.gapic.model.ResourceName;
import com.google.api.generator.gapic.model.Service;
import com.google.api.generator.gapic.utils.JavaStyle;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.longrunning.Operation;
import com.google.rpc.Status;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Generated;

public class ServiceClientClassComposer {
  private static final ServiceClientClassComposer INSTANCE = new ServiceClientClassComposer();
  private static final String PAGED_RESPONSE_TYPE_NAME_PATTERN = "%sPagedResponse";
  private static final String CALLABLE_NAME_PATTERN = "%sCallable";
  private static final String PAGED_CALLABLE_NAME_PATTERN = "%sPagedCallable";
  private static final String OPERATION_CALLABLE_NAME_PATTERN = "%sOperationCallable";

  private static final Reference LIST_REFERENCE = ConcreteReference.withClazz(List.class);
  private static final Reference MAP_REFERENCE = ConcreteReference.withClazz(Map.class);
  private static final TypeNode OBJECTS_TYPE =
      TypeNode.withReference(ConcreteReference.withClazz(Objects.class));

  private enum CallableMethodKind {
    REGULAR,
    LRO,
    PAGED,
  }

  private ServiceClientClassComposer() {}

  public static ServiceClientClassComposer instance() {
    return INSTANCE;
  }

  public GapicClass generate(
      Service service, Map<String, Message> messageTypes, Map<String, ResourceName> resourceNames) {
    Map<String, TypeNode> types = createTypes(service, messageTypes);
    String className = ClassNames.getServiceClientClassName(service);
    GapicClass.Kind kind = Kind.MAIN;
    String pakkage = service.pakkage();
    boolean hasLroClient = hasLroMethods(service);

    ClassDefinition classDef =
        ClassDefinition.builder()
            .setHeaderCommentStatements(
                ServiceClientCommentComposer.createClassHeaderComments(
                    service,
                    types.get(ClassNames.getServiceClientClassName(service)),
                    types.get(ClassNames.getServiceSettingsClassName(service))))
            .setPackageString(pakkage)
            .setAnnotations(createClassAnnotations(types))
            .setScope(ScopeNode.PUBLIC)
            .setName(className)
            .setImplementsTypes(createClassImplements(types))
            .setStatements(createFieldDeclarations(service, types, hasLroClient))
            .setMethods(
                createClassMethods(service, messageTypes, types, resourceNames, hasLroClient))
            .setNestedClasses(createNestedPagingClasses(service, messageTypes, types))
            .build();
    return GapicClass.create(kind, classDef);
  }

  private static List<AnnotationNode> createClassAnnotations(Map<String, TypeNode> types) {
    return Arrays.asList(
        AnnotationNode.withType(types.get("BetaApi")),
        AnnotationNode.builder()
            .setType(types.get("Generated"))
            .setDescription("by gapic-generator")
            .build());
  }

  private static List<TypeNode> createClassImplements(Map<String, TypeNode> types) {
    return Arrays.asList(types.get("BackgroundResource"));
  }

  private static List<MethodDefinition> createClassMethods(
      Service service,
      Map<String, Message> messageTypes,
      Map<String, TypeNode> types,
      Map<String, ResourceName> resourceNames,
      boolean hasLroClient) {
    List<MethodDefinition> methods = new ArrayList<>();
    methods.addAll(createStaticCreatorMethods(service, types));
    methods.addAll(createConstructorMethods(service, types, hasLroClient));
    methods.addAll(createGetterMethods(service, types, hasLroClient));
    methods.addAll(createServiceMethods(service, messageTypes, types, resourceNames));
    methods.addAll(createBackgroundResourceMethods(service, types));
    return methods;
  }

  private static boolean hasLroMethods(Service service) {
    for (Method method : service.methods()) {
      if (method.hasLro()) {
        return true;
      }
    }
    return false;
  }

  private static List<Statement> createFieldDeclarations(
      Service service, Map<String, TypeNode> types, boolean hasLroClient) {
    Map<String, TypeNode> fieldNameToTypes = new HashMap<>();
    fieldNameToTypes.put("settings", types.get(ClassNames.getServiceSettingsClassName(service)));
    fieldNameToTypes.put("stub", types.get(ClassNames.getServiceStubClassName(service)));
    if (hasLroClient) {
      fieldNameToTypes.put("operationsClient", types.get("OperationsClient"));
    }

    return fieldNameToTypes.entrySet().stream()
        .map(
            e -> {
              String varName = e.getKey();
              TypeNode varType = e.getValue();
              Variable variable = Variable.builder().setName(varName).setType(varType).build();
              VariableExpr varExpr =
                  VariableExpr.builder()
                      .setVariable(variable)
                      .setScope(ScopeNode.PRIVATE)
                      .setIsFinal(true)
                      .setIsDecl(true)
                      .build();
              return ExprStatement.withExpr(varExpr);
            })
        .collect(Collectors.toList());
  }

  private static List<MethodDefinition> createStaticCreatorMethods(
      Service service, Map<String, TypeNode> types) {
    List<MethodDefinition> methods = new ArrayList<>();
    String thisClientName = ClassNames.getServiceClientClassName(service);
    String settingsName = ClassNames.getServiceSettingsClassName(service);
    TypeNode thisClassType = types.get(thisClientName);
    TypeNode exceptionType = types.get("IOException");

    TypeNode settingsType = types.get(settingsName);
    Preconditions.checkNotNull(settingsType, String.format("Type %s not found", settingsName));

    MethodInvocationExpr newBuilderExpr =
        MethodInvocationExpr.builder()
            .setMethodName("newBuilder")
            .setStaticReferenceType(settingsType)
            .build();
    MethodInvocationExpr buildExpr =
        MethodInvocationExpr.builder()
            .setMethodName("build")
            .setExprReferenceExpr(newBuilderExpr)
            .build();
    MethodInvocationExpr createMethodInvocationExpr =
        MethodInvocationExpr.builder()
            .setMethodName("create")
            .setArguments(Arrays.asList(buildExpr))
            .setReturnType(types.get(thisClientName))
            .build();

    MethodDefinition createMethodOne =
        MethodDefinition.builder()
            .setHeaderCommentStatements(
                ServiceClientCommentComposer.createMethodNoArgComment(
                    ClassNames.getServiceClientClassName(service)))
            .setScope(ScopeNode.PUBLIC)
            .setIsStatic(true)
            .setIsFinal(true)
            .setReturnType(thisClassType)
            .setName("create")
            .setThrowsExceptions(Arrays.asList(exceptionType))
            .setReturnExpr(createMethodInvocationExpr)
            .build();
    methods.add(createMethodOne);

    // Second create(ServiceSettings settings) method.
    VariableExpr settingsVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName("settings").setType(types.get(settingsName)).build());

    methods.add(
        MethodDefinition.builder()
            .setHeaderCommentStatements(
                ServiceClientCommentComposer.createMethodSettingsArgComment(
                    ClassNames.getServiceClientClassName(service)))
            .setScope(ScopeNode.PUBLIC)
            .setIsStatic(true)
            .setIsFinal(true)
            .setReturnType(thisClassType)
            .setName("create")
            .setThrowsExceptions(Arrays.asList(exceptionType))
            .setArguments(settingsVarExpr.toBuilder().setIsDecl(true).build())
            .setReturnExpr(
                NewObjectExpr.builder()
                    .setType(thisClassType)
                    .setArguments(settingsVarExpr)
                    .build())
            .build());

    // Third create(ServiceStub stub) method.
    VariableExpr stubVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(types.get(ClassNames.getServiceStubClassName(service)))
                .setName("stub")
                .build());
    AnnotationNode betaAnnotation =
        AnnotationNode.builder()
            .setType(types.get("BetaApi"))
            .setDescription(
                "A restructuring of stub classes is planned, so this may break in the future")
            .build();
    methods.add(
        MethodDefinition.builder()
            .setHeaderCommentStatements(
                ServiceClientCommentComposer.createCreateMethodStubArgComment(
                    ClassNames.getServiceClientClassName(service), settingsVarExpr.type()))
            .setAnnotations(Arrays.asList(betaAnnotation))
            .setScope(ScopeNode.PUBLIC)
            .setIsStatic(true)
            .setIsFinal(true)
            .setReturnType(thisClassType)
            .setName("create")
            .setArguments(stubVarExpr.toBuilder().setIsDecl(true).build())
            .setReturnExpr(
                NewObjectExpr.builder().setType(thisClassType).setArguments(stubVarExpr).build())
            .build());

    return methods;
  }

  private static List<MethodDefinition> createConstructorMethods(
      Service service, Map<String, TypeNode> types, boolean hasLroClient) {
    List<MethodDefinition> methods = new ArrayList<>();
    String thisClientName = ClassNames.getServiceClientClassName(service);
    String settingsName = ClassNames.getServiceSettingsClassName(service);
    TypeNode thisClassType = types.get(thisClientName);
    TypeNode stubSettingsType = types.get(ClassNames.getServiceStubSettingsClassName(service));
    TypeNode operationsClientType = types.get("OperationsClient");
    TypeNode exceptionType = types.get("IOException");

    TypeNode settingsType = types.get(settingsName);
    VariableExpr settingsVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName("settings").setType(types.get(settingsName)).build());
    VariableExpr stubVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(types.get(ClassNames.getServiceStubClassName(service)))
                .setName("stub")
                .build());
    VariableExpr operationsClientVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setType(operationsClientType).setName("operationsClient").build());

    // Create the ServiceClient(ServiceSettings settings) ctor.
    List<Expr> ctorAssignmentExprs = new ArrayList<>();
    Expr thisExpr = ValueExpr.withValue(ThisObjectValue.withType(thisClassType));
    ctorAssignmentExprs.add(
        AssignmentExpr.builder()
            .setVariableExpr(settingsVarExpr.toBuilder().setExprReferenceExpr(thisExpr).build())
            .setValueExpr(settingsVarExpr)
            .build());
    ctorAssignmentExprs.add(
        AssignmentExpr.builder()
            .setVariableExpr(stubVarExpr.toBuilder().setExprReferenceExpr(thisExpr).build())
            .setValueExpr(
                MethodInvocationExpr.builder()
                    .setExprReferenceExpr(
                        CastExpr.builder()
                            .setType(stubSettingsType)
                            .setExpr(
                                MethodInvocationExpr.builder()
                                    .setExprReferenceExpr(settingsVarExpr)
                                    .setMethodName("getStubSettings")
                                    .setReturnType(stubSettingsType)
                                    .build())
                            .build())
                    .setMethodName("createStub")
                    .setReturnType(stubVarExpr.type())
                    .build())
            .build());

    Expr clientArgExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(stubVarExpr.toBuilder().setExprReferenceExpr(thisExpr).build())
            .setMethodName("getOperationsStub")
            .build();
    AssignmentExpr operationsClientAssignExpr =
        AssignmentExpr.builder()
            .setVariableExpr(
                operationsClientVarExpr.toBuilder().setExprReferenceExpr(thisExpr).build())
            .setValueExpr(
                MethodInvocationExpr.builder()
                    .setStaticReferenceType(operationsClientType)
                    .setMethodName("create")
                    .setArguments(clientArgExpr)
                    .setReturnType(operationsClientVarExpr.type())
                    .build())
            .build();
    if (hasLroClient) {
      ctorAssignmentExprs.add(operationsClientAssignExpr);
    }

    methods.add(
        MethodDefinition.constructorBuilder()
            .setHeaderCommentStatements(
                ServiceClientCommentComposer.createProtectedCtorSettingsArgComment(
                    ClassNames.getServiceClientClassName(service)))
            .setScope(ScopeNode.PROTECTED)
            .setReturnType(thisClassType)
            .setArguments(settingsVarExpr.toBuilder().setIsDecl(true).build())
            .setThrowsExceptions(Arrays.asList(exceptionType))
            .setBody(
                ctorAssignmentExprs.stream()
                    .map(e -> ExprStatement.withExpr(e))
                    .collect(Collectors.toList()))
            .build());

    // Create the ServiceClient(ServiceStub stub) ctor.
    ctorAssignmentExprs.clear();
    ctorAssignmentExprs.add(
        AssignmentExpr.builder()
            .setVariableExpr(settingsVarExpr.toBuilder().setExprReferenceExpr(thisExpr).build())
            .setValueExpr(ValueExpr.createNullExpr())
            .build());
    ctorAssignmentExprs.add(
        AssignmentExpr.builder()
            .setVariableExpr(stubVarExpr.toBuilder().setExprReferenceExpr(thisExpr).build())
            .setValueExpr(stubVarExpr)
            .build());
    if (hasLroClient) {
      ctorAssignmentExprs.add(operationsClientAssignExpr);
    }
    AnnotationNode betaAnnotation =
        AnnotationNode.builder()
            .setType(types.get("BetaApi"))
            .setDescription(
                "A restructuring of stub classes is planned, so this may break in the future")
            .build();
    methods.add(
        MethodDefinition.constructorBuilder()
            .setAnnotations(Arrays.asList(betaAnnotation))
            .setScope(ScopeNode.PROTECTED)
            .setReturnType(thisClassType)
            .setArguments(stubVarExpr.toBuilder().setIsDecl(true).build())
            .setBody(
                ctorAssignmentExprs.stream()
                    .map(e -> ExprStatement.withExpr(e))
                    .collect(Collectors.toList()))
            .build());

    return methods;
  }

  private static List<MethodDefinition> createGetterMethods(
      Service service, Map<String, TypeNode> types, boolean hasLroClient) {
    Map<String, TypeNode> methodNameToTypes = new LinkedHashMap<>();
    methodNameToTypes.put(
        "getSettings", types.get(ClassNames.getServiceSettingsClassName(service)));
    methodNameToTypes.put("getStub", types.get(ClassNames.getServiceStubClassName(service)));
    String getOperationsClientMethodName = "getOperationsClient";
    if (hasLroClient) {
      methodNameToTypes.put(getOperationsClientMethodName, types.get("OperationsClient"));
    }
    AnnotationNode betaStubAnnotation =
        AnnotationNode.builder()
            .setType(types.get("BetaApi"))
            .setDescription(
                "A restructuring of stub classes is planned, so this may break in the future")
            .build();

    return methodNameToTypes.entrySet().stream()
        .map(
            e -> {
              String methodName = e.getKey();
              TypeNode methodReturnType = e.getValue();
              String returnVariableName = JavaStyle.toLowerCamelCase(methodName.substring(3));
              MethodDefinition.Builder methodBuilder = MethodDefinition.builder();
              if (methodName.equals(getOperationsClientMethodName)) {
                methodBuilder =
                    methodBuilder.setHeaderCommentStatements(
                        ServiceClientCommentComposer.GET_OPERATIONS_CLIENT_METHOD_COMMENT);
              }
              return methodBuilder
                  .setAnnotations(
                      methodName.equals("getStub")
                          ? Arrays.asList(betaStubAnnotation)
                          : Collections.emptyList())
                  .setScope(ScopeNode.PUBLIC)
                  .setName(methodName)
                  .setIsFinal(!methodName.equals("getStub"))
                  .setReturnType(methodReturnType)
                  .setReturnExpr(
                      VariableExpr.builder()
                          .setVariable(
                              Variable.builder()
                                  .setName(returnVariableName)
                                  .setType(methodReturnType)
                                  .build())
                          .build())
                  .build();
            })
        .collect(Collectors.toList());
  }

  private static List<MethodDefinition> createServiceMethods(
      Service service,
      Map<String, Message> messageTypes,
      Map<String, TypeNode> types,
      Map<String, ResourceName> resourceNames) {
    List<MethodDefinition> javaMethods = new ArrayList<>();
    for (Method method : service.methods()) {
      if (method.stream().equals(Stream.NONE)) {
        javaMethods.addAll(
            createMethodVariants(
                method, getClientClassName(service), messageTypes, types, resourceNames));
        javaMethods.add(createMethodDefaultMethod(method, types));
      }
      if (method.hasLro()) {
        javaMethods.add(createLroCallableMethod(service, method, types));
      }
      if (method.isPaged()) {
        javaMethods.add(createPagedCallableMethod(service, method, types));
      }
      javaMethods.add(createCallableMethod(service, method, types));
    }
    return javaMethods;
  }

  private static List<MethodDefinition> createMethodVariants(
      Method method,
      String clientName,
      Map<String, Message> messageTypes,
      Map<String, TypeNode> types,
      Map<String, ResourceName> resourceNames) {
    List<MethodDefinition> javaMethods = new ArrayList<>();
    String methodName = JavaStyle.toLowerCamelCase(method.name());
    TypeNode methodInputType = method.inputType();
    TypeNode methodOutputType =
        method.isPaged()
            ? types.get(String.format(PAGED_RESPONSE_TYPE_NAME_PATTERN, method.name()))
            : method.outputType();
    if (method.hasLro()) {
      LongrunningOperation lro = method.lro();
      methodOutputType =
          TypeNode.withReference(
              types
                  .get("OperationFuture")
                  .reference()
                  .copyAndSetGenerics(
                      Arrays.asList(
                          lro.responseType().reference(), lro.metadataType().reference())));
    }

    String methodInputTypeName = methodInputType.reference().name();
    for (List<MethodArgument> signature : method.methodSignatures()) {
      // Get the argument list.
      List<VariableExpr> arguments =
          signature.stream()
              .map(
                  methodArg ->
                      VariableExpr.builder()
                          .setVariable(
                              Variable.builder()
                                  .setName(JavaStyle.toLowerCamelCase(methodArg.name()))
                                  .setType(methodArg.type())
                                  .build())
                          .setIsDecl(true)
                          .build())
              .collect(Collectors.toList());

      // Request proto builder.
      VariableExpr requestVarExpr =
          VariableExpr.builder()
              .setVariable(Variable.builder().setName("request").setType(methodInputType).build())
              .setIsDecl(true)
              .build();

      Expr requestBuilderExpr = createRequestBuilderExpr(method, signature, types);

      AssignmentExpr requestAssignmentExpr =
          AssignmentExpr.builder()
              .setVariableExpr(requestVarExpr)
              .setValueExpr(requestBuilderExpr)
              .build();

      List<Statement> statements = new ArrayList<>();
      statements.add(ExprStatement.withExpr(requestAssignmentExpr));

      MethodInvocationExpr rpcInvocationExpr =
          MethodInvocationExpr.builder()
              .setMethodName(String.format(method.hasLro() ? "%sAsync" : "%s", methodName))
              .setArguments(Arrays.asList(requestVarExpr.toBuilder().setIsDecl(false).build()))
              .setReturnType(methodOutputType)
              .build();

      Optional<String> methodSampleCode = Optional.empty();
      if (!method.isPaged() && !method.hasLro()) {
        // TODO(summerji): Remove the condition check once finished the implementation on paged
        // sample code and lro sample code.
        methodSampleCode =
            Optional.of(
                ServiceClientSampleCodeComposer.composeRpcMethodHeaderSampleCode(
                    method, types.get(clientName), signature, resourceNames));
      }
      MethodDefinition.Builder methodVariantBuilder =
          MethodDefinition.builder()
              .setHeaderCommentStatements(
                  ServiceClientCommentComposer.createRpcMethodHeaderComment(
                      method, signature, methodSampleCode))
              .setScope(ScopeNode.PUBLIC)
              .setIsFinal(true)
              .setName(String.format(method.hasLro() ? "%sAsync" : "%s", methodName))
              .setArguments(arguments);

      if (isProtoEmptyType(methodOutputType)) {
        statements.add(ExprStatement.withExpr(rpcInvocationExpr));
        methodVariantBuilder = methodVariantBuilder.setReturnType(TypeNode.VOID);
      } else {
        methodVariantBuilder =
            methodVariantBuilder.setReturnType(methodOutputType).setReturnExpr(rpcInvocationExpr);
      }
      methodVariantBuilder = methodVariantBuilder.setBody(statements);
      javaMethods.add(methodVariantBuilder.build());
    }

    return javaMethods;
  }

  private static MethodDefinition createMethodDefaultMethod(
      Method method, Map<String, TypeNode> types) {
    String methodName = JavaStyle.toLowerCamelCase(method.name());
    TypeNode methodInputType = method.inputType();
    TypeNode methodOutputType =
        method.isPaged()
            ? types.get(String.format(PAGED_RESPONSE_TYPE_NAME_PATTERN, method.name()))
            : method.outputType();
    if (method.hasLro()) {
      LongrunningOperation lro = method.lro();
      methodOutputType =
          TypeNode.withReference(
              types
                  .get("OperationFuture")
                  .reference()
                  .copyAndSetGenerics(
                      Arrays.asList(
                          lro.responseType().reference(), lro.metadataType().reference())));
    }

    // Construct the method that accepts a request proto.
    VariableExpr requestArgVarExpr =
        VariableExpr.builder()
            .setVariable(Variable.builder().setName("request").setType(methodInputType).build())
            .setIsDecl(true)
            .build();
    String callableMethodName =
        method.isPaged()
            ? String.format(PAGED_CALLABLE_NAME_PATTERN, methodName)
            : String.format(CALLABLE_NAME_PATTERN, methodName);
    if (method.hasLro()) {
      callableMethodName = String.format(OPERATION_CALLABLE_NAME_PATTERN, methodName);
    }

    MethodInvocationExpr callableMethodExpr =
        MethodInvocationExpr.builder().setMethodName(callableMethodName).build();
    callableMethodExpr =
        MethodInvocationExpr.builder()
            .setMethodName(method.hasLro() ? "futureCall" : "call")
            .setArguments(Arrays.asList(requestArgVarExpr.toBuilder().setIsDecl(false).build()))
            .setExprReferenceExpr(callableMethodExpr)
            .setReturnType(methodOutputType)
            .build();
    MethodDefinition.Builder methodBuilder =
        MethodDefinition.builder()
            .setHeaderCommentStatements(
                ServiceClientCommentComposer.createRpcMethodHeaderComment(method))
            .setScope(ScopeNode.PUBLIC)
            .setIsFinal(true)
            .setName(String.format(method.hasLro() ? "%sAsync" : "%s", methodName))
            .setArguments(Arrays.asList(requestArgVarExpr));

    if (isProtoEmptyType(methodOutputType)) {
      methodBuilder =
          methodBuilder
              .setBody(Arrays.asList(ExprStatement.withExpr(callableMethodExpr)))
              .setReturnType(TypeNode.VOID);
    } else {
      methodBuilder =
          methodBuilder.setReturnExpr(callableMethodExpr).setReturnType(methodOutputType);
    }
    return methodBuilder.build();
  }

  private static MethodDefinition createLroCallableMethod(
      Service service, Method method, Map<String, TypeNode> types) {
    return createCallableMethod(service, method, types, CallableMethodKind.LRO);
  }

  private static MethodDefinition createCallableMethod(
      Service service, Method method, Map<String, TypeNode> types) {
    return createCallableMethod(service, method, types, CallableMethodKind.REGULAR);
  }

  private static MethodDefinition createPagedCallableMethod(
      Service service, Method method, Map<String, TypeNode> types) {
    return createCallableMethod(service, method, types, CallableMethodKind.PAGED);
  }

  private static MethodDefinition createCallableMethod(
      Service service,
      Method method,
      Map<String, TypeNode> types,
      CallableMethodKind callableMethodKind) {
    TypeNode rawCallableReturnType = null;
    if (callableMethodKind.equals(CallableMethodKind.LRO)) {
      rawCallableReturnType = types.get("OperationCallable");
    } else {
      switch (method.stream()) {
        case CLIENT:
          rawCallableReturnType = types.get("ClientStreamingCallable");
          break;
        case SERVER:
          rawCallableReturnType = types.get("ServerStreamingCallable");
          break;
        case BIDI:
          rawCallableReturnType = types.get("BidiStreamingCallable");
          break;
        case NONE:
          // Fall through.
        default:
          rawCallableReturnType = types.get("UnaryCallable");
      }
    }

    // Set generics.
    TypeNode returnType =
        TypeNode.withReference(
            rawCallableReturnType
                .reference()
                .copyAndSetGenerics(getGenericsForCallable(callableMethodKind, method, types)));

    String rawMethodName = JavaStyle.toLowerCamelCase(method.name());
    String methodName = getCallableName(callableMethodKind, rawMethodName);
    TypeNode stubType = types.get(ClassNames.getServiceStubClassName(service));
    MethodInvocationExpr returnExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(
                VariableExpr.builder()
                    .setVariable(Variable.builder().setName("stub").setType(stubType).build())
                    .build())
            .setMethodName(methodName)
            .setReturnType(returnType)
            .build();

    return MethodDefinition.builder()
        .setHeaderCommentStatements(
            ServiceClientCommentComposer.createRpcCallableMethodHeaderComment(method))
        .setScope(ScopeNode.PUBLIC)
        .setIsFinal(true)
        .setName(methodName)
        .setReturnType(returnType)
        .setReturnExpr(returnExpr)
        .build();
  }

  private static List<MethodDefinition> createBackgroundResourceMethods(
      Service service, Map<String, TypeNode> types) {
    List<MethodDefinition> methods = new ArrayList<>();

    VariableExpr stubVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(types.get(ClassNames.getServiceStubClassName(service)))
                .setName("stub")
                .build());
    MethodDefinition closeMethod =
        MethodDefinition.builder()
            .setIsOverride(true)
            .setScope(ScopeNode.PUBLIC)
            .setIsFinal(true)
            .setReturnType(TypeNode.VOID)
            .setName("close")
            .setBody(
                Arrays.asList(
                    ExprStatement.withExpr(
                        MethodInvocationExpr.builder()
                            .setExprReferenceExpr(stubVarExpr)
                            .setMethodName("close")
                            .build())))
            .build();
    methods.add(closeMethod);

    MethodDefinition shutdownMethod =
        MethodDefinition.builder()
            .setIsOverride(true)
            .setScope(ScopeNode.PUBLIC)
            .setReturnType(TypeNode.VOID)
            .setName("shutdown")
            .setBody(
                Arrays.asList(
                    ExprStatement.withExpr(
                        MethodInvocationExpr.builder()
                            .setExprReferenceExpr(stubVarExpr)
                            .setMethodName("shutdown")
                            .build())))
            .build();
    methods.add(shutdownMethod);

    MethodDefinition isShutdownMethod =
        MethodDefinition.builder()
            .setIsOverride(true)
            .setScope(ScopeNode.PUBLIC)
            .setReturnType(TypeNode.BOOLEAN)
            .setName("isShutdown")
            .setReturnExpr(
                MethodInvocationExpr.builder()
                    .setExprReferenceExpr(stubVarExpr)
                    .setMethodName("isShutdown")
                    .setReturnType(TypeNode.BOOLEAN)
                    .build())
            .build();
    methods.add(isShutdownMethod);

    MethodDefinition isTerminatedMethod =
        MethodDefinition.builder()
            .setIsOverride(true)
            .setScope(ScopeNode.PUBLIC)
            .setReturnType(TypeNode.BOOLEAN)
            .setName("isTerminated")
            .setReturnExpr(
                MethodInvocationExpr.builder()
                    .setExprReferenceExpr(stubVarExpr)
                    .setMethodName("isTerminated")
                    .setReturnType(TypeNode.BOOLEAN)
                    .build())
            .build();
    methods.add(isTerminatedMethod);

    MethodDefinition shutdownNowMethod =
        MethodDefinition.builder()
            .setIsOverride(true)
            .setScope(ScopeNode.PUBLIC)
            .setReturnType(TypeNode.VOID)
            .setName("shutdownNow")
            .setBody(
                Arrays.asList(
                    ExprStatement.withExpr(
                        MethodInvocationExpr.builder()
                            .setExprReferenceExpr(stubVarExpr)
                            .setMethodName("shutdownNow")
                            .build())))
            .build();
    methods.add(shutdownNowMethod);

    List<VariableExpr> arguments =
        Arrays.asList(
            VariableExpr.builder().setVariable(createVariable("duration", TypeNode.LONG)).build(),
            VariableExpr.builder()
                .setVariable(createVariable("unit", types.get("TimeUnit")))
                .build());

    MethodDefinition awaitTerminationMethod =
        MethodDefinition.builder()
            .setIsOverride(true)
            .setScope(ScopeNode.PUBLIC)
            .setReturnType(TypeNode.BOOLEAN)
            .setName("awaitTermination")
            .setArguments(
                arguments.stream()
                    .map(v -> v.toBuilder().setIsDecl(true).build())
                    .collect(Collectors.toList()))
            .setThrowsExceptions(Arrays.asList(types.get("InterruptedException")))
            .setReturnExpr(
                MethodInvocationExpr.builder()
                    .setExprReferenceExpr(stubVarExpr)
                    .setMethodName("awaitTermination")
                    .setArguments(
                        arguments.stream().map(v -> (Expr) v).collect(Collectors.toList()))
                    .setReturnType(TypeNode.BOOLEAN)
                    .build())
            .build();
    methods.add(awaitTerminationMethod);

    return methods;
  }

  private static List<ClassDefinition> createNestedPagingClasses(
      Service service, Map<String, Message> messageTypes, Map<String, TypeNode> types) {
    List<ClassDefinition> nestedClasses = new ArrayList<>();
    for (Method method : service.methods()) {
      if (!method.isPaged()) {
        continue;
      }
      // Find the repeated field.
      Message methodOutputMessage = messageTypes.get(method.outputType().reference().simpleName());
      Field repeatedPagedResultsField = methodOutputMessage.findAndUnwrapFirstRepeatedField();
      Preconditions.checkNotNull(
          repeatedPagedResultsField,
          String.format(
              "No repeated field found on message %s for method %s",
              methodOutputMessage.name(), method.name()));

      TypeNode repeatedResponseType = repeatedPagedResultsField.type();

      nestedClasses.add(
          createNestedRpcPagedResponseClass(method, repeatedResponseType, messageTypes, types));
      nestedClasses.add(
          createNestedRpcPageClass(method, repeatedResponseType, messageTypes, types));
      nestedClasses.add(
          createNestedRpcFixedSizeCollectionClass(
              method, repeatedResponseType, messageTypes, types));
    }

    return nestedClasses;
  }

  private static ClassDefinition createNestedRpcPagedResponseClass(
      Method method,
      TypeNode repeatedResponseType,
      Map<String, Message> messageTypes,
      Map<String, TypeNode> types) {
    Preconditions.checkState(
        method.isPaged(), String.format("Expected method %s to be paged", method.name()));

    String className = String.format("%sPagedResponse", JavaStyle.toUpperCamelCase(method.name()));
    TypeNode thisClassType = types.get(className);

    String upperJavaMethodName = JavaStyle.toUpperCamelCase(method.name());
    TypeNode methodPageType = types.get(String.format("%sPage", upperJavaMethodName));
    TypeNode classExtendsType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(AbstractPagedListResponse.class)
                .setGenerics(
                    Arrays.asList(
                            method.inputType(),
                            method.outputType(),
                            repeatedResponseType,
                            methodPageType,
                            types.get(String.format("%sFixedSizeCollection", upperJavaMethodName)))
                        .stream()
                        .map(t -> t.reference())
                        .collect(Collectors.toList()))
                .build());

    // createAsync method - variables.
    VariableExpr contextVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setName("context")
                .setType(
                    TypeNode.withReference(
                        ConcreteReference.builder()
                            .setClazz(PageContext.class)
                            .setGenerics(
                                Arrays.asList(
                                        method.inputType(),
                                        method.outputType(),
                                        repeatedResponseType)
                                    .stream()
                                    .map(t -> t.reference())
                                    .collect(Collectors.toList()))
                            .build()))
                .build());
    VariableExpr futureResponseVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setName("futureResponse")
                .setType(
                    TypeNode.withReference(
                        ConcreteReference.builder()
                            .setClazz(ApiFuture.class)
                            .setGenerics(Arrays.asList(method.outputType().reference()))
                            .build()))
                .build());

    VariableExpr futurePageVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setName("futurePage")
                .setType(
                    TypeNode.withReference(
                        ConcreteReference.builder()
                            .setClazz(ApiFuture.class)
                            .setGenerics(Arrays.asList(methodPageType.reference()))
                            .build()))
                .build());

    // createAsync method - assignment expression.
    MethodInvocationExpr createPageAsyncExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(methodPageType)
            .setMethodName("createEmptyPage")
            .build();
    createPageAsyncExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(createPageAsyncExpr)
            .setMethodName("createPageAsync")
            .setArguments(contextVarExpr, futureResponseVarExpr)
            .setReturnType(futurePageVarExpr.type())
            .build();
    AssignmentExpr futurePageAssignExpr =
        AssignmentExpr.builder()
            .setVariableExpr(futurePageVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(createPageAsyncExpr)
            .build();

    // createAsync method - anonymous class expression.
    VariableExpr inputVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName("input").setType(methodPageType).build());
    TypeNode anonClassType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(ApiFunction.class)
                .setGenerics(Arrays.asList(methodPageType.reference(), thisClassType.reference()))
                .build());
    Expr pageToTransformExpr =
        AnonymousClassExpr.builder()
            .setType(anonClassType)
            .setMethods(
                Arrays.asList(
                    MethodDefinition.builder()
                        .setIsOverride(true)
                        .setScope(ScopeNode.PUBLIC)
                        .setReturnType(thisClassType)
                        .setName("apply")
                        .setArguments(inputVarExpr.toBuilder().setIsDecl(true).build())
                        .setReturnExpr(
                            NewObjectExpr.builder()
                                .setType(thisClassType)
                                .setArguments(inputVarExpr)
                                .build())
                        .build()))
            .build();

    // createAsync method - return expression.
    TypeNode returnType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(ApiFuture.class)
                .setGenerics(Arrays.asList(types.get(className).reference()))
                .build());
    Expr returnExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(types.get("ApiFutures"))
            .setMethodName("transform")
            .setArguments(
                futurePageVarExpr,
                pageToTransformExpr,
                MethodInvocationExpr.builder()
                    .setStaticReferenceType(types.get("MoreExecutors"))
                    .setMethodName("directExecutor")
                    .build())
            .setReturnType(returnType)
            .build();

    MethodDefinition createAsyncMethod =
        MethodDefinition.builder()
            .setScope(ScopeNode.PUBLIC)
            .setIsStatic(true)
            .setReturnType(returnType)
            .setName("createAsync")
            .setArguments(
                Arrays.asList(contextVarExpr, futureResponseVarExpr).stream()
                    .map(e -> e.toBuilder().setIsDecl(true).build())
                    .collect(Collectors.toList()))
            .setBody(Arrays.asList(ExprStatement.withExpr(futurePageAssignExpr)))
            .setReturnExpr(returnExpr)
            .build();

    // Private constructor.
    VariableExpr pageVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName("page").setType(methodPageType).build());
    MethodDefinition privateCtor =
        MethodDefinition.constructorBuilder()
            .setScope(ScopeNode.PRIVATE)
            .setReturnType(thisClassType)
            .setArguments(pageVarExpr.toBuilder().setIsDecl(true).build())
            .setBody(
                Arrays.asList(
                    ExprStatement.withExpr(
                        // Shortcut.
                        ReferenceConstructorExpr.superBuilder()
                            .setType(methodPageType)
                            .setArguments(
                                pageVarExpr,
                                MethodInvocationExpr.builder()
                                    .setStaticReferenceType(
                                        types.get(
                                            String.format(
                                                "%sFixedSizeCollection", upperJavaMethodName)))
                                    .setMethodName("createEmptyCollection")
                                    .build())
                            .build())))
            .build();

    List<MethodDefinition> javaMethods = new ArrayList<>();
    javaMethods.add(createAsyncMethod);
    javaMethods.add(privateCtor);

    return ClassDefinition.builder()
        .setIsNested(true)
        .setScope(ScopeNode.PUBLIC)
        .setIsStatic(true)
        .setExtendsType(classExtendsType)
        .setName(className)
        .setMethods(javaMethods)
        .build();
  }

  private static ClassDefinition createNestedRpcPageClass(
      Method method,
      TypeNode repeatedResponseType,
      Map<String, Message> messageTypes,
      Map<String, TypeNode> types) {
    Preconditions.checkState(
        method.isPaged(), String.format("Expected method %s to be paged", method.name()));

    String upperJavaMethodName = JavaStyle.toUpperCamelCase(method.name());
    String className = String.format("%sPage", upperJavaMethodName);
    TypeNode classType = types.get(className);
    TypeNode classExtendsType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(AbstractPage.class)
                .setGenerics(
                    Arrays.asList(
                            method.inputType(),
                            method.outputType(),
                            repeatedResponseType,
                            classType)
                        .stream()
                        .map(t -> t.reference())
                        .collect(Collectors.toList()))
                .build());

    // Private constructor.
    VariableExpr contextVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setName("context")
                .setType(
                    TypeNode.withReference(
                        ConcreteReference.builder()
                            .setClazz(PageContext.class)
                            .setGenerics(
                                Arrays.asList(
                                        method.inputType(),
                                        method.outputType(),
                                        repeatedResponseType)
                                    .stream()
                                    .map(t -> t.reference())
                                    .collect(Collectors.toList()))
                            .build()))
                .build());
    VariableExpr responseVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName("response").setType(method.outputType()).build());
    MethodDefinition privateCtor =
        MethodDefinition.constructorBuilder()
            .setScope(ScopeNode.PRIVATE)
            .setReturnType(classType)
            .setArguments(
                Arrays.asList(contextVarExpr, responseVarExpr).stream()
                    .map(e -> e.toBuilder().setIsDecl(true).build())
                    .collect(Collectors.toList()))
            .setBody(
                Arrays.asList(
                    ExprStatement.withExpr(
                        ReferenceConstructorExpr.superBuilder()
                            .setType(classExtendsType)
                            .setArguments(contextVarExpr, responseVarExpr)
                            .build())))
            .build();

    // createEmptyPage method.
    ValueExpr nullExpr = ValueExpr.createNullExpr();
    MethodDefinition createEmptyPageMethod =
        MethodDefinition.builder()
            .setScope(ScopeNode.PRIVATE)
            .setIsStatic(true)
            .setReturnType(classType)
            .setName("createEmptyPage")
            .setReturnExpr(
                NewObjectExpr.builder().setType(classType).setArguments(nullExpr, nullExpr).build())
            .build();

    // createPage method.
    MethodDefinition createPageMethod =
        MethodDefinition.builder()
            .setIsOverride(true)
            .setScope(ScopeNode.PROTECTED)
            .setReturnType(classType)
            .setName("createPage")
            .setArguments(
                Arrays.asList(contextVarExpr, responseVarExpr).stream()
                    .map(e -> e.toBuilder().setIsDecl(true).build())
                    .collect(Collectors.toList()))
            .setReturnExpr(
                NewObjectExpr.builder()
                    .setType(classType)
                    .setArguments(contextVarExpr, responseVarExpr)
                    .build())
            .build();

    // createPageAsync method.
    Function<TypeNode, TypeNode> futureTypeFn =
        t ->
            TypeNode.withReference(
                ConcreteReference.builder()
                    .setClazz(ApiFuture.class)
                    .setGenerics(Arrays.asList(t.reference()))
                    .build());
    VariableExpr futureResponseVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setName("futureResponse")
                .setType(futureTypeFn.apply(method.outputType()))
                .build());
    TypeNode futurePageType = futureTypeFn.apply(classType);
    MethodDefinition createPageAsyncMethod =
        MethodDefinition.builder()
            .setIsOverride(true)
            .setScope(ScopeNode.PUBLIC)
            .setReturnType(futurePageType)
            .setName("createPageAsync")
            .setArguments(
                Arrays.asList(contextVarExpr, futureResponseVarExpr).stream()
                    .map(e -> e.toBuilder().setIsDecl(true).build())
                    .collect(Collectors.toList()))
            .setReturnExpr(
                MethodInvocationExpr.builder()
                    .setExprReferenceExpr(
                        ValueExpr.withValue(SuperObjectValue.withType(classExtendsType)))
                    .setMethodName("createPageAsync")
                    .setArguments(contextVarExpr, futureResponseVarExpr)
                    .setReturnType(futurePageType)
                    .build())
            .build();

    // Build the class.
    List<MethodDefinition> javaMethods = new ArrayList<>();
    javaMethods.add(privateCtor);
    javaMethods.add(createEmptyPageMethod);
    javaMethods.add(createPageMethod);
    javaMethods.add(createPageAsyncMethod);

    return ClassDefinition.builder()
        .setIsNested(true)
        .setScope(ScopeNode.PUBLIC)
        .setIsStatic(true)
        .setExtendsType(classExtendsType)
        .setName(className)
        .setMethods(javaMethods)
        .build();
  }

  private static ClassDefinition createNestedRpcFixedSizeCollectionClass(
      Method method,
      TypeNode repeatedResponseType,
      Map<String, Message> messageTypes,
      Map<String, TypeNode> types) {
    String upperJavaMethodName = JavaStyle.toUpperCamelCase(method.name());
    String className = String.format("%sFixedSizeCollection", upperJavaMethodName);
    TypeNode classType = types.get(className);
    TypeNode methodPageType = types.get(String.format("%sPage", upperJavaMethodName));

    TypeNode classExtendsType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(AbstractFixedSizeCollection.class)
                .setGenerics(
                    Arrays.asList(
                            method.inputType(),
                            method.outputType(),
                            repeatedResponseType,
                            methodPageType,
                            classType)
                        .stream()
                        .map(t -> t.reference())
                        .collect(Collectors.toList()))
                .build());

    // Private constructor.
    VariableExpr pagesVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setName("pages")
                .setType(
                    TypeNode.withReference(
                        ConcreteReference.builder()
                            .setClazz(List.class)
                            .setGenerics(Arrays.asList(methodPageType.reference()))
                            .build()))
                .build());
    VariableExpr collectionSizeVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName("collectionSize").setType(TypeNode.INT).build());

    MethodDefinition privateCtor =
        MethodDefinition.constructorBuilder()
            .setScope(ScopeNode.PRIVATE)
            .setReturnType(classType)
            .setArguments(
                Arrays.asList(pagesVarExpr, collectionSizeVarExpr).stream()
                    .map(e -> e.toBuilder().setIsDecl(true).build())
                    .collect(Collectors.toList()))
            .setBody(
                Arrays.asList(
                    ExprStatement.withExpr(
                        ReferenceConstructorExpr.superBuilder()
                            .setType(classExtendsType)
                            .setArguments(pagesVarExpr, collectionSizeVarExpr)
                            .build())))
            .build();

    // createEmptyCollection method.
    MethodDefinition createEmptyCollectionMethod =
        MethodDefinition.builder()
            .setScope(ScopeNode.PRIVATE)
            .setIsStatic(true)
            .setReturnType(classType)
            .setName("createEmptyCollection")
            .setReturnExpr(
                NewObjectExpr.builder()
                    .setType(classType)
                    .setArguments(
                        ValueExpr.createNullExpr(),
                        ValueExpr.withValue(
                            PrimitiveValue.builder().setType(TypeNode.INT).setValue("0").build()))
                    .build())
            .build();

    // createCollection method.
    MethodDefinition createCollectionMethod =
        MethodDefinition.builder()
            .setIsOverride(true)
            .setScope(ScopeNode.PROTECTED)
            .setReturnType(classType)
            .setName("createCollection")
            .setArguments(
                Arrays.asList(pagesVarExpr, collectionSizeVarExpr).stream()
                    .map(e -> e.toBuilder().setIsDecl(true).build())
                    .collect(Collectors.toList()))
            .setReturnExpr(
                NewObjectExpr.builder()
                    .setType(classType)
                    .setArguments(pagesVarExpr, collectionSizeVarExpr)
                    .build())
            .build();

    List<MethodDefinition> javaMethods = new ArrayList<>();
    javaMethods.add(privateCtor);
    javaMethods.add(createEmptyCollectionMethod);
    javaMethods.add(createCollectionMethod);

    return ClassDefinition.builder()
        .setIsNested(true)
        .setScope(ScopeNode.PUBLIC)
        .setIsStatic(true)
        .setExtendsType(classExtendsType)
        .setName(className)
        .setMethods(javaMethods)
        .build();
  }

  @VisibleForTesting
  static Expr createRequestBuilderExpr(
      Method method, List<MethodArgument> signature, Map<String, TypeNode> types) {
    TypeNode methodInputType = method.inputType();
    MethodInvocationExpr newBuilderExpr =
        MethodInvocationExpr.builder()
            .setMethodName("newBuilder")
            .setStaticReferenceType(methodInputType)
            .build();
    for (MethodArgument argument : signature) {
      newBuilderExpr = buildNestedSetterInvocationExpr(argument, newBuilderExpr);
    }

    MethodInvocationExpr builderExpr =
        MethodInvocationExpr.builder()
            .setMethodName("build")
            .setExprReferenceExpr(newBuilderExpr)
            .setReturnType(methodInputType)
            .build();

    return builderExpr;
  }

  @VisibleForTesting
  static MethodInvocationExpr buildNestedSetterInvocationExpr(
      MethodArgument argument, Expr referenceExpr) {
    String argumentName = JavaStyle.toLowerCamelCase(argument.name());
    TypeNode argumentType = argument.type();

    Expr argVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName(argumentName).setType(argumentType).build());
    if (argument.isResourceNameHelper()) {
      Expr nullExpr = ValueExpr.createNullExpr();
      Expr isNullCheckExpr = RelationalOperationExpr.equalToWithExprs(argVarExpr, nullExpr);
      MethodInvocationExpr toStringExpr =
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(argVarExpr)
              .setMethodName("toString")
              .setReturnType(TypeNode.STRING)
              .build();
      argVarExpr =
          TernaryExpr.builder()
              .setConditionExpr(isNullCheckExpr)
              .setThenExpr(nullExpr)
              .setElseExpr(toStringExpr)
              .build();
    }

    String setterMethodName =
        String.format(
            typeToSetterMethodName(argumentType), JavaStyle.toUpperCamelCase(argumentName));
    return MethodInvocationExpr.builder()
        .setExprReferenceExpr(referenceExpr)
        .setMethodName(setterMethodName)
        .setArguments(argVarExpr)
        .build();
  }

  private static String typeToSetterMethodName(TypeNode type) {
    String setterMethodVariantPattern = "set%s";
    if (TypeNode.isReferenceType(type)) {
      if (LIST_REFERENCE.isSupertypeOrEquals(type.reference())) {
        setterMethodVariantPattern = "addAll%s";
      } else if (MAP_REFERENCE.isSupertypeOrEquals(type.reference())) {
        setterMethodVariantPattern = "putAll%s";
      }
    }
    return setterMethodVariantPattern;
  }

  private static Map<String, TypeNode> createTypes(
      Service service, Map<String, Message> messageTypes) {
    Map<String, TypeNode> types = new HashMap<>();
    types.putAll(createConcreteTypes());
    types.putAll(createVaporTypes(service));
    types.putAll(createProtoMessageTypes(service.pakkage(), messageTypes));
    return types;
  }

  private static Map<String, TypeNode> createConcreteTypes() {
    List<Class> concreteClazzes =
        Arrays.asList(
            AbstractPagedListResponse.class,
            ApiFunction.class,
            ApiFuture.class,
            ApiFutures.class,
            BackgroundResource.class,
            BetaApi.class,
            BidiStreamingCallable.class,
            ClientStreamingCallable.class,
            Generated.class,
            InterruptedException.class,
            IOException.class,
            MoreExecutors.class,
            Objects.class,
            Operation.class,
            OperationFuture.class,
            OperationCallable.class,
            ServerStreamingCallable.class,
            Status.class,
            Strings.class,
            TimeUnit.class,
            UnaryCallable.class);
    return concreteClazzes.stream()
        .collect(
            Collectors.toMap(
                c -> c.getSimpleName(),
                c -> TypeNode.withReference(ConcreteReference.withClazz(c))));
  }

  private static Map<String, TypeNode> createVaporTypes(Service service) {
    // Client stub types.
    Map<String, TypeNode> types =
        Arrays.asList(
                ClassNames.getServiceStubClassName(service),
                ClassNames.getServiceStubSettingsClassName(service))
            .stream()
            .collect(
                Collectors.toMap(
                    n -> n,
                    n ->
                        TypeNode.withReference(
                            VaporReference.builder()
                                .setName(n)
                                .setPakkage(String.format("%s.stub", service.pakkage()))
                                .build())));
    // Client ServiceClient and ServiceSettings types.
    types.putAll(
        Arrays.asList(
                ClassNames.getServiceClientClassName(service),
                ClassNames.getServiceSettingsClassName(service))
            .stream()
            .collect(
                Collectors.toMap(
                    n -> n,
                    n ->
                        TypeNode.withReference(
                            VaporReference.builder()
                                .setName(n)
                                .setPakkage(service.pakkage())
                                .build()))));

    // Nested class types.
    for (Method method : service.methods()) {
      if (!method.isPaged()) {
        continue;
      }
      types.putAll(
          Arrays.asList("%sPagedResponse", "%sPage", "%sFixedSizeCollection").stream()
              .collect(
                  Collectors.toMap(
                      t -> String.format(t, JavaStyle.toUpperCamelCase(method.name())),
                      t ->
                          TypeNode.withReference(
                              VaporReference.builder()
                                  .setName(
                                      String.format(t, JavaStyle.toUpperCamelCase(method.name())))
                                  .setEnclosingClassNames(
                                      ClassNames.getServiceClientClassName(service))
                                  .setPakkage(service.pakkage())
                                  .setIsStaticImport(true) // Same class, so they won't be imported.
                                  .build()))));
    }
    // LRO Gapic-generated types.
    types.put(
        "OperationsClient",
        TypeNode.withReference(
            VaporReference.builder()
                .setName("OperationsClient")
                .setPakkage("com.google.longrunning")
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
                                .setEnclosingClassNames(
                                    ClassNames.getServiceClientClassName(service))
                                .setIsStaticImport(true)
                                .build()))));
    return types;
  }

  private static Map<String, TypeNode> createProtoMessageTypes(
      String pakkage, Map<String, Message> messageTypes) {
    // Vapor message types.
    return messageTypes.entrySet().stream()
        // Short-term hack for messages that have nested subtypes with colliding names. This
        // should work as long as there isn't heavy usage of fully-qualified nested subtypes in
        // general. A long-term fix would involve adding a custom type-store that handles
        // fully-qualified types.
        .filter(e -> e.getValue().outerNestedTypes().isEmpty())
        .collect(
            Collectors.toMap(
                e -> e.getValue().name(),
                e ->
                    TypeNode.withReference(
                        VaporReference.builder()
                            .setName(e.getValue().name())
                            .setPakkage(pakkage)
                            .build())));
  }

  private static Variable createVariable(String name, TypeNode type) {
    return Variable.builder().setName(name).setType(type).build();
  }

  private static List<Reference> getGenericsForCallable(
      CallableMethodKind kind, Method method, Map<String, TypeNode> types) {
    if (kind.equals(CallableMethodKind.LRO)) {
      return Arrays.asList(
          method.inputType().reference(),
          method.lro().responseType().reference(),
          method.lro().metadataType().reference());
    }
    if (kind.equals(CallableMethodKind.PAGED)) {
      return Arrays.asList(
          method.inputType().reference(),
          types.get(String.format(PAGED_RESPONSE_TYPE_NAME_PATTERN, method.name())).reference());
    }
    return Arrays.asList(method.inputType().reference(), method.outputType().reference());
  }

  private static String getCallableName(CallableMethodKind kind, String rawMethodName) {
    if (kind.equals(CallableMethodKind.LRO)) {
      return String.format(OPERATION_CALLABLE_NAME_PATTERN, rawMethodName);
    }
    if (kind.equals(CallableMethodKind.PAGED)) {
      return String.format(PAGED_CALLABLE_NAME_PATTERN, rawMethodName);
    }
    return String.format(CALLABLE_NAME_PATTERN, rawMethodName);
  }

  private static boolean isProtoEmptyType(TypeNode type) {
    return type.reference().pakkage().equals("com.google.protobuf")
        && type.reference().name().equals("Empty");
  }
}
