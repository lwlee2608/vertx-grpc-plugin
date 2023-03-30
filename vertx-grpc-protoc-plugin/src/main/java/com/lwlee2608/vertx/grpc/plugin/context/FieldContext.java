package com.lwlee2608.vertx.grpc.plugin.context;

import com.lwlee2608.vertx.grpc.plugin.Util;

public class FieldContext {
    public String protoName;
    public String protoTypeName;
    public String name;
    public String javaType;
    public String nullableType;
    public Boolean isEnum;
    public Boolean isNullable;
    public Boolean isMessage;

    public String getter() {
        return Util.camelCase("get_" + name);
    }

    public String setter() {
        return Util.camelCase("set_" + name);
    }

    public String hasFunction() {
        return Util.camelCase("has_" + name);
    }
}
