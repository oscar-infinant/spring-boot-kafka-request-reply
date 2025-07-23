# Technical Overview
This application implements an asynchronous request-response architecture using Spring Boot and Apache Kafka.

The <b>frontend service</b>, built with Spring Boot, exposes a non-blocking HTTP API powered by NIO. The RestController provides two main endpoints:
- One for reading information
- One for writing data

Both endpoints are designed to avoid thread blocking by using CompletableFuture.supplyAsync, ensuring that incoming HTTP requests do not tie up the request-handling threads.

Each request is processed asynchronously by publishing a message to a Kafka request topic, and the application waits for a reply on a corresponding reply topic. The response is then returned to the HTTP client once available.

The <b>backend service</b>, built with Spring Boot. This application listens to two different Kafka topics, one for read operations and another for write operations. It is integrated with a relational database using Spring Data JPA. Upon receiving a message:
- It processes the request (e.g., fetch or persist data via JPA)
- Constructs a response
- Sends it back to the reply topic, using the KafkaHeaders.REPLY_TOPIC and KafkaHeaders.CORRELATION_ID from the incoming message header to correlate responses

To _maximize parallelism and throughput_, both Kafka listener methods use CompletableFuture.supplyAsync, enabling multi-threaded, asynchronous processing without blocking the Kafka consumer threads.

# Software Requirements
- Kafka server (*)
- Postgres DB (*)
- Postman (Only to test endpoints)
- k6 (Only for load testing)

(*) _Required_

# Postman Collection
Import the `ReqReply.postman_collection.json` located in the root folder and import it into Postman for testing.

# K6 Scripts
Under k6 folder can be found scripts to conduct load test locally.
- k6/get-message-req-reply.js
- k6/write-message-req-reply.js

# Replier

## Database DDL & DML
```
DROP TABLE IF EXISTS greeting_message;
code
CREATE TABLE greeting_message (
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
message VARCHAR(255) NOT NULL,
sender VARCHAR(100) NOT NULL,
created_at TIMESTAMP NOT NULL
);
```
```
DO $$
BEGIN
    FOR i IN 1..1000000 LOOP
        INSERT INTO greeting_message (message, sender, created_at)
        VALUES (
            'Hello number ' || i,
            'user' || (1 + floor(random() * 50))::int,
            now() - (i || ' minutes')::interval
         );
    END LOOP;
END $$;
```

## Execute
```
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081 --spring.application.name=replier-1"
```
# Requester

## Execute
```
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8090 --kafka.request.message=foo --kafka.reply.group=repliesGroup-0 --spring.application.name=requester-0"
```
