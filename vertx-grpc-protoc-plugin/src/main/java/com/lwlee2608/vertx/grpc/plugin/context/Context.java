package com.lwlee2608.vertx.grpc.plugin.context;

import java.util.ArrayList;
import java.util.List;

public class Context {
    public final List<ServiceContext> services = new ArrayList<>();
    public final List<MessageContext> messages = new ArrayList<>();
}
