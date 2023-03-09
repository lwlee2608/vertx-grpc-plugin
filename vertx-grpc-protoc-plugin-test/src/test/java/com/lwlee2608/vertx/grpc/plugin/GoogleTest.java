package com.lwlee2608.vertx.grpc.plugin;


import com.google.protobuf.ByteString;
import com.google.protobuf.EmptyProtos;
import io.grpc.testing.integration.Messages;
import io.grpc.testing.integration.VertxTestServiceGrpcClient;
import io.grpc.testing.integration.VertxTestServiceGrpcServer;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
public class GoogleTest {

    VertxTestServiceGrpcClient client;
    int port;

    @BeforeAll
    public void init(Vertx vertx, VertxTestContext should) throws IOException {
        port = getFreePort();

        // Create gRPC Server
        VertxTestServiceGrpcServer server = new VertxTestServiceGrpcServer(vertx)
                .callHandlers(new VertxTestServiceGrpcServer.TestServiceApi() {
                    @Override
                    public Future<EmptyProtos.Empty> emptyCall(EmptyProtos.Empty request) {
                        return Future.failedFuture("Not yet implemented");
                    }

                    // Implement following RPC defined in test.proto:
                    //     rpc UnaryCall(SimpleRequest) returns (SimpleResponse);
                    @Override
                    public Future<Messages.SimpleResponse> unaryCall(Messages.SimpleRequest request) {
                        return Future.succeededFuture(
                                Messages.SimpleResponse.newBuilder()
                                        .setUsername("FooBar")
                                        .build());
                    }

                    @Override
                    public Future<EmptyProtos.Empty> unimplementedCall(EmptyProtos.Empty request) {
                        return Future.failedFuture("Not yet implemented");
                    }

                    // Implement following RPC defined in test.proto:
                    //     rpc StreamingInputCall(stream StreamingInputCallRequest) returns (StreamingInputCallResponse);
                    @Override
                    public Future<Messages.StreamingInputCallResponse> streamingInputCall(ReadStream<Messages.StreamingInputCallRequest> request) {
                        Promise<Messages.StreamingInputCallResponse> promise = Promise.promise();
                        List<Messages.StreamingInputCallRequest> list = new ArrayList<>();
                        request.handler(list::add);
                        request.endHandler($ -> {
                            Messages.StreamingInputCallResponse resp = Messages.StreamingInputCallResponse.newBuilder()
                                    .setAggregatedPayloadSize(list.size())
                                    .build();
                            promise.complete(resp);
                        });
                        return promise.future();
                    }

                    // Implement following RPC defined in test.proto:
                    //     rpc StreamingOutputCall(StreamingOutputCallRequest) returns (stream StreamingOutputCallResponse);
                    @Override
                    public Consumer<WriteStream<Messages.StreamingOutputCallResponse>> streamingOutputCall(io.grpc.testing.integration.Messages.StreamingOutputCallRequest request) {
                         return response -> {
                            response.write(Messages.StreamingOutputCallResponse.newBuilder()
                                    .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-1", StandardCharsets.UTF_8)).build())
                                    .build());
                            response.write(Messages.StreamingOutputCallResponse.newBuilder()
                                    .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-2", StandardCharsets.UTF_8)).build())
                                    .build());
                            response.end();
                        };
                    }

                    // Implement following RPC defined in test.proto:
                    //     rpc FullDuplexCall(stream StreamingOutputCallRequest) returns (stream StreamingOutputCallResponse);
                    @Override
                    public Consumer<WriteStream<Messages.StreamingOutputCallResponse>> fullDuplexCall(ReadStream<Messages.StreamingOutputCallRequest> request) {
                        return response -> {
                            request.endHandler($ -> {
                                response.write(Messages.StreamingOutputCallResponse.newBuilder()
                                        .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-1", StandardCharsets.UTF_8)).build())
                                        .build());
                                response.write(Messages.StreamingOutputCallResponse.newBuilder()
                                        .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-2", StandardCharsets.UTF_8)).build())
                                        .build());
                                response.end();
                            });
                        };
                    }

                    @Override
                    public Consumer<WriteStream<Messages.StreamingOutputCallResponse>> halfDuplexCall(ReadStream<Messages.StreamingOutputCallRequest> request) {
                        return response -> {};
                    }
                });

        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(server.getGrpcServer())
                .listen(port)
                .onSuccess($ -> should.completeNow())
                .onFailure(should::failNow);

        // Create gRPC Client
        client = new VertxTestServiceGrpcClient(vertx, SocketAddress.inetSocketAddress(port, "localhost"));
    }

    @Test
    void testUnaryUnary(VertxTestContext should) {
        client.unaryCall(Messages.SimpleRequest.newBuilder()
                        .setFillUsername(true)
                        .build())
                .onSuccess(reply -> Assertions.assertEquals("FooBar", reply.getUsername()))
                .onSuccess(reply -> should.completeNow())
                .onFailure(should::failNow);
    }

    @Test
    void testManyUnary(VertxTestContext should) {
        client.streamingInputCall(req -> {
                    req.write(Messages.StreamingInputCallRequest.newBuilder()
                            .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingInputRequest-1", StandardCharsets.UTF_8)).build())
                            .build());
                    req.write(Messages.StreamingInputCallRequest.newBuilder()
                            .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingInputRequest-2", StandardCharsets.UTF_8)).build())
                            .build());
                    req.end();
                })
                .onSuccess(reply -> Assertions.assertEquals(2, reply.getAggregatedPayloadSize()))
                .onSuccess(reply -> should.completeNow())
                .onFailure(should::failNow);
    }

    @Test
    void testUnaryMany(VertxTestContext should) {
        Messages.StreamingOutputCallRequest request = Messages.StreamingOutputCallRequest.newBuilder()
                .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputRequest", StandardCharsets.UTF_8)).build())
                .build();
        client.streamingOutputCall(request)
                .onSuccess(response -> {
                    List<Messages.StreamingOutputCallResponse> list = new ArrayList<>();
                    response.handler(list::add);
                    response.endHandler($ -> {
                        Assertions.assertEquals(2, list.size());
                        should.completeNow();
                    });
                    response.exceptionHandler(should::failNow);
                });
    }

    @Test
    void testManyMany(VertxTestContext should) {
        client.fullDuplexCall(req -> {
                    req.write(Messages.StreamingOutputCallRequest.newBuilder()
                            .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputRequest-1", StandardCharsets.UTF_8)).build())
                            .build());
                    req.write(Messages.StreamingOutputCallRequest.newBuilder()
                            .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputRequest-2", StandardCharsets.UTF_8)).build())
                            .build());
                    req.end();
                })
                .onSuccess(response -> {
                    List<Messages.StreamingOutputCallResponse> list = new ArrayList<>();
                    response.handler(list::add);
                    response.endHandler($ -> {
                        Assertions.assertEquals(2, list.size());
                        should.completeNow();
                    });
                    response.exceptionHandler(should::failNow);
                });
    }

    private Integer getFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
