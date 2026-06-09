FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml ./pom.xml
COPY src ./src
RUN mvn install -DskipTests -B

COPY slo/pom.xml ./slo/pom.xml
COPY slo/src ./slo/src
RUN cd slo && mvn package -DskipTests -B

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /build/slo/target/ydb-slo-workload-1.0.0-SNAPSHOT-exec.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
