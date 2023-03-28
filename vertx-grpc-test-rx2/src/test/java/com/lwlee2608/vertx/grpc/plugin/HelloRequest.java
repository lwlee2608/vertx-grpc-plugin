package com.lwlee2608.vertx.grpc.plugin;

import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class HelloRequest {
    private String name;
    private int age;
    private String address;
    private Integer count;

    static public io.grpc.examples.helloworld.HelloRequest toProto(HelloRequest pojo) {
        io.grpc.examples.helloworld.HelloRequest.Builder builder = io.grpc.examples.helloworld.HelloRequest.newBuilder();
        builder.setName(pojo.name == null ? "" : pojo.name);
        builder.setAge(pojo.age);
        if (pojo.address != null) {
            builder.setAddress(StringValue.of(pojo.address));
        }
        if (pojo.count != null) {
            builder.setCount(Int32Value.of(pojo.count));
        }
        return builder.build();
    }

    static public HelloRequest fromProto(io.grpc.examples.helloworld.HelloRequest proto) {
        HelloRequest pojo = new HelloRequest();
        pojo.setName(proto.getName());
        pojo.setAge(proto.getAge());
        if (proto.hasAddress()) {
            pojo.setAddress(proto.getAddress().getValue());
        }
        if (proto.hasCount()) {
            pojo.setCount(proto.getCount().getValue());
        }
        return pojo;
    }
}

