package com.lwlee2608.vertx.grpc.plugin;


import io.vertx.core.Handler;
import io.vertx.grpc.common.GrpcReadStream;

public class GrpcStreamClientResponse<Resp> {

    private final GrpcReadStream<Resp> stream;

    public GrpcStreamClientResponse(GrpcReadStream<Resp> stream) {
        this.stream = stream;
    }

    public GrpcStreamClientResponse<Resp> exceptionHandler(Handler<Throwable> exceptionHandler) {
        stream.exceptionHandler(exceptionHandler);
        return this;
    }

    public GrpcStreamClientResponse<Resp> handler(Handler<Resp> requestHandler) {
        stream.handler(requestHandler);
        return this;
    }

    public GrpcStreamClientResponse<Resp> endHandler(Handler<Void> endHandler) {
        stream.endHandler(endHandler);
        return this;
    }
}
