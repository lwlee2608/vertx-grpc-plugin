package com.lwlee2608.vertx.grpc.plugin;


import io.grpc.examples.helloworld.pojo.HelloReply;
import io.grpc.examples.helloworld.pojo.HelloRequest;
import io.grpc.examples.helloworld.component.VertxGreeterGrpcClient;
import io.grpc.examples.helloworld.component.VertxGreeterGrpcServer;
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
                    public Future<HelloReply> sayHello(HelloRequest request) {
                        return Future.succeededFuture(
                                new HelloReply()
                                        .setMessage(request.getName() + " World"));
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
        client.sayHello(new HelloRequest()
                        .setName("Hello"))
                .onSuccess(helloReply -> Assertions.assertEquals("Hello World", helloReply.getMessage()))
                .onSuccess(helloReply -> should.completeNow())
                .onFailure(should::failNow);
    }


    private Integer getFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
