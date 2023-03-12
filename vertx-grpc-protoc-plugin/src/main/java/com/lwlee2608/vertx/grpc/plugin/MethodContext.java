package com.lwlee2608.vertx.grpc.plugin;

import com.google.common.base.Strings;

public class MethodContext {
    // CHECKSTYLE DISABLE VisibilityModifier FOR 10 LINES
    public String methodName;
    public String inputType;
    public String outputType;
    public boolean deprecated;
    public boolean isManyInput;
    public boolean isManyOutput;
    public int methodNumber;
    public String javaDoc;

    public String methodNameGetter() {
        return Util.mixedLower("get_" + methodName + "_method");
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
