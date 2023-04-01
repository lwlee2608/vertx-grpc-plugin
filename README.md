# Vertx gRPC Protoc Plugin

A new gRPC protoc-plugin using the new [vertx-grpc](https://github.com/eclipse-vertx/vertx-grpc) library. 

## Compatibility
* `VertxGrpcGenerator` Compatible with vertx 4.4 or newer.

## Usage
* Install vertx-grpc-protoc-plugin with the [protobuf-maven-plugin](https://www.xolstice.org/protobuf-maven-plugin/examples/protoc-plugin.html).

```xml
    <protocPlugin>
        <id>vertx-grpc-protoc-plugin</id>
        <groupId>io.github.lwlee2608</groupId>
        <artifactId>vertx-grpc-protoc-plugin</artifactId>
        <version>[VERSION]</version>
        <mainClass>com.lwlee2608.vertx.grpc.plugin.VertxGrpcGenerator</mainClass>
    </protocPlugin>
```

* Make sure vertx dependencies are added.

```xml
    <dependency>
        <groupId>io.vertx</groupId>
        <artifactId>vertx-grpc-client</artifactId>
    </dependency>
    <dependency>
        <groupId>io.vertx</groupId>
        <artifactId>vertx-grpc-server</artifactId>
    </dependency>
    <dependency>
        <groupId>io.vertx</groupId>
        <artifactId>vertx-rx-java2</artifactId>
    </dependency>
 ```