rm -f docker/providers/keycloak-ydb-extension-1.0-SNAPSHOT.jar

mvn clean package

mkdir -p docker/providers

JAR_FILE="target/keycloak-ydb-extension-1.0-SNAPSHOT.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "Ошибка: Файл $JAR_FILE не найден!"
    echo "Сборка проекта, возможно, завершилась неудачно."
    exit 1
fi

cp "$JAR_FILE" docker/providers/keycloak-ydb-extension-1.0-SNAPSHOT.jar

docker-compose -f docker/docker-compose.yml up -d