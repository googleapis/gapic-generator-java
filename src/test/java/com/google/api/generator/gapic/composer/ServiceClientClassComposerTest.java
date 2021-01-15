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

import static com.google.api.generator.test.framework.Assert.assertCodeEquals;
import static junit.framework.Assert.assertEquals;

import com.google.api.generator.engine.writer.JavaWriterVisitor;
import com.google.api.generator.gapic.composer.constants.ComposerConstants;
import com.google.api.generator.gapic.model.GapicClass;
import com.google.api.generator.gapic.model.Message;
import com.google.api.generator.gapic.model.ResourceName;
import com.google.api.generator.gapic.model.Service;
import com.google.api.generator.gapic.protoparser.Parser;
import com.google.api.generator.test.framework.Utils;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.showcase.v1beta1.EchoOuterClass;
import com.google.showcase.v1beta1.IdentityOuterClass;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public class ServiceClientClassComposerTest {
  @Test
  public void generateServiceClasses() {
    FileDescriptor echoFileDescriptor = EchoOuterClass.getDescriptor();
    ServiceDescriptor echoService = echoFileDescriptor.getServices().get(0);
    assertEquals(echoService.getName(), "Echo");

    Map<String, Message> messageTypes = Parser.parseMessages(echoFileDescriptor);
    Map<String, ResourceName> resourceNames = Parser.parseResourceNames(echoFileDescriptor);
    Set<ResourceName> outputResourceNames = new HashSet<>();
    List<Service> services =
        Parser.parseService(
            echoFileDescriptor, messageTypes, resourceNames, Optional.empty(), outputResourceNames);

    Service echoProtoService = services.get(0);
    GapicClass clazz =
        ServiceClientClassComposer.instance()
            .generate(echoProtoService, messageTypes, resourceNames);

    JavaWriterVisitor visitor = new JavaWriterVisitor();
    clazz.classDefinition().accept(visitor);
    Utils.saveCodegenToFile(this.getClass(), "EchoClient.golden", visitor.write());
    Path goldenFilePath = Paths.get(ComposerConstants.GOLDENFILES_DIRECTORY, "EchoClient.golden");
    assertCodeEquals(goldenFilePath, visitor.write());
  }

  @Test
  public void generateServiceClasses_methodSignatureHasNestedFields() {
    FileDescriptor fileDescriptor = IdentityOuterClass.getDescriptor();
    ServiceDescriptor identityService = fileDescriptor.getServices().get(0);
    assertEquals(identityService.getName(), "Identity");

    Map<String, Message> messageTypes = Parser.parseMessages(fileDescriptor);
    Map<String, ResourceName> resourceNames = Parser.parseResourceNames(fileDescriptor);
    Set<ResourceName> outputResourceNames = new HashSet<>();
    List<Service> services =
        Parser.parseService(
            fileDescriptor, messageTypes, resourceNames, Optional.empty(), outputResourceNames);

    Service protoService = services.get(0);
    GapicClass clazz =
        ServiceClientClassComposer.instance().generate(protoService, messageTypes, resourceNames);

    JavaWriterVisitor visitor = new JavaWriterVisitor();
    clazz.classDefinition().accept(visitor);
    Utils.saveCodegenToFile(this.getClass(), "IdentityClient.golden", visitor.write());
    Path goldenFilePath =
        Paths.get(ComposerConstants.GOLDENFILES_DIRECTORY, "IdentityClient.golden");
    assertCodeEquals(goldenFilePath, visitor.write());
  }
}
