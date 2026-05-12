package ydb.jimmer.dialect;

import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.Literals;
import org.babyfish.jimmer.sql.ast.impl.query.ForUpdate;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.jetbrains.annotations.Nullable;

import java.sql.Types;

import static ydb.jimmer.dialect.constant.YdbClassMapping.classToJdbcType;
import static ydb.jimmer.dialect.constant.YdbClassMapping.classToYdbType;

public class YdbDialect extends DefaultDialect {
    @Override
    public String sqlType(Class<?> elementType) {
        return classToYdbType.get(elementType);
    }

    @Override
    public int resolveJdbcType(Class<?> sqlType) {
        Integer jdbcType = classToJdbcType.get(sqlType);
        if (jdbcType == null) {
            return Types.OTHER;
        }
        return jdbcType;
    }

    @Override
    public boolean isDeleteAliasSupported() {
        return false;
    }

    @Override
    public boolean isUpdateAliasSupported() {
        return false;
    }

    @Override
    public boolean isTableOfSubQueryMutable() {
        return false;
    }

    @Override
    public boolean isForeignKeySupported() {
        return false;
    }

    @Override
    public boolean isInsertedIdReturningRequired() {
        return true;
    }

    @Override
    public boolean isExplicitBatchRequired() {
        return true;
    }

    @Override
    public boolean isBatchDumb() {
        return true;
    }

    @Override
    public boolean isUpsertSupported() {
        return true;
    }

    @Override
    public boolean isNoIdUpsertSupported() {
        return false;
    }

    @Override
    public boolean isUpsertWithOptimisticLockSupported() {
        return true;
    }

    @Override
    public boolean isUpsertWithNullableKeySupported() {
        return true;
    }

    @Override
    public void update(UpdateContext ctx) {
        ctx.sql("UPDATE ")
                .appendTableName()
                .enter(AbstractSqlBuilder.ScopeType.SET)
                .appendAssignments()
                .leave()
                .enter(AbstractSqlBuilder.ScopeType.WHERE)
                .appendPredicates()
                .leave()
                .sql(" RETURNING ")
                .appendId();
    }

    public void batchUpdate(UpdateContext ctx) {
        update(ctx.sql("BATCH "));
    }

    @Override
    public void upsert(UpsertContext ctx) {
        if (ctx.isUpdateIgnored() || !ctx.hasUpdatedColumns()) {
            ctx.sql("INSERT INTO ")
                    .appendTableName()
                    .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                    .appendInsertedColumns("")
                    .leave()
                    .sql(" VALUES")
                    .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                    .appendInsertingValues()
                    .leave();
        } else {
            ctx.sql("UPSERT INTO ")
                    .appendTableName()
                    .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                    .appendInsertedColumns("")
                    .leave()
                    .sql(" VALUES")
                    .enter(AbstractSqlBuilder.ScopeType.MULTIPLE_LINE_TUPLE)
                    .appendInsertingValues()
                    .leave();
        }
    }

    public void bulkUpsert(UpsertContext ctx) {
        upsert(ctx.sql("BULK "));
    }

    @Override
    public String transCacheOperatorTableDDL() {
        return """
                CREATE TABLE JIMMER_TRANS_CACHE_OPERATOR(
                \tID Uuid NOT NULL,
                \tIMMUTABLE_TYPE String,
                \tIMMUTABLE_PROP String,
                \tCACHE_KEY String NOT NULL,
                \tREASON String,
                \tPRIMARY KEY(ID)
                )""";
    }

    @Override
    public void renderLPad(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast expression,
            Ast length,
            Ast padString
    ) {
        throw new UnsupportedOperationException(
                "The current dialect \"" + getClass().getName() + "\" does not support LPad."
        );
    }

    @Override
    public void renderRPad(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast expression,
            Ast length,
            Ast padString
    ) {
        throw new UnsupportedOperationException(
                "The current dialect \"" + getClass().getName() + "\" does not support RPad."
        );
    }

    @Override
    public void renderPosition(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast subStrAst,
            Ast expressionAst,
            @Nullable Ast startAst
    ) {
        builder.sql("FIND(")
                .ast(expressionAst, currentPrecedence)
                .sql(", ")
                .ast(subStrAst, currentPrecedence);
        if (startAst != null) {
            builder.sql(", ")
                    .ast(startAst, currentPrecedence);
        }
        builder.sql(")");
    }

    @Override
    public void renderLeft(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast expressionAst,
            Ast lengthAst
    ) {
        renderSubString(builder, currentPrecedence, expressionAst, (Ast) Literals.number(0), lengthAst);
    }

    @Override
    public void renderRight(
            AbstractSqlBuilder<?> builder,
            int currentPrecedence,
            Ast expressionAst,
            Ast lengthAst
    ) {
        builder.sql("substring(")
                .ast(expressionAst, currentPrecedence)
                .sql(", (LENGTH(")
                .ast(expressionAst, currentPrecedence)
                .sql(") - ")
                .ast(lengthAst, currentPrecedence)
                .sql("))");
    }

    @Override
    public void renderForUpdate(AbstractSqlBuilder<?> builder, ForUpdate forUpdate) {
        throw new UnsupportedOperationException(
                "The current dialect \"" + getClass().getName() + "\" does not support 'for update' hint."
        );
    }
}
