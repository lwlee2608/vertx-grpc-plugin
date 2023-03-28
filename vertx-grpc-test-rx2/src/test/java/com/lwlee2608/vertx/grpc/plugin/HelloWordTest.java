package com.lwlee2608.vertx.grpc.plugin;


import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
public class HelloWordTest {

    private static final Logger logger = LoggerFactory.getLogger(HelloWordTest.class);

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
                        logger.info("request is {}", request);
                        request.getAllFields().entrySet().forEach( e -> {
                            logger.info("field {} = {}", e.getKey(), e.getValue());
                        });
                        if (request.hasCount()) {
                            logger.info("count is {}", request.getCount().getValue());
                        }

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
        logger.info("request is ");
        client.sayHello(HelloRequest.newBuilder()
                        .setName("Hello")
                        .setAddress(StringValue.of("mid valley"))
                        .setAge(0)
                        .setCount(Int32Value.of(0))
                        .build())
//                .doOnSuccess(helloReply -> Assertions.assertEquals("Hello World", helloReply.getMessage()))
                .subscribe(helloReply -> should.completeNow(), should::failNow);
    }


    private Integer getFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
