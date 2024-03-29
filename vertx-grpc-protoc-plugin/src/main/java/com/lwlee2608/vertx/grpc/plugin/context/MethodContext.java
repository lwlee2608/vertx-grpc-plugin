package com.lwlee2608.vertx.grpc.plugin.context;

import com.google.common.base.Strings;
import com.lwlee2608.vertx.grpc.plugin.Util;

public class MethodContext {
    public String methodName;

    public String inputType;
    public String outputType;
    public String inputTypeFullName;
    public String outputTypeFullName;
    public MessageContext inputMessage;
    public MessageContext outputMessage;

    public boolean deprecated;
    public boolean isManyInput;
    public boolean isManyOutput;
    public int methodNumber;
    public String javaDoc;

    public String methodNameGetter() {
        return Util.camelCase("get_" + methodName + "_method");
    }

    public String methodHeader() {
        String mh = "";
        if (!Strings.isNullOrEmpty(javaDoc)) {
            mh = javaDoc;
        }

        if (deprecated) {
            mh += "\n        @Deprecated";
        }

        return mh;
    }
}
