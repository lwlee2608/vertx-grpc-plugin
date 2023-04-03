package com.lwlee2608.vertx.grpc.plugin;

import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos;
import com.lwlee2608.vertx.grpc.plugin.context.ComponentContext;
import com.lwlee2608.vertx.grpc.plugin.context.ComponentType;
import com.lwlee2608.vertx.grpc.plugin.context.Context;
import com.lwlee2608.vertx.grpc.plugin.context.FieldContext;
import com.lwlee2608.vertx.grpc.plugin.context.MessageContext;
import com.lwlee2608.vertx.grpc.plugin.context.MethodContext;
import com.lwlee2608.vertx.grpc.plugin.context.ServiceContext;
import com.salesforce.jprotoc.Generator;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtoTypeMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AbstractVertxGenerator extends Generator {

    private static final int SERVICE_NUMBER_OF_PATHS = 2;
    private static final int METHOD_NUMBER_OF_PATHS = 4;
    private static final String CLASS_PREFIX = "Vertx";

    private final String clientTemplate;
    private final String serverTemplate;

    // A replacement for the original ProtoTypeMap, we need to store the entire MessageContext
    private final Map<String, MessageContext> pojoTypeMap = new HashMap<>();

    public AbstractVertxGenerator(String clientTemplate, String serverTemplate) {
        this.clientTemplate = clientTemplate;
        this.serverTemplate = serverTemplate;
    }

    private String getServiceJavaDocPrefix() {
        return "    ";
    }

    private String getMethodJavaDocPrefix() {
        return "        ";
    }

    @Override
    protected List<PluginProtos.CodeGeneratorResponse.Feature> supportedFeatures() {
        return Collections.singletonList(PluginProtos.CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL);
    }

    @Override
    public List<PluginProtos.CodeGeneratorResponse.File> generateFiles(PluginProtos.CodeGeneratorRequest request) throws GeneratorException {
        ProtoTypeMap typeMap = ProtoTypeMap.of(request.getProtoFileList());

        List<DescriptorProtos.FileDescriptorProto> protosToGenerate = request.getProtoFileList().stream()
                .filter(protoFile -> request.getFileToGenerateList().contains(protoFile.getName()))
                .collect(Collectors.toList());

        Context context = new Context();
        context.messages.addAll(protosToGenerate.stream()
                .flatMap(fileProto -> {
                    String packageName = extractPackageName(fileProto);
                    String outerClassName = extractOuterClassname(fileProto);
                    return fileProto.getMessageTypeList()
                            .stream()
                            .map(msg -> {
                                MessageContext messageContext = buildMessageContext(msg, packageName, outerClassName, typeMap);
                                String key = "." + fileProto.getPackage() + "." + msg.getName();
                                pojoTypeMap.put(key, messageContext);
                                return messageContext;
                            });
                })
                .collect(Collectors.toList()));

        // update pojo imports
        pojoTypeMap.forEach((typeName, messageContext) -> {
            messageContext.fields.forEach(fieldContext -> {
                if (fieldContext.isMessage) {
                    messageContext.imports.add(pojoTypeMap.get(fieldContext.protoTypeName).packageName + "." + fieldContext.javaType);
                }
            });
        });

        context.components.addAll(buildComponents(protosToGenerate, typeMap));
        return generateFiles(context);
    }

    private List<ComponentContext> buildComponents(List<DescriptorProtos.FileDescriptorProto> protos, ProtoTypeMap typeMap) {
        List<ComponentContext> contexts = new ArrayList<>();

        protos.forEach(fileProto -> {
            findServices(fileProto, typeMap).forEach(service -> {
                String packageName = extractPackageName(fileProto);
                // Client
                ComponentContext clientContext = new ComponentContext();
                clientContext.type = ComponentType.Client;
                clientContext.service = service;
                clientContext.className = CLASS_PREFIX + service.serviceName + "GrpcClient";
                clientContext.packageName = packageName + ".component";
                clientContext.protoPackage = packageName;
                clientContext.imports.add(packageName + "." + service.serviceName + "Grpc");
                service.methods.forEach(method -> {
                    clientContext.imports.add(method.inputTypeFullName);
                    clientContext.imports.add(method.outputTypeFullName);
                });
                contexts.add(clientContext);

                // Server
                ComponentContext serverContext = new ComponentContext();
                serverContext.type = ComponentType.Server;
                serverContext.service = service;
                serverContext.className = CLASS_PREFIX + service.serviceName + "GrpcServer";
                serverContext.packageName = packageName + ".component";
                serverContext.protoPackage = packageName;
                serverContext.imports.add(packageName + "." + service.serviceName + "Grpc");
                service.methods.forEach(method -> {
                    serverContext.imports.add(method.inputTypeFullName);
                    serverContext.imports.add(method.outputTypeFullName);
                });
                contexts.add(serverContext);
            });
        });

        return contexts;
    }

    private List<ServiceContext> findServices(DescriptorProtos.FileDescriptorProto fileProto, ProtoTypeMap typeMap) {
        List<ServiceContext> contexts = new ArrayList<>();

        for (int serviceNumber = 0; serviceNumber < fileProto.getServiceCount(); serviceNumber++) {
            ServiceContext serviceContext = buildServiceContext(
                    fileProto.getService(serviceNumber),
                    typeMap,
                    fileProto.getSourceCodeInfo().getLocationList(),
                    serviceNumber
            );
            serviceContext.protoName = fileProto.getName();
            contexts.add(serviceContext);
        }

        return contexts;
    }

    private MessageContext buildMessageContext(DescriptorProtos.DescriptorProto descriptor, String packageName, String outerClassname, ProtoTypeMap typeMap) {
        MessageContext messageContext = new MessageContext();
        messageContext.name = descriptor.getName();
        messageContext.className = messageContext.name;
        messageContext.packageName = packageName + ".pojo";
        messageContext.protoPackage = packageName;
        messageContext.outerClassname = outerClassname;

        // populate fields
        descriptor.getFieldList().forEach(fieldDescriptor -> {
            FieldContext fieldContext = new FieldContext();
            fieldContext.protoName = fieldDescriptor.getName();
            fieldContext.protoTypeName = fieldDescriptor.getTypeName();
            fieldContext.name = Util.camelCase(fieldDescriptor.getName());
            fieldContext.javaType = getJavaType(fieldDescriptor);
            fieldContext.defaultValue = getDefaultValue(fieldDescriptor);
            fieldContext.isEnum = fieldDescriptor.getType() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM;
            fieldContext.isNullable = isNullable(fieldDescriptor);
            fieldContext.isMessage = isMessage(fieldDescriptor);
            fieldContext.isList = isList(fieldDescriptor);
            if (fieldContext.isList) {
                messageContext.imports.add("java.util.List");
                messageContext.imports.add("java.util.ArrayList");
                messageContext.imports.add("java.util.stream.Collectors");
            }

            if (fieldContext.isEnum) {
                // TODO Enum type may be defined in another proto file
                if (Strings.isNullOrEmpty(outerClassname)) {
                    messageContext.imports.add(packageName + "." + fieldContext.javaType);
                } else {
                    messageContext.imports.add(packageName + "." + messageContext.outerClassname + "." + fieldContext.javaType);
                }
            }
            if (fieldContext.isNullable && !fieldContext.isMessage) {
                fieldContext.nullableType = Util.getSimpleClass(fieldDescriptor.getTypeName());
                messageContext.imports.add("com.google.protobuf." + fieldContext.nullableType);
            }
            if (fieldContext.javaType.equals("ByteString")) {
                messageContext.imports.add("com.google.protobuf.ByteString");
            }
            messageContext.fields.add(fieldContext);
        });
        return messageContext;
    }

    private String getJavaType(DescriptorProtos.FieldDescriptorProto descriptor) {
        switch (descriptor.getType()) {
            case TYPE_DOUBLE:
                return "Double";
            case TYPE_FLOAT:
                return "Float";
            case TYPE_INT64:
            case TYPE_FIXED64:
            case TYPE_SFIXED64:
            case TYPE_SINT64:
            case TYPE_UINT64:
                return "Long";
            case TYPE_INT32:
            case TYPE_UINT32:
            case TYPE_SFIXED32:
            case TYPE_SINT32:
            case TYPE_FIXED32:
                return "Integer";
            case TYPE_BOOL:
                return "Boolean";
            case TYPE_STRING:
                return "String";
            case TYPE_MESSAGE: {
                String typeName = descriptor.getTypeName();
                switch (typeName) {
                    case ".google.protobuf.Int32Value":
                    case ".google.protobuf.UInt32Value":
                        return "Integer";
                    case ".google.protobuf.Int64Value":
                    case ".google.protobuf.UInt64Value":
                        return "Long";
                    case ".google.protobuf.StringValue":
                        return "String";
                    case ".google.protobuf.BoolValue":
                        return "Boolean";
                    case ".google.protobuf.FloatValue":
                        return "Float";
                    case ".google.protobuf.DoubleValue":
                        return "Double";
                    case ".google.protobuf.BytesValue":
                        return "Bytes";
                    default:
                        return Util.getSimpleClass(typeName);
                }
            }
            case TYPE_ENUM:
                return Util.getSimpleClass(descriptor.getTypeName());
            case TYPE_BYTES:
                return "ByteString";
            case TYPE_GROUP:
            default:
                throw new RuntimeException("Type '" + descriptor.getType() + "' Not supported yet");
        }
    }

    private String getDefaultValue(DescriptorProtos.FieldDescriptorProto descriptor) {
        switch (descriptor.getType()) {
            case TYPE_DOUBLE:
            case TYPE_FLOAT:
                return "0.0";
            case TYPE_INT64:
            case TYPE_FIXED64:
            case TYPE_SFIXED64:
            case TYPE_SINT64:
            case TYPE_UINT64:
                return "0L";
            case TYPE_INT32:
            case TYPE_UINT32:
            case TYPE_SFIXED32:
            case TYPE_SINT32:
            case TYPE_FIXED32:
                return "0";
            case TYPE_BOOL:
                return "false";
            case TYPE_STRING:
                return "\"\"";
            case TYPE_MESSAGE:
                return "null";
            case TYPE_ENUM: {
                return Util.getSimpleClass(descriptor.getTypeName()) + ".valueOf(0)";
            }
            case TYPE_BYTES:
                return "ByteString.EMPTY";
            case TYPE_GROUP:
            default:
                throw new RuntimeException("Type '" + descriptor.getType() + "' Not supported yet");
        }
    }

    private boolean isNullable(DescriptorProtos.FieldDescriptorProto descriptor) {
        if (isList(descriptor)) {
            return false; // repeated field must not be nullable
        }
        return descriptor.getType() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE;
    }

    private boolean isMessage(DescriptorProtos.FieldDescriptorProto descriptor) {
        if (descriptor.getType() == DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE) {
            String typeName = descriptor.getTypeName();
            switch (typeName) {
                case ".google.protobuf.Int32Value":
                case ".google.protobuf.UInt32Value":
                case ".google.protobuf.Int64Value":
                case ".google.protobuf.UInt64Value":
                case ".google.protobuf.StringValue":
                case ".google.protobuf.BoolValue":
                case ".google.protobuf.FloatValue":
                case ".google.protobuf.DoubleValue":
                case ".google.protobuf.BytesValue":
                    return false;
                default:
                    return true;
            }
        }
        return false;
    }

    private boolean isList(DescriptorProtos.FieldDescriptorProto descriptor) {
        if (descriptor.hasLabel() && descriptor.getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED) {
            return true;
        } else {
            return false;
        }
    }

    private String extractPackageName(DescriptorProtos.FileDescriptorProto proto) {
        DescriptorProtos.FileOptions options = proto.getOptions();
        if (options != null) {
            String javaPackage = options.getJavaPackage();
            if (!Strings.isNullOrEmpty(javaPackage)) {
                return javaPackage;
            }
        }

        return Strings.nullToEmpty(proto.getPackage());
    }

    private String extractOuterClassname(DescriptorProtos.FileDescriptorProto proto) {
        DescriptorProtos.FileOptions options = proto.getOptions();
        if (options != null) {
            if (!options.getJavaMultipleFiles()) {
                String outerClassname = options.getJavaOuterClassname();
                if (!Strings.isNullOrEmpty(outerClassname)) {
                    return outerClassname;
                } else {
                    StringBuilder filename = new StringBuilder(proto.getName().replace(".proto", ""));
                    filename.setCharAt(0, Character.toUpperCase(filename.charAt(0)));
                    return filename.toString();
                }
            }
        }
        return null;
    }

    private ServiceContext buildServiceContext(DescriptorProtos.ServiceDescriptorProto serviceProto, ProtoTypeMap typeMap, List<DescriptorProtos.SourceCodeInfo.Location> locations, int serviceNumber) {
        ServiceContext serviceContext = new ServiceContext();
        // Set Later
        //serviceContext.fileName = CLASS_PREFIX + serviceProto.getName() + "Grpc.java";
        //serviceContext.className = CLASS_PREFIX + serviceProto.getName() + "Grpc";
        serviceContext.serviceName = serviceProto.getName();
        serviceContext.deprecated = serviceProto.getOptions() != null && serviceProto.getOptions().getDeprecated();

        List<DescriptorProtos.SourceCodeInfo.Location> allLocationsForService = locations.stream()
                .filter(location ->
                        location.getPathCount() >= 2 &&
                                location.getPath(0) == DescriptorProtos.FileDescriptorProto.SERVICE_FIELD_NUMBER &&
                                location.getPath(1) == serviceNumber
                )
                .collect(Collectors.toList());

        DescriptorProtos.SourceCodeInfo.Location serviceLocation = allLocationsForService.stream()
                .filter(location -> location.getPathCount() == SERVICE_NUMBER_OF_PATHS)
                .findFirst()
                .orElseGet(DescriptorProtos.SourceCodeInfo.Location::getDefaultInstance);
        serviceContext.javaDoc = getJavaDoc(getComments(serviceLocation), getServiceJavaDocPrefix());

        for (int methodNumber = 0; methodNumber < serviceProto.getMethodCount(); methodNumber++) {
            MethodContext methodContext = buildMethodContext(
                    serviceProto.getMethod(methodNumber),
                    typeMap,
                    locations,
                    methodNumber
            );

            serviceContext.methods.add(methodContext);
        }
        return serviceContext;
    }

    private MethodContext buildMethodContext(DescriptorProtos.MethodDescriptorProto methodProto, ProtoTypeMap typeMap, List<DescriptorProtos.SourceCodeInfo.Location> locations, int methodNumber) {
        MethodContext methodContext = new MethodContext();
        methodContext.methodName = Util.camelCase(methodProto.getName());

        MessageContext inputMessage = pojoTypeMap.get(methodProto.getInputType());
        MessageContext outputMessage = pojoTypeMap.get(methodProto.getOutputType());
        methodContext.inputMessage = inputMessage;
        methodContext.outputMessage = outputMessage;
        methodContext.inputType = inputMessage.className;
        methodContext.outputType = outputMessage.className;
        methodContext.inputTypeFullName = inputMessage.pojoFullName();
        methodContext.outputTypeFullName = outputMessage.pojoFullName();

        methodContext.deprecated = methodProto.getOptions() != null && methodProto.getOptions().getDeprecated();
        methodContext.isManyInput = methodProto.getClientStreaming();
        methodContext.isManyOutput = methodProto.getServerStreaming();
        methodContext.methodNumber = methodNumber;

        DescriptorProtos.SourceCodeInfo.Location methodLocation = locations.stream()
                .filter(location ->
                        location.getPathCount() == METHOD_NUMBER_OF_PATHS &&
                                location.getPath(METHOD_NUMBER_OF_PATHS - 1) == methodNumber
                )
                .findFirst()
                .orElseGet(DescriptorProtos.SourceCodeInfo.Location::getDefaultInstance);
        methodContext.javaDoc = getJavaDoc(getComments(methodLocation), getMethodJavaDocPrefix());
        return methodContext;
    }

    private List<PluginProtos.CodeGeneratorResponse.File> generateFiles(Context context) {
        List<PluginProtos.CodeGeneratorResponse.File> files = new ArrayList<>();

        List<PluginProtos.CodeGeneratorResponse.File> pojoFiles =
                context.messages.stream()
                        .map(this::buildPojo)
                        .collect(Collectors.toList());

        List<PluginProtos.CodeGeneratorResponse.File> componentFiles =
                context.components.stream()
                        .map(this::buildFile)
                        .collect(Collectors.toList());

        files.addAll(pojoFiles);
        files.addAll(componentFiles);
        return files;
    }

    private PluginProtos.CodeGeneratorResponse.File buildPojo(MessageContext context) {
        String content = applyTemplate("pojo/pojo.mustache", context);
        return PluginProtos.CodeGeneratorResponse.File
                .newBuilder()
                .setName(absoluteFileName(context))
                .setContent(content)
                .build();
    }

    private PluginProtos.CodeGeneratorResponse.File buildFile(ComponentContext context) {
        String template = context.type == ComponentType.Server ? serverTemplate : clientTemplate;
        String content = applyTemplate(template, context);
        return PluginProtos.CodeGeneratorResponse.File
                .newBuilder()
                .setName(absoluteFileName(context))
                .setContent(content)
                .build();
    }

    private String absoluteFileName(ComponentContext ctx) {
        String dir = ctx.packageName.replace('.', '/');
        String fileName = ctx.className + ".java";
        if (Strings.isNullOrEmpty(dir)) {
            return fileName;
        } else {
            return dir + "/" + fileName;
        }
    }

    // TODO combine
    private String absoluteFileName(MessageContext ctx) {
        String dir = ctx.packageName.replace('.', '/');
        String fileName = ctx.className + ".java";
        if (Strings.isNullOrEmpty(dir)) {
            return fileName;
        } else {
            return dir + "/" + fileName;
        }
    }

    private String getComments(DescriptorProtos.SourceCodeInfo.Location location) {
        return location.getLeadingComments().isEmpty() ? location.getTrailingComments() : location.getLeadingComments();
    }

    private String getJavaDoc(String comments, String prefix) {
        if (!comments.isEmpty()) {
            StringBuilder builder = new StringBuilder("/**\n")
                    .append(prefix).append(" * <pre>\n");
            Arrays.stream(HtmlEscapers.htmlEscaper().escape(comments).split("\n"))
                    .map(line -> line.replace("*/", "&#42;&#47;").replace("*", "&#42;"))
                    .forEach(line -> builder.append(prefix).append(" * ").append(line).append("\n"));
            builder
                    .append(prefix).append(" * </pre>\n")
                    .append(prefix).append(" */");
            return builder.toString();
        }
        return null;
    }
}