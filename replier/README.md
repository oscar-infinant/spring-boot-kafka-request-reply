# Replier

## Commands
     mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081 --spring.application.name=replier-1"

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