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

package com.google.api.generator.engine.ast;

public interface AstNodeVisitor {
  /** Writes the syntatically-correct Java code representation of this node. */
  public void visit(IdentifierNode identifier);

  public void visit(TypeNode type);

  public void visit(ScopeNode scope);

  public void visit(AnnotationNode annotation);

  public void visit(NewObjectValue newObjectValue);
  /** =============================== EXPRESSIONS =============================== */
  public void visit(ValueExpr valueExpr);

  public void visit(VariableExpr variableExpr);

  public void visit(TernaryExpr tenaryExpr);

  public void visit(AssignmentExpr assignmentExpr);

  public void visit(MethodInvocationExpr methodInvocationExpr);

  /** =============================== STATEMENTS =============================== */
  public void visit(ExprStatement exprStatement);

  public void visit(BlockStatement blockStatement);

  public void visit(IfStatement ifStatement);

  public void visit(ForStatement forStatement);

  public void visit(WhileStatement whileStatement);

  public void visit(TryCatchStatement tryCatchStatement);

  /** =============================== OTHER =============================== */
  public void visit(MethodDefinition methodDefinition);

  public void visit(ClassDefinition classDefinition);
}
