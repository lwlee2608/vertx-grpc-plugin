package com.lwlee2608.vertx.grpc;


import io.vertx.core.Future;
import io.vertx.grpc.common.GrpcWriteStream;

public class GrpcStreamServerResponse<Resp> {

    private final GrpcWriteStream<Resp> serverResponse;

    public GrpcStreamServerResponse(GrpcWriteStream<Resp> serverResponse) {
        this.serverResponse = serverResponse;
    }

    public Future<Void> write(Resp response) {
        return serverResponse.write(response);
    }
    public Future<Void> end() {
        return serverResponse.end();
    }
}
