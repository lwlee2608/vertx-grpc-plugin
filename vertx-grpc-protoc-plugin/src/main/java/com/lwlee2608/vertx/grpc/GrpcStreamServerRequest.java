package com.lwlee2608.vertx.grpc;


import io.vertx.core.Handler;
import io.vertx.grpc.common.GrpcReadStream;

public class GrpcStreamServerRequest<Req> {

    private final GrpcReadStream<Req> stream;

    public GrpcStreamServerRequest(GrpcReadStream<Req> stream) {
        this.stream = stream;
    }

    public GrpcStreamServerRequest<Req> exceptionHandler(Handler<Throwable> exceptionHandler) {
        stream.exceptionHandler(exceptionHandler);
        return this;
    }

    public GrpcStreamServerRequest<Req> handler(Handler<Req> requestHandler) {
        stream.handler(requestHandler);
        return this;
    }

    public GrpcStreamServerRequest<Req> endHandler(Handler<Void> endHandler) {
        stream.endHandler(endHandler);
        return this;
    }
}
