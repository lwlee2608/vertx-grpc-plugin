package com.lwlee2608.vertx.grpc.plugin;


import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.VertxGreeterGrpcClient;
import io.grpc.examples.helloworld.VertxGreeterGrpcServer;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.SocketAddress;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.ServerSocket;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
public class HelloWordTest {

    VertxGreeterGrpcClient client;
    int port;

    @BeforeAll
    public void init(Vertx vertx, VertxTestContext should) throws IOException {
        port = getFreePort();

        // Create gRPC Server
        VertxGreeterGrpcServer server = new VertxGreeterGrpcServer(vertx)
                .callHandlers(new VertxGreeterGrpcServer.GreeterApi() {
                    @Override
                    public Single<HelloReply> sayHello(HelloRequest request) {
                        return Single.just(
                                HelloReply.newBuilder()
                                        .setMessage(request.getName() + " World")
                                        .build());
                    }
                });
        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(server.getGrpcServer())
                .listen(port)
                .onSuccess($ -> should.completeNow())
                .onFailure(should::failNow);

        // Create gRPC Client
        client = new VertxGreeterGrpcClient(vertx, SocketAddress.inetSocketAddress(port, "localhost"));
    }

    @Test
    void testServerClient(VertxTestContext should) {
        client.sayHello(HelloRequest.newBuilder()
                        .setName("Hello")
                        .build())
                .doOnSuccess(helloReply -> Assertions.assertEquals("Hello World", helloReply.getMessage()))
                .subscribe(helloReply -> should.completeNow(), should::failNow);
    }


    private Integer getFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
