package com.lwlee2608.vertx.grpc.plugin.context;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessageContext {
    public String className;
    public String packageName;
    public String protoPackage;
    public String name;
    public String outerClassname;
    public final List<FieldContext> fields = new ArrayList<>();
    public final Set<String> imports = new HashSet<>();

    public String pojoFullName() {
        return packageName + "." + className;
    }

    public String protoFullName() {
        if (!Strings.isNullOrEmpty(outerClassname)) {
            return protoPackage + "." + outerClassname + "." + className;
        } else {
            return protoPackage + "." + className;
        }
    }
}
