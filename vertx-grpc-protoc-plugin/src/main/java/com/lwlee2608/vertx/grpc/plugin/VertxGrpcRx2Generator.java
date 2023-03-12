package com.lwlee2608.vertx.grpc.plugin;

import com.salesforce.jprotoc.ProtocPlugin;

public class VertxGrpcRx2Generator extends AbstractVertxGenerator {
    public VertxGrpcRx2Generator(String clientTemplate, String serverTemplate) {
        super(clientTemplate, serverTemplate);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            ProtocPlugin.generate(new VertxGrpcRx2Generator("rxjava2/client.mustache", "rxjava2/server.mustache"));
        } else {
            ProtocPlugin.debug(new VertxGrpcRx2Generator("rxjava2/client.mustache", "rxjava2/server.mustache"), args[0]);
        }
    }
}