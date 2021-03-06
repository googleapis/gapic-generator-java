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

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class TryCatchStatement implements Statement {

  // Required.
  public abstract ImmutableList<Statement> tryBody();
  // Optional only if the sample code bit is set (i.e. this is sample code).
  @Nullable
  public abstract VariableExpr catchVariableExpr();
  // Optional only if the sample code bit is set (i.e. this is sample code).
  public abstract ImmutableList<Statement> catchBody();
  // Optional.
  @Nullable
  public abstract AssignmentExpr tryResourceExpr();

  public abstract boolean isSampleCode();

  @Override
  public void accept(AstNodeVisitor visitor) {
    visitor.visit(this);
  }

  public static Builder builder() {
    return new AutoValue_TryCatchStatement.Builder()
        .setIsSampleCode(false)
        .setCatchBody(Collections.emptyList());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setTryResourceExpr(AssignmentExpr assignmentExpr);

    public abstract Builder setTryBody(List<Statement> body);

    public abstract Builder setCatchVariableExpr(VariableExpr variableExpr);

    public abstract Builder setCatchBody(List<Statement> body);

    public abstract Builder setIsSampleCode(boolean isSampleCode);

    abstract TryCatchStatement autoBuild();

    public TryCatchStatement build() {
      TryCatchStatement tryCatchStatement = autoBuild();
      NodeValidator.checkNoNullElements(tryCatchStatement.tryBody(), "try body", "try-catch");
      NodeValidator.checkNoNullElements(tryCatchStatement.catchBody(), "catch body", "try-catch");

      if (!tryCatchStatement.isSampleCode()) {
        Preconditions.checkNotNull(
            tryCatchStatement.catchVariableExpr(),
            "Catch variable expression must be set for real, non-sample try-catch blocks.");
        Preconditions.checkState(
            tryCatchStatement.catchVariableExpr().isDecl(),
            "Catch variable expression must be a declaration");
        Preconditions.checkState(
            TypeNode.isExceptionType(tryCatchStatement.catchVariableExpr().variable().type()),
            "Catch variable must be an Exception object reference");
      }

      return tryCatchStatement;
    }
  }
}
