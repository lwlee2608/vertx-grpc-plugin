package com.lwlee2608.vertx.grpc.plugin;

import com.google.protobuf.ByteString;
import com.google.protobuf.pojo.Empty;
import io.grpc.testing.integration.component.VertxTestServiceGrpcClient;
import io.grpc.testing.integration.component.VertxTestServiceGrpcServer;
import io.grpc.testing.integration.pojo.Payload;
import io.grpc.testing.integration.pojo.SimpleRequest;
import io.grpc.testing.integration.pojo.SimpleResponse;
import io.grpc.testing.integration.pojo.StreamingInputCallRequest;
import io.grpc.testing.integration.pojo.StreamingInputCallResponse;
import io.grpc.testing.integration.pojo.StreamingOutputCallRequest;
import io.grpc.testing.integration.pojo.StreamingOutputCallResponse;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.net.SocketAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
@SuppressWarnings("ResultOfMethodCallIgnored")
public class GoogleTest {
    private static final Logger logger = LoggerFactory.getLogger(GoogleTest.class);

    VertxTestServiceGrpcClient client;
    int port;

    @BeforeAll
    public void init(Vertx vertx, VertxTestContext should) throws IOException {
        port = getFreePort();

        // Create gRPC Server
        VertxTestServiceGrpcServer server = new VertxTestServiceGrpcServer(vertx)
                .callHandlers(new VertxTestServiceGrpcServer.TestServiceApi() {
                    @Override
                    public Single<Empty> emptyCall(Empty request) {
                        return Single.error(new RuntimeException("Not yet implemented"));
                    }

                    // Implement following RPC defined in test.proto:
                    //     rpc UnaryCall(SimpleRequest) returns (SimpleResponse);
                    @Override
                    public Single<SimpleResponse> unaryCall(SimpleRequest request) {
                        return Single.just(new SimpleResponse()
                                .setUsername("FooBar"));
                    }

                    @Override
                    public Single<Empty> unimplementedCall(Empty request) {
                        return Single.error(new RuntimeException("Not yet implemented"));
                    }

                    // Implement following RPC defined in test.proto:
                    //     rpc StreamingInputCall(stream StreamingInputCallRequest) returns (StreamingInputCallResponse);
                    @Override
                    public Single<StreamingInputCallResponse> streamingInputCall(Observable<StreamingInputCallRequest> request) {
                        return Single.create(emitter -> {
                            List<StreamingInputCallRequest> list = new ArrayList<>();
                            request.doOnNext(list::add)
                                    .doOnComplete(() -> {
                                        StreamingInputCallResponse resp = new StreamingInputCallResponse()
                                                .setAggregatedPayloadSize(list.size());
                                        emitter.onSuccess(resp);
                                    })
                                    .subscribe();
                        });
                    }

                    // Implement following RPC defined in test.proto:
                    //     rpc StreamingOutputCall(StreamingOutputCallRequest) returns (stream StreamingOutputCallResponse);
                    @Override
                    public Observable<StreamingOutputCallResponse> streamingOutputCall(StreamingOutputCallRequest request) {
                        return Observable.create(emitter -> {
                            emitter.onNext(new StreamingOutputCallResponse()
                                    .setPayload(new Payload().setBody(ByteString.copyFrom("StreamingOutputResponse-1", StandardCharsets.UTF_8))));
                            emitter.onNext(new StreamingOutputCallResponse()
                                    .setPayload(new Payload().setBody(ByteString.copyFrom("StreamingOutputResponse-2", StandardCharsets.UTF_8))));
                            emitter.onComplete();
                        });
                    }

                    // Implement following RPC defined in test.proto:
                    //     rpc FullDuplexCall(stream StreamingOutputCallRequest) returns (stream StreamingOutputCallResponse);
                    @Override
                    public Observable<StreamingOutputCallResponse> fullDuplexCall(Observable<StreamingOutputCallRequest> request) {
                        return Observable.create(emitter -> {
                            request.subscribe(req -> {
                            }, error -> {
                            }, () -> {
                                emitter.onNext(new StreamingOutputCallResponse()
                                        .setPayload(new Payload().setBody(ByteString.copyFrom("StreamingOutputResponse-1", StandardCharsets.UTF_8))));
                                emitter.onNext(new StreamingOutputCallResponse()
                                        .setPayload(new Payload().setBody(ByteString.copyFrom("StreamingOutputResponse-2", StandardCharsets.UTF_8))));
                                emitter.onComplete();
                            });
                        });
                    }

                    @Override
                    public Observable<StreamingOutputCallResponse> halfDuplexCall(Observable<StreamingOutputCallRequest> request) {
                        return Observable.error(new RuntimeException("Not yet implemented"));
                    }
                });

        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(server.getGrpcServer())
                .rxListen(port)
                .subscribe($ -> should.completeNow(), should::failNow);

        // Create gRPC Client
        client = new VertxTestServiceGrpcClient(vertx, SocketAddress.inetSocketAddress(port, "localhost"));
    }

    @Test
    void testUnaryUnary(VertxTestContext should) {
        client.unaryCall(new SimpleRequest()
                        .setFillUsername(true))
                .doOnSuccess(reply -> Assertions.assertEquals("FooBar", reply.getUsername()))
                .subscribe(reply -> should.completeNow(), should::failNow);
    }

    @Test
    void testManyUnary(VertxTestContext should) {
        client.streamingInputCall(Observable.create(emitter -> {
                    emitter.onNext(new StreamingInputCallRequest()
                            .setPayload(new Payload().setBody(ByteString.copyFrom("StreamingInputRequest-1", StandardCharsets.UTF_8))));
                    emitter.onNext(new StreamingInputCallRequest()
                            .setPayload(new Payload().setBody(ByteString.copyFrom("StreamingInputRequest-2", StandardCharsets.UTF_8))));
                    emitter.onComplete();
                }))
                .doOnSuccess(reply -> Assertions.assertEquals(2, reply.getAggregatedPayloadSize()))
                .subscribe(reply -> should.completeNow(), should::failNow);
    }

    @Test
    void testUnaryMany(VertxTestContext should) throws InterruptedException {
        StreamingOutputCallRequest request = new StreamingOutputCallRequest()
                .setPayload(new Payload().setBody(ByteString.copyFrom("StreamingOutputRequest", StandardCharsets.UTF_8)));
        CountDownLatch receivedLatch = new CountDownLatch(2);
        client.streamingOutputCall(request)
                .subscribe(resp -> receivedLatch.countDown(), should::failNow, should::completeNow);
        receivedLatch.await();
    }

    @Test
    void testManyMany(VertxTestContext should) throws InterruptedException {
        CountDownLatch receivedLatch = new CountDownLatch(2);
        client.fullDuplexCall(Observable.create(emitter -> {
                    emitter.onNext(new StreamingOutputCallRequest()
                            .setPayload(new Payload().setBody(ByteString.copyFrom("StreamingOutputRequest-1", StandardCharsets.UTF_8))));
                    emitter.onNext(new StreamingOutputCallRequest()
                            .setPayload(new Payload().setBody(ByteString.copyFrom("StreamingOutputRequest-2", StandardCharsets.UTF_8))));
                    emitter.onComplete();
                }))
                .subscribe(resp -> receivedLatch.countDown(), should::failNow, should::completeNow);
        receivedLatch.await();
    }

    private Integer getFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}