FROM maven:3.8.4-openjdk-17 AS build
ARG SRC_PATH
COPY . .
WORKDIR /${SRC_PATH}
RUN mvn package -DskipTests

FROM openjdk:21 AS run
WORKDIR /app
COPY --from=build . .
ENTRYPOINT ["java", "-jar", "./hibernate/target/hibernate-0.0.1-SNAPSHOT.jar"]