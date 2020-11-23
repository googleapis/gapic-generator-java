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

import com.google.api.generator.engine.ast.AssignmentExpr;
import com.google.api.generator.engine.ast.CommentStatement;
import com.google.api.generator.engine.ast.Expr;
import com.google.api.generator.engine.ast.ExprStatement;
import com.google.api.generator.engine.ast.LineComment;
import com.google.api.generator.engine.ast.MethodInvocationExpr;
import com.google.api.generator.engine.ast.TryCatchStatement;
import com.google.api.generator.engine.ast.TypeNode;
import com.google.api.generator.engine.ast.Variable;
import com.google.api.generator.engine.ast.VariableExpr;
import com.google.api.generator.gapic.model.Method;
import com.google.api.generator.gapic.model.MethodArgument;
import com.google.api.generator.gapic.model.ResourceName;
import com.google.api.generator.gapic.utils.JavaStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SampleCodeHelperComposer {
  private static String RESPONSE_VAR_NAME = "response";

  public static TryCatchStatement composeRpcMethodSampleCode(
      Method method,
      List<MethodArgument> arguments,
      TypeNode clientType,
      Map<String, ResourceName> resourceNames) {
    // Paged Unary RPC method.
    if (method.isPaged()) {
      return composePagedUnaryRpcMethodSampleCode(method, arguments, clientType, resourceNames);
    }
    // Long-running operation Unary RPC method.
    if (method.hasLro()) {
      return composeLroUnaryRpcMethodSampleCode(method, arguments, clientType, resourceNames);
    }
    // Pure Unary RPC method.
    return composeUnaryRpcMethodSampleCode(method, arguments, clientType, resourceNames);
  }

  public static TryCatchStatement composeRpcDefaultMethodSampleCode(
      Method method, TypeNode clientType, Map<String, ResourceName> resourceNames) {
    // TODO(summerji): compose sample code for unary default rpc method.
    VariableExpr clientVarExpr = createVariableExpr(getClientName(clientType), clientType);
    String content =
        String.format(
            "Note: Not implemented yet, placeholder for unary %s rpc method sample code.",
            (!method.hasLro() && !method.isPaged()
                ? "default"
                : (method.hasLro() ? "lro default" : "paged default")));
    return TryCatchStatement.builder()
        .setTryResourceExpr(assignClientVariableWithCreateMethodExpr(clientVarExpr))
        .setTryBody(Arrays.asList(createLineCommentStatement(content)))
        .setIsSampleCode(true)
        .build();
  }

  private static TryCatchStatement composeUnaryRpcMethodSampleCode(
      Method method,
      List<MethodArgument> arguments,
      TypeNode clientType,
      Map<String, ResourceName> resourceNames) {
    // TODO(summerji): Add unit tests.
    VariableExpr clientVarExpr = createVariableExpr(getClientName(clientType), clientType);
    // Assign each method arguments with default value.
    List<Expr> bodyExpr =
        arguments.stream()
            .map(methodArg -> assignMethodArgumentWithDefaultValue(methodArg, resourceNames))
            .collect(Collectors.toList());
    // Invoke current method based on return type.
    // e.g. if return void, echoClient.echo(..); or,
    // e.g. if return other type, EchoResponse response = echoClient.echo(...);
    if (method.outputType().equals(TypeNode.VOID)) {
      bodyExpr.add(
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(clientVarExpr)
              .setMethodName(method.name())
              .setReturnType(clientType)
              .build());
    } else {
      bodyExpr.add(
          createAssignExprForVariableWithClientMethod(
              RESPONSE_VAR_NAME, method.outputType(), clientVarExpr, method.name(), arguments));
    }

    return TryCatchStatement.builder()
        .setTryResourceExpr(assignClientVariableWithCreateMethodExpr(clientVarExpr))
        .setTryBody(
            bodyExpr.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()))
        .setIsSampleCode(true)
        .build();
  }

  private static TryCatchStatement composeLroUnaryRpcMethodSampleCode(
      Method method,
      List<MethodArgument> arguments,
      TypeNode clientType,
      Map<String, ResourceName> resourceNames) {
    // TODO(summerji): compose sample code for unary lro rpc method.
    // TODO(summerji): Add unit tests.
    VariableExpr clientVarExpr = createVariableExpr(getClientName(clientType), clientType);
    return TryCatchStatement.builder()
        .setTryResourceExpr(assignClientVariableWithCreateMethodExpr(clientVarExpr))
        .setTryBody(
            Arrays.asList(
                createLineCommentStatement(
                    "Note: Not implemented yet, placeholder for lro Unary rpc method sample code.")))
        .setIsSampleCode(true)
        .build();
  }

  private static TryCatchStatement composePagedUnaryRpcMethodSampleCode(
      Method method,
      List<MethodArgument> arguments,
      TypeNode clientType,
      Map<String, ResourceName> resourceNames) {
    // TODO(summerji): compose sample code for unary paged rpc method.
    // TODO(summerji): Add unit tests.
    VariableExpr clientVarExpr = createVariableExpr(getClientName(clientType), clientType);
    return TryCatchStatement.builder()
        .setTryResourceExpr(assignClientVariableWithCreateMethodExpr(clientVarExpr))
        .setTryBody(
            Arrays.asList(
                createLineCommentStatement(
                    "Note: Not implemented yet, placeholder for paged unary rpc method sample code.")))
        .setIsSampleCode(true)
        .build();
  }
  // ==================================Helpers===================================================//

  // Assign client variable expr with create client.
  // e.g EchoClient echoClient = EchoClient.create()
  private static AssignmentExpr assignClientVariableWithCreateMethodExpr(
      VariableExpr clientVarExpr) {
    return AssignmentExpr.builder()
        .setVariableExpr(clientVarExpr.toBuilder().setIsDecl(true).build())
        .setValueExpr(
            MethodInvocationExpr.builder()
                .setStaticReferenceType(clientVarExpr.variable().type())
                .setReturnType(clientVarExpr.variable().type())
                .setMethodName("create")
                .build())
        .build();
  }

  private static Expr assignMethodArgumentWithDefaultValue(
      MethodArgument argument, Map<String, ResourceName> resourceNames) {
    return AssignmentExpr.builder()
        .setVariableExpr(
            createVariableDeclExpr(JavaStyle.toLowerCamelCase(argument.name()), argument.type()))
        .setValueExpr(DefaultValueComposer.createDefaultValue(argument, resourceNames))
        .build();
  }

  private static Expr createAssignExprForVariableWithClientMethod(
      String variableName,
      TypeNode variableType,
      VariableExpr clientVarExpr,
      String methodName,
      List<MethodArgument> arguments) {
    VariableExpr varExpr = createVariableExpr(variableName, variableType);
    MethodInvocationExpr clientMethodInvocationExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(clientVarExpr)
            .setMethodName(JavaStyle.toLowerCamelCase(methodName))
            .setArguments(mapMethodArgumentsToVariableExprs(arguments))
            .setReturnType(variableType)
            .build();
    return AssignmentExpr.builder()
        .setVariableExpr(varExpr.toBuilder().setIsDecl(true).build())
        .setValueExpr(clientMethodInvocationExpr)
        .build();
  }

  private static List<Expr> mapMethodArgumentsToVariableExprs(List<MethodArgument> arguments) {
    return arguments.stream()
        .map(arg -> createVariableExpr(arg.name(), arg.type()))
        .collect(Collectors.toList());
  }

  private static String getClientName(TypeNode clientType) {
    return JavaStyle.toLowerCamelCase(clientType.reference().name());
  }

  private static CommentStatement createLineCommentStatement(String content) {
    return CommentStatement.withComment(LineComment.withComment(content));
  }

  private static VariableExpr createVariableExpr(String variableName, TypeNode type) {
    return createVariableExpr(variableName, type, false);
  }

  private static VariableExpr createVariableDeclExpr(String variableName, TypeNode type) {
    return createVariableExpr(variableName, type, true);
  }

  private static VariableExpr createVariableExpr(
      String variableName, TypeNode type, boolean isDecl) {
    return VariableExpr.builder()
        .setVariable(Variable.builder().setName(variableName).setType(type).build())
        .setIsDecl(isDecl)
        .build();
  }
}
