// Copyright 2021 Google LLC
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

package com.google.api.generator.gapic.composer.rest;

import com.google.api.gax.httpjson.GaxHttpJsonProperties;
import com.google.api.gax.httpjson.HttpJsonTransportChannel;
import com.google.api.gax.httpjson.InstantiatingHttpJsonChannelProvider;
import com.google.api.gax.rpc.ApiClientHeaderProvider;
import com.google.api.generator.engine.ast.AnnotationNode;
import com.google.api.generator.engine.ast.ConcreteReference;
import com.google.api.generator.engine.ast.MethodDefinition;
import com.google.api.generator.engine.ast.MethodInvocationExpr;
import com.google.api.generator.engine.ast.ScopeNode;
import com.google.api.generator.engine.ast.StringObjectValue;
import com.google.api.generator.engine.ast.TypeNode;
import com.google.api.generator.engine.ast.ValueExpr;
import com.google.api.generator.engine.ast.Variable;
import com.google.api.generator.engine.ast.VariableExpr;
import com.google.api.generator.gapic.composer.comment.SettingsCommentComposer;
import com.google.api.generator.gapic.composer.common.AbstractServiceStubSettingsClassComposer;
import com.google.api.generator.gapic.composer.store.TypeStore;
import com.google.api.generator.gapic.composer.utils.ClassNames;
import com.google.api.generator.gapic.model.Service;
import java.util.Arrays;

public class ServiceStubSettingsClassComposer extends AbstractServiceStubSettingsClassComposer {
  private static final ServiceStubSettingsClassComposer INSTANCE =
      new ServiceStubSettingsClassComposer();

  private static final TypeStore FIXED_REST_TYPESTORE = createStaticTypes();

  public static ServiceStubSettingsClassComposer instance() {
    return INSTANCE;
  }

  protected ServiceStubSettingsClassComposer() {
    super(RestContext.instance());
  }

  private static TypeStore createStaticTypes() {
    return new TypeStore(
        Arrays.asList(
            GaxHttpJsonProperties.class,
            HttpJsonTransportChannel.class,
            InstantiatingHttpJsonChannelProvider.class));
  }

  @Override
  protected MethodDefinition createDefaultTransportTransportProviderBuilderMethod() {
    // Create the defaultHttpJsonTransportProviderBuilder method.
    TypeNode returnType =
        TypeNode.withReference(
            ConcreteReference.withClazz(InstantiatingHttpJsonChannelProvider.Builder.class));
    MethodInvocationExpr transportChannelProviderBuilderExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(
                FIXED_REST_TYPESTORE.get(
                    InstantiatingHttpJsonChannelProvider.class.getSimpleName()))
            .setMethodName("newBuilder")
            .setReturnType(returnType)
            .build();
    return MethodDefinition.builder()
        .setHeaderCommentStatements(
            SettingsCommentComposer.DEFAULT_TRANSPORT_PROVIDER_BUILDER_METHOD_COMMENT)
        .setScope(ScopeNode.PUBLIC)
        .setIsStatic(true)
        .setReturnType(returnType)
        .setName("defaultHttpJsonTransportProviderBuilder")
        .setReturnExpr(transportChannelProviderBuilderExpr)
        .build();
  }

  @Override
  protected MethodDefinition createDefaultApiClientHeaderProviderBuilderMethod(
      Service service, TypeStore typeStore) {
    // Create the defaultApiClientHeaderProviderBuilder method.
    TypeNode returnType =
        TypeNode.withReference(ConcreteReference.withClazz(ApiClientHeaderProvider.Builder.class));
    MethodInvocationExpr returnExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(FIXED_TYPESTORE.get("ApiClientHeaderProvider"))
            .setMethodName("newBuilder")
            .build();

    MethodInvocationExpr versionArgExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(FIXED_TYPESTORE.get("GaxProperties"))
            .setMethodName("getLibraryVersion")
            .setArguments(
                VariableExpr.builder()
                    .setVariable(
                        Variable.builder().setType(TypeNode.CLASS_OBJECT).setName("class").build())
                    .setStaticReferenceType(
                        typeStore.get(ClassNames.getServiceStubSettingsClassName(service)))
                    .build())
            .build();

    returnExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(returnExpr)
            .setMethodName("setGeneratedLibToken")
            .setArguments(ValueExpr.withValue(StringObjectValue.withValue("gapic")), versionArgExpr)
            .build();
    returnExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(returnExpr)
            .setMethodName("setTransportToken")
            .setArguments(
                MethodInvocationExpr.builder()
                    .setStaticReferenceType(
                        FIXED_REST_TYPESTORE.get(GaxHttpJsonProperties.class.getSimpleName()))
                    .setMethodName("getHttpJsonTokenName")
                    .build(),
                MethodInvocationExpr.builder()
                    .setStaticReferenceType(
                        FIXED_REST_TYPESTORE.get(GaxHttpJsonProperties.class.getSimpleName()))
                    .setMethodName("getHttpJsonVersion")
                    .build())
            .setReturnType(returnType)
            .build();

    AnnotationNode annotation =
        AnnotationNode.builder()
            .setType(FIXED_TYPESTORE.get("BetaApi"))
            .setDescription(
                "The surface for customizing headers is not stable yet and may change in the"
                    + " future.")
            .build();
    return MethodDefinition.builder()
        .setAnnotations(Arrays.asList(annotation))
        .setScope(ScopeNode.PUBLIC)
        .setIsStatic(true)
        .setReturnType(returnType)
        .setName("defaultApiClientHeaderProviderBuilder")
        .setReturnExpr(returnExpr)
        .build();
  }

  @Override
  public MethodDefinition createDefaultTransportChannelProviderMethod() {
    TypeNode returnType = FIXED_TYPESTORE.get("TransportChannelProvider");
    MethodInvocationExpr transportProviderBuilderExpr =
        MethodInvocationExpr.builder()
            .setMethodName("defaultHttpJsonTransportProviderBuilder")
            .build();
    transportProviderBuilderExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(transportProviderBuilderExpr)
            .setMethodName("build")
            .setReturnType(returnType)
            .build();
    return MethodDefinition.builder()
        .setScope(ScopeNode.PUBLIC)
        .setIsStatic(true)
        .setReturnType(returnType)
        .setName("defaultTransportChannelProvider")
        .setReturnExpr(transportProviderBuilderExpr)
        .build();
  }
}
