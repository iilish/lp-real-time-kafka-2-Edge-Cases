# lp-electrical-grid-real-time-kafka (2. Edge cases at scale)
liveProject for Managing a Distributed Electrical Grid in Real Time with Kafka


## Building the project

```
 mvn clean package
```

Creates a jar called `real-time-1.0-SNAPSHOT.jar` in the `target/` directory, which can then be run.

## Building the docker images

```
 docker build -f Dockerfile -t iilish/lp-manning-kafka:api-server.1.0 .
```

Creates an image with repository name `iilish/lp-manning-kafka` and the tag name `api-server.1.0`, which can then be run.


## Running
Run the web server to ingest device events from docker container

```
 docker run iilish/lp-manning-kafka:api-server.1.0
````

Run the raw to canonical process that listens to Kafka and split the big/normal record from docker container.

```
 docker run iilish/lp-manning-kafka:stream-canonical.1.0
````

Run the slow record process that listens slow topic and writes into canonical topic from docker container.

```
 docker run iilish/lp-manning-kafka:stream-slow.1.0
````

Run the state process that listens canonical topic and writes the state event to the database from docker container.

```
 docker run iilish/lp-manning-kafka:stream-state.1.0
````

The data flow of this project is represented in a diagram from file `data_flow.pdf` 

