# Replier

## Sample Commands
     mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081 --spring.application.name=replier-1"


CREATE TABLE greeting_message (
id BIGINT PRIMARY KEY,
message VARCHAR(255) NOT NULL
);

INSERT INTO greeting_message (id, message)
VALUES (1, 'hello from db');