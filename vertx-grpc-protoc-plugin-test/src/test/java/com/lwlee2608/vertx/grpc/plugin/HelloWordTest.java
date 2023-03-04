package com.lwlee2608.vertx.grpc.plugin;


import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.VertxGreeterGrpcClient;
import io.grpc.examples.helloworld.VertxGreeterGrpcServer;
import io.grpc.examples.helloworld.VertxGreeterGrpcServerApi;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.SocketAddress;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.ServerSocket;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
public class HelloWordTest {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWordTest.class);

    int port;

    @BeforeAll
    public void init(Vertx vertx, VertxTestContext should) throws IOException {
        port = getFreePort();
        VertxGreeterGrpcServer server = new VertxGreeterGrpcServer(vertx)
                .callHandlers(new VertxGreeterGrpcServerApi() {
                    @Override
                    public Future<HelloReply> sayHello(HelloRequest request) {
                        return Future.succeededFuture(
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
    }

    @Test
    void testServerClient(Vertx vertx, VertxTestContext should) {
        VertxGreeterGrpcClient client = new VertxGreeterGrpcClient(vertx, SocketAddress.inetSocketAddress(port, "localhost"));
        client.sayHello(HelloRequest.newBuilder()
                .setName("Hello")
                .build())
                .onSuccess(helloReply -> LOG.info("Reply received"))
                .onSuccess(helloReply -> should.completeNow())
                .onFailure(should::failNow);
    }


    private Integer getFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
