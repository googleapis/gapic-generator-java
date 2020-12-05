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

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.api.generator.engine.ast.ConcreteReference;
import com.google.api.generator.engine.ast.Reference;
import com.google.api.generator.engine.ast.TypeNode;
import com.google.api.generator.engine.ast.VaporReference;
import com.google.api.generator.gapic.composer.samplecode.SampleCodeWriter;
import com.google.api.generator.gapic.model.Field;
import com.google.api.generator.gapic.model.Message;
import com.google.api.generator.gapic.model.Method;
import com.google.api.generator.gapic.model.MethodArgument;
import com.google.api.generator.gapic.model.ResourceName;
import com.google.api.generator.gapic.model.ResourceReference;
import com.google.api.generator.gapic.protoparser.Parser;
import com.google.api.generator.testutils.LineFormatter;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.showcase.v1beta1.EchoOuterClass;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServiceClientSampleCodeComposerTest {
  private static final String PACKAGE_NAME = "com.google.showcase.v1beta1";
  private static final TypeNode clientType =
      TypeNode.withReference(
          VaporReference.builder().setName("EchoClient").setPakkage(PACKAGE_NAME).build());

  FileDescriptor echoFileDescriptor;
  Map<String, ResourceName> resourceNames;
  Map<String, Message> messageTypes;

  @Before
  public void setUp() {
    echoFileDescriptor = EchoOuterClass.getDescriptor();
    resourceNames = Parser.parseResourceNames(echoFileDescriptor);
    messageTypes = Parser.parseMessages(echoFileDescriptor);
  }

  @Test
  public void validComposeRpcMethodHeaderSampleCode_pureUnaryRpc() {
    TypeNode inputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoRequest").setPakkage(PACKAGE_NAME).build());
    TypeNode outputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoResponse").setPakkage(PACKAGE_NAME).build());
    List<MethodArgument> methodArguments = Collections.emptyList();
    Method method =
        Method.builder()
            .setName("echo")
            .setMethodSignatures(Arrays.asList(methodArguments))
            .setInputType(inputType)
            .setOutputType(outputType)
            .build();
    String results =
        ServiceClientSampleCodeComposer.composeRpcMethodHeaderSampleCode(
            method, methodArguments, clientType, resourceNames, messageTypes);
    String expected =
        LineFormatter.lines(
            "try (EchoClient echoClient = EchoClient.create()) {\n",
            "  EchoResponse response = echoClient.echo();\n",
            "}");
    assertEquals(expected, results);
  }

  @Test
  public void invalidComposeRpcMethodHeaderSampleCode_noMatchedRepeatedResponseTypeInPagedMethod() {
    TypeNode inputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoRequest").setPakkage(PACKAGE_NAME).build());
    TypeNode outputType =
        TypeNode.withReference(
            VaporReference.builder().setName("PagedResponse").setPakkage(PACKAGE_NAME).build());
    List<MethodArgument> methodArguments = Collections.emptyList();
    Method method =
        Method.builder()
            .setName("simplePagedMethod")
            .setMethodSignatures(Arrays.asList(methodArguments))
            .setInputType(inputType)
            .setOutputType(outputType)
            .setIsPaged(true)
            .build();
    assertThrows(
        NullPointerException.class,
        () ->
            ServiceClientSampleCodeComposer.composeRpcMethodHeaderSampleCode(
                method, methodArguments, clientType, resourceNames, messageTypes));
  }

  @Test
  public void invalidComposeRpcMethodHeaderSampleCode_noRepeatedResponseTypeInPagedMethod() {
    TypeNode inputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoRequest").setPakkage(PACKAGE_NAME).build());
    TypeNode outputType =
        TypeNode.withReference(
            VaporReference.builder().setName("PagedResponse").setPakkage(PACKAGE_NAME).build());
    List<MethodArgument> methodArguments = Collections.emptyList();
    Method method =
        Method.builder()
            .setName("simplePagedMethod")
            .setMethodSignatures(Arrays.asList(methodArguments))
            .setInputType(inputType)
            .setOutputType(outputType)
            .setIsPaged(true)
            .build();
    Field responseField =
        Field.builder()
            .setName("response")
            .setType(
                TypeNode.withReference(
                    ConcreteReference.builder()
                        .setClazz(List.class)
                        .setGenerics(ConcreteReference.withClazz(String.class))
                        .build()))
            .setIsMessage(true)
            .setIsRepeated(false)
            .build();
    Field nextPageToken =
        Field.builder().setName("next_page_token").setType(TypeNode.STRING).build();
    Message noRepeatedFiledMessage =
        Message.builder()
            .setName("PagedResponse")
            .setType(outputType)
            .setFields(Arrays.asList(responseField, nextPageToken))
            .build();
    messageTypes.put("PagedResponse", noRepeatedFiledMessage);
    assertThrows(
        NullPointerException.class,
        () ->
            ServiceClientSampleCodeComposer.composeRpcMethodHeaderSampleCode(
                method, methodArguments, clientType, resourceNames, messageTypes));
  }

  // ==========================================Unary RPC Method Sample Code=======================//
  @Test
  public void composeUnaryRpcMethodSampleCode_resourceNameMethodArgument() {
    TypeNode inputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoRequest").setPakkage(PACKAGE_NAME).build());
    TypeNode outputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoResponse").setPakkage(PACKAGE_NAME).build());
    TypeNode resourceNameType =
        TypeNode.withReference(
            ConcreteReference.withClazz(com.google.api.resourcenames.ResourceName.class));
    MethodArgument arg =
        MethodArgument.builder()
            .setName("parent")
            .setType(resourceNameType)
            .setField(
                Field.builder()
                    .setName("parent")
                    .setType(TypeNode.STRING)
                    .setResourceReference(
                        ResourceReference.withType("showcase.googleapis.com/AnythingGoes"))
                    .build())
            .setIsResourceNameHelper(true)
            .build();
    List<List<MethodArgument>> signatures = Arrays.asList(Arrays.asList(arg));
    Method unaryMethod =
        Method.builder()
            .setName("echo")
            .setMethodSignatures(signatures)
            .setInputType(inputType)
            .setOutputType(outputType)
            .build();
    String results =
        SampleCodeWriter.write(
            ServiceClientSampleCodeComposer.composeUnaryRpcMethodSampleCode(
                unaryMethod, signatures.get(0), clientType, resourceNames));
    String expected =
        LineFormatter.lines(
            "try (EchoClient echoClient = EchoClient.create()) {\n",
            "  ResourceName parent = FoobarName.ofProjectFoobarName(\"[PROJECT]\", \"[FOOBAR]\");\n",
            "  EchoResponse response = echoClient.echo(parent);\n",
            "}");
    assertEquals(expected, results);
  }

  @Test
  public void composeUnaryRpcMethodSampleCode_superReferenceIsResourceNameMethodArgument() {
    TypeNode inputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoRequest").setPakkage(PACKAGE_NAME).build());
    TypeNode outputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoResponse").setPakkage(PACKAGE_NAME).build());
    TypeNode methodArgType =
        TypeNode.withReference(
            VaporReference.builder()
                .setName("FoobarName")
                .setPakkage(PACKAGE_NAME)
                .setSupertypeReference(
                    ConcreteReference.withClazz(com.google.api.resourcenames.ResourceName.class))
                .build());
    Field methodArgField =
        Field.builder()
            .setName("name")
            .setType(TypeNode.STRING)
            .setResourceReference(ResourceReference.withType("showcase.googleapis.com/Foobar"))
            .build();
    MethodArgument arg =
        MethodArgument.builder()
            .setName("name")
            .setType(methodArgType)
            .setField(methodArgField)
            .setIsResourceNameHelper(true)
            .build();
    List<List<MethodArgument>> signatures = Arrays.asList(Arrays.asList(arg));
    Method unaryMethod =
        Method.builder()
            .setName("echo")
            .setMethodSignatures(signatures)
            .setInputType(inputType)
            .setOutputType(outputType)
            .build();
    String results =
        SampleCodeWriter.write(
            ServiceClientSampleCodeComposer.composeUnaryRpcMethodSampleCode(
                unaryMethod, signatures.get(0), clientType, resourceNames));
    String expected =
        LineFormatter.lines(
            "try (EchoClient echoClient = EchoClient.create()) {\n",
            "  FoobarName name = FoobarName.ofProjectFoobarName(\"[PROJECT]\", \"[FOOBAR]\");\n",
            "  EchoResponse response = echoClient.echo(name);\n",
            "}");
    assertEquals(expected, results);
  }

  @Test
  public void composeUnaryRpcMethodSampleCode_stringWithResourceReferenceMethodArgument() {
    TypeNode inputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoRequest").setPakkage(PACKAGE_NAME).build());
    TypeNode outputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoResponse").setPakkage(PACKAGE_NAME).build());
    Field methodArgField =
        Field.builder()
            .setName("name")
            .setType(TypeNode.STRING)
            .setResourceReference(ResourceReference.withType("showcase.googleapis.com/Foobar"))
            .build();
    MethodArgument arg =
        MethodArgument.builder()
            .setName("name")
            .setType(TypeNode.STRING)
            .setField(methodArgField)
            .build();
    List<List<MethodArgument>> signatures = Arrays.asList(Arrays.asList(arg));
    Method unaryMethod =
        Method.builder()
            .setName("echo")
            .setMethodSignatures(signatures)
            .setInputType(inputType)
            .setOutputType(outputType)
            .build();
    String results =
        SampleCodeWriter.write(
            ServiceClientSampleCodeComposer.composeUnaryRpcMethodSampleCode(
                unaryMethod, signatures.get(0), clientType, resourceNames));
    String expected =
        LineFormatter.lines(
            "try (EchoClient echoClient = EchoClient.create()) {\n",
            "  String name = FoobarName.ofProjectFoobarName(\"[PROJECT]\", \"[FOOBAR]\").toString();\n",
            "  EchoResponse response = echoClient.echo(name);\n",
            "}");
    assertEquals(expected, results);
  }

  @Test
  public void composeUnaryRpcMethodSampleCode_stringWithParentResourceReferenceMethodArgument() {
    TypeNode inputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoRequest").setPakkage(PACKAGE_NAME).build());
    TypeNode outputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoResponse").setPakkage(PACKAGE_NAME).build());
    Field methodArgField =
        Field.builder()
            .setName("parent")
            .setType(TypeNode.STRING)
            .setResourceReference(
                ResourceReference.withChildType("showcase.googleapis.com/AnythingGoes"))
            .build();
    MethodArgument arg =
        MethodArgument.builder()
            .setName("parent")
            .setType(TypeNode.STRING)
            .setField(methodArgField)
            .build();
    List<List<MethodArgument>> signatures = Arrays.asList(Arrays.asList(arg));
    Method unaryMethod =
        Method.builder()
            .setName("echo")
            .setMethodSignatures(signatures)
            .setInputType(inputType)
            .setOutputType(outputType)
            .build();
    String results =
        SampleCodeWriter.write(
            ServiceClientSampleCodeComposer.composeUnaryRpcMethodSampleCode(
                unaryMethod, signatures.get(0), clientType, resourceNames));
    String expected =
        LineFormatter.lines(
            "try (EchoClient echoClient = EchoClient.create()) {\n",
            "  String parent = FoobarName.ofProjectFoobarName(\"[PROJECT]\", \"[FOOBAR]\").toString();\n",
            "  EchoResponse response = echoClient.echo(parent);\n",
            "}");
    assertEquals(expected, results);
  }

  @Test
  public void composeUnaryRpcMethodSampleCode_isMessageMethodArgument() {
    TypeNode inputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoRequest").setPakkage(PACKAGE_NAME).build());
    TypeNode outputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoResponse").setPakkage(PACKAGE_NAME).build());
    TypeNode methodArgType =
        TypeNode.withReference(
            VaporReference.builder().setName("Status").setPakkage("com.google.rpc").build());
    Field methodArgField =
        Field.builder()
            .setName("error")
            .setType(methodArgType)
            .setIsMessage(true)
            .setIsContainedInOneof(true)
            .build();
    MethodArgument arg =
        MethodArgument.builder()
            .setName("error")
            .setType(methodArgType)
            .setField(methodArgField)
            .build();
    List<List<MethodArgument>> signatures = Arrays.asList(Arrays.asList(arg));
    Method unaryMethod =
        Method.builder()
            .setName("echo")
            .setMethodSignatures(signatures)
            .setInputType(inputType)
            .setOutputType(outputType)
            .build();
    String results =
        SampleCodeWriter.write(
            ServiceClientSampleCodeComposer.composeUnaryRpcMethodSampleCode(
                unaryMethod, signatures.get(0), clientType, resourceNames));
    String expected =
        LineFormatter.lines(
            "try (EchoClient echoClient = EchoClient.create()) {\n",
            "  Status error = Status.newBuilder().build();\n",
            "  EchoResponse response = echoClient.echo(error);\n",
            "}");
    assertEquals(expected, results);
  }

  @Test
  public void composeUnaryRpcMethodSampleCode_multipleWordNameMethodArgument() {
    TypeNode inputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoRequest").setPakkage(PACKAGE_NAME).build());
    TypeNode outputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoResponse").setPakkage(PACKAGE_NAME).build());
    Field methodArgField =
        Field.builder()
            .setName("display_name")
            .setType(TypeNode.STRING)
            .setResourceReference(
                ResourceReference.withChildType("showcase.googleapis.com/AnythingGoes"))
            .build();
    Reference userRef = VaporReference.builder().setName("User").setPakkage(PACKAGE_NAME).build();
    Field nestFiled =
        Field.builder()
            .setName("user")
            .setType(TypeNode.withReference(userRef))
            .setIsMessage(true)
            .build();
    MethodArgument argDisplayName =
        MethodArgument.builder()
            .setName("display_name")
            .setType(TypeNode.STRING)
            .setField(methodArgField)
            .setNestedFields(Arrays.asList(nestFiled))
            .build();
    MethodArgument argOtherName =
        MethodArgument.builder()
            .setName("other_name")
            .setType(TypeNode.STRING)
            .setField(Field.builder().setName("other_name").setType(TypeNode.STRING).build())
            .setNestedFields(Arrays.asList(nestFiled))
            .build();
    List<List<MethodArgument>> signatures =
        Arrays.asList(Arrays.asList(argDisplayName, argOtherName));
    Method unaryMethod =
        Method.builder()
            .setName("echo")
            .setMethodSignatures(signatures)
            .setInputType(inputType)
            .setOutputType(outputType)
            .build();
    String results =
        SampleCodeWriter.write(
            ServiceClientSampleCodeComposer.composeUnaryRpcMethodSampleCode(
                unaryMethod, signatures.get(0), clientType, resourceNames));
    String expected =
        LineFormatter.lines(
            "try (EchoClient echoClient = EchoClient.create()) {\n",
            "  String displayName = FoobarName.ofProjectFoobarName(\"[PROJECT]\", \"[FOOBAR]\").toString();\n",
            "  String otherName = \"other_name-182411686\";\n",
            "  EchoResponse response = echoClient.echo(displayName, otherName);\n",
            "}");
    assertEquals(expected, results);
  }

  @Test
  public void composeUnaryRpcMethodSampleCode_stringIsContainedInOneOfMethodArgument() {
    TypeNode inputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoRequest").setPakkage(PACKAGE_NAME).build());
    TypeNode outputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoResponse").setPakkage(PACKAGE_NAME).build());
    Field methodArgField =
        Field.builder()
            .setName("content")
            .setType(TypeNode.STRING)
            .setIsContainedInOneof(true)
            .build();
    MethodArgument arg =
        MethodArgument.builder()
            .setName("content")
            .setType(TypeNode.STRING)
            .setField(methodArgField)
            .build();
    List<List<MethodArgument>> signatures = Arrays.asList(Arrays.asList(arg));
    Method unaryMethod =
        Method.builder()
            .setName("echo")
            .setMethodSignatures(signatures)
            .setInputType(inputType)
            .setOutputType(outputType)
            .build();
    String results =
        SampleCodeWriter.write(
            ServiceClientSampleCodeComposer.composeUnaryRpcMethodSampleCode(
                unaryMethod, signatures.get(0), clientType, resourceNames));
    String expected =
        LineFormatter.lines(
            "try (EchoClient echoClient = EchoClient.create()) {\n",
            "  String content = \"content951530617\";\n",
            "  EchoResponse response = echoClient.echo(content);\n",
            "}");
    assertEquals(expected, results);
  }

  @Test
  public void composeUnaryRpcMethodSampleCode_multipleMethodArguments() {
    TypeNode inputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoRequest").setPakkage(PACKAGE_NAME).build());
    TypeNode outputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoResponse").setPakkage(PACKAGE_NAME).build());
    MethodArgument arg1 =
        MethodArgument.builder()
            .setName("content")
            .setType(TypeNode.STRING)
            .setField(Field.builder().setName("content").setType(TypeNode.STRING).build())
            .build();
    TypeNode severityType =
        TypeNode.withReference(
            VaporReference.builder().setName("Severity").setPakkage(PACKAGE_NAME).build());
    MethodArgument arg2 =
        MethodArgument.builder()
            .setName("severity")
            .setType(severityType)
            .setField(
                Field.builder().setName("severity").setType(severityType).setIsEnum(true).build())
            .build();
    List<List<MethodArgument>> signatures = Arrays.asList(Arrays.asList(arg1, arg2));
    Method unaryMethod =
        Method.builder()
            .setName("echo")
            .setMethodSignatures(signatures)
            .setInputType(inputType)
            .setOutputType(outputType)
            .build();
    String results =
        SampleCodeWriter.write(
            ServiceClientSampleCodeComposer.composeUnaryRpcMethodSampleCode(
                unaryMethod, signatures.get(0), clientType, resourceNames));
    String expected =
        LineFormatter.lines(
            "try (EchoClient echoClient = EchoClient.create()) {\n",
            "  String content = \"content951530617\";\n",
            "  Severity severity = Severity.forNumber(0);\n",
            "  EchoResponse response = echoClient.echo(content, severity);\n",
            "}");
    assertEquals(expected, results);
  }

  @Test
  public void composeUnaryRpcMethodSampleCode_noMethodArguments() {
    TypeNode inputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoRequest").setPakkage(PACKAGE_NAME).build());
    TypeNode outputType =
        TypeNode.withReference(
            VaporReference.builder().setName("EchoResponse").setPakkage(PACKAGE_NAME).build());
    List<List<MethodArgument>> signatures = Arrays.asList(Collections.emptyList());
    Method unaryMethod =
        Method.builder()
            .setName("echo")
            .setMethodSignatures(signatures)
            .setInputType(inputType)
            .setOutputType(outputType)
            .build();
    String results =
        SampleCodeWriter.write(
            ServiceClientSampleCodeComposer.composeUnaryRpcMethodSampleCode(
                unaryMethod, signatures.get(0), clientType, resourceNames));
    String expected =
        LineFormatter.lines(
            "try (EchoClient echoClient = EchoClient.create()) {\n",
            "  EchoResponse response = echoClient.echo();\n",
            "}");
    assertEquals(expected, results);
  }

  @Test
  public void composeUnaryRpcMethodSampleCode_methodReturnVoid() {
    TypeNode inputType =
        TypeNode.withReference(
            VaporReference.builder()
                .setName("DeleteUserRequest")
                .setPakkage("com.google.showcase.v1beta1")
                .build());
    TypeNode outputType =
        TypeNode.withReference(
            VaporReference.builder().setName("Empty").setPakkage("com.google.protobuf").build());
    List<List<MethodArgument>> methodSignatures =
        Arrays.asList(
            Arrays.asList(
                MethodArgument.builder()
                    .setName("name")
                    .setType(TypeNode.STRING)
                    .setField(Field.builder().setName("name").setType(TypeNode.STRING).build())
                    .build()));
    Method unaryMethod =
        Method.builder()
            .setName("delete")
            .setMethodSignatures(methodSignatures)
            .setInputType(inputType)
            .setOutputType(outputType)
            .build();
    String results =
        SampleCodeWriter.write(
            ServiceClientSampleCodeComposer.composeUnaryRpcMethodSampleCode(
                unaryMethod, methodSignatures.get(0), clientType, resourceNames));
    String expected =
        LineFormatter.lines(
            "try (EchoClient echoClient = EchoClient.create()) {\n",
            "  String name = \"name3373707\";\n",
            "  echoClient.delete(name);\n",
            "}");
    Assert.assertEquals(results, expected);
  }

  // ===================================Unary Paged RPC Method Sample Code ======================//
  @Test
  public void validComposeUnaryPagedRpcMethodSampleCode_multipleMethodArguments() {
    TypeNode inputType =
        TypeNode.withReference(
            VaporReference.builder()
                .setName("ListContentRequest")
                .setPakkage(PACKAGE_NAME)
                .build());
    TypeNode outputType =
        TypeNode.withReference(
            VaporReference.builder()
                .setName("ListContentResponse")
                .setPakkage(PACKAGE_NAME)
                .build());
    TypeNode resourceNameType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(List.class)
                .setGenerics(ConcreteReference.withClazz(String.class))
                .build());
    List<MethodArgument> arguments =
        Arrays.asList(
            MethodArgument.builder()
                .setName("resourceName")
                .setType(resourceNameType)
                .setField(
                    Field.builder()
                        .setName("resourceName")
                        .setType(resourceNameType)
                        .setIsRepeated(true)
                        .build())
                .build(),
            MethodArgument.builder()
                .setName("filter")
                .setType(TypeNode.STRING)
                .setField(Field.builder().setName("filter").setType(TypeNode.STRING).build())
                .build());
    Method method =
        Method.builder()
            .setName("ListContent")
            .setMethodSignatures(Arrays.asList(arguments))
            .setInputType(inputType)
            .setOutputType(outputType)
            .setIsPaged(true)
            .build();
    TypeNode repeatedResponseType =
        TypeNode.withReference(
            VaporReference.builder().setName("Content").setPakkage(PACKAGE_NAME).build());

    String results =
        SampleCodeWriter.write(
            ServiceClientSampleCodeComposer.composeUnaryPagedRpcMethodSampleCode(
                method, arguments, clientType, resourceNames, repeatedResponseType));
    String expected =
        LineFormatter.lines(
            "try (EchoClient echoClient = EchoClient.create()) {\n",
            "  List<String> resourceName = new ArrayList<>();\n",
            "  String filter = \"filter-1274492040\";\n",
            "  for (Content element : echoClient.listContent(resourceName, filter).iterateAll()) {\n",
            "    // doThingsWith(element);\n",
            "  }\n",
            "}");
    Assert.assertEquals(results, expected);
  }

  @Test
  public void validComposeUnaryPagedRpcMethodSampleCode_noMethodArguments() {
    TypeNode inputType =
        TypeNode.withReference(
            VaporReference.builder()
                .setName("ListContentRequest")
                .setPakkage(PACKAGE_NAME)
                .build());
    TypeNode outputType =
        TypeNode.withReference(
            VaporReference.builder()
                .setName("ListContentResponse")
                .setPakkage(PACKAGE_NAME)
                .build());
    List<MethodArgument> arguments = Collections.emptyList();
    Method method =
        Method.builder()
            .setName("ListContent")
            .setMethodSignatures(Arrays.asList(arguments))
            .setInputType(inputType)
            .setOutputType(outputType)
            .setIsPaged(true)
            .build();
    TypeNode repeatedResponseType =
        TypeNode.withReference(
            VaporReference.builder().setName("Content").setPakkage(PACKAGE_NAME).build());

    String results =
        SampleCodeWriter.write(
            ServiceClientSampleCodeComposer.composeUnaryPagedRpcMethodSampleCode(
                method, arguments, clientType, resourceNames, repeatedResponseType));
    String expected =
        LineFormatter.lines(
            "try (EchoClient echoClient = EchoClient.create()) {\n",
            "  for (Content element : echoClient.listContent().iterateAll()) {\n",
            "    // doThingsWith(element);\n",
            "  }\n",
            "}");
    Assert.assertEquals(results, expected);
  }
}
