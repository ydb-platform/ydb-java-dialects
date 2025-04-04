package tech.ydb.data.core;

import java.lang.annotation.Annotation;
import java.sql.SQLType;

import org.springframework.core.MethodParameter;
import org.springframework.data.jdbc.repository.query.JdbcParameter;
import org.springframework.data.jdbc.repository.query.JdbcParameters;
import org.springframework.data.relational.repository.query.RelationalParameters;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;

import tech.ydb.data.core.convert.YQLType;
import tech.ydb.data.core.convert.YdbType;
import tech.ydb.table.values.PrimitiveType;

/**
 * Parameters of the YDB query. Custom implementation of {@link JdbcParameters}, emerged from requirement
 * to support {@link YdbType} on method parameters:
 * <p/>
 * <pre class="code">
 * &#064;Query("SELECT * FROM my_table WHERE type = :type")
 * Optional<MyEntity> findByType(@Param("type") @YdbType("Text") String type);
 * </pre>
 *
 * @author Mikhail Polivakha
 */
public class YdbQueryParameters extends JdbcParameters {

    public YdbQueryParameters(ParametersSource parametersSource) {
        super(parametersSource, methodParameter -> {

            YdbType ydbType = methodParameter.getParameterAnnotation(YdbType.class);

            if (ydbType != null) {
                YQLType sqlType = new YQLType(PrimitiveType.valueOf(ydbType.value()));
                return new YdbMethodParameter(
                  methodParameter,
                  parametersSource.getDomainTypeInformation(),
                  sqlType,
                  Lazy.of(() -> sqlType) // TODO: Do we support the array/collection-like types?
                );
            } else {
                return new YdbMethodParameter(methodParameter, parametersSource.getDomainTypeInformation());
            }
        });
    }

    static class YdbMethodParameter extends JdbcParameter {

        public YdbMethodParameter(MethodParameter parameter, TypeInformation<?> domainType) {
            super(parameter, domainType);
        }

        public YdbMethodParameter(MethodParameter parameter, TypeInformation<?> domainType, SQLType sqlType, Lazy<SQLType> actualSqlType) {
            super(parameter, domainType, sqlType, actualSqlType);
        }
    }
}
