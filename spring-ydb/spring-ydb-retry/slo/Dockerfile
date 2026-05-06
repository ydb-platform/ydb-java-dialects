FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml ./pom.xml
RUN mvn install -N -B

COPY spring-ydb-retry/pom.xml ./spring-ydb-retry/pom.xml
COPY spring-ydb-retry/src ./spring-ydb-retry/src
RUN mvn install -DskipTests -B -pl spring-ydb-retry || mvn install -DskipTests -B -pl spring-ydb-retry

COPY spring-ydb-retry/slo/pom.xml ./spring-ydb-retry/slo/pom.xml
COPY spring-ydb-retry/slo/src ./spring-ydb-retry/slo/src
RUN cd spring-ydb-retry/slo && mvn package -DskipTests -B || mvn package -DskipTests -B

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /build/spring-ydb-retry/slo/target/ydb-slo-workload-1.0.0-SNAPSHOT-exec.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
