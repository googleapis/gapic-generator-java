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

import com.google.api.gax.core.GoogleCredentialsProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GaxGrpcProperties;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.rpc.ApiClientHeaderProvider;
import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.api.gax.rpc.ClientStreamingCallable;
import com.google.api.gax.rpc.InvalidArgumentException;
import com.google.api.gax.rpc.OperationCallSettings;
import com.google.api.gax.rpc.PagedCallSettings;
import com.google.api.gax.rpc.ServerStreamingCallSettings;
import com.google.api.gax.rpc.ServerStreamingCallable;
import com.google.api.gax.rpc.StatusCode;
import com.google.api.gax.rpc.StreamingCallSettings;
import com.google.api.gax.rpc.UnaryCallSettings;
import com.google.api.generator.engine.ast.AnnotationNode;
import com.google.api.generator.engine.ast.AssignmentExpr;
import com.google.api.generator.engine.ast.CastExpr;
import com.google.api.generator.engine.ast.ClassDefinition;
import com.google.api.generator.engine.ast.CommentStatement;
import com.google.api.generator.engine.ast.ConcreteReference;
import com.google.api.generator.engine.ast.EmptyLineStatement;
import com.google.api.generator.engine.ast.EnumRefExpr;
import com.google.api.generator.engine.ast.Expr;
import com.google.api.generator.engine.ast.ExprStatement;
import com.google.api.generator.engine.ast.InstanceofExpr;
import com.google.api.generator.engine.ast.LineComment;
import com.google.api.generator.engine.ast.MethodDefinition;
import com.google.api.generator.engine.ast.MethodInvocationExpr;
import com.google.api.generator.engine.ast.NewObjectExpr;
import com.google.api.generator.engine.ast.PrimitiveValue;
import com.google.api.generator.engine.ast.Reference;
import com.google.api.generator.engine.ast.ScopeNode;
import com.google.api.generator.engine.ast.Statement;
import com.google.api.generator.engine.ast.StringObjectValue;
import com.google.api.generator.engine.ast.TryCatchStatement;
import com.google.api.generator.engine.ast.TypeNode;
import com.google.api.generator.engine.ast.ValueExpr;
import com.google.api.generator.engine.ast.VaporReference;
import com.google.api.generator.engine.ast.Variable;
import com.google.api.generator.engine.ast.VariableExpr;
import com.google.api.generator.gapic.composer.defaultvalue.DefaultValueComposer;
import com.google.api.generator.gapic.composer.store.TypeStore;
import com.google.api.generator.gapic.composer.utils.ClassNames;
import com.google.api.generator.gapic.model.Field;
import com.google.api.generator.gapic.model.GapicClass;
import com.google.api.generator.gapic.model.GapicClass.Kind;
import com.google.api.generator.gapic.model.Message;
import com.google.api.generator.gapic.model.Method;
import com.google.api.generator.gapic.model.MethodArgument;
import com.google.api.generator.gapic.model.ResourceName;
import com.google.api.generator.gapic.model.Service;
import com.google.api.generator.gapic.utils.JavaStyle;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.longrunning.Operation;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Any;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

// TODO(miraleung): Refactor classComposer.
public class ServiceClientTestClassComposer {
  private static final Statement EMPTY_LINE_STATEMENT = EmptyLineStatement.create();

  private static final String CHANNEL_PROVIDER_VAR_NAME = "channelProvider";
  private static final String CLASS_NAME_PATTERN = "%sClientTest";
  private static final String CLIENT_VAR_NAME = "client";
  private static final String GRPC_TESTING_PACKAGE = "com.google.api.gax.grpc.testing";
  private static final String MOCK_SERVICE_VAR_NAME_PATTERN = "mock%s";
  private static final String PAGED_RESPONSE_TYPE_NAME_PATTERN = "%sPagedResponse";
  private static final String SERVICE_HELPER_VAR_NAME = "mockServiceHelper";
  private static final String STUB_SETTINGS_PATTERN = "%sSettings";

  private static final ServiceClientTestClassComposer INSTANCE =
      new ServiceClientTestClassComposer();

  private static final TypeStore FIXED_TYPESTORE = createStaticTypes();
  private static final TypeNode LIST_TYPE =
      TypeNode.withReference(ConcreteReference.withClazz(List.class));
  private static final TypeNode MAP_TYPE =
      TypeNode.withReference(ConcreteReference.withClazz(Map.class));
  private static final TypeNode RESOURCE_NAME_TYPE =
      TypeNode.withReference(
          ConcreteReference.withClazz(com.google.api.resourcenames.ResourceName.class));

  private static final AnnotationNode TEST_ANNOTATION =
      AnnotationNode.withType(FIXED_TYPESTORE.get("Test"));
  // Avoid conflicting types with com.google.rpc.Status.
  private static final TypeNode GRPC_STATUS_TYPE =
      TypeNode.withReference(
          ConcreteReference.builder().setClazz(io.grpc.Status.class).setUseFullName(true).build());

  private ServiceClientTestClassComposer() {}

  public static ServiceClientTestClassComposer instance() {
    return INSTANCE;
  }

  public GapicClass generate(
      Service service, Map<String, ResourceName> resourceNames, Map<String, Message> messageTypes) {
    String pakkage = service.pakkage();
    TypeStore typeStore = createDynamicTypes(service);
    String className = ClassNames.getServiceClientTestClassName(service);
    GapicClass.Kind kind = Kind.MAIN;

    Map<String, VariableExpr> classMemberVarExprs = createClassMemberVarExprs(service, typeStore);

    ClassDefinition classDef =
        ClassDefinition.builder()
            .setPackageString(pakkage)
            .setAnnotations(createClassAnnotations())
            .setScope(ScopeNode.PUBLIC)
            .setName(className)
            .setStatements(createClassMemberFieldDecls(classMemberVarExprs))
            .setMethods(
                createClassMethods(
                    service, classMemberVarExprs, typeStore, resourceNames, messageTypes))
            .build();
    return GapicClass.create(kind, classDef);
  }

  private static List<AnnotationNode> createClassAnnotations() {
    return Arrays.asList(
        AnnotationNode.builder()
            .setType(FIXED_TYPESTORE.get("Generated"))
            .setDescription("by gapic-generator-java")
            .build());
  }

