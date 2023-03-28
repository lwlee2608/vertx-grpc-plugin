package com.lwlee2608.vertx.grpc.plugin;

import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos;
import com.salesforce.jprotoc.Generator;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtoTypeMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AbstractVertxGenerator extends Generator {

    private static final int SERVICE_NUMBER_OF_PATHS = 2;
    private static final int METHOD_NUMBER_OF_PATHS = 4;
    private static final String CLASS_PREFIX = "Vertx";

    private final String clientTemplate;
    private final String serverTemplate;

    public AbstractVertxGenerator(String clientTemplate, String serverTemplate) {
        this.clientTemplate = clientTemplate;
        this.serverTemplate =  serverTemplate;
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
                    return fileProto.getMessageTypeList()
                            .stream()
                            .map(msg -> buildMessageContext(msg, packageName));
                })
                .collect(Collectors.toList()));

        context.services.addAll(findServices(protosToGenerate, typeMap));
        return generateFiles(context);
    }

    private List<ServiceContext> findServices(List<DescriptorProtos.FileDescriptorProto> protos, ProtoTypeMap typeMap) {
        List<ServiceContext> contexts = new ArrayList<>();

        protos.forEach(fileProto -> {
            for (int serviceNumber = 0; serviceNumber < fileProto.getServiceCount(); serviceNumber++) {
                ServiceContext serviceContext = buildServiceContext(
                        fileProto.getService(serviceNumber),
                        typeMap,
                        fileProto.getSourceCodeInfo().getLocationList(),
                        serviceNumber
                );
                serviceContext.protoName = fileProto.getName();
                serviceContext.packageName = extractPackageName(fileProto);
                contexts.add(serviceContext);
            }
        });

        return contexts;
    }

    private MessageContext buildMessageContext(DescriptorProtos.DescriptorProto descriptor, String packageName) {
        MessageContext messageContext = new MessageContext();
        messageContext.name = descriptor.getName();
        messageContext.packageName = packageName;
        messageContext.fileName = messageContext.name + "Pojo.java";
        messageContext.className = messageContext.name + "Pojo";
        // populate fields
        descriptor.getFieldList().forEach(fieldDescriptor -> {
            FieldContext fieldContext = new FieldContext();
            fieldContext.name = fieldDescriptor.getName();
            messageContext.fields.add(fieldContext);
        });
        return messageContext;
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
        methodContext.methodName = Util.mixedLower(methodProto.getName());
        methodContext.inputType = typeMap.toJavaTypeName(methodProto.getInputType());
        methodContext.outputType = typeMap.toJavaTypeName(methodProto.getOutputType());
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

        List<PluginProtos.CodeGeneratorResponse.File> serviceFiles =
                context.services.stream()
                        .map(this::buildFiles)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());

        files.addAll(pojoFiles);
        files.addAll(serviceFiles);
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

    private List<PluginProtos.CodeGeneratorResponse.File> buildFiles(ServiceContext context) {
        return List.of(
                buildClientFile(context),
                buildServerFile(context));
    }

    private PluginProtos.CodeGeneratorResponse.File buildClientFile(ServiceContext context) {
        context.fileName = CLASS_PREFIX + context.serviceName + "GrpcClient.java";
        context.className = CLASS_PREFIX + context.serviceName + "GrpcClient";
        return buildFile(context, applyTemplate(clientTemplate, context));
    }

    private PluginProtos.CodeGeneratorResponse.File buildServerFile(ServiceContext context) {
        context.fileName = CLASS_PREFIX + context.serviceName + "GrpcServer.java";
        context.className = CLASS_PREFIX + context.serviceName + "GrpcServer";
        return buildFile(context, applyTemplate(serverTemplate, context));
    }

    private PluginProtos.CodeGeneratorResponse.File buildFile(ServiceContext context, String content) {
        return PluginProtos.CodeGeneratorResponse.File
                .newBuilder()
                .setName(absoluteFileName(context))
                .setContent(content)
                .build();
    }

    private String absoluteFileName(ServiceContext ctx) {
        String dir = ctx.packageName.replace('.', '/');
        if (Strings.isNullOrEmpty(dir)) {
            return ctx.fileName;
        } else {
            return dir + "/" + ctx.fileName;
        }
    }

    // TODO combine
    private String absoluteFileName(MessageContext ctx) {
        String dir = ctx.packageName.replace('.', '/');
        if (Strings.isNullOrEmpty(dir)) {
            return ctx.fileName;
        } else {
            return dir + "/" + ctx.fileName;
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