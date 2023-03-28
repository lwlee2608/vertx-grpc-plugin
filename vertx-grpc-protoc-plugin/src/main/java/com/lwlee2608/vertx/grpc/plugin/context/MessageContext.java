package com.lwlee2608.vertx.grpc.plugin.context;

import java.util.ArrayList;
import java.util.List;

public class MessageContext {
    public String fileName;
    public String packageName;
    public String className;
    public String name;
    public final List<FieldContext> fields = new ArrayList<>();
    public String protoObject;
}
