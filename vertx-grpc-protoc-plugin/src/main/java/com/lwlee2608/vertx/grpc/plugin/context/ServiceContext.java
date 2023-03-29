package com.lwlee2608.vertx.grpc.plugin.context;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceContext {
    // CHECKSTYLE DISABLE VisibilityModifier FOR 8 LINES
    public String protoName;
    public String serviceName;
    public boolean deprecated;
    public String javaDoc;
    public final List<MethodContext> methods = new ArrayList<>();

    public List<MethodContext> streamMethods() {
        return methods.stream().filter(m -> m.isManyInput || m.isManyOutput).collect(Collectors.toList());
    }

    public List<MethodContext> unaryMethods() {
        return methods.stream().filter(m -> !m.isManyInput && !m.isManyOutput).collect(Collectors.toList());
    }

    public List<MethodContext> unaryManyMethods() {
        return methods.stream().filter(m -> !m.isManyInput && m.isManyOutput).collect(Collectors.toList());
    }

    public List<MethodContext> manyUnaryMethods() {
        return methods.stream().filter(m -> m.isManyInput && !m.isManyOutput).collect(Collectors.toList());
    }

    public List<MethodContext> manyManyMethods() {
        return methods.stream().filter(m -> m.isManyInput && m.isManyOutput).collect(Collectors.toList());
    }
}
