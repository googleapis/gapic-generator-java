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

package com.google.api.generator.gapic.protoparser;

import com.google.api.ClientProto;
import com.google.api.generator.engine.ast.TypeNode;
import com.google.api.generator.engine.ast.VaporReference;
import com.google.api.generator.gapic.model.Field;
import com.google.api.generator.gapic.model.LongrunningOperation;
import com.google.api.generator.gapic.model.Message;
import com.google.api.generator.gapic.model.Method;
import com.google.api.generator.gapic.model.Service;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.longrunning.OperationInfo;
import com.google.longrunning.OperationsProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodOptions;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Parser {
  // Collapses any whitespace between elements.
  private static final String METHOD_SIGNATURE_DELIMITER = "\\s*,\\s*";

  static class GapicParserException extends RuntimeException {
    public GapicParserException(String errorMessage) {
      super(errorMessage);
    }
  }

  public static List<Service> parseServices(
      CodeGeneratorRequest request, Map<String, Message> messageTypes) {
    Map<String, FileDescriptor> fileDescriptors = getFilesToGenerate(request);
    List<Service> services = new ArrayList<>();
    for (String fileToGenerate : request.getFileToGenerateList()) {
      FileDescriptor fileDescriptor =
          Preconditions.checkNotNull(
              fileDescriptors.get(fileToGenerate),
              "Missing file descriptor for [%s]",
              fileToGenerate);

      services.addAll(parseService(fileDescriptor, messageTypes));
    }

    return services;
  }

  public static List<Service> parseService(
      FileDescriptor fileDescriptor, Map<String, Message> messageTypes) {
    String pakkage = TypeParser.getPackage(fileDescriptor);
    return fileDescriptor.getServices().stream()
        .map(
            s ->
                Service.builder()
                    .setName(s.getName())
                    .setPakkage(pakkage)
                    .setMethods(parseMethods(s, messageTypes))
                    .build())
        .collect(Collectors.toList());
  }

  public static Map<String, Message> parseMessages(CodeGeneratorRequest request) {
    Map<String, FileDescriptor> fileDescriptors = getFilesToGenerate(request);
    Map<String, Message> messages = new HashMap<>();
    for (String fileToGenerate : request.getFileToGenerateList()) {
      FileDescriptor fileDescriptor =
          Preconditions.checkNotNull(
              fileDescriptors.get(fileToGenerate),
              "Missing file descriptor for [%s]",
              fileToGenerate);
      messages.putAll(parseMessages(fileDescriptor));
    }
    return messages;
  }

  public static Map<String, Message> parseMessages(FileDescriptor fileDescriptor) {
    String pakkage = TypeParser.getPackage(fileDescriptor);
    return fileDescriptor.getMessageTypes().stream()
        .collect(
            Collectors.toMap(
                md -> md.getName(),
                md ->
                    Message.builder()
                        .setType(
                            TypeNode.withReference(
                                VaporReference.builder()
                                    .setName(md.getName())
                                    .setPakkage(pakkage)
                                    .build()))
                        .setName(md.getName())
                        .setFields(parseFields(md))
                        .build()));
  }

  @VisibleForTesting
  static List<Method> parseMethods(
      ServiceDescriptor serviceDescriptor, Map<String, Message> messageTypes) {
    return serviceDescriptor.getMethods().stream()
        .map(
            md ->
                Method.builder()
                    .setName(md.getName())
                    .setInputType(TypeParser.parseType(md.getInputType()))
                    .setOutputType(TypeParser.parseType(md.getOutputType()))
                    .setStream(Method.toStream(md.isClientStreaming(), md.isServerStreaming()))
                    .setLro(parseLro(md, messageTypes))
                    .setMethodSignatures(parseMethodSignatures(md))
                    .build())
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  static LongrunningOperation parseLro(
      MethodDescriptor methodDescriptor, Map<String, Message> messageTypes) {
    MethodOptions methodOptions = methodDescriptor.getOptions();
    if (!methodOptions.hasExtension(OperationsProto.operationInfo)) {
      return null;
    }

    OperationInfo lroInfo =
        methodDescriptor.getOptions().getExtension(OperationsProto.operationInfo);
    String responseTypeName = lroInfo.getResponseType();
    String metadataTypeName = lroInfo.getMetadataType();
    Message responseMessage = messageTypes.get(responseTypeName);
    Message metadataMessage = messageTypes.get(metadataTypeName);
    Preconditions.checkNotNull(
        responseMessage, String.format("LRO response message %s not found", responseTypeName));
    Preconditions.checkNotNull(
        metadataMessage, String.format("LRO metadata message %s not found", metadataTypeName));

    return LongrunningOperation.withTypes(responseMessage.type(), metadataMessage.type());
  }

  @VisibleForTesting
  static List<List<String>> parseMethodSignatures(MethodDescriptor methodDescriptor) {
    List<String> signatures =
        methodDescriptor.getOptions().getExtension(ClientProto.methodSignature);
    // Example from Expand in echo.proto:
    // Input: ["content,error", "content,error,info"].
    // Output: [["content", "error"], ["content", "error", "info"]].
    return methodDescriptor.getOptions().getExtension(ClientProto.methodSignature).stream()
        .map(signature -> Arrays.asList(signature.split(METHOD_SIGNATURE_DELIMITER)))
        .collect(Collectors.toList());
  }

  private static List<Field> parseFields(Descriptor messageDescriptor) {
    return messageDescriptor.getFields().stream()
        .map(f -> Field.builder().setName(f.getName()).setType(TypeParser.parseType(f)).build())
        .collect(Collectors.toList());
  }

  private static Map<String, FileDescriptor> getFilesToGenerate(CodeGeneratorRequest request) {
    // Build the fileDescriptors map so that we can create the FDs for the filesToGenerate.
    Map<String, FileDescriptor> fileDescriptors = Maps.newHashMap();
    for (FileDescriptorProto fileDescriptorProto : request.getProtoFileList()) {
      // Look up the imported files from previous file descriptors.  It is sufficient to look at
      // only previous file descriptors because CodeGeneratorRequest guarantees that the files
      // are sorted in topological order.
      FileDescriptor[] deps = new FileDescriptor[fileDescriptorProto.getDependencyCount()];
      for (int i = 0; i < fileDescriptorProto.getDependencyCount(); i++) {
        String name = fileDescriptorProto.getDependency(i);
        deps[i] =
            Preconditions.checkNotNull(
                fileDescriptors.get(name), "Missing file descriptor for [%s]", name);
      }

      FileDescriptor fileDescriptor = null;
      try {
        fileDescriptor = FileDescriptor.buildFrom(fileDescriptorProto, deps);
      } catch (DescriptorValidationException e) {
        throw new GapicParserException(e.getMessage());
      }

      fileDescriptors.put(fileDescriptor.getName(), fileDescriptor);
    }
    return fileDescriptors;
  }
}
