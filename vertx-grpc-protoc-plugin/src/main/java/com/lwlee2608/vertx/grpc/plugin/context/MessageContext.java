package com.lwlee2608.vertx.grpc.plugin.context;

import java.util.ArrayList;
import java.util.List;

public class MessageContext {
    public String className;
    public String packageName;
    public String protoPackage;
    public String name;
    public final List<FieldContext> fields = new ArrayList<>();

    public String pojoFullName() {
        return packageName + "." + className;
    }

    public String protoFullName() {
        return protoPackage + "." + className;
    }
}
