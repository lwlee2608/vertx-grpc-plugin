package com.lwlee2608.vertx.grpc.plugin;

import com.salesforce.jprotoc.ProtocPlugin;

public class VertxGrpcGenerator extends AbstractVertxGenerator {
    public VertxGrpcGenerator(String clientTemplate, String serverTemplate) {
        super(clientTemplate, serverTemplate);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            ProtocPlugin.generate(new VertxGrpcGenerator("component/client.mustache", "component/server.mustache"));
        } else {
            ProtocPlugin.debug(new VertxGrpcGenerator("component/client.mustache", "component/server.mustache"), args[0]);
        }
    }
}