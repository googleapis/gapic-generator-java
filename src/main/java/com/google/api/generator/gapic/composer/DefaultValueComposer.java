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

import com.google.api.generator.engine.ast.ConcreteReference;
import com.google.api.generator.engine.ast.Expr;
import com.google.api.generator.engine.ast.MethodInvocationExpr;
import com.google.api.generator.engine.ast.NewObjectExpr;
import com.google.api.generator.engine.ast.PrimitiveValue;
import com.google.api.generator.engine.ast.StringObjectValue;
import com.google.api.generator.engine.ast.TypeNode;
import com.google.api.generator.engine.ast.ValueExpr;
import com.google.api.generator.engine.ast.Variable;
import com.google.api.generator.engine.ast.VariableExpr;
import com.google.api.generator.gapic.model.Field;
import com.google.api.generator.gapic.model.Message;
import com.google.api.generator.gapic.model.MethodArgument;
import com.google.api.generator.gapic.model.ResourceName;
import com.google.api.generator.gapic.utils.JavaStyle;
import com.google.api.generator.gapic.utils.ResourceNameConstants;
import com.google.common.base.Preconditions;
import com.google.longrunning.Operation;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultValueComposer {
  private static TypeNode OPERATION_TYPE =
      TypeNode.withReference(ConcreteReference.withClazz(Operation.class));
  private static TypeNode ANY_TYPE = TypeNode.withReference(ConcreteReference.withClazz(Any.class));
  private static TypeNode BYTESTRING_TYPE =
      TypeNode.withReference(ConcreteReference.withClazz(ByteString.class));

  static Expr createDefaultValue(
      MethodArgument methodArg, Map<String, ResourceName> resourceNames) {
    if (methodArg.isResourceNameHelper()) {
      Preconditions.checkState(
          methodArg.field().hasResourceReference(),
          String.format(
              "No corresponding resource reference for argument %s found on field %s %s",
              methodArg.name(), methodArg.field().type(), methodArg.field().name()));
      ResourceName resourceName =
          resourceNames.get(methodArg.field().resourceReference().resourceTypeString());
      Preconditions.checkNotNull(
          resourceName,
          String.format(
              "No resource name found for reference %s",
              methodArg.field().resourceReference().resourceTypeString()));
      return createDefaultValue(
          resourceName, resourceNames.values().stream().collect(Collectors.toList()));
    }

    if (methodArg.type().equals(methodArg.field().type())) {
      return createDefaultValue(methodArg.field());
    }

    return createDefaultValue(
        Field.builder().setName(methodArg.name()).setType(methodArg.type()).build());
  }

  static Expr createDefaultValue(Field f) {
    return createDefaultValue(f, false);
  }

  static Expr createDefaultValue(Field f, boolean useExplicitInitTypeInGenerics) {
    if (f.isRepeated()) {
      ConcreteReference.Builder refBuilder =
          ConcreteReference.builder().setClazz(f.isMap() ? HashMap.class : ArrayList.class);
      if (useExplicitInitTypeInGenerics) {
        if (f.isMap()) {
          refBuilder = refBuilder.setGenerics(f.type().reference().generics().subList(0, 2));
        } else {
          refBuilder = refBuilder.setGenerics(f.type().reference().generics().get(0));
        }
      }

      TypeNode newType = TypeNode.withReference(refBuilder.build());
      return NewObjectExpr.builder().setType(newType).setIsGeneric(true).build();
    }

    if (f.isEnum()) {
      return MethodInvocationExpr.builder()
          .setStaticReferenceType(f.type())
          .setMethodName("forNumber")
          .setArguments(
              ValueExpr.withValue(
                  PrimitiveValue.builder().setType(TypeNode.INT).setValue("0").build()))
          .setReturnType(f.type())
          .build();
    }

    if (f.isMessage()) {
      MethodInvocationExpr newBuilderExpr =
          MethodInvocationExpr.builder()
              .setStaticReferenceType(f.type())
              .setMethodName("newBuilder")
              .build();
      return MethodInvocationExpr.builder()
          .setExprReferenceExpr(newBuilderExpr)
          .setMethodName("build")
          .setReturnType(f.type())
          .build();
    }

    if (f.type().equals(TypeNode.STRING)) {
      return ValueExpr.withValue(
          StringObjectValue.withValue(String.format("%s%s", f.name(), f.name().hashCode())));
    }

    if (TypeNode.isNumericType(f.type())) {
      return ValueExpr.withValue(
          PrimitiveValue.builder()
              .setType(f.type())
              .setValue(String.format("%s", f.name().hashCode()))
              .build());
    }

    if (f.type().equals(TypeNode.BOOLEAN)) {
      return ValueExpr.withValue(
          PrimitiveValue.builder().setType(f.type()).setValue("true").build());
    }

    if (f.type().equals(TypeNode.BYTESTRING)) {
      return VariableExpr.builder()
          .setStaticReferenceType(TypeNode.BYTESTRING)
          .setVariable(Variable.builder().setName("EMPTY").setType(TypeNode.BYTESTRING).build())
          .build();
    }

    throw new UnsupportedOperationException(
        String.format(
            "Default value for field %s with type %s not implemented yet.", f.name(), f.type()));
  }

  static Expr createDefaultValue(ResourceName resourceName, List<ResourceName> resnames) {
    boolean hasOnePattern = resourceName.patterns().size() == 1;
    if (resourceName.isOnlyWildcard()) {
      List<ResourceName> unexaminedResnames = new ArrayList<>(resnames);
      for (ResourceName resname : resnames) {
        if (resname.isOnlyWildcard()) {
          unexaminedResnames.remove(resname);
          continue;
        }
        unexaminedResnames.remove(resname);
        return createDefaultValue(resname, unexaminedResnames);
      }
      // Should not get here.
      Preconditions.checkState(
          !unexaminedResnames.isEmpty(),
          String.format(
              "No default resource name found for wildcard %s", resourceName.resourceTypeString()));
    }

    // The cost tradeoffs of new ctors versus distinct() don't really matter here, since this list
    // will usually have a very small number of elements.
    List<String> patterns = new ArrayList<>(new HashSet<>(resourceName.patterns()));
    boolean containsOnlyDeletedTopic =
        patterns.size() == 1 && patterns.get(0).equals(ResourceNameConstants.DELETED_TOPIC_LITERAL);
    String ofMethodName = "of";
    List<String> patternPlaceholderTokens = new ArrayList<>();

    if (containsOnlyDeletedTopic) {
      ofMethodName = "ofDeletedTopic";
    } else {
      for (String pattern : resourceName.patterns()) {
        if (pattern.equals(ResourceNameConstants.WILDCARD_PATTERN)
            || pattern.equals(ResourceNameConstants.DELETED_TOPIC_LITERAL)) {
          continue;
        }
        patternPlaceholderTokens.addAll(
            ResourceNameTokenizer.parseTokenHierarchy(Arrays.asList(pattern)).get(0));
        break;
      }
    }

    if (!hasOnePattern) {
      ofMethodName =
          String.format(
              "of%sName",
              String.join(
                  "",
                  patternPlaceholderTokens.stream()
                      .map(s -> JavaStyle.toUpperCamelCase(s))
                      .collect(Collectors.toList())));
    }

    TypeNode resourceNameJavaType = resourceName.type();
    List<Expr> argExprs =
        patternPlaceholderTokens.stream()
            .map(
                s ->
                    ValueExpr.withValue(
                        StringObjectValue.withValue(String.format("[%s]", s.toUpperCase()))))
            .collect(Collectors.toList());
    return MethodInvocationExpr.builder()
        .setStaticReferenceType(resourceNameJavaType)
        .setMethodName(ofMethodName)
        .setArguments(argExprs)
        .setReturnType(resourceNameJavaType)
        .build();
  }

  static Expr createSimpleMessageBuilderExpr(
      Message message, Map<String, ResourceName> resourceNames, Map<String, Message> messageTypes) {
    MethodInvocationExpr builderExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(message.type())
            .setMethodName("newBuilder")
            .build();
    for (Field field : message.fields()) {
      if (field.isContainedInOneof() // Avoid colliding fields.
          || ((field.isMessage() || field.isEnum()) // Avoid importing unparsed messages.
              && !field.isRepeated()
              && !messageTypes.containsKey(field.type().reference().name()))) {
        continue;
      }
      String setterMethodNamePattern = "set%s";
      if (field.isRepeated()) {
        setterMethodNamePattern = field.isMap() ? "putAll%s" : "addAll%s";
      }
      Expr defaultExpr = null;
      if (field.hasResourceReference()
          && resourceNames.get(field.resourceReference().resourceTypeString()) != null) {
        defaultExpr =
            createDefaultValue(
                resourceNames.get(field.resourceReference().resourceTypeString()),
                resourceNames.values().stream().collect(Collectors.toList()));
        defaultExpr =
            MethodInvocationExpr.builder()
                .setExprReferenceExpr(defaultExpr)
                .setMethodName("toString")
                .setReturnType(TypeNode.STRING)
                .build();
      } else {
        defaultExpr = createDefaultValue(field, true);
      }
      builderExpr =
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(builderExpr)
              .setMethodName(
                  String.format(setterMethodNamePattern, JavaStyle.toUpperCamelCase(field.name())))
              .setArguments(defaultExpr)
              .build();
    }

    return MethodInvocationExpr.builder()
        .setExprReferenceExpr(builderExpr)
        .setMethodName("build")
        .setReturnType(message.type())
        .build();
  }

  static Expr createSimpleOperationBuilderExpr(String name, VariableExpr responseExpr) {
    Expr operationExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(OPERATION_TYPE)
            .setMethodName("newBuilder")
            .build();
    operationExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(operationExpr)
            .setMethodName("setName")
            .setArguments(ValueExpr.withValue(StringObjectValue.withValue(name)))
            .build();
    operationExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(operationExpr)
            .setMethodName("setDone")
            .setArguments(
                ValueExpr.withValue(
                    PrimitiveValue.builder().setType(TypeNode.BOOLEAN).setValue("true").build()))
            .build();
    operationExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(operationExpr)
            .setMethodName("setResponse")
            .setArguments(
                MethodInvocationExpr.builder()
                    .setStaticReferenceType(ANY_TYPE)
                    .setMethodName("pack")
                    .setArguments(responseExpr)
                    .build())
            .build();
    return MethodInvocationExpr.builder()
        .setExprReferenceExpr(operationExpr)
        .setMethodName("build")
        .setReturnType(OPERATION_TYPE)
        .build();
  }

  static Expr createSimplePagedResponse(
      TypeNode responseType, String repeatedFieldName, Expr responseElementVarExpr) {
    Expr pagedResponseExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(responseType)
            .setMethodName("newBuilder")
            .build();
    pagedResponseExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(pagedResponseExpr)
            .setMethodName("setNextPageToken")
            .setArguments(ValueExpr.withValue(StringObjectValue.withValue("")))
            .build();
    pagedResponseExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(pagedResponseExpr)
            .setMethodName(String.format("addAll%s", JavaStyle.toUpperCamelCase(repeatedFieldName)))
            .setArguments(
                MethodInvocationExpr.builder()
                    .setStaticReferenceType(
                        TypeNode.withReference(ConcreteReference.withClazz(Arrays.class)))
                    .setMethodName("asList")
                    .setArguments(responseElementVarExpr)
                    .build())
            .build();
    return MethodInvocationExpr.builder()
        .setExprReferenceExpr(pagedResponseExpr)
        .setMethodName("build")
        .setReturnType(responseType)
        .build();
  }
}
