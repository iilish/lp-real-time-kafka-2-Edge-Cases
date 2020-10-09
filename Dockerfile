FROM store/oracle/serverjre:1.8.0_241-b07
WORKDIR /api-server
COPY ./target/real-time-*.jar ./api-server.jar
COPY ./src/main/resources/api-server.yml ./api-server.yml
EXPOSE 8081 8080
CMD ["java", "-jar", "api-server.jar","server", "api-server.yml"]
