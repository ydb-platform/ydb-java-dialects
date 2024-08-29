FROM maven:3.8.4-openjdk-17 AS build
ARG SRC_PATH
COPY . /app
RUN ls
WORKDIR /app/${SRC_PATH}
RUN ls
RUN mvn package -DskipTests
RUN cp -r ./target/* ./..
RUN ls ..

FROM openjdk:21 AS run
COPY --from=build /app .
ENTRYPOINT ["java", "-jar", "./slo.jar"]