rm -f docker/providers/keycloak-ydb-extension-1.0-SNAPSHOT.jar

mvn -f core/pom.xml clean package

mkdir -p docker/providers

JAR_FILE="core/target/keycloak-ydb-extension-1.0-SNAPSHOT.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: File $JAR_FILE not found!"
    echo "The project build may have failed."
    exit 1
fi

cp "$JAR_FILE" docker/providers/keycloak-ydb-extension-1.0-SNAPSHOT.jar

docker-compose -f docker/docker-compose.yml up -d