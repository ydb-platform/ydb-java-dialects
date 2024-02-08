package tech.ydb.liquibase.exception;

import java.util.function.Supplier;
import liquibase.exception.ValidationErrors;
import liquibase.statement.SqlStatement;

/**
 * @author Kirill Kurdyukov
 */
public class YdbMessageException {

    public final static String DOES_NOT_SUPPORT_UNIQUE_CONSTRAINTS = "YDB doesn't support UNIQUE CONSTRAINTS! ";

    public final static String DOES_NOT_SUPPORT_AUTO_INCREMENT = "YDB doesn't support AUTO INCREMENT! ";

    public final static String DOES_NOT_SUPPORT_NOT_NULL_CONSTRAINT = "YDB doesn't support NOT NULL CONSTRAINT! ";

    public final static String DOES_NOT_SUPPORT_PRIMARY_KEY_OUTSIDE_CREATE_TABLE =
            "YDB doesn't support PRIMARY KEY it was created by CREATE TABLE! ";

    public final static String DOES_NOT_SUPPORT_DEFAULT_VALUE = "YDB doesn't support DEFAULT VALUE! ";

    public final static String DOES_NOT_SUPPORT_FOREIGN_KEY_CONSTRAINTS =
            "YDB doesn't support FOREIGN KEY CONSTRAINTS! ";

    private YdbMessageException() {
    }

    public static String badTableStrPointer(Supplier<String> tableName) {
        return "[table_name = " + tableName.get() + "]";
    }

    public static <T extends SqlStatement> ValidationErrors ydbDoesNotSupportStatement(T statement) {
        ValidationErrors validationErrors = new ValidationErrors();

        validationErrors.addError("YDB doesn't support this statement: " + statement.getClass().getTypeName());

        return validationErrors;
    }
}
