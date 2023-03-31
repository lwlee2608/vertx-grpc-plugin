package com.lwlee2608.vertx.grpc.plugin;


import io.grpc.examples.helloworld.component.VertxGreeterGrpcClient;
import io.grpc.examples.helloworld.component.VertxGreeterGrpcServer;
import io.grpc.examples.helloworld.pojo.HelloReply;
import io.grpc.examples.helloworld.pojo.HelloRequest;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.net.SocketAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
@SuppressWarnings("ResultOfMethodCallIgnored")
public class HelloWordTest {
    private static final Logger logger = LoggerFactory.getLogger(HelloWordTest.class);

    @Test
    void testServerClient(Vertx vertx, VertxTestContext should) throws IOException {
        int port = getFreePort();

        // Create gRPC Server
        VertxGreeterGrpcServer server = new VertxGreeterGrpcServer(vertx)
                .callHandlers(new VertxGreeterGrpcServer.GreeterApi() {
                    @Override
                    public Single<HelloReply> sayHello(HelloRequest request) {
                        should.verify(() -> Assertions.assertNull(request.getCount()));
                        return Single.just(new HelloReply().setMessage(request.getName() + " World"));
                    }
                });
        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(server.getGrpcServer());
        Completable listen = httpServer.rxListen(port)
                .doOnError(should::failNow)
                .ignoreElement();

        // Create gRPC Client
        VertxGreeterGrpcClient client = new VertxGreeterGrpcClient(vertx, SocketAddress.inetSocketAddress(port, "localhost"));
        Completable sendMsg = client.sayHello(new HelloRequest().setName("Hello"))
                .doOnSuccess(helloReply -> {
                    Assertions.assertEquals("Hello World", helloReply.getMessage());
                    Assertions.assertEquals(0, helloReply.getId()); // id is not nullable and default value is 0
                    Assertions.assertNull(helloReply.getAddress());
                    Assertions.assertNull(helloReply.getAge());
                })
                .ignoreElement();

        listen.andThen(sendMsg).subscribe(should::completeNow, should::failNow);
    }


    private Integer getFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
