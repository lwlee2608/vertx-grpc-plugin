package com.lwlee2608.vertx.grpc.plugin.context;

import java.util.HashSet;
import java.util.Set;

public class ComponentContext {
    public ComponentType type;
    public String className;
    public String packageName;
    public String protoPackage;
    public ServiceContext service;
    public final Set<String> imports = new HashSet<>();
}
