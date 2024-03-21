YDB_JDBC_DRIVER_VERSION=2.0.7

echo Stress test using ydb-jdbc-driver-shaded:"$YDB_JDBC_DRIVER_VERSION"

curl -L -o ydb-jdbc-driver.jar https://repo1.maven.org/maven2/tech/ydb/jdbc/ydb-jdbc-driver-shaded/$YDB_JDBC_DRIVER_VERSION/ydb-jdbc-driver-shaded-$YDB_JDBC_DRIVER_VERSION.jar

cd ..
mvn clean package -DskipTests=true

LIQUIBASE_DIALECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

cd stress-test

echo Stress test using liquibase-ydb-dialect:"$LIQUIBASE_DIALECT_VERSION"

cp ../target/liquibase-ydb-dialect-"$LIQUIBASE_DIALECT_VERSION".jar ./liquibase-ydb-dialect.jar

docker build -t liquibase-ydb .

docker network create stress-test-network

docker run -d --rm --network stress-test-network -h ydb.local \
--name ydb-local \
-p 2135:2135 -p 8765:8765 -p 2136:2136 \
-e YDB_DEFAULT_LOG_LEVEL=NOTICE \
-e GRPC_TLS_PORT=2135 -e GRPC_PORT=2136 -e MON_PORT=8765 \
cr.yandex/yc/yandex-docker-local-ydb:latest

while [[ "${'$'}(docker inspect --format='{{json .State.Health.Status}}' ydb-local)" != '"healthy"' ]]
do
    echo "awaiting ydb-local healthy..."
    sleep 1
done

echo Start Go test

go test

docker stop ydb-local

docker network rm stress-test-network
