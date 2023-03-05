package com.lwlee2608.vertx.grpc;


import io.vertx.core.Future;
import io.vertx.grpc.common.GrpcWriteStream;

public class GrpcStreamClientRequest<Req> {

    private final GrpcWriteStream<Req> stream;

    public GrpcStreamClientRequest(GrpcWriteStream<Req> stream) {
        this.stream = stream;
    }

    public Future<Void> write(Req response) {
        return stream.write(response);
    }

    public Future<Void> end() {
        return stream.end();
    }
}