  private static Map<String, VariableExpr> createClassMemberVarExprs(
      Service service, TypeStore typeStore) {
    BiFunction<String, TypeNode, VariableExpr> varExprFn =
        (name, type) ->
            VariableExpr.withVariable(Variable.builder().setName(name).setType(type).build());
    Map<String, TypeNode> fields = new LinkedHashMap<>();
    fields.put(
        getMockServiceVarName(service), typeStore.get(ClassNames.getMockServiceClassName(service)));
    fields.put(SERVICE_HELPER_VAR_NAME, FIXED_TYPESTORE.get("MockServiceHelper"));
    fields.put(CLIENT_VAR_NAME, typeStore.get(ClassNames.getServiceClientClassName(service)));
    fields.put(CHANNEL_PROVIDER_VAR_NAME, FIXED_TYPESTORE.get("LocalChannelProvider"));
    return fields.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey(), e -> varExprFn.apply(e.getKey(), e.getValue())));
  }

  private static List<Statement> createClassMemberFieldDecls(
      Map<String, VariableExpr> classMemberVarExprs) {
    return classMemberVarExprs.values().stream()
        .map(
            v ->
                ExprStatement.withExpr(
                    v.toBuilder()
                        .setIsDecl(true)
                        .setScope(ScopeNode.PRIVATE)
                        .setIsStatic(v.type().reference().name().startsWith("Mock"))
                        .build()))
        .collect(Collectors.toList());
  }

  private static List<MethodDefinition> createClassMethods(
      Service service,
      Map<String, VariableExpr> classMemberVarExprs,
      TypeStore typeStore,
      Map<String, ResourceName> resourceNames,
      Map<String, Message> messageTypes) {
    List<MethodDefinition> javaMethods = new ArrayList<>();
    javaMethods.addAll(createTestAdminMethods(service, classMemberVarExprs, typeStore));
    javaMethods.addAll(
        createTestMethods(service, classMemberVarExprs, resourceNames, messageTypes));
    return javaMethods;
  }

  private static List<MethodDefinition> createTestAdminMethods(
      Service service, Map<String, VariableExpr> classMemberVarExprs, TypeStore typeStore) {
    List<MethodDefinition> javaMethods = new ArrayList<>();
    javaMethods.add(createStartStaticServerMethod(service, classMemberVarExprs));
    javaMethods.add(createStopServerMethod(service, classMemberVarExprs));
    javaMethods.add(createSetUpMethod(service, classMemberVarExprs, typeStore));
    javaMethods.add(createTearDownMethod(service, classMemberVarExprs));
    return javaMethods;
  }

  private static MethodDefinition createStartStaticServerMethod(
      Service service, Map<String, VariableExpr> classMemberVarExprs) {
    VariableExpr mockServiceVarExpr = classMemberVarExprs.get(getMockServiceVarName(service));
    VariableExpr serviceHelperVarExpr = classMemberVarExprs.get(SERVICE_HELPER_VAR_NAME);
    Expr initMockServiceExpr =
        AssignmentExpr.builder()
            .setVariableExpr(mockServiceVarExpr)
            .setValueExpr(NewObjectExpr.builder().setType(mockServiceVarExpr.type()).build())
            .build();

    MethodInvocationExpr firstArg =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(FIXED_TYPESTORE.get("UUID"))
            .setMethodName("randomUUID")
            .build();
    firstArg =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(firstArg)
            .setMethodName("toString")
            .build();

    MethodInvocationExpr secondArg =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(FIXED_TYPESTORE.get("Arrays"))
            .setGenerics(Arrays.asList(FIXED_TYPESTORE.get("MockGrpcService").reference()))
            .setMethodName("asList")
            .setArguments(Arrays.asList(mockServiceVarExpr))
            .build();

    Expr initServiceHelperExpr =
        AssignmentExpr.builder()
            .setVariableExpr(serviceHelperVarExpr)
            .setValueExpr(
                NewObjectExpr.builder()
                    .setType(serviceHelperVarExpr.type())
                    .setArguments(Arrays.asList(firstArg, secondArg))
                    .build())
            .build();

    Expr startServiceHelperExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(serviceHelperVarExpr)
            .setMethodName("start")
            .build();

    return MethodDefinition.builder()
        .setAnnotations(Arrays.asList(AnnotationNode.withType(FIXED_TYPESTORE.get("BeforeClass"))))
        .setScope(ScopeNode.PUBLIC)
        .setIsStatic(true)
        .setReturnType(TypeNode.VOID)
        .setName("startStaticServer")
        .setBody(
            Arrays.asList(initMockServiceExpr, initServiceHelperExpr, startServiceHelperExpr)
                .stream()
                .map(e -> ExprStatement.withExpr(e))
                .collect(Collectors.toList()))
        .build();
  }

  private static MethodDefinition createStopServerMethod(
      Service service, Map<String, VariableExpr> classMemberVarExprs) {
    return MethodDefinition.builder()
        .setAnnotations(Arrays.asList(AnnotationNode.withType(FIXED_TYPESTORE.get("AfterClass"))))
        .setScope(ScopeNode.PUBLIC)
        .setIsStatic(true)
        .setReturnType(TypeNode.VOID)
        .setName("stopServer")
        .setBody(
            Arrays.asList(
                ExprStatement.withExpr(
                    MethodInvocationExpr.builder()
                        .setExprReferenceExpr(classMemberVarExprs.get(SERVICE_HELPER_VAR_NAME))
                        .setMethodName("stop")
                        .build())))
        .build();
  }

  private static MethodDefinition createSetUpMethod(
      Service service, Map<String, VariableExpr> classMemberVarExprs, TypeStore typeStore) {
    VariableExpr clientVarExpr = classMemberVarExprs.get(CLIENT_VAR_NAME);
    VariableExpr serviceHelperVarExpr = classMemberVarExprs.get(SERVICE_HELPER_VAR_NAME);
    VariableExpr channelProviderVarExpr = classMemberVarExprs.get(CHANNEL_PROVIDER_VAR_NAME);

    Expr resetServiceHelperExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(serviceHelperVarExpr)
            .setMethodName("reset")
            .build();
    Expr channelProviderInitExpr =
        AssignmentExpr.builder()
            .setVariableExpr(channelProviderVarExpr)
            .setValueExpr(
                MethodInvocationExpr.builder()
                    .setExprReferenceExpr(serviceHelperVarExpr)
                    .setMethodName("createChannelProvider")
                    .setReturnType(channelProviderVarExpr.type())
                    .build())
            .build();

    TypeNode settingsType = typeStore.get(ClassNames.getServiceSettingsClassName(service));
    VariableExpr localSettingsVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setName("settings").setType(settingsType).build());

    Expr settingsBuilderExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(settingsType)
            .setMethodName("newBuilder")
            .build();
    Function<Expr, BiFunction<String, Expr, MethodInvocationExpr>> methodBuilderFn =
        methodExpr ->
            (mName, argExpr) ->
                MethodInvocationExpr.builder()
                    .setExprReferenceExpr(methodExpr)
                    .setMethodName(mName)
                    .setArguments(Arrays.asList(argExpr))
                    .build();
    settingsBuilderExpr =
        methodBuilderFn
            .apply(settingsBuilderExpr)
            .apply(
                "setTransportChannelProvider", classMemberVarExprs.get(CHANNEL_PROVIDER_VAR_NAME));
    settingsBuilderExpr =
        methodBuilderFn
            .apply(settingsBuilderExpr)
            .apply(
                "setCredentialsProvider",
                MethodInvocationExpr.builder()
                    .setStaticReferenceType(FIXED_TYPESTORE.get("NoCredentialsProvider"))
                    .setMethodName("create")
                    .build());
    settingsBuilderExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(settingsBuilderExpr)
            .setMethodName("build")
            .setReturnType(localSettingsVarExpr.type())
            .build();

    Expr initLocalSettingsExpr =
        AssignmentExpr.builder()
            .setVariableExpr(localSettingsVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(settingsBuilderExpr)
            .build();

    Expr initClientExpr =
        AssignmentExpr.builder()
            .setVariableExpr(clientVarExpr)
            .setValueExpr(
                MethodInvocationExpr.builder()
                    .setStaticReferenceType(
                        typeStore.get(ClassNames.getServiceClientClassName(service)))
                    .setMethodName("create")
                    .setArguments(Arrays.asList(localSettingsVarExpr))
                    .setReturnType(clientVarExpr.type())
                    .build())
            .build();

    return MethodDefinition.builder()
        .setAnnotations(Arrays.asList(AnnotationNode.withType(FIXED_TYPESTORE.get("Before"))))
        .setScope(ScopeNode.PUBLIC)
        .setReturnType(TypeNode.VOID)
        .setName("setUp")
        .setThrowsExceptions(Arrays.asList(FIXED_TYPESTORE.get("IOException")))
        .setBody(
            Arrays.asList(
                    resetServiceHelperExpr,
                    channelProviderInitExpr,
                    initLocalSettingsExpr,
                    initClientExpr)
                .stream()
                .map(e -> ExprStatement.withExpr(e))
                .collect(Collectors.toList()))
        .build();
  }

  private static MethodDefinition createTearDownMethod(
      Service service, Map<String, VariableExpr> classMemberVarExprs) {
    return MethodDefinition.builder()
        .setAnnotations(Arrays.asList(AnnotationNode.withType(FIXED_TYPESTORE.get("After"))))
        .setScope(ScopeNode.PUBLIC)
        .setReturnType(TypeNode.VOID)
        .setName("tearDown")
        .setThrowsExceptions(
            Arrays.asList(TypeNode.withReference(ConcreteReference.withClazz(Exception.class))))
        .setBody(
            Arrays.asList(
                ExprStatement.withExpr(
                    MethodInvocationExpr.builder()
                        .setExprReferenceExpr(classMemberVarExprs.get(CLIENT_VAR_NAME))
                        .setMethodName("close")
                        .build())))
        .build();
  }

  private static List<MethodDefinition> createTestMethods(
      Service service,
      Map<String, VariableExpr> classMemberVarExprs,
      Map<String, ResourceName> resourceNames,
      Map<String, Message> messageTypes) {
    List<MethodDefinition> javaMethods = new ArrayList<>();
    for (Method method : service.methods()) {
      // Ignore variants for streaming methods as well.
      if (method.methodSignatures().isEmpty() || !method.stream().equals(Method.Stream.NONE)) {
        javaMethods.add(
            createRpcTestMethod(
                method,
                service,
                Collections.emptyList(),
                0,
                true,
                classMemberVarExprs,
                resourceNames,
                messageTypes));
        javaMethods.add(
            createRpcExceptionTestMethod(
                method,
                service,
                Collections.emptyList(),
                0,
                classMemberVarExprs,
                resourceNames,
                messageTypes));
      } else {
        for (int i = 0; i < method.methodSignatures().size(); i++) {
          javaMethods.add(
              createRpcTestMethod(
                  method,
                  service,
                  method.methodSignatures().get(i),
                  i,
                  false,
                  classMemberVarExprs,
                  resourceNames,
                  messageTypes));
          javaMethods.add(
              createRpcExceptionTestMethod(
                  method,
                  service,
                  method.methodSignatures().get(i),
                  i,
                  classMemberVarExprs,
                  resourceNames,
                  messageTypes));
        }
      }
    }
    return javaMethods;
  }

  private static MethodDefinition createRpcTestMethod(
      Method method,
      Service service,
      List<MethodArgument> methodSignature,
      int variantIndex,
      boolean isRequestArg,
      Map<String, VariableExpr> classMemberVarExprs,
      Map<String, ResourceName> resourceNames,
      Map<String, Message> messageTypes) {
    if (!method.stream().equals(Method.Stream.NONE)) {
      return createStreamingRpcTestMethod(
          service, method, classMemberVarExprs, resourceNames, messageTypes);
    }
    // Construct the expected response.
    TypeNode methodOutputType = method.hasLro() ? method.lro().responseType() : method.outputType();
    List<Expr> methodExprs = new ArrayList<>();

    TypeNode repeatedResponseType = null;
    VariableExpr responsesElementVarExpr = null;
    if (method.isPaged()) {
      Message methodOutputMessage = messageTypes.get(method.outputType().reference().simpleName());
      Field repeatedPagedResultsField = methodOutputMessage.findAndUnwrapFirstRepeatedField();
      Preconditions.checkNotNull(
          repeatedPagedResultsField,
          String.format(
              "No repeated field found for paged method %s with output message type %s",
              method.name(), methodOutputMessage.name()));

      // Must be a non-repeated type.
      repeatedResponseType = repeatedPagedResultsField.type();
      responsesElementVarExpr =
          VariableExpr.withVariable(
              Variable.builder().setType(repeatedResponseType).setName("responsesElement").build());
      methodExprs.add(
          AssignmentExpr.builder()
              .setVariableExpr(responsesElementVarExpr.toBuilder().setIsDecl(true).build())
              .setValueExpr(
                  DefaultValueComposer.createDefaultValue(
                      Field.builder()
                          .setType(repeatedResponseType)
                          .setName("responsesElement")
                          .setIsMessage(!repeatedResponseType.isProtoPrimitiveType())
                          .build()))
              .build());
    }

    VariableExpr expectedResponseVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setType(methodOutputType).setName("expectedResponse").build());
    Expr expectedResponseValExpr = null;
    if (method.isPaged()) {
      Message methodOutputMessage = messageTypes.get(method.outputType().reference().simpleName());
      Field firstRepeatedField = methodOutputMessage.findAndUnwrapFirstRepeatedField();
      Preconditions.checkNotNull(
          firstRepeatedField,
          String.format(
              "Expected paged RPC %s to have a repeated field in the response %s but found none",
              method.name(), methodOutputMessage.name()));

      expectedResponseValExpr =
          DefaultValueComposer.createSimplePagedResponse(
              method.outputType(), firstRepeatedField.name(), responsesElementVarExpr);
    } else {
      if (messageTypes.containsKey(methodOutputType.reference().name())) {
        expectedResponseValExpr =
            DefaultValueComposer.createSimpleMessageBuilderExpr(
                messageTypes.get(methodOutputType.reference().simpleName()),
                resourceNames,
                messageTypes);
      } else {
        // Wrap this in a field so we don't have to split the helper into lots of different methods,
        // or duplicate it for VariableExpr.
        expectedResponseValExpr =
            DefaultValueComposer.createDefaultValue(
                Field.builder()
                    .setType(methodOutputType)
                    .setIsMessage(true)
                    .setName("expectedResponse")
                    .build());
      }
    }

    methodExprs.add(
        AssignmentExpr.builder()
            .setVariableExpr(expectedResponseVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(expectedResponseValExpr)
            .build());
    if (method.hasLro()) {
      VariableExpr resultOperationVarExpr =
          VariableExpr.withVariable(
              Variable.builder()
                  .setType(FIXED_TYPESTORE.get("Operation"))
                  .setName("resultOperation")
                  .build());
      methodExprs.add(
          AssignmentExpr.builder()
              .setVariableExpr(resultOperationVarExpr.toBuilder().setIsDecl(true).build())
              .setValueExpr(
                  DefaultValueComposer.createSimpleOperationBuilderExpr(
                      String.format("%sTest", JavaStyle.toLowerCamelCase(method.name())),
                      expectedResponseVarExpr))
              .build());
      methodExprs.add(
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(classMemberVarExprs.get(getMockServiceVarName(service)))
              .setMethodName("addResponse")
              .setArguments(resultOperationVarExpr)
              .build());
    } else {
      methodExprs.add(
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(classMemberVarExprs.get(getMockServiceVarName(service)))
              .setMethodName("addResponse")
              .setArguments(expectedResponseVarExpr)
              .build());
    }
    List<Statement> methodStatements = new ArrayList<>();
    methodStatements.addAll(
        methodExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()));
    methodExprs.clear();
    methodStatements.add(EMPTY_LINE_STATEMENT);

    // Construct the request or method arguments.
    VariableExpr requestVarExpr = null;
    Message requestMessage = null;
    List<VariableExpr> argExprs = new ArrayList<>();
    if (isRequestArg) {
      requestVarExpr =
          VariableExpr.withVariable(
              Variable.builder().setType(method.inputType()).setName("request").build());
      argExprs.add(requestVarExpr);
      requestMessage = messageTypes.get(method.inputType().reference().simpleName());
      Preconditions.checkNotNull(requestMessage);
      Expr valExpr =
          DefaultValueComposer.createSimpleMessageBuilderExpr(
              requestMessage, resourceNames, messageTypes);
      methodExprs.add(
          AssignmentExpr.builder()
              .setVariableExpr(requestVarExpr.toBuilder().setIsDecl(true).build())
              .setValueExpr(valExpr)
              .build());
    } else {
      for (MethodArgument methodArg : methodSignature) {
        String methodArgName = JavaStyle.toLowerCamelCase(methodArg.name());
        VariableExpr varExpr =
            VariableExpr.withVariable(
                Variable.builder().setType(methodArg.type()).setName(methodArgName).build());
        argExprs.add(varExpr);
        Expr valExpr = DefaultValueComposer.createDefaultValue(methodArg, resourceNames);
        methodExprs.add(
            AssignmentExpr.builder()
                .setVariableExpr(varExpr.toBuilder().setIsDecl(true).build())
                .setValueExpr(valExpr)
                .build());
      }
    }
    methodStatements.addAll(
        methodExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()));
    methodExprs.clear();
    methodStatements.add(EMPTY_LINE_STATEMENT);

    // Call the RPC Java method.
    VariableExpr actualResponseVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(
                    method.isPaged() ? getPagedResponseType(method, service) : methodOutputType)
                .setName(method.isPaged() ? "pagedListResponse" : "actualResponse")
                .build());
    Expr rpcJavaMethodInvocationExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(classMemberVarExprs.get("client"))
            .setMethodName(
                JavaStyle.toLowerCamelCase(method.name()) + (method.hasLro() ? "Async" : ""))
            .setArguments(argExprs.stream().map(e -> (Expr) e).collect(Collectors.toList()))
            .setReturnType(actualResponseVarExpr.type())
            .build();
    if (method.hasLro()) {
      rpcJavaMethodInvocationExpr =
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(rpcJavaMethodInvocationExpr)
              .setMethodName("get")
              .setReturnType(rpcJavaMethodInvocationExpr.type())
              .build();
    }

    boolean returnsVoid = isProtoEmptyType(methodOutputType);
    if (returnsVoid) {
      methodExprs.add(rpcJavaMethodInvocationExpr);
    } else {
      methodExprs.add(
          AssignmentExpr.builder()
              .setVariableExpr(actualResponseVarExpr.toBuilder().setIsDecl(true).build())
              .setValueExpr(rpcJavaMethodInvocationExpr)
              .build());
    }

    if (method.isPaged()) {
      // Assign the resources variable.
      VariableExpr resourcesVarExpr =
          VariableExpr.withVariable(
              Variable.builder()
                  .setType(
                      TypeNode.withReference(
                          ConcreteReference.builder()
                              .setClazz(List.class)
                              .setGenerics(Arrays.asList(repeatedResponseType.reference()))
                              .build()))
                  .setName("resources")
                  .build());
      Expr iterateAllExpr =
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(actualResponseVarExpr)
              .setMethodName("iterateAll")
              .build();
      Expr resourcesValExpr =
          MethodInvocationExpr.builder()
              .setStaticReferenceType(FIXED_TYPESTORE.get("Lists"))
              .setMethodName("newArrayList")
              .setArguments(iterateAllExpr)
              .setReturnType(resourcesVarExpr.type())
              .build();

      methodStatements.addAll(
          methodExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()));
      methodExprs.clear();
      methodStatements.add(EMPTY_LINE_STATEMENT);

      methodStatements.add(
          ExprStatement.withExpr(
              AssignmentExpr.builder()
                  .setVariableExpr(resourcesVarExpr.toBuilder().setIsDecl(true).build())
                  .setValueExpr(resourcesValExpr)
                  .build()));
      methodStatements.add(EMPTY_LINE_STATEMENT);

      // Assert the size is equivalent.
      methodExprs.add(
          MethodInvocationExpr.builder()
              .setStaticReferenceType(FIXED_TYPESTORE.get("Assert"))
              .setMethodName("assertEquals")
              .setArguments(
                  ValueExpr.withValue(
                      PrimitiveValue.builder().setType(TypeNode.INT).setValue("1").build()),
                  MethodInvocationExpr.builder()
                      .setExprReferenceExpr(resourcesVarExpr)
                      .setMethodName("size")
                      .setReturnType(TypeNode.INT)
                      .build())
              .build());

      // Assert the responses are equivalent.
      Message methodOutputMessage = messageTypes.get(method.outputType().reference().simpleName());
      Field repeatedPagedResultsField = methodOutputMessage.findAndUnwrapFirstRepeatedField();
      Preconditions.checkNotNull(
          repeatedPagedResultsField,
          String.format(
              "No repeated field found for paged method %s with output message type %s",
              method.name(), methodOutputMessage.name()));

      Expr zeroExpr =
          ValueExpr.withValue(PrimitiveValue.builder().setType(TypeNode.INT).setValue("0").build());
      Expr expectedPagedResponseExpr =
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(expectedResponseVarExpr)
              .setMethodName(
                  String.format(
                      "get%sList", JavaStyle.toUpperCamelCase(repeatedPagedResultsField.name())))
              .build();
      expectedPagedResponseExpr =
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(expectedPagedResponseExpr)
              .setMethodName("get")
              .setArguments(zeroExpr)
              .build();
      Expr actualPagedResponseExpr =
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(resourcesVarExpr)
              .setMethodName("get")
              .setArguments(zeroExpr)
              .build();

      methodExprs.add(
          MethodInvocationExpr.builder()
              .setStaticReferenceType(FIXED_TYPESTORE.get("Assert"))
              .setMethodName("assertEquals")
              .setArguments(expectedPagedResponseExpr, actualPagedResponseExpr)
              .build());
    } else if (!returnsVoid) {
      methodExprs.add(
          MethodInvocationExpr.builder()
              .setStaticReferenceType(FIXED_TYPESTORE.get("Assert"))
              .setMethodName("assertEquals")
              .setArguments(expectedResponseVarExpr, actualResponseVarExpr)
              .build());
    }
    methodStatements.addAll(
        methodExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()));
    methodExprs.clear();
    methodStatements.add(EMPTY_LINE_STATEMENT);

    // Construct the request checker logic.
    VariableExpr actualRequestsVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(
                    TypeNode.withReference(
                        ConcreteReference.builder()
                            .setClazz(List.class)
                            .setGenerics(
                                Arrays.asList(ConcreteReference.withClazz(AbstractMessage.class)))
                            .build()))
                .setName("actualRequests")
                .build());
    methodExprs.add(
        AssignmentExpr.builder()
            .setVariableExpr(actualRequestsVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(
                MethodInvocationExpr.builder()
                    .setExprReferenceExpr(classMemberVarExprs.get(getMockServiceVarName(service)))
                    .setMethodName("getRequests")
                    .setReturnType(actualRequestsVarExpr.type())
                    .build())
            .build());

    methodExprs.add(
        MethodInvocationExpr.builder()
            .setStaticReferenceType(FIXED_TYPESTORE.get("Assert"))
            .setMethodName("assertEquals")
            .setArguments(
                ValueExpr.withValue(
                    PrimitiveValue.builder().setType(TypeNode.INT).setValue("1").build()),
                MethodInvocationExpr.builder()
                    .setExprReferenceExpr(actualRequestsVarExpr)
                    .setMethodName("size")
                    .build())
            .build());

    VariableExpr actualRequestVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setType(method.inputType()).setName("actualRequest").build());
    Expr getFirstRequestExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(actualRequestsVarExpr)
            .setMethodName("get")
            .setArguments(
                ValueExpr.withValue(
                    PrimitiveValue.builder().setType(TypeNode.INT).setValue("0").build()))
            .setReturnType(FIXED_TYPESTORE.get("AbstractMessage"))
            .build();
    getFirstRequestExpr =
        CastExpr.builder().setType(method.inputType()).setExpr(getFirstRequestExpr).build();
    methodExprs.add(
        AssignmentExpr.builder()
            .setVariableExpr(actualRequestVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(getFirstRequestExpr)
            .build());
    methodStatements.addAll(
        methodExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()));
    methodExprs.clear();
    methodStatements.add(EMPTY_LINE_STATEMENT);

    // Assert field equality.
    if (isRequestArg) {
      // TODO(miraleung): Replace these with a simple request object equals?
      Preconditions.checkNotNull(requestVarExpr);
      Preconditions.checkNotNull(requestMessage);
      for (Field field : requestMessage.fields()) {
        String fieldGetterMethodNamePatternTemp = "get%s";
        if (field.isRepeated()) {
          fieldGetterMethodNamePatternTemp = field.isMap() ? "get%sMap" : "get%sList";
        }
        final String fieldGetterMethodNamePattern = fieldGetterMethodNamePatternTemp;
        Function<VariableExpr, Expr> checkExprFn =
            v ->
                MethodInvocationExpr.builder()
                    .setExprReferenceExpr(v)
                    .setMethodName(
                        String.format(
                            fieldGetterMethodNamePattern, JavaStyle.toUpperCamelCase(field.name())))
                    .build();
        Expr expectedFieldExpr = checkExprFn.apply(requestVarExpr);
        Expr actualFieldExpr = checkExprFn.apply(actualRequestVarExpr);
        List<Expr> assertEqualsArguments = new ArrayList<>();
        assertEqualsArguments.add(expectedFieldExpr);
        assertEqualsArguments.add(actualFieldExpr);
        if (TypeNode.isFloatingPointType(field.type())) {
          boolean isFloat = field.type().equals(TypeNode.FLOAT);
          assertEqualsArguments.add(
              ValueExpr.withValue(
                  PrimitiveValue.builder()
                      .setType(isFloat ? TypeNode.FLOAT : TypeNode.DOUBLE)
                      .setValue(String.format("0.0001%s", isFloat ? "f" : ""))
                      .build()));
        }
        methodExprs.add(
            MethodInvocationExpr.builder()
                .setStaticReferenceType(FIXED_TYPESTORE.get("Assert"))
                .setMethodName("assertEquals")
                .setArguments(assertEqualsArguments)
                .build());
      }
    } else {
      for (VariableExpr argVarExpr : argExprs) {
        Variable variable = argVarExpr.variable();
        String fieldGetterMethodNamePattern = "get%s";
        if (LIST_TYPE.isSupertypeOrEquals(variable.type())) {
          fieldGetterMethodNamePattern = "get%sList";
        } else if (MAP_TYPE.isSupertypeOrEquals(variable.type())) {
          fieldGetterMethodNamePattern = "get%sMap";
        }
        Expr actualFieldExpr =
            MethodInvocationExpr.builder()
                .setExprReferenceExpr(actualRequestVarExpr)
                .setMethodName(
                    String.format(
                        fieldGetterMethodNamePattern,
                        JavaStyle.toUpperCamelCase(variable.identifier().name())))
                .build();
        Expr expectedFieldExpr = argVarExpr;
        if (RESOURCE_NAME_TYPE.isSupertypeOrEquals(argVarExpr.type())) {
          expectedFieldExpr =
              MethodInvocationExpr.builder()
                  .setExprReferenceExpr(argVarExpr)
                  .setMethodName("toString")
                  .build();
        }
        methodExprs.add(
            MethodInvocationExpr.builder()
                .setStaticReferenceType(FIXED_TYPESTORE.get("Assert"))
                .setMethodName("assertEquals")
                .setArguments(expectedFieldExpr, actualFieldExpr)
                .build());
      }
    }

    // Assert header equality.
    Expr headerKeyExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(FIXED_TYPESTORE.get("ApiClientHeaderProvider"))
            .setMethodName("getDefaultApiClientHeaderKey")
            .build();
    Expr headerPatternExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(FIXED_TYPESTORE.get("GaxGrpcProperties"))
            .setMethodName("getDefaultApiClientHeaderPattern")
            .build();
    Expr headerSentExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(classMemberVarExprs.get("channelProvider"))
            .setMethodName("isHeaderSent")
            .setArguments(headerKeyExpr, headerPatternExpr)
            .build();
    methodExprs.add(
        MethodInvocationExpr.builder()
            .setStaticReferenceType(FIXED_TYPESTORE.get("Assert"))
            .setMethodName("assertTrue")
            .setArguments(headerSentExpr)
            .build());
    methodStatements.addAll(
        methodExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()));
    methodExprs.clear();

    String testMethodName =
        String.format(
            "%sTest%s",
            JavaStyle.toLowerCamelCase(method.name()), variantIndex > 0 ? variantIndex + 1 : "");

    return MethodDefinition.builder()
        .setAnnotations(Arrays.asList(TEST_ANNOTATION))
        .setScope(ScopeNode.PUBLIC)
        .setReturnType(TypeNode.VOID)
        .setName(testMethodName)
        .setThrowsExceptions(Arrays.asList(TypeNode.withExceptionClazz(Exception.class)))
        .setBody(methodStatements)
        .build();
  }

  private static MethodDefinition createStreamingRpcTestMethod(
      Service service,
      Method method,
      Map<String, VariableExpr> classMemberVarExprs,
      Map<String, ResourceName> resourceNames,
      Map<String, Message> messageTypes) {
    TypeNode methodOutputType = method.hasLro() ? method.lro().responseType() : method.outputType();
    List<Expr> methodExprs = new ArrayList<>();
    VariableExpr expectedResponseVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setType(methodOutputType).setName("expectedResponse").build());
    Expr expectedResponseValExpr = null;
    if (messageTypes.containsKey(methodOutputType.reference().name())) {
      expectedResponseValExpr =
          DefaultValueComposer.createSimpleMessageBuilderExpr(
              messageTypes.get(methodOutputType.reference().simpleName()),
              resourceNames,
              messageTypes);
    } else {
      // Wrap this in a field so we don't have to split the helper into lots of different methods,
      // or duplicate it for VariableExpr.
      expectedResponseValExpr =
          DefaultValueComposer.createDefaultValue(
              Field.builder()
                  .setType(methodOutputType)
                  .setIsMessage(true)
                  .setName("expectedResponse")
                  .build());
    }
    methodExprs.add(
        AssignmentExpr.builder()
            .setVariableExpr(expectedResponseVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(expectedResponseValExpr)
            .build());
    if (method.hasLro()) {
      VariableExpr resultOperationVarExpr =
          VariableExpr.withVariable(
              Variable.builder()
                  .setType(FIXED_TYPESTORE.get("Operation"))
                  .setName("resultOperation")
                  .build());
      methodExprs.add(
          AssignmentExpr.builder()
              .setVariableExpr(resultOperationVarExpr.toBuilder().setIsDecl(true).build())
              .setValueExpr(
                  DefaultValueComposer.createSimpleOperationBuilderExpr(
                      String.format("%sTest", JavaStyle.toLowerCamelCase(method.name())),
                      expectedResponseVarExpr))
              .build());
      methodExprs.add(
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(classMemberVarExprs.get(getMockServiceVarName(service)))
              .setMethodName("addResponse")
              .setArguments(resultOperationVarExpr)
              .build());
    } else {
      methodExprs.add(
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(classMemberVarExprs.get(getMockServiceVarName(service)))
              .setMethodName("addResponse")
              .setArguments(expectedResponseVarExpr)
              .build());
    }

    // Construct the request or method arguments.
    VariableExpr requestVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setType(method.inputType()).setName("request").build());
    Message requestMessage = messageTypes.get(method.inputType().reference().simpleName());
    Preconditions.checkNotNull(requestMessage);
    Expr valExpr =
        DefaultValueComposer.createSimpleMessageBuilderExpr(
            requestMessage, resourceNames, messageTypes);
    methodExprs.add(
        AssignmentExpr.builder()
            .setVariableExpr(requestVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(valExpr)
            .build());

    List<Statement> methodStatements = new ArrayList<>();
    methodStatements.addAll(
        methodExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()));
    methodExprs.clear();
    methodStatements.add(EMPTY_LINE_STATEMENT);

    // Construct the mock stream observer.
    VariableExpr responseObserverVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(
                    TypeNode.withReference(
                        FIXED_TYPESTORE
                            .get("MockStreamObserver")
                            .reference()
                            .copyAndSetGenerics(Arrays.asList(method.outputType().reference()))))
                .setName("responseObserver")
                .build());

    methodStatements.add(
        ExprStatement.withExpr(
            AssignmentExpr.builder()
                .setVariableExpr(responseObserverVarExpr.toBuilder().setIsDecl(true).build())
                .setValueExpr(
                    NewObjectExpr.builder()
                        .setType(FIXED_TYPESTORE.get("MockStreamObserver"))
                        .setIsGeneric(true)
                        .build())
                .build()));
    methodStatements.add(EMPTY_LINE_STATEMENT);

    // Build the callable variable and assign it.
    VariableExpr callableVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setType(getCallableType(method)).setName("callable").build());
    Expr streamingCallExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(classMemberVarExprs.get("client"))
            .setMethodName(String.format("%sCallable", JavaStyle.toLowerCamelCase(method.name())))
            .setReturnType(callableVarExpr.type())
            .build();
    methodExprs.add(
        AssignmentExpr.builder()
            .setVariableExpr(callableVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(streamingCallExpr)
            .build());

    // Call the streaming-variant callable method.
    if (method.stream().equals(Method.Stream.SERVER)) {
      methodExprs.add(
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(callableVarExpr)
              .setMethodName("serverStreamingCall")
              .setArguments(requestVarExpr, responseObserverVarExpr)
              .build());
    } else {
      VariableExpr requestObserverVarExpr =
          VariableExpr.withVariable(
              Variable.builder()
                  .setType(
                      TypeNode.withReference(
                          FIXED_TYPESTORE
                              .get("ApiStreamObserver")
                              .reference()
                              .copyAndSetGenerics(Arrays.asList(method.inputType().reference()))))
                  .setName("requestObserver")
                  .build());
      List<Expr> callableMethodArgs = new ArrayList<>();
      if (!method.stream().equals(Method.Stream.BIDI)
          && !method.stream().equals(Method.Stream.CLIENT)) {
        callableMethodArgs.add(requestVarExpr);
      }
      callableMethodArgs.add(responseObserverVarExpr);
      methodExprs.add(
          AssignmentExpr.builder()
              .setVariableExpr(requestObserverVarExpr.toBuilder().setIsDecl(true).build())
              .setValueExpr(
                  MethodInvocationExpr.builder()
                      .setExprReferenceExpr(callableVarExpr)
                      .setMethodName(getCallableMethodName(method))
                      .setArguments(callableMethodArgs)
                      .setReturnType(requestObserverVarExpr.type())
                      .build())
              .build());

      methodStatements.addAll(
          methodExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()));
      methodExprs.clear();
      methodStatements.add(EMPTY_LINE_STATEMENT);

      methodExprs.add(
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(requestObserverVarExpr)
              .setMethodName("onNext")
              .setArguments(requestVarExpr)
              .build());
      methodExprs.add(
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(requestObserverVarExpr)
              .setMethodName("onCompleted")
              .build());
    }
    methodStatements.addAll(
        methodExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()));
    methodExprs.clear();
    methodStatements.add(EMPTY_LINE_STATEMENT);

    // Check the actual responses.
    VariableExpr actualResponsesVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(
                    TypeNode.withReference(
                        ConcreteReference.builder()
                            .setClazz(List.class)
                            .setGenerics(Arrays.asList(method.outputType().reference()))
                            .build()))
                .setName("actualResponses")
                .build());

    Expr getFutureResponseExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(responseObserverVarExpr)
            .setMethodName("future")
            .build();
    getFutureResponseExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(getFutureResponseExpr)
            .setMethodName("get")
            .setReturnType(actualResponsesVarExpr.type())
            .build();
    methodExprs.add(
        AssignmentExpr.builder()
            .setVariableExpr(actualResponsesVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(getFutureResponseExpr)
            .build());

    // Assert the size is equivalent.
    methodExprs.add(
        MethodInvocationExpr.builder()
            .setStaticReferenceType(FIXED_TYPESTORE.get("Assert"))
            .setMethodName("assertEquals")
            .setArguments(
                ValueExpr.withValue(
                    PrimitiveValue.builder().setType(TypeNode.INT).setValue("1").build()),
                MethodInvocationExpr.builder()
                    .setExprReferenceExpr(actualResponsesVarExpr)
                    .setMethodName("size")
                    .setReturnType(TypeNode.INT)
                    .build())
            .build());

    // Assert the responses are equivalent.
    Expr zeroExpr =
        ValueExpr.withValue(PrimitiveValue.builder().setType(TypeNode.INT).setValue("0").build());
    Expr actualResponseExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(actualResponsesVarExpr)
            .setMethodName("get")
            .setArguments(zeroExpr)
            .build();

    methodExprs.add(
        MethodInvocationExpr.builder()
            .setStaticReferenceType(FIXED_TYPESTORE.get("Assert"))
            .setMethodName("assertEquals")
            .setArguments(expectedResponseVarExpr, actualResponseExpr)
            .build());

    methodStatements.addAll(
        methodExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()));
    methodExprs.clear();
    methodStatements.add(EMPTY_LINE_STATEMENT);

    String testMethodName = String.format("%sTest", JavaStyle.toLowerCamelCase(method.name()));
    return MethodDefinition.builder()
        .setAnnotations(Arrays.asList(TEST_ANNOTATION))
        .setScope(ScopeNode.PUBLIC)
        .setReturnType(TypeNode.VOID)
        .setName(testMethodName)
        .setThrowsExceptions(Arrays.asList(TypeNode.withExceptionClazz(Exception.class)))
        .setBody(methodStatements)
        .build();
  }

  // TODO(imraleung): Reorder params.
  private static MethodDefinition createRpcExceptionTestMethod(
      Method method,
      Service service,
      List<MethodArgument> methodSignature,
      int variantIndex,
      Map<String, VariableExpr> classMemberVarExprs,
      Map<String, ResourceName> resourceNames,
      Map<String, Message> messageTypes) {
    VariableExpr exceptionVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(FIXED_TYPESTORE.get("StatusRuntimeException"))
                .setName("exception")
                .build());

    // First two assignment lines.
    Expr exceptionAssignExpr =
        AssignmentExpr.builder()
            .setVariableExpr(exceptionVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(
                NewObjectExpr.builder()
                    .setType(FIXED_TYPESTORE.get("StatusRuntimeException"))
                    .setArguments(
                        EnumRefExpr.builder()
                            .setType(GRPC_STATUS_TYPE)
                            .setName("INVALID_ARGUMENT")
                            .build())
                    .build())
            .build();
    Expr addExceptionExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(classMemberVarExprs.get(getMockServiceVarName(service)))
            .setMethodName("addException")
            .setArguments(exceptionVarExpr)
            .build();

    // Try-catch block. Build the method call.
    String exceptionTestMethodName =
        String.format(
            "%sExceptionTest%s",
            JavaStyle.toLowerCamelCase(method.name()), variantIndex > 0 ? variantIndex + 1 : "");

    boolean isStreaming = !method.stream().equals(Method.Stream.NONE);
    List<Statement> methodBody = new ArrayList<>();
    methodBody.add(ExprStatement.withExpr(exceptionAssignExpr));
    methodBody.add(ExprStatement.withExpr(addExceptionExpr));
    if (isStreaming) {
      methodBody.addAll(
          createStreamingRpcExceptionTestStatements(
              method, classMemberVarExprs, resourceNames, messageTypes));
    } else {
      methodBody.addAll(
          createRpcExceptionTestStatements(
              method, methodSignature, classMemberVarExprs, resourceNames, messageTypes));
    }

    return MethodDefinition.builder()
        .setAnnotations(Arrays.asList(TEST_ANNOTATION))
        .setScope(ScopeNode.PUBLIC)
        .setReturnType(TypeNode.VOID)
        .setName(exceptionTestMethodName)
        .setThrowsExceptions(Arrays.asList(TypeNode.withExceptionClazz(Exception.class)))
        .setBody(methodBody)
        .build();
  }

  private static List<Statement> createStreamingRpcExceptionTestStatements(
      Method method,
      Map<String, VariableExpr> classMemberVarExprs,
      Map<String, ResourceName> resourceNames,
      Map<String, Message> messageTypes) {
    // Build the request variable and assign it.
    VariableExpr requestVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setType(method.inputType()).setName("request").build());
    Message requestMessage = messageTypes.get(method.inputType().reference().simpleName());
    Preconditions.checkNotNull(requestMessage);
    Expr valExpr =
        DefaultValueComposer.createSimpleMessageBuilderExpr(
            requestMessage, resourceNames, messageTypes);

    List<Statement> statements = new ArrayList<>();
    statements.add(
        ExprStatement.withExpr(
            AssignmentExpr.builder()
                .setVariableExpr(requestVarExpr.toBuilder().setIsDecl(true).build())
                .setValueExpr(valExpr)
                .build()));
    statements.add(EMPTY_LINE_STATEMENT);

    // Build the responseObserver variable.
    VariableExpr responseObserverVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(
                    TypeNode.withReference(
                        FIXED_TYPESTORE
                            .get("MockStreamObserver")
                            .reference()
                            .copyAndSetGenerics(Arrays.asList(method.outputType().reference()))))
                .setName("responseObserver")
                .build());

    statements.add(
        ExprStatement.withExpr(
            AssignmentExpr.builder()
                .setVariableExpr(responseObserverVarExpr.toBuilder().setIsDecl(true).build())
                .setValueExpr(
                    NewObjectExpr.builder()
                        .setType(FIXED_TYPESTORE.get("MockStreamObserver"))
                        .setIsGeneric(true)
                        .build())
                .build()));
    statements.add(EMPTY_LINE_STATEMENT);

    // Build the callable variable and assign it.
    VariableExpr callableVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setType(getCallableType(method)).setName("callable").build());
    Expr streamingCallExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(classMemberVarExprs.get("client"))
            .setMethodName(String.format("%sCallable", JavaStyle.toLowerCamelCase(method.name())))
            .setReturnType(callableVarExpr.type())
            .build();

    List<Expr> exprs = new ArrayList<>();
    exprs.add(
        AssignmentExpr.builder()
            .setVariableExpr(callableVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(streamingCallExpr)
            .build());

    if (method.stream().equals(Method.Stream.SERVER)) {
      exprs.add(
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(callableVarExpr)
              .setMethodName("serverStreamingCall")
              .setArguments(requestVarExpr, responseObserverVarExpr)
              .build());
    } else {
      // Call the streaming-variant callable method.
      VariableExpr requestObserverVarExpr =
          VariableExpr.withVariable(
              Variable.builder()
                  .setType(
                      TypeNode.withReference(
                          FIXED_TYPESTORE
                              .get("ApiStreamObserver")
                              .reference()
                              .copyAndSetGenerics(Arrays.asList(method.inputType().reference()))))
                  .setName("requestObserver")
                  .build());

      List<Expr> callableMethodArgs = new ArrayList<>();
      if (!method.stream().equals(Method.Stream.BIDI)
          && !method.stream().equals(Method.Stream.CLIENT)) {
        callableMethodArgs.add(requestVarExpr);
      }
      callableMethodArgs.add(responseObserverVarExpr);
      exprs.add(
          AssignmentExpr.builder()
              .setVariableExpr(requestObserverVarExpr.toBuilder().setIsDecl(true).build())
              .setValueExpr(
                  MethodInvocationExpr.builder()
                      .setExprReferenceExpr(callableVarExpr)
                      .setMethodName(getCallableMethodName(method))
                      .setArguments(callableMethodArgs)
                      .setReturnType(requestObserverVarExpr.type())
                      .build())
              .build());

      statements.addAll(
          exprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()));
      exprs.clear();
      statements.add(EMPTY_LINE_STATEMENT);

      exprs.add(
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(requestObserverVarExpr)
              .setMethodName("onNext")
              .setArguments(requestVarExpr)
              .build());
    }
    statements.addAll(
        exprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()));
    exprs.clear();
    statements.add(EMPTY_LINE_STATEMENT);

    List<Expr> tryBodyExprs = new ArrayList<>();
    // TODO(v2): This variable is unused in the generated test, it can be deleted.
    VariableExpr actualResponsesVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(
                    TypeNode.withReference(
                        ConcreteReference.builder()
                            .setClazz(List.class)
                            .setGenerics(Arrays.asList(method.outputType().reference()))
                            .build()))
                .setName("actualResponses")
                .build());

    Expr getFutureResponseExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(responseObserverVarExpr)
            .setMethodName("future")
            .build();
    getFutureResponseExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(getFutureResponseExpr)
            .setMethodName("get")
            .setReturnType(actualResponsesVarExpr.type())
            .build();
    tryBodyExprs.add(
        AssignmentExpr.builder()
            .setVariableExpr(actualResponsesVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(getFutureResponseExpr)
            .build());
    // Assert a failure if no exception was raised.
    tryBodyExprs.add(
        MethodInvocationExpr.builder()
            .setStaticReferenceType(FIXED_TYPESTORE.get("Assert"))
            .setMethodName("fail")
            .setArguments(ValueExpr.withValue(StringObjectValue.withValue("No exception thrown")))
            .build());

    VariableExpr catchExceptionVarExpr =
        VariableExpr.builder()
            .setVariable(
                Variable.builder()
                    .setType(TypeNode.withExceptionClazz(ExecutionException.class))
                    .setName("e")
                    .build())
            .build();

    TryCatchStatement tryCatchBlock =
        TryCatchStatement.builder()
            .setTryBody(
                tryBodyExprs.stream()
                    .map(e -> ExprStatement.withExpr(e))
                    .collect(Collectors.toList()))
            .setCatchVariableExpr(catchExceptionVarExpr.toBuilder().setIsDecl(true).build())
            .setCatchBody(createRpcLroExceptionTestCatchBody(catchExceptionVarExpr, true))
            .build();

    statements.add(tryCatchBlock);
    return statements;
  }

  private static List<Statement> createRpcExceptionTestStatements(
      Method method,
      List<MethodArgument> methodSignature,
      Map<String, VariableExpr> classMemberVarExprs,
      Map<String, ResourceName> resourceNames,
      Map<String, Message> messageTypes) {
    List<VariableExpr> argVarExprs = new ArrayList<>();
    List<Expr> tryBodyExprs = new ArrayList<>();
    if (methodSignature.isEmpty()) {
      // Construct the actual request.
      VariableExpr varExpr =
          VariableExpr.withVariable(
              Variable.builder().setType(method.inputType()).setName("request").build());
      argVarExprs.add(varExpr);
      Message requestMessage = messageTypes.get(method.inputType().reference().simpleName());
      Preconditions.checkNotNull(requestMessage);
      Expr valExpr =
          DefaultValueComposer.createSimpleMessageBuilderExpr(
              requestMessage, resourceNames, messageTypes);
      tryBodyExprs.add(
          AssignmentExpr.builder()
              .setVariableExpr(varExpr.toBuilder().setIsDecl(true).build())
              .setValueExpr(valExpr)
              .build());
    } else {
      for (MethodArgument methodArg : methodSignature) {
        String methodArgName = JavaStyle.toLowerCamelCase(methodArg.name());
        VariableExpr varExpr =
            VariableExpr.withVariable(
                Variable.builder().setType(methodArg.type()).setName(methodArgName).build());
        argVarExprs.add(varExpr);
        Expr valExpr = DefaultValueComposer.createDefaultValue(methodArg, resourceNames);
        tryBodyExprs.add(
            AssignmentExpr.builder()
                .setVariableExpr(varExpr.toBuilder().setIsDecl(true).build())
                .setValueExpr(valExpr)
                .build());
      }
    }
    String rpcJavaName = JavaStyle.toLowerCamelCase(method.name());
    if (method.hasLro()) {
      rpcJavaName += "Async";
    }
    MethodInvocationExpr rpcJavaMethodInvocationExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(classMemberVarExprs.get("client"))
            .setMethodName(rpcJavaName)
            .setArguments(argVarExprs.stream().map(e -> (Expr) e).collect(Collectors.toList()))
            .build();
    if (method.hasLro()) {
      rpcJavaMethodInvocationExpr =
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(rpcJavaMethodInvocationExpr)
              .setMethodName("get")
              .build();
    }
    tryBodyExprs.add(rpcJavaMethodInvocationExpr);

    VariableExpr catchExceptionVarExpr =
        VariableExpr.builder()
            .setVariable(
                Variable.builder()
                    .setType(
                        TypeNode.withExceptionClazz(
                            method.hasLro()
                                ? ExecutionException.class
                                : InvalidArgumentException.class))
                    .setName("e")
                    .build())
            .build();

    List<Statement> catchBody =
        method.hasLro()
            ? createRpcLroExceptionTestCatchBody(catchExceptionVarExpr, false)
            : Arrays.asList(
                CommentStatement.withComment(LineComment.withComment("Expected exception.")));
    // Assert a failure if no exception was raised.
    tryBodyExprs.add(
        MethodInvocationExpr.builder()
            .setStaticReferenceType(FIXED_TYPESTORE.get("Assert"))
            .setMethodName("fail")
            .setArguments(ValueExpr.withValue(StringObjectValue.withValue("No exception raised")))
            .build());

    TryCatchStatement tryCatchBlock =
        TryCatchStatement.builder()
            .setTryBody(
                tryBodyExprs.stream()
                    .map(e -> ExprStatement.withExpr(e))
                    .collect(Collectors.toList()))
            .setCatchVariableExpr(catchExceptionVarExpr.toBuilder().setIsDecl(true).build())
            .setCatchBody(catchBody)
            .build();

    return Arrays.asList(EMPTY_LINE_STATEMENT, tryCatchBlock);
  }

  private static List<Statement> createRpcLroExceptionTestCatchBody(
      VariableExpr exceptionExpr, boolean isStreaming) {
    List<Expr> catchBodyExprs = new ArrayList<>();

    Expr testExpectedValueExpr =
        VariableExpr.builder()
            .setVariable(
                Variable.builder()
                    .setType(TypeNode.withReference(ConcreteReference.withClazz(Class.class)))
                    .setName("class")
                    .build())
            .setStaticReferenceType(FIXED_TYPESTORE.get("InvalidArgumentException"))
            .build();
    Expr getCauseExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(exceptionExpr)
            .setMethodName("getCause")
            .setReturnType(TypeNode.withReference(ConcreteReference.withClazz(Throwable.class)))
            .build();
    Expr testActualValueExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(getCauseExpr)
            .setMethodName("getClass")
            .build();

    if (isStreaming) {
      InstanceofExpr checkInstanceExpr =
          InstanceofExpr.builder()
              .setExpr(getCauseExpr)
              .setCheckType(FIXED_TYPESTORE.get("InvalidArgumentException"))
              .build();
      catchBodyExprs.add(
          MethodInvocationExpr.builder()
              .setStaticReferenceType(FIXED_TYPESTORE.get("Assert"))
              .setMethodName("assertTrue")
              .setArguments(checkInstanceExpr)
              .build());
    } else {
      // Constructs `Assert.assertEquals(InvalidArgumentException.class, e.getCaus().getClass());`.
      catchBodyExprs.add(
          MethodInvocationExpr.builder()
              .setStaticReferenceType(FIXED_TYPESTORE.get("Assert"))
              .setMethodName("assertEquals")
              .setArguments(testExpectedValueExpr, testActualValueExpr)
              .build());
    }

    // Construct the apiException variable.
    VariableExpr apiExceptionVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(FIXED_TYPESTORE.get("InvalidArgumentException"))
                .setName("apiException")
                .build());
    Expr castedCauseExpr =
        CastExpr.builder()
            .setType(FIXED_TYPESTORE.get("InvalidArgumentException"))
            .setExpr(getCauseExpr)
            .build();
    catchBodyExprs.add(
        AssignmentExpr.builder()
            .setVariableExpr(apiExceptionVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(castedCauseExpr)
            .build());

    // Construct the last assert statement.
    testExpectedValueExpr =
        EnumRefExpr.builder()
            .setType(
                TypeNode.withReference(
                    ConcreteReference.builder()
                        .setClazz(StatusCode.Code.class)
                        .setIsStaticImport(false)
                        .build()))
            .setName("INVALID_ARGUMENT")
            .build();
    testActualValueExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(apiExceptionVarExpr)
            .setMethodName("getStatusCode")
            .build();
    testActualValueExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(testActualValueExpr)
            .setMethodName("getCode")
            .build();
    catchBodyExprs.add(
        MethodInvocationExpr.builder()
            .setStaticReferenceType(FIXED_TYPESTORE.get("Assert"))
            .setMethodName("assertEquals")
            .setArguments(testExpectedValueExpr, testActualValueExpr)
            .build());

    return catchBodyExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList());
  }

  /* =========================================
   * Type creator methods.
   * =========================================
   */

  private static TypeStore createStaticTypes() {
    List<Class> concreteClazzes =
        Arrays.asList(
            AbstractMessage.class,
            After.class,
            AfterClass.class,
            Any.class,
            ApiClientHeaderProvider.class,
            ApiStreamObserver.class,
            Arrays.class,
            Assert.class,
            Before.class,
            BeforeClass.class,
            BidiStreamingCallable.class,
            ClientStreamingCallable.class,
            ExecutionException.class,
            GaxGrpcProperties.class,
            Generated.class,
            IOException.class,
            InvalidArgumentException.class,
            List.class,
            Lists.class,
            NoCredentialsProvider.class,
            Operation.class,
            ServerStreamingCallable.class,
            StatusCode.class,
            StatusRuntimeException.class,
            Test.class,
            UUID.class);
    TypeStore typeStore = new TypeStore(concreteClazzes);
    typeStore.putAll(
        GRPC_TESTING_PACKAGE,
        Arrays.asList(
            "LocalChannelProvider", "MockGrpcService", "MockServiceHelper", "MockStreamObserver"));
    return typeStore;
  }

  private static Map<String, TypeNode> createDefaultMethodNamesToTypes() {
    Function<Class, TypeNode> typeMakerFn =
        c -> TypeNode.withReference(ConcreteReference.withClazz(c));
    Map<String, TypeNode> javaMethodNameToReturnType = new LinkedHashMap<>();
    javaMethodNameToReturnType.put(
        "defaultExecutorProviderBuilder",
        typeMakerFn.apply(InstantiatingExecutorProvider.Builder.class));
    javaMethodNameToReturnType.put("getDefaultEndpoint", TypeNode.STRING);
    javaMethodNameToReturnType.put(
        "getDefaultServiceScopes",
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(List.class)
                .setGenerics(Arrays.asList(TypeNode.STRING.reference()))
                .build()));
    javaMethodNameToReturnType.put(
        "defaultCredentialsProviderBuilder",
        typeMakerFn.apply(GoogleCredentialsProvider.Builder.class));
    javaMethodNameToReturnType.put(
        "defaultGrpcTransportProviderBuilder",
        typeMakerFn.apply(InstantiatingGrpcChannelProvider.Builder.class));
    javaMethodNameToReturnType.put(
        "defaultTransportChannelProvider", FIXED_TYPESTORE.get("TransportChannelProvider"));
    return javaMethodNameToReturnType;
  }

  private static TypeStore createDynamicTypes(Service service) {
    TypeStore typeStore = new TypeStore();
    typeStore.putAll(
        service.pakkage(),
        Arrays.asList(
            ClassNames.getMockServiceClassName(service),
            ClassNames.getServiceClientClassName(service),
            ClassNames.getServiceSettingsClassName(service)));

    // Pagination types.
    typeStore.putAll(
        service.pakkage(),
        service.methods().stream()
            .filter(m -> m.isPaged())
            .map(m -> String.format(PAGED_RESPONSE_TYPE_NAME_PATTERN, m.name()))
            .collect(Collectors.toList()),
        true,
        ClassNames.getServiceClientClassName(service));
    return typeStore;
  }

  private static TypeNode getOperationCallSettingsType(Method protoMethod) {
    return getOperationCallSettingsTypeHelper(protoMethod, false);
  }

  private static TypeNode getOperationCallSettingsBuilderType(Method protoMethod) {
    return getOperationCallSettingsTypeHelper(protoMethod, true);
  }

  private static TypeNode getOperationCallSettingsTypeHelper(
      Method protoMethod, boolean isBuilder) {
    Preconditions.checkState(
        protoMethod.hasLro(),
        String.format("Cannot get OperationCallSettings on non-LRO method %s", protoMethod.name()));
    Class callSettingsClazz =
        isBuilder ? OperationCallSettings.Builder.class : OperationCallSettings.class;
    return TypeNode.withReference(
        ConcreteReference.builder()
            .setClazz(callSettingsClazz)
            .setGenerics(
                Arrays.asList(
                    protoMethod.inputType().reference(),
                    protoMethod.lro().responseType().reference(),
                    protoMethod.lro().metadataType().reference()))
            .build());
  }

  private static TypeNode getCallSettingsType(Method protoMethod, TypeStore typeStore) {
    return getCallSettingsTypeHelper(protoMethod, typeStore, false);
  }

  private static TypeNode getCallSettingsBuilderType(Method protoMethod, TypeStore typeStore) {
    return getCallSettingsTypeHelper(protoMethod, typeStore, true);
  }

  private static TypeNode getCallSettingsTypeHelper(
      Method protoMethod, TypeStore typeStore, boolean isBuilder) {
    Class callSettingsClazz = isBuilder ? UnaryCallSettings.Builder.class : UnaryCallSettings.class;
    if (protoMethod.isPaged()) {
      callSettingsClazz = isBuilder ? PagedCallSettings.Builder.class : PagedCallSettings.class;
    } else {
      switch (protoMethod.stream()) {
        case CLIENT:
          // Fall through.
        case BIDI:
          callSettingsClazz =
              isBuilder ? StreamingCallSettings.Builder.class : StreamingCallSettings.class;
          break;
        case SERVER:
          callSettingsClazz =
              isBuilder
                  ? ServerStreamingCallSettings.Builder.class
                  : ServerStreamingCallSettings.class;
          break;
        case NONE:
          // Fall through
        default:
          // Fall through
      }
    }

    List<Reference> generics = new ArrayList<>();
    generics.add(protoMethod.inputType().reference());
    generics.add(protoMethod.outputType().reference());
    if (protoMethod.isPaged()) {
      generics.add(
          typeStore
              .get(String.format(PAGED_RESPONSE_TYPE_NAME_PATTERN, protoMethod.name()))
              .reference());
    }

    return TypeNode.withReference(
        ConcreteReference.builder().setClazz(callSettingsClazz).setGenerics(generics).build());
  }

  private static TypeNode getCallableType(Method protoMethod) {
    Preconditions.checkState(
        !protoMethod.stream().equals(Method.Stream.NONE),
        "No callable type exists for non-streaming methods.");

    Class callableClazz = ClientStreamingCallable.class;
    switch (protoMethod.stream()) {
      case BIDI:
        callableClazz = BidiStreamingCallable.class;
        break;
      case SERVER:
        callableClazz = ServerStreamingCallable.class;
        break;
      case CLIENT:
        // Fall through.
      case NONE:
        // Fall through
      default:
        // Fall through
    }

    List<Reference> generics = new ArrayList<>();
    generics.add(protoMethod.inputType().reference());
    generics.add(protoMethod.outputType().reference());

    return TypeNode.withReference(
        ConcreteReference.builder().setClazz(callableClazz).setGenerics(generics).build());
  }

  private static TypeNode getPagedResponseType(Method method, Service service) {
    return TypeNode.withReference(
        VaporReference.builder()
            .setName(String.format(PAGED_RESPONSE_TYPE_NAME_PATTERN, method.name()))
            .setPakkage(service.pakkage())
            .setEnclosingClassNames(ClassNames.getServiceClientClassName(service))
            .setIsStaticImport(true)
            .build());
  }

  private static String getCallableMethodName(Method protoMethod) {
    Preconditions.checkState(
        !protoMethod.stream().equals(Method.Stream.NONE),
        "No callable type exists for non-streaming methods.");

    switch (protoMethod.stream()) {
      case BIDI:
        return "bidiStreamingCall";
      case SERVER:
        return "serverStreamingCall";
      case CLIENT:
        // Fall through.
      case NONE:
        // Fall through
      default:
        return "clientStreamingCall";
    }
  }

  private static String getMockServiceVarName(Service service) {
    return String.format(MOCK_SERVICE_VAR_NAME_PATTERN, service.name());
  }

  private static boolean isProtoEmptyType(TypeNode type) {
    return type.reference().pakkage().equals("com.google.protobuf")
        && type.reference().name().equals("Empty");
  }
}
