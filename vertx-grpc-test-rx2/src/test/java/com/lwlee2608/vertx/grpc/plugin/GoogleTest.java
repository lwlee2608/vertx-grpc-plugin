package com.lwlee2608.vertx.grpc.plugin;

/*
import com.google.protobuf.ByteString;
import com.google.protobuf.EmptyProtos;
import io.grpc.testing.integration.Messages;
import io.grpc.testing.integration.VertxTestServiceGrpcClient;
import io.grpc.testing.integration.VertxTestServiceGrpcServer;
import io.reactivex.Observable;
import io.reactivex.Single;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
@SuppressWarnings("ResultOfMethodCallIgnored")
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
                    public Single<EmptyProtos.Empty> emptyCall(EmptyProtos.Empty request) {
                        return Single.error(new RuntimeException("Not yet implemented"));
                    }

                    // Implement following RPC defined in test.proto:
                    //     rpc UnaryCall(SimpleRequest) returns (SimpleResponse);
                    @Override
                    public Single<Messages.SimpleResponse> unaryCall(Messages.SimpleRequest request) {
                        return Single.just(Messages.SimpleResponse.newBuilder()
                                .setUsername("FooBar")
                                .build());
                    }

                    @Override
                    public Single<EmptyProtos.Empty> unimplementedCall(EmptyProtos.Empty request) {
                        return Single.error(new RuntimeException("Not yet implemented"));
                    }

                    // Implement following RPC defined in test.proto:
                    //     rpc StreamingInputCall(stream StreamingInputCallRequest) returns (StreamingInputCallResponse);
                    @Override
                    public Single<Messages.StreamingInputCallResponse> streamingInputCall(Observable<Messages.StreamingInputCallRequest> request) {
                        return Single.create(emitter -> {
                            List<Messages.StreamingInputCallRequest> list = new ArrayList<>();
                            request.doOnNext(list::add)
                                    .doOnComplete(() -> {
                                        Messages.StreamingInputCallResponse resp = Messages.StreamingInputCallResponse.newBuilder()
                                                .setAggregatedPayloadSize(list.size())
                                                .build();
                                        emitter.onSuccess(resp);
                                    })
                                    .subscribe();
                        });
                    }

                    // Implement following RPC defined in test.proto:
                    //     rpc StreamingOutputCall(StreamingOutputCallRequest) returns (stream StreamingOutputCallResponse);
                    @Override
                    public Observable<Messages.StreamingOutputCallResponse> streamingOutputCall(Messages.StreamingOutputCallRequest request) {
                        return Observable.create(emitter -> {
                            emitter.onNext(Messages.StreamingOutputCallResponse.newBuilder()
                                    .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-1", StandardCharsets.UTF_8)).build())
                                    .build());
                            emitter.onNext(Messages.StreamingOutputCallResponse.newBuilder()
                                    .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-2", StandardCharsets.UTF_8)).build())
                                    .build());
                            emitter.onComplete();
                        });
                    }

                    // Implement following RPC defined in test.proto:
                    //     rpc FullDuplexCall(stream StreamingOutputCallRequest) returns (stream StreamingOutputCallResponse);
                    @Override
                    public Observable<Messages.StreamingOutputCallResponse> fullDuplexCall(Observable<Messages.StreamingOutputCallRequest> request) {
                        return Observable.create(emitter -> {
                            request.subscribe(req -> {}, error -> {}, () -> {
                                emitter.onNext(Messages.StreamingOutputCallResponse.newBuilder()
                                        .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-1", StandardCharsets.UTF_8)).build())
                                        .build());
                                emitter.onNext(Messages.StreamingOutputCallResponse.newBuilder()
                                        .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-2", StandardCharsets.UTF_8)).build())
                                        .build());
                                emitter.onComplete();
                            });
                        });
                    }

                    @Override
                    public Observable<Messages.StreamingOutputCallResponse> halfDuplexCall(Observable<Messages.StreamingOutputCallRequest> request) {
                        return Observable.error(new RuntimeException("Not yet implemented"));
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
                .doOnSuccess(reply -> Assertions.assertEquals("FooBar", reply.getUsername()))
                .subscribe(reply -> should.completeNow(), should::failNow);
    }

    @Test
    void testManyUnary(VertxTestContext should) {
        client.streamingInputCall(Observable.create(emitter -> {
                    emitter.onNext(Messages.StreamingInputCallRequest.newBuilder()
                            .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingInputRequest-1", StandardCharsets.UTF_8)).build())
                            .build());
                    emitter.onNext(Messages.StreamingInputCallRequest.newBuilder()
                            .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingInputRequest-2", StandardCharsets.UTF_8)).build())
                            .build());
                    emitter.onComplete();
                }))
                .doOnSuccess(reply -> Assertions.assertEquals(2, reply.getAggregatedPayloadSize()))
                .subscribe(reply -> should.completeNow(), should::failNow);
    }

    @Test
    void testUnaryMany(VertxTestContext should) {
        Messages.StreamingOutputCallRequest request = Messages.StreamingOutputCallRequest.newBuilder()
                .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputRequest", StandardCharsets.UTF_8)).build())
                .build();
        List<Messages.StreamingOutputCallResponse> list = new ArrayList<>();
        client.streamingOutputCall(request)
                .subscribe(list::add, should::failNow, () -> {
                    Assertions.assertEquals(2, list.size());
                    should.completeNow();
                });
    }

    @Test
    void testManyMany(VertxTestContext should) {
        List<Messages.StreamingOutputCallResponse> list = new ArrayList<>();
        client.fullDuplexCall(Observable.create(emitter -> {
                    emitter.onNext(Messages.StreamingOutputCallRequest.newBuilder()
                            .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputRequest-1", StandardCharsets.UTF_8)).build())
                            .build());
                    emitter.onNext(Messages.StreamingOutputCallRequest.newBuilder()
                            .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputRequest-2", StandardCharsets.UTF_8)).build())
                            .build());
                    emitter.onComplete();
                }))
                .subscribe(list::add, should::failNow, () -> {
                    Assertions.assertEquals(2, list.size());
                    should.completeNow();
                });
    }

    private Integer getFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
*/
