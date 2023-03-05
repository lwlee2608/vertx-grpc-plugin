package com.lwlee2608.vertx.grpc.plugin;


import io.vertx.core.Handler;
import io.vertx.grpc.server.GrpcServerRequest;

public class GrpcStreamRequest<Req> {

    private final GrpcServerRequest<Req, ?> serverRequest;

    public GrpcStreamRequest(GrpcServerRequest<Req, ?> serverRequest) {
        this.serverRequest =  serverRequest;
    }

    public GrpcStreamRequest<Req> exceptionHandler(Handler<Throwable> exceptionHandler) {
        serverRequest.exceptionHandler(exceptionHandler);
        return this;
    }

    public GrpcStreamRequest<Req> handler(Handler<Req> requestHandler) {
        serverRequest.handler(requestHandler);
        return this;
    }

    public GrpcStreamRequest<Req> endHandler(Handler<Void> endHandler) {
        serverRequest.endHandler(endHandler);
        return this;
    }
}
