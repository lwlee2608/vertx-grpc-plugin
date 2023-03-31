package com.lwlee2608.vertx.grpc.plugin.context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessageContext {
    public String className;
    public String packageName;
    public String protoPackage;
    public String name;
    public final List<FieldContext> fields = new ArrayList<>();
    public final Set<String> imports = new HashSet<>();

    public String pojoFullName() {
        return packageName + "." + className;
    }

    public String protoFullName() {
        return protoPackage + "." + className;
    }
}
