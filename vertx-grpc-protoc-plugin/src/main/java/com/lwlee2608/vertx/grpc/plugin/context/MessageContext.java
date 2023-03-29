package com.lwlee2608.vertx.grpc.plugin.context;

import java.util.ArrayList;
import java.util.List;

public class MessageContext {
    public String className;
    public String packageName;
    public String name;
    public final List<FieldContext> fields = new ArrayList<>();
    public String pojoPackageName;

    public String pojoFullName() {
        return pojoPackageName + "." + className;
    }

    public String fullName() {
        return packageName + "." + className;
    }
}
