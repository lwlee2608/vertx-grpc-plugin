package com.lwlee2608.vertx.grpc.plugin;


import com.google.protobuf.ByteString;
import com.google.protobuf.EmptyProtos;
import io.grpc.testing.integration.Messages;
import io.grpc.testing.integration.VertxTestServiceGrpcClient;
import io.grpc.testing.integration.VertxTestServiceGrpcServer;
import io.grpc.testing.integration.VertxTestServiceGrpcServerApi;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.common.GrpcReadStream;
import io.vertx.grpc.server.GrpcServerRequest;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
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
public class GoogleTest {
    private static final Logger LOG = LoggerFactory.getLogger(GoogleTest.class);

    VertxTestServiceGrpcClient client;
    int port;

    @BeforeAll
    public void init(Vertx vertx, VertxTestContext should) throws IOException {
        port = getFreePort();
        VertxTestServiceGrpcServer server = new VertxTestServiceGrpcServer(vertx)
                .callHandlers(new VertxTestServiceGrpcServerApi() {
                    @Override
                    public Future<EmptyProtos.Empty> emptyCall(EmptyProtos.Empty request) {
                        return Future.failedFuture("Not yet implemented");
                    }

                    @Override
                    public Future<Messages.SimpleResponse> unaryCall(Messages.SimpleRequest request) {
                        LOG.info("Request received " + request);
                        return Future.succeededFuture(
                                Messages.SimpleResponse.newBuilder()
                                        .setUsername("FooBar")
                                        .build());
                    }

                    @Override
                    public Future<EmptyProtos.Empty> unimplementedCall(EmptyProtos.Empty request) {
                        return Future.failedFuture("Not yet implemented");
                    }

                    @Override
                    public void streamingInputCall(GrpcServerRequest<Messages.StreamingInputCallRequest, Messages.StreamingInputCallResponse> request) {
                        List<Messages.StreamingInputCallRequest> list = new ArrayList<>();
                        request.handler(list::add);
                        request.endHandler($ -> {
                            Messages.StreamingInputCallResponse resp = Messages.StreamingInputCallResponse.newBuilder()
                                    .setAggregatedPayloadSize(list.size())
                                    .build();
                            request.response().end(resp);
                        });
                    }

                    @Override
                    public void streamingOutputCall(GrpcServerRequest<Messages.StreamingOutputCallRequest, Messages.StreamingOutputCallResponse> request) {
                        request.handler(req -> LOG.info("Request received " + req));
                        request.endHandler($ -> {
                            request.response().write(Messages.StreamingOutputCallResponse.newBuilder()
                                    .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-1", StandardCharsets.UTF_8)).build())
                                    .build());
                            request.response().write(Messages.StreamingOutputCallResponse.newBuilder()
                                    .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-2", StandardCharsets.UTF_8)).build())
                                    .build());
                            request.response().end();
                        });
                    }

                    @Override
                    public void fullDuplexCall(GrpcServerRequest<Messages.StreamingOutputCallRequest, Messages.StreamingOutputCallResponse> request) {
                        request.handler(req -> LOG.info("Request received " + req));
                        request.endHandler($ -> {
                            request.response().write(Messages.StreamingOutputCallResponse.newBuilder()
                                    .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-1", StandardCharsets.UTF_8)).build())
                                    .build());
                            request.response().write(Messages.StreamingOutputCallResponse.newBuilder()
                                    .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputResponse-2", StandardCharsets.UTF_8)).build())
                                    .build());
                            request.response().end();
                        });
                    }

                    @Override
                    public void halfDuplexCall(GrpcServerRequest<Messages.StreamingOutputCallRequest, Messages.StreamingOutputCallResponse> request) {
                    }
                });

        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(server.getGrpcServer())
                .listen(port)
                .onSuccess($ -> should.completeNow())
                .onFailure(should::failNow);
        client = new VertxTestServiceGrpcClient(vertx, SocketAddress.inetSocketAddress(port, "localhost"));
    }

    @Test
    void testUnaryUnary(VertxTestContext should) {
        client.unaryCall(Messages.SimpleRequest.newBuilder()
                        .setFillUsername(true)
                        .build())
                .onSuccess(reply -> LOG.info("Reply received " + reply))
                .onSuccess(reply -> should.completeNow())
                .onFailure(should::failNow);
    }

    @Test
    void testManyUnary(VertxTestContext should) {
        client.streamingInputCall().compose(req -> {
                    req.write(Messages.StreamingInputCallRequest.newBuilder()
                            .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingInputRequest-1", StandardCharsets.UTF_8)).build())
                            .build());
                    req.write(Messages.StreamingInputCallRequest.newBuilder()
                            .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingInputRequest-2", StandardCharsets.UTF_8)).build())
                            .build());
                    req.end();
                    return req.response().compose(GrpcReadStream::last);
                })
                .onSuccess(reply -> LOG.info("Reply received " + reply))
                .onSuccess(reply -> should.completeNow())
                .onFailure(should::failNow);
    }

    @Test
    void testUnaryMany(VertxTestContext should) {
        client.streamingOutputCall().compose(req -> {
                    req.end(Messages.StreamingOutputCallRequest.newBuilder()
                            .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputRequest", StandardCharsets.UTF_8)).build())
                            .build());
                    return req.response();
                })
                .onSuccess(response -> {
                    response.handler(reply -> LOG.info("Reply received " + reply));
                    response.endHandler($ -> should.completeNow());
                    response.exceptionHandler(should::failNow);
                });
    }

    @Test
    void testManyMany(VertxTestContext should) {
        client.fullDuplexCall().compose(req -> {
                    req.write(Messages.StreamingOutputCallRequest.newBuilder()
                            .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputRequest-1", StandardCharsets.UTF_8)).build())
                            .build());
                    req.write(Messages.StreamingOutputCallRequest.newBuilder()
                            .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFrom("StreamingOutputRequest-2", StandardCharsets.UTF_8)).build())
                            .build());
                    req.end();
                    return req.response();
                })
                .onSuccess(response -> {
                    response.handler(reply -> LOG.info("Reply received " + reply));
                    response.endHandler($ -> should.completeNow());
                    response.exceptionHandler(should::failNow);
                });
    }

    private Integer getFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
