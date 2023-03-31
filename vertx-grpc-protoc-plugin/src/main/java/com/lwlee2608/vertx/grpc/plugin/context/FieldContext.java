package com.lwlee2608.vertx.grpc.plugin.context;

import com.lwlee2608.vertx.grpc.plugin.Util;

public class FieldContext {
    public String protoName;
    public String protoTypeName;
    public String name;
    public String javaType;
    public String nullableType;
    public String defaultValue;
    public Boolean isEnum;
    public Boolean isNullable;
    public Boolean isMessage;
    public Boolean isList;

    public String getter() {
        return Util.camelCase("get_" + name);
    }

    public String setter() {
        return Util.camelCase("set_" + name);
    }

    public String addAll() {
        return Util.camelCase("add_all_" + name);
    }

    public String getList() {
        return Util.camelCase("get_" + name + "_list");
    }

    public String hasFunction() {
        return Util.camelCase("has_" + name);
    }

    public String declareField() {
        if (!isList) {
            if (isNullable) {
                return String.format("private %s %s;", javaType, name);
            } else {
                return String.format("private %s %s = %s;", javaType, name, defaultValue);
            }
        } else {
            return String.format("private List<%s> %s = new ArrayList<>();", javaType, name);
        }
    }

    public String toProtoFunction() {
        if (!isList) {
            if (isMessage) {
                return String.format("builder.%s(%s.toProto(pojo.%s));", setter(), javaType, name);
            } else if (isNullable) {
                return String.format("builder.%s(%s.of(pojo.%s));", setter(), nullableType, name);
            } else {
                return String.format("builder.%s(pojo.%s);", setter(), name);
            }
        } else {
            if (isMessage) {
                return String.format("builder.%s(pojo.%s.stream().map(%s::toProto).collect(Collectors.toList()));", addAll(), name, javaType);
            } else {
                return String.format("builder.%s(pojo.%s);", addAll(), name);
            }
        }
    }

    public String fromProtoFunction() {
        if (!isList) {
            if (isMessage) {
                return String.format("pojo.%s(%s.fromProto(proto.%s()));", setter(), javaType, getter());
            } else if (isNullable) {
                return String.format("pojo.%s(proto.%s().getValue());", setter(), getter());
            } else {
                return String.format("pojo.%s(proto.%s());", setter(), getter());
            }
        } else {
            if (isMessage) {
                return String.format("pojo.%s(proto.%s().stream().map(%s::fromProto).collect(Collectors.toList()));", setter(), getList(), javaType);
            } else {
                return String.format("pojo.%s(proto.%s());", setter(), getList());
            }
        }
    }
}
