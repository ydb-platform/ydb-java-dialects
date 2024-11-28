YDB_JDBC_DRIVER_VERSION=2.3.6

echo Stress test using ydb-jdbc-driver-shaded:"$YDB_JDBC_DRIVER_VERSION"

curl -L -o ydb-jdbc-driver.jar https://repo1.maven.org/maven2/tech/ydb/jdbc/ydb-jdbc-driver-shaded/$YDB_JDBC_DRIVER_VERSION/ydb-jdbc-driver-shaded-$YDB_JDBC_DRIVER_VERSION.jar

cd ..
mvn clean package -DskipTests=true

LIQUIBASE_DIALECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

LIQUIBASE_CORE_VERSION=$(mvn help:evaluate -Dexpression=liquibase.core.version -q -DforceStdout)

cd stress-test

echo Stress test using liquibase-ydb-dialect:"$LIQUIBASE_DIALECT_VERSION"

echo Stress test using liquibase-core:"$LIQUIBASE_CORE_VERSION"

cp ../target/liquibase-ydb-dialect-"$LIQUIBASE_DIALECT_VERSION".jar ./liquibase-ydb-dialect.jar

mkdir liquibase-cli
cd liquibase-cli

curl -L "https://github.com/liquibase/liquibase/releases/download/v$LIQUIBASE_CORE_VERSION/liquibase-$LIQUIBASE_CORE_VERSION.zip" -o liquibase.zip

unzip liquibase.zip

cd ..

cp liquibase-ydb-dialect.jar ./liquibase-cli/internal/lib/
cp ydb-jdbc-driver.jar ./liquibase-cli/internal/lib/

echo Start Go test

go test

rm -rf liquibase-cli
