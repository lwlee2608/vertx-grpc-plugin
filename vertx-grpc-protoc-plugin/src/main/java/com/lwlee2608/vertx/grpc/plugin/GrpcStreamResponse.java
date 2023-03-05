package com.lwlee2608.vertx.grpc.plugin;


import io.vertx.core.Future;
import io.vertx.grpc.server.GrpcServerResponse;

public class GrpcStreamResponse<Resp> {

    private final GrpcServerResponse<?, Resp> serverResponse;

    public GrpcStreamResponse(GrpcServerResponse<?, Resp> serverResponse) {
        this.serverResponse = serverResponse;
    }

    public Future<Void> write(Resp response) {
        return serverResponse.write(response);
    }
    public Future<Void> end() {
        return serverResponse.end();
    }
}
