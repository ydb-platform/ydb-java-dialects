mvn clean package

mkdir -p docker/providers

cp target/keycloak-ydb-extension-1.0-SNAPSHOT.jar docker/providers/keycloak-ydb-extension-1.0-SNAPSHOT.jar

docker-compose -f docker/docker-compose.yml up -d