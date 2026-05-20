FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/keycloak-ydb-retry-proxy-1.0-SNAPSHOT.jar retry-proxy.jar
ENV TARGET_URL=http://keycloak:8080
ENV MAX_RETRIES=10
ENV BASE_DELAY_MS=50
ENV MAX_DELAY_MS=2000
ENV LISTEN_PORT=8080
EXPOSE 8080
CMD ["java", "-jar", "retry-proxy.jar"]
