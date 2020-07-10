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

package com.google.api.generator.gapic;

import com.google.api.generator.gapic.composer.Composer;
import com.google.api.generator.gapic.model.GapicClass;
import com.google.api.generator.gapic.model.Message;
import com.google.api.generator.gapic.model.Service;
import com.google.api.generator.gapic.protoparser.Parser;
import com.google.api.generator.gapic.protowriter.Writer;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import java.util.List;
import java.util.Map;

public class Generator {
  public static CodeGeneratorResponse generateGapic(
      CodeGeneratorRequest request, String outputFilePath) {
    Map<String, Message> messageTypes = Parser.parseMessages(request);
    List<Service> services = Parser.parseServices(request, messageTypes);
    List<GapicClass> clazzes = Composer.composeServiceClasses(services, messageTypes);
    CodeGeneratorResponse response = Writer.writeCode(clazzes, outputFilePath);
    return response;
  }
}
