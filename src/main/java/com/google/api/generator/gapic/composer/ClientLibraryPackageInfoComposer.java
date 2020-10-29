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

import com.google.api.generator.engine.ast.AnnotationNode;
import com.google.api.generator.engine.ast.CommentStatement;
import com.google.api.generator.engine.ast.ConcreteReference;
import com.google.api.generator.engine.ast.JavaDocComment;
import com.google.api.generator.engine.ast.PackageInfoDefinition;
import com.google.api.generator.engine.ast.TypeNode;
import com.google.api.generator.gapic.model.GapicContext;
import com.google.api.generator.gapic.model.GapicPackageInfo;
import com.google.api.generator.gapic.model.Service;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import javax.annotation.Generated;

public class ClientLibraryPackageInfoComposer {
  private static final String DIVIDER = "=======================";

  private static final String PACKAGE_INFO_DESCRIPTION =
      "The interfaces provided are listed below, along with usage samples.";

  private static final String CLIENT_PATTERN = "%sClient";
  private static final String PACKAGE_INFO_TITLE_PATTERN = "A client to %s";
  private static final String SAMPLE_CODE_HEADER_PATTERN = "Sample for %s:";
  private static final String SERVICE_DESCRIPTION_HEADER_PATTERN = "Service Description: %s";

  public static GapicPackageInfo generatePackageInfo(GapicContext context) {
    Preconditions.checkState(!context.services().isEmpty(), "No services found to generate");
    // Pick some service's package, as we assume they are all the same.
    String libraryPakkage = context.services().get(0).pakkage();

    PackageInfoDefinition packageInfo =
        PackageInfoDefinition.builder()
            .setPakkage(libraryPakkage)
            .setHeaderCommentStatements(createPackageInfoJavadoc(context))
            .setAnnotations(
                AnnotationNode.builder()
                    .setType(TypeNode.withReference(ConcreteReference.withClazz(Generated.class)))
                    .setDescription("by gapic-generator-java")
                    .build())
            .build();
    return GapicPackageInfo.with(packageInfo);
  }

  private static CommentStatement createPackageInfoJavadoc(GapicContext context) {
    JavaDocComment.Builder javaDocCommentBuilder = JavaDocComment.builder();
    if (context.hasServiceYamlProto()
        && !Strings.isNullOrEmpty(context.serviceYamlProto().getTitle())) {
      javaDocCommentBuilder =
          javaDocCommentBuilder.addComment(
              String.format(PACKAGE_INFO_TITLE_PATTERN, context.serviceYamlProto().getTitle()));
    }

    javaDocCommentBuilder = javaDocCommentBuilder.addParagraph(PACKAGE_INFO_DESCRIPTION);

    for (Service service : context.services()) {
      String javaClientName = String.format(CLIENT_PATTERN, service.name());
      javaDocCommentBuilder =
          javaDocCommentBuilder.addParagraph(
              String.format("%s %s %s", DIVIDER, javaClientName, DIVIDER));

      // TODO(miraleung): Paragraphs
      if (service.hasDescription()) {
        String[] descriptionParagraphs = service.description().split("\\r?\\n");
        for (int i = 0; i < descriptionParagraphs.length; i++) {
          if (i == 0) {
            javaDocCommentBuilder =
                javaDocCommentBuilder.addParagraph(
                    String.format(SERVICE_DESCRIPTION_HEADER_PATTERN, descriptionParagraphs[i]));
          } else {
            javaDocCommentBuilder = javaDocCommentBuilder.addParagraph(descriptionParagraphs[i]);
          }
        }
      }

      javaDocCommentBuilder =
          javaDocCommentBuilder.addParagraph(
              String.format(SAMPLE_CODE_HEADER_PATTERN, javaClientName));

      // TODO(summerji): Add package-info.java sample code here.
    }

    return CommentStatement.withComment(javaDocCommentBuilder.build());
  }
}
