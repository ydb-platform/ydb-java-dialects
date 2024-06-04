package tech.ydb.jooq;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.types.UByte;
import org.jooq.types.UInteger;
import org.jooq.types.ULong;
import org.jooq.types.UNumber;
import tech.ydb.jooq.dsl.function.aggregate.*;
import tech.ydb.jooq.dsl.function.builtin.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

import static org.jooq.impl.YdbTools.*;
import static tech.ydb.jooq.YDB.val;

public final class YdbFunction {

    private YdbFunction() {
        throw new UnsupportedOperationException();
    }

    /**
     * The <code>COALESCE</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#coalesce">documentation</a>
     */
    @NotNull
    public static <T> Field<T> coalesce(Field<T> field, T value) {
        return coalesce(field, DSL.val(value, field.getDataType()));
    }

    /**
     * The <code>COALESCE</code> function for multiple fields.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#coalesce">documentation</a>
     */
    @NotNull
    @SafeVarargs
    public static <T> Field<T> coalesce(Field<T> field, Field<T>... fields) {
        return new Coalesce<>(combineTyped(field, fields));
    }


    /**
     * The <code>LENGTH</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#length">documentation</a>
     */
    @NotNull
    public static Field<UInteger> length(@NotNull Field<?> value) {
        return new Length(value);
    }

    /**
     * The <code>LEN</code> function (alias for <code>Length</code>)
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#length">documentation</a>
     */
    @NotNull
    public static Field<UInteger> len(@NotNull Field<?> value) {
        return length(value);
    }


    /**
     * The <code>SUBSTRING</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#substring">documentation</a>
     */
    @NotNull
    public static Field<byte[]> substring(@NotNull Field<byte[]> source, int startingPosition) {
        return substring(source, UInteger.valueOf(startingPosition));
    }

    /**
     * The <code>SUBSTRING</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#substring">documentation</a>
     */
    @NotNull
    public static Field<byte[]> substring(@NotNull Field<byte[]> source, int startingPosition, int length) {
        return substring(source, UInteger.valueOf(startingPosition), UInteger.valueOf(length));
    }

    /**
     * The <code>SUBSTRING</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#substring">documentation</a>
     */
    @NotNull
    public static Field<byte[]> substring(@NotNull Field<byte[]> source,
                                          @Nullable UInteger startingPosition) {
        return substring(source, val(startingPosition), null);
    }

    /**
     * The <code>SUBSTRING</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#substring">documentation</a>
     */
    @NotNull
    public static Field<byte[]> substring(@NotNull Field<byte[]> source,
                                          @Nullable UInteger startingPosition,
                                          @Nullable UInteger length) {
        return substring(source, val(startingPosition), val(length));
    }

    /**
     * The <code>SUBSTRING</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#substring">documentation</a>
     */
    @NotNull
    public static Field<byte[]> substring(@NotNull Field<byte[]> source,
                                          @NotNull Field<UInteger> startingPosition,
                                          @Nullable Field<UInteger> length) {
        return new Substring(source, startingPosition, length);
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    @NotNull
    public static Field<UInteger> find(@NotNull Field<byte[]> source,
                                       byte[] substring) {
        return find(source, val(substring));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    @NotNull
    public static Field<UInteger> find(@NotNull Field<byte[]> source,
                                       byte[] substring,
                                       int startingPosition) {
        return find(source, substring, UInteger.valueOf(startingPosition));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    @NotNull
    public static Field<UInteger> find(@NotNull Field<byte[]> source,
                                       byte[] substring,
                                       @Nullable UInteger startingPosition) {
        return find(source, val(substring), val(startingPosition));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    @NotNull
    public static Field<UInteger> find(@NotNull Field<String> source,
                                       @NotNull String substring) {
        return findUtf8(source, val(substring));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    @NotNull
    public static Field<UInteger> find(@NotNull Field<String> source,
                                       @NotNull String substring,
                                       int startingPosition) {
        return find(source, substring, UInteger.valueOf(startingPosition));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    @NotNull
    public static Field<UInteger> find(@NotNull Field<String> source,
                                       @NotNull String substring,
                                       @Nullable UInteger startingPosition) {
        return findUtf8(source, val(substring), val(startingPosition));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    @NotNull
    public static Field<UInteger> find(@NotNull Field<byte[]> source,
                                       @NotNull Field<byte[]> substring) {
        return find(source, substring, (Field<UInteger>) null);
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    @NotNull
    public static Field<UInteger> findUtf8(@NotNull Field<String> source,
                                           @NotNull Field<String> substring) {
        return findUtf8(source, substring, (Field<UInteger>) null);
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    @NotNull
    public static Field<UInteger> find(@NotNull Field<byte[]> source,
                                       @NotNull Field<byte[]> substring,
                                       @Nullable UInteger startingPosition) {
        return find(source, substring, val(startingPosition));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    @NotNull
    public static Field<UInteger> findUtf8(@NotNull Field<String> source,
                                           @NotNull Field<String> substring,
                                           @Nullable UInteger startingPosition) {
        return findUtf8(source, substring, val(startingPosition));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    @NotNull
    public static Field<UInteger> find(@NotNull Field<byte[]> source,
                                           @NotNull Field<byte[]> substring,
                                           @Nullable Field<UInteger> startingPosition) {
        return new Find<>(source, substring, startingPosition);
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    @NotNull
    public static Field<UInteger> findUtf8(@NotNull Field<String> source,
                                           @NotNull Field<String> substring,
                                           @Nullable Field<UInteger> startingPosition) {
        return new Find<>(source, substring, startingPosition);
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    @NotNull
    public static Field<UInteger> rFind(@NotNull Field<byte[]> source,
                                        byte[] substring) {
        return rFind(source, val(substring));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    @NotNull
    public static Field<UInteger> rFind(@NotNull Field<byte[]> source,
                                        byte[] substring,
                                        int startingPosition) {
        return rFind(source, substring, UInteger.valueOf(startingPosition));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    @NotNull
    public static Field<UInteger> rFind(@NotNull Field<byte[]> source,
                                        byte[] substring,
                                        UInteger startingPosition) {
        return rFind(source, val(substring), val(startingPosition));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    @NotNull
    public static Field<UInteger> rFind(@NotNull Field<String> source,
                                        String substring) {
        return rFindUtf8(source, val(substring));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    @NotNull
    public static Field<UInteger> rFind(@NotNull Field<String> source,
                                        String substring,
                                        int startingPosition) {
        return rFind(source, substring, UInteger.valueOf(startingPosition));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    @NotNull
    public static Field<UInteger> rFind(@NotNull Field<String> source,
                                        String substring,
                                        UInteger startingPosition) {
        return rFindUtf8(source, val(substring), val(startingPosition));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    @NotNull
    public static Field<UInteger> rFind(@NotNull Field<byte[]> source,
                                        @NotNull Field<byte[]> substring) {
        return rFind(source, substring, (Field<UInteger>) null);
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    @NotNull
    public static Field<UInteger> rFindUtf8(@NotNull Field<String> source,
                                            @NotNull Field<String> substring) {
        return rFindUtf8(source, substring, (Field<UInteger>) null);
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    @NotNull
    public static Field<UInteger> rFind(@NotNull Field<byte[]> source,
                                        @NotNull Field<byte[]> substring,
                                        @Nullable UInteger startingPosition) {
        return rFind(source, substring, val(startingPosition));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    @NotNull
    public static Field<UInteger> rFindUtf8(@NotNull Field<String> source,
                                            @NotNull Field<String> substring,
                                            @Nullable UInteger startingPosition) {
        return rFindUtf8(source, substring, val(startingPosition));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    @NotNull
    public static Field<UInteger> rFind(@NotNull Field<byte[]> source,
                                        @NotNull Field<byte[]> substring,
                                        @Nullable Field<UInteger> startingPosition) {
        return new RFind<>(source, substring, startingPosition);
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    @NotNull
    public static Field<UInteger> rFindUtf8(@NotNull Field<String> source,
                                            @NotNull Field<String> substring,
                                            @Nullable Field<UInteger> startingPosition) {
        return new RFind<>(source, substring, startingPosition);
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition startsWith(@NotNull Field<byte[]> source,
                                       byte[] substring) {
        return startsWith(source, val(substring));
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition startsWithUtf8(@NotNull Field<String> source,
                                           byte[] substring) {
        return startsWith(source, val(substring));
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition startsWith(@NotNull Field<byte[]> source,
                                       @Nullable String substring) {
        return startsWith(source, val(substring));
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition startsWithUtf8(@NotNull Field<String> source,
                                           @Nullable String substring) {
        return startsWith(source, val(substring));
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition startsWith(byte[] source,
                                       @NotNull Field<byte[]> substring) {
        return startsWith(val(source), substring);
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition startsWithUtf8(byte[] source,
                                           @NotNull Field<String> substring) {
        return startsWith(val(source), substring);
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition startsWith(@Nullable String source,
                                       @NotNull Field<byte[]> substring) {
        return startsWith(val(source), substring);
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition startsWithUtf8(@Nullable String source,
                                           @NotNull Field<String> substring) {
        return startsWith(val(source), substring);
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition startsWith(@NotNull Field<?> source,
                                       @NotNull Field<?> substring) {
        return new StartsWith(source, substring);
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition endsWith(@NotNull Field<byte[]> source,
                                     byte[] substring) {
        return endsWith(source, val(substring));
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition endsWithUtf8(@NotNull Field<String> source,
                                         byte[] substring) {
        return endsWith(source, val(substring));
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition endsWith(@NotNull Field<byte[]> source,
                                     @Nullable String substring) {
        return endsWith(source, val(substring));
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition endsWithUtf8(@NotNull Field<String> source,
                                         @Nullable String substring) {
        return endsWith(source, val(substring));
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition endsWith(byte[] source,
                                     @NotNull Field<byte[]> substring) {
        return endsWith(val(source), substring);
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition endsWithUtf8(byte[] source,
                                         @NotNull Field<String> substring) {
        return endsWith(val(source), substring);
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition endsWith(@Nullable String source,
                                     @NotNull Field<byte[]> substring) {
        return endsWith(val(source), substring);
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition endsWithUtf8(@Nullable String source,
                                         @NotNull Field<String> substring) {
        return endsWith(val(source), substring);
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    @NotNull
    public static Condition endsWith(@NotNull Field<?> source,
                                     @NotNull Field<?> substring) {
        return new EndsWith(source, substring);
    }

    /**
     * The <code>IF</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#if">documentation</a>
     */
    @NotNull
    public static <T> Field<T> if_(@NotNull Condition condition,
                                   @Nullable T ifTrue) {
        return if_(condition, (Field<T>) val(ifTrue), (Field<T>) null);
    }

    /**
     * The <code>IF</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#if">documentation</a>
     */
    @NotNull
    public static <T> Field<T> if_(@NotNull Condition condition,
                                   @NotNull Field<T> ifTrue) {
        return if_(condition, ifTrue, (Field<T>) null);
    }

    /**
     * The <code>IF</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#if">documentation</a>
     */
    @NotNull
    public static <T> Field<T> if_(@NotNull Condition condition,
                                   @Nullable T ifTrue,
                                   @Nullable T ifFalse) {
        return if_(condition, val(ifTrue), ifFalse);
    }

    /**
     * The <code>IF</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#if">documentation</a>
     */
    @NotNull
    public static <T> Field<T> if_(@NotNull Condition condition,
                                   @Nullable T ifTrue,
                                   @NotNull Field<T> ifFalse) {
        return if_(condition, DSL.val(ifTrue, ifFalse.getDataType()), ifFalse);
    }

    /**
     * The <code>IF</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#if">documentation</a>
     */
    @NotNull
    public static <T> Field<T> if_(@NotNull Condition condition,
                                   @NotNull Field<T> ifTrue,
                                   @Nullable T ifFalse) {
        return if_(condition, ifTrue, DSL.val(ifFalse, ifTrue.getDataType()));
    }

    /**
     * The <code>IF</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#if">documentation</a>
     */
    @NotNull
    public static <T> Field<T> if_(@NotNull Condition condition,
                                   @NotNull Field<T> ifTrue,
                                   @Nullable Field<T> ifFalse) {
        return new If<>(condition, ifTrue, ifFalse);
    }

    /**
     * The <code>NANVL</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#nanvl">documentation</a>
     */
    @NotNull
    public static Field<Float> nanvl(@NotNull Field<Float> expression,
                                     @Nullable Float replacement) {
        return nanvl(expression, val(replacement));
    }

    /**
     * The <code>NANVL</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#nanvl">documentation</a>
     */
    @NotNull
    public static Field<Double> nanvl(@NotNull Field<Double> expression,
                                      @Nullable Double replacement) {
        return nanvl(expression, val(replacement));
    }

    /**
     * The <code>NANVL</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#nanvl">documentation</a>
     */
    @NotNull
    public static <T> Field<T> nanvl(@NotNull Field<T> condition,
                                     @NotNull Field<T> replacement) {
        return new NaNvl<>(condition, replacement);
    }

    /**
     * The <code>Random</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    @NotNull
    public static Field<Double> random(@NotNull Object value,
                                       @NotNull Object... values) {
        return new Random(combine(val(value), fieldsArray(values)));
    }

    /**
     * The <code>Random</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    @NotNull
    public static Field<Double> random(@NotNull Field<?> field,
                                       @NotNull Object value) {
        return new Random(new Field[]{field, val(value)});
    }

    /**
     * The <code>Random</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    @NotNull
    public static Field<Double> random(@NotNull Field<?> field,
                                       @NotNull Field<?>... fields) {
        return new Random(combine(field, fields));
    }

    /**
     * The <code>RandomNumber</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    @NotNull
    public static Field<ULong> randomNumber(@NotNull Object value,
                                            @NotNull Object... values) {
        return new RandomNumber(combine(val(value), fieldsArray(values)));
    }

    /**
     * The <code>RandomNumber</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    @NotNull
    public static Field<ULong> randomNumber(@NotNull Field<?> field,
                                            @NotNull Object value) {
        return new RandomNumber(new Field[]{field, val(value)});
    }

    /**
     * The <code>RandomNumber</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    @NotNull
    public static Field<ULong> randomNumber(@NotNull Field<?> field,
                                            @NotNull Field<?>... fields) {
        return new RandomNumber(combine(field, fields));
    }

    /**
     * The <code>RandomUuid</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    @NotNull
    public static Field<UUID> randomUuid(@NotNull Object value,
                                         @NotNull Object... values) {
        return new RandomUuid(combine(val(value), fieldsArray(values)));
    }

    /**
     * The <code>RandomUuid</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    @NotNull
    public static Field<UUID> randomUuid(@NotNull Field<?> field,
                                         @NotNull Object value) {
        return new RandomUuid(new Field[]{field, val(value)});
    }

    /**
     * The <code>RandomUuid</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    @NotNull
    public static Field<UUID> randomUuid(@NotNull Field<?> field,
                                         @NotNull Field<?>... fields) {
        return new RandomUuid(combine(field, fields));
    }

    /**
     * The <code>CurrentUtcDate</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-utc">documentation</a>
     */
    @NotNull
    public static Field<LocalDate> currentUtcDate(@NotNull Object... values) {
        return currentUtcDate(fieldsArray(values));
    }

    /**
     * The <code>CurrentUtcDate</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-utc">documentation</a>
     */
    @NotNull
    public static Field<LocalDate> currentUtcDate(@NotNull Field<?>... fields) {
        return new CurrentUtcDate(fields);
    }

    /**
     * The <code>CurrentUtcDatetime</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-utc">documentation</a>
     */
    @NotNull
    public static Field<LocalDateTime> currentUtcDatetime(@NotNull Object... values) {
        return currentUtcDatetime(fieldsArray(values));
    }

    /**
     * The <code>CurrentUtcDatetime</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-utc">documentation</a>
     */
    @NotNull
    public static Field<LocalDateTime> currentUtcDatetime(@NotNull Field<?>... fields) {
        return new CurrentUtcDatetime(fields);
    }

    /**
     * The <code>CurrentUtcTimestamp</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-utc">documentation</a>
     */
    @NotNull
    public static Field<Instant> currentUtcTimestamp(@NotNull Object... values) {
        return currentUtcTimestamp(fieldsArray(values));
    }

    /**
     * The <code>CurrentUtcTimestamp</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-utc">documentation</a>
     */
    @NotNull
    public static Field<Instant> currentUtcTimestamp(@NotNull Field<?>... fields) {
        return new CurrentUtcTimestamp(fields);
    }

    /**
     * The <code>CurrentTzDate</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    @NotNull
    public static Field<ZonedDateTime> currentTzDate(@NotNull ZoneId timeZone,
                                                     @NotNull Object... values) {
        return currentTzDate(val(timeZone.toString().getBytes()), fieldsArray(values));
    }

    /**
     * The <code>CurrentTzDate</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    @NotNull
    public static Field<ZonedDateTime> currentTzDate(@NotNull String timeZone,
                                                     @NotNull Object... values) {
        return currentTzDate(val(timeZone.getBytes()), fieldsArray(values));
    }

    /**
     * The <code>CurrentTzDate</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    @NotNull
    public static Field<ZonedDateTime> currentTzDate(@NotNull Field<byte[]> timeZone,
                                                     @NotNull Field<?>... fields) {
        return new CurrentTzDate(timeZone, fields);
    }

    /**
     * The <code>CurrentTzDatetime</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    @NotNull
    public static Field<ZonedDateTime> currentTzDatetime(@NotNull ZoneId timeZone,
                                                         @NotNull Object... values) {
        return currentTzDatetime(val(timeZone.toString().getBytes()), fieldsArray(values));
    }

    /**
     * The <code>CurrentTzDatetime</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    @NotNull
    public static Field<ZonedDateTime> currentTzDatetime(@NotNull String timeZone,
                                                         @NotNull Object... values) {
        return currentTzDatetime(val(timeZone.getBytes()), fieldsArray(values));
    }

    /**
     * The <code>CurrentTzDatetime</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    @NotNull
    public static Field<ZonedDateTime> currentTzDatetime(@NotNull Field<byte[]> timeZone,
                                                         @NotNull Field<?>... fields) {
        return new CurrentTzDatetime(timeZone, fields);
    }

    /**
     * The <code>CurrentTzTimestamp</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    @NotNull
    public static Field<ZonedDateTime> currentTzTimestamp(@NotNull ZoneId timeZone,
                                                          @NotNull Object... values) {
        return currentTzTimestamp(timeZone.toString(), values);
    }

    /**
     * The <code>CurrentTzTimestamp</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    @NotNull
    public static Field<ZonedDateTime> currentTzTimestamp(@NotNull String timeZone,
                                                          @NotNull Object... values) {
        return currentTzTimestamp(val(timeZone.getBytes()), fieldsArray(values));
    }

    /**
     * The <code>CurrentTzTimestamp</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    @NotNull
    public static Field<ZonedDateTime> currentTzTimestamp(@NotNull Field<byte[]> timeZone,
                                                          @NotNull Field<?>... fields) {
        return new CurrentTzTimestamp(timeZone, fields);
    }

    /**
     * The <code>AddTimezone</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#addtimezone">documentation</a>
     */
    @NotNull
    public static Field<ZonedDateTime> addTimezone(@Nullable LocalDate date,
                                                   @NotNull ZoneId timeZone) {
        return addTimezone(val(date), timeZone);
    }

    /**
     * The <code>AddTimezone</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#addtimezone">documentation</a>
     */
    @NotNull
    public static Field<ZonedDateTime> addTimezone(@Nullable LocalDateTime date,
                                                   @NotNull ZoneId timeZone) {
        return addTimezone(val(date), timeZone);
    }

    /**
     * The <code>AddTimezone</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#addtimezone">documentation</a>
     */
    @NotNull
    public static Field<ZonedDateTime> addTimezone(@Nullable Instant date,
                                                   @NotNull ZoneId timeZone) {
        return addTimezone(val(date), timeZone);
    }

    /**
     * The <code>AddTimezone</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#addtimezone">documentation</a>
     */
    @NotNull
    public static Field<ZonedDateTime> addTimezone(@NotNull Field<?> date,
                                                   @NotNull ZoneId timeZone) {
        return addTimezone(date, val(timeZone.toString().getBytes()));
    }

    /**
     * The <code>AddTimezone</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#addtimezone">documentation</a>
     */
    @NotNull
    public static Field<ZonedDateTime> addTimezone(@NotNull Field<?> date,
                                                   @NotNull Field<byte[]> timeZone) {
        return new AddTimezone(date, timeZone);
    }

    /**
     * The <code>RemoveTimezone</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#removetimezone">documentation</a>
     */
    @NotNull
    public static <T> Field<T> removeTimezone(@NotNull Field<ZonedDateTime> date,
                                              @NotNull DataType<T> type) {
        return new RemoveTimezone<>(date, type);
    }

    /**
     * The <code>MaxOf</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    @NotNull
    public static <T> Field<T> maxOf(Field<T> field, T value) {
        return maxOf(field, DSL.val(value, field.getDataType()));
    }

    /**
     * The <code>MaxOf</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    @NotNull
    @SafeVarargs
    public static <T> Field<T> maxOf(Field<T> field, Field<T>... fields) {
        return new MaxOf<>(combineTyped(field, fields));
    }

    /**
     * The <code>MinOf</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    @NotNull
    public static <T> Field<T> minOf(Field<T> field, T value) {
        return minOf(field, DSL.val(value, field.getDataType()));
    }

    /**
     * The <code>MinOf</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    @NotNull
    @SafeVarargs
    public static <T> Field<T> minOf(Field<T> field, Field<T>... fields) {
        return new MinOf<>(combineTyped(field, fields));
    }

    /**
     * The <code>Greatest</code> function (alias for <code>MaxOf</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    @NotNull
    public static <T> Field<T> greatest(Field<T> field, T value) {
        return maxOf(field, value);
    }

    /**
     * The <code>Greatest</code> function (alias for <code>MaxOf</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    @NotNull
    @SafeVarargs
    public static <T> Field<T> greatest(Field<T> field, Field<T>... fields) {
        return maxOf(field, fields);
    }

    /**
     * The <code>Least</code> function (alias for <code>MinOf</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    @NotNull
    public static <T> Field<T> least(Field<T> field, T value) {
        return minOf(field, value);
    }

    /**
     * The <code>Least</code> function (alias for <code>MinOf</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    @NotNull
    @SafeVarargs
    public static <T> Field<T> least(Field<T> field, Field<T>... fields) {
        return minOf(field, fields);
    }

    /**
     * The <code>TableRow</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#tablerow">documentation</a>
     */
    @NotNull
    public static Field<?> tableRow() {
        return new TableRow();
    }

    /**
     * The <code>JoinTableRow</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#tablerow">documentation</a>
     */
    @NotNull
    public static Field<?> joinTableRow() {
        return new JoinTableRow();
    }

    /**
     * The <code>Ensure</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#ensure">documentation</a>
     */
    @NotNull
    public static <T> Field<T> ensure(@NotNull Field<T> value,
                                      @NotNull Condition condition) {
        return ensure(value, condition, (Field<byte[]>) null);
    }

    /**
     * The <code>Ensure</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#ensure">documentation</a>
     */
    @NotNull
    public static <T> Field<T> ensure(@NotNull Field<T> value,
                                      @NotNull Condition condition,
                                      @Nullable String message) {
        return ensure(value, condition, message != null ? val(message.getBytes()) : null);
    }

    /**
     * The <code>Ensure</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#ensure">documentation</a>
     */
    @NotNull
    public static <T> Field<T> ensure(@NotNull Field<T> value,
                                      @NotNull Condition condition,
                                      @Nullable Field<byte[]> message) {
        return new Ensure<>(value, condition, message);
    }

    /**
     * The <code>AssumeStrict</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#assumestrict">documentation</a>
     */
    @NotNull
    public static <T> Field<T> assumeStrict(@NotNull Field<T> value) {
        return new AssumeStrict<>(value);
    }

    /**
     * The <code>Likely</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#likely">documentation</a>
     */
    @NotNull
    public static Condition likely(@NotNull Condition condition) {
        return new Likely(condition);
    }

    /**
     * The <code>ToBytes</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#to-from-bytes">documentation</a>
     */
    @NotNull
    public static Field<byte[]> toBytes(@NotNull Field<?> value) {
        return new ToBytes(value);
    }

    /**
     * The <code>FromBytes</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#to-from-bytes">documentation</a>
     */
    @NotNull
    public static <T> Field<T> fromBytes(@NotNull Field<byte[]> bytes,
                                         @NotNull DataType<T> type) {
        return new FromBytes<>(bytes, type);
    }

    /**
     * The <code>ByteAt</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#byteat">documentation</a>
     */
    @NotNull
    public static Field<UByte> byteAt(@NotNull Field<byte[]> source,
                                      int index) {
        return byteAt(source, UInteger.valueOf(index));
    }

    /**
     * The <code>ByteAt</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#byteat">documentation</a>
     */
    @NotNull
    public static Field<UByte> byteAt(@NotNull Field<byte[]> source,
                                      @NotNull UInteger index) {
        return byteAt(source, val(index));
    }

    /**
     * The <code>ByteAt</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#byteat">documentation</a>
     */
    @NotNull
    public static Field<UByte> byteAt(@NotNull Field<byte[]> source,
                                      @NotNull Field<UInteger> index) {
        return new ByteAt(source, index);
    }

    /**
     * The <code>ByteAt</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#byteat">documentation</a>
     */
    @NotNull
    public static Field<UByte> byteAtUtf8(@NotNull Field<String> source,
                                          int index) {
        return byteAtUtf8(source, UInteger.valueOf(index));
    }

    /**
     * The <code>ByteAt</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#byteat">documentation</a>
     */
    @NotNull
    public static Field<UByte> byteAtUtf8(@NotNull Field<String> source,
                                          @NotNull UInteger index) {
        return byteAtUtf8(source, val(index));
    }

    /**
     * The <code>ByteAt</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#byteat">documentation</a>
     */
    @NotNull
    public static Field<UByte> byteAtUtf8(@NotNull Field<String> source,
                                          @NotNull Field<UInteger> index) {
        return new ByteAt(source, index);
    }

    /**
     * The <code>TestBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    @NotNull
    public static Condition testBit(@NotNull Field<? extends UNumber> source,
                                    int index) {
        return testBit(source, UByte.valueOf(index));
    }

    /**
     * The <code>TestBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    @NotNull
    public static Condition testBit(@NotNull Field<? extends UNumber> source,
                                    @NotNull UByte index) {
        return testBit(source, val(index));
    }

    /**
     * The <code>TestBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    @NotNull
    public static Condition testBit(@NotNull Field<? extends UNumber> source,
                                    @NotNull Field<UByte> index) {
        return new TestBit(source, index);
    }

    /**
     * The <code>ClearBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    @NotNull
    public static <T extends UNumber> Field<T> clearBit(@NotNull Field<T> source,
                                                        int index) {
        return clearBit(source, UByte.valueOf(index));
    }

    /**
     * The <code>ClearBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    @NotNull
    public static <T extends UNumber> Field<T> clearBit(@NotNull Field<T> source,
                                                        @NotNull UByte index) {
        return clearBit(source, val(index));
    }

    public static <T extends UNumber> Field<T> clearBit(@NotNull Field<T> source,
                                                        @NotNull Field<UByte> index) {
        return new ClearBit<>(source, index);
    }

    /**
     * The <code>SetBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    @NotNull
    public static <T extends UNumber> Field<T> setBit(@NotNull Field<T> source,
                                                      int index) {
        return setBit(source, UByte.valueOf(index));
    }

    /**
     * The <code>SetBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    @NotNull
    public static <T extends UNumber> Field<T> setBit(@NotNull Field<T> source,
                                                      @NotNull UByte index) {
        return setBit(source, val(index));
    }

    /**
     * The <code>SetBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    @NotNull
    public static <T extends UNumber> Field<T> setBit(@NotNull Field<T> source,
                                                      @NotNull Field<UByte> index) {
        return new SetBit<>(source, index);
    }

    /**
     * The <code>FlipBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    @NotNull
    public static <T extends UNumber> Field<T> flipBit(@NotNull Field<T> source,
                                                       int index) {
        return flipBit(source, UByte.valueOf(index));
    }

    /**
     * The <code>FlipBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    @NotNull
    public static <T extends UNumber> Field<T> flipBit(@NotNull Field<T> source,
                                                       @NotNull UByte index) {
        return flipBit(source, val(index));
    }

    /**
     * The <code>FlipBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    @NotNull
    public static <T extends UNumber> Field<T> flipBit(@NotNull Field<T> source,
                                                       @NotNull Field<UByte> index) {
        return new FlipBit<>(source, index);
    }

    /**
     * The <code>Abs</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#abs">documentation</a>
     */
    @NotNull
    public static <T extends Number> Field<T> abs(@NotNull Field<T> value) {
        return new Abs<>(value);
    }

    /**
     * The <code>Just</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#optional-ops">documentation</a>
     */
    @NotNull
    public static <T> Field<T> just(@NotNull Field<T> value) {
        return new Just<>(value);
    }

    /**
     * The <code>Unwrap</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#optional-ops">documentation</a>
     */
    @NotNull
    public static <T> Field<T> unwrap(@NotNull Field<T> value) {
        return unwrap(value, (Field<byte[]>) null);
    }

    /**
     * The <code>Unwrap</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#optional-ops">documentation</a>
     */
    @NotNull
    public static <T> Field<T> unwrap(@NotNull Field<T> value, @NotNull String message) {
        return unwrap(value, val(message.getBytes()));
    }

    /**
     * The <code>Unwrap</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#optional-ops">documentation</a>
     */
    @NotNull
    public static <T> Field<T> unwrap(@NotNull Field<T> value,
                                      @Nullable Field<byte[]> message) {
        return new Unwrap<>(value, message);
    }

    /**
     * The <code>Nothing</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#optional-ops">documentation</a>
     */
    @NotNull
    public static <T> Field<T> nothing(@NotNull DataType<T> type) {
        return new Nothing<>(type);
    }

    /**
     * The <code>Pickle</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#pickle">documentation</a>
     */
    @NotNull
    public static Field<byte[]> pickle(@NotNull Field<?> value) {
        return new Pickle(value);
    }

    /**
     * The <code>StablePickle</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#pickle">documentation</a>
     */
    @NotNull
    public static Field<byte[]> stablePickle(@NotNull Field<?> value) {
        return new StablePickle(value);
    }

    /**
     * The <code>Unpickle</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#pickle">documentation</a>
     */
    @NotNull
    public static <T> Field<T> unpickle(@NotNull DataType<T> type,
                                        @NotNull Field<byte[]> bytes) {
        return new Unpickle<>(type, bytes);
    }

    /**
     * The <code>COUNT</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#count">documentation</a>
     */
    @NotNull
    public static AggregateFunction<ULong> count(@NotNull Field<?> field) {
        return new Count(field, false);
    }

    /**
     * The <code>COUNT</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#count">documentation</a>
     */
    @NotNull
    public static AggregateFunction<ULong> count(@NotNull SelectFieldOrAsterisk field) {
        return new Count(DSL.field("{0}", field), false);
    }

    /**
     * The <code>COUNT(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#count">documentation</a>
     */
    @NotNull
    public static AggregateFunction<ULong> countDistinct(@NotNull Field<?> field) {
        return new Count(field, true);
    }

    /**
     * The <code>MIN</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#min-max">documentation</a>
     */
    @NotNull
    public static <T> AggregateFunction<T> min(@NotNull Field<T> field) {
        return new Min<>(field, false);
    }

    /**
     * The <code>MIN(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#min-max">documentation</a>
     */
    @NotNull
    public static <T> AggregateFunction<T> minDistinct(@NotNull Field<T> field) {
        return new Min<>(field, true);
    }

    /**
     * The <code>MAX</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#min-max">documentation</a>
     */
    @NotNull
    public static <T> AggregateFunction<T> max(@NotNull Field<T> field) {
        return new Max<>(field, false);
    }

    /**
     * The <code>MAX(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#min-max">documentation</a>
     */
    @NotNull
    public static <T> AggregateFunction<T> maxDistinct(@NotNull Field<T> field) {
        return new Max<>(field, true);
    }

    /**
     * The <code>SUM</code> aggregate function for unsigned types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    @NotNull
    public static AggregateFunction<ULong> sumUnsigned(@NotNull Field<? extends UNumber> field) {
        return new Sum<>(field, false, YdbTypes.UINT64);
    }

    /**
     * The <code>SUM</code> aggregate function for signed types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Long> sumSigned(@NotNull Field<? extends Number> field) {
        return new Sum<>(field, false, YdbTypes.INT64);
    }

    /**
     * The <code>SUM</code> aggregate function for INTERVAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Duration> sumInterval(@NotNull Field<Duration> field) {
        return new Sum<>(field, false, YdbTypes.INTERVAL);
    }

    /**
     * The <code>SUM</code> aggregate function for DECIMAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    @NotNull
    public static AggregateFunction<BigDecimal> sumDecimal(@NotNull Field<BigDecimal> field) {
        return new Sum<>(field, false, YdbTypes.DECIMAL);
    }

    /**
     * The <code>SUM(DISTINCT field)</code> aggregate function for unsigned types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    @NotNull
    public static AggregateFunction<ULong> sumUnsignedDistinct(@NotNull Field<? extends UNumber> field) {
        return new Sum<>(field, true, YdbTypes.UINT64);
    }

    /**
     * The <code>SUM(DISTINCT field)</code> aggregate function for signed types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Long> sumSignedDistinct(@NotNull Field<? extends Number> field) {
        return new Sum<>(field, true, YdbTypes.INT64);
    }

    /**
     * The <code>SUM(DISTINCT field)</code> aggregate function for INTERVAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Duration> sumIntervalDistinct(@NotNull Field<Duration> field) {
        return new Sum<>(field, true, YdbTypes.INTERVAL);
    }

    /**
     * The <code>SUM(DISTINCT field)</code> aggregate function for DECIMAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    @NotNull
    public static AggregateFunction<BigDecimal> sumDecimalDistinct(@NotNull Field<BigDecimal> field) {
        return new Sum<>(field, true, YdbTypes.DECIMAL);
    }

    /**
     * The <code>AVG</code> aggregate function for DOUBLE.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#avg">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> avgDouble(@NotNull Field<Double> field) {
        return new Avg<>(field, false, YdbTypes.DOUBLE);
    }

    /**
     * The <code>AVG</code> aggregate function for INTERVAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#avg">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Duration> avgInterval(@NotNull Field<Duration> field) {
        return new Avg<>(field, false, YdbTypes.INTERVAL);
    }

    /**
     * The <code>AVG</code> aggregate function for DECIMAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#avg">documentation</a>
     */
    @NotNull
    public static AggregateFunction<BigDecimal> avgDecimal(@NotNull Field<BigDecimal> field) {
        return new Avg<>(field, false, YdbTypes.DECIMAL);
    }

    /**
     * The <code>AVG(DISTINCT field)</code> aggregate function for DOUBLE.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#avg">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> avgDoubleDistinct(@NotNull Field<Double> field) {
        return new Avg<>(field, false, YdbTypes.DOUBLE);
    }

    /**
     * The <code>AVG(DISTINCT field)</code> aggregate function for INTERVAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#avg">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Duration> avgIntervalDistinct(@NotNull Field<Duration> field) {
        return new Avg<>(field, false, YdbTypes.INTERVAL);
    }

    /**
     * The <code>AVG(DISTINCT field)</code> aggregate function for DECIMAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#avg">documentation</a>
     */
    @NotNull
    public static AggregateFunction<BigDecimal> avgDecimalDistinct(@NotNull Field<BigDecimal> field) {
        return new Avg<>(field, false, YdbTypes.DECIMAL);
    }

    /**
     * The <code>COUNT_IF</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#count-if">documentation</a>
     */
    @NotNull
    public static AggregateFunction<ULong> countIf(@NotNull Condition condition) {
        return new CountIf(condition);
    }

    /**
     * The <code>SUM_IF</code> aggregate function for unsigned types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    @NotNull
    public static AggregateFunction<ULong> sumIfUnsigned(@NotNull Field<? extends UNumber> field,
                                                         @NotNull Condition condition) {
        return new SumIf<>(field, condition, false, YdbTypes.UINT64);
    }

    /**
     * The <code>SUM_IF</code> aggregate function for signed types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Long> sumIfSigned(@NotNull Field<? extends Number> field,
                                                      @NotNull Condition condition) {
        return new SumIf<>(field, condition, false, YdbTypes.INT64);
    }

    /**
     * The <code>SUM_IF</code> aggregate function for INTERVAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Duration> sumIfInterval(@NotNull Field<Duration> field,
                                                            @NotNull Condition condition) {
        return new SumIf<>(field, condition, false, YdbTypes.INTERVAL);
    }

    /**
     * The <code>SUM_IF(DISTINCT field)</code> aggregate function for unsigned types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    @NotNull
    public static AggregateFunction<ULong> sumIfUnsignedDistinct(@NotNull Field<? extends UNumber> field,
                                                                 @NotNull Condition condition) {
        return new SumIf<>(field, condition, true, YdbTypes.UINT64);
    }

    /**
     * The <code>SUM_IF(DISTINCT field)</code> aggregate function for signed types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Long> sumIfSignedDistinct(@NotNull Field<? extends Number> field,
                                                              @NotNull Condition condition) {
        return new SumIf<>(field, condition, true, YdbTypes.INT64);
    }

    /**
     * The <code>SUM_IF(DISTINCT field)</code> aggregate function for INTERVAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Duration> sumIfIntervalDistinct(@NotNull Field<Duration> field,
                                                                    @NotNull Condition condition) {
        return new SumIf<>(field, condition, true, YdbTypes.INTERVAL);
    }

    /**
     * The <code>AVG_IF</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> avgIf(@NotNull Field<Double> field,
                                                  @NotNull Condition condition) {
        return new AvgIf(field, condition, false);
    }

    /**
     * The <code>AVG_IF(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> avgIfDistinct(@NotNull Field<Double> field,
                                                          @NotNull Condition condition) {
        return new AvgIf(field, condition, true);
    }

    /**
     * The <code>SOME</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#some">documentation</a>
     */
    @NotNull
    public static <T> AggregateFunction<T> some(@NotNull Field<T> field) {
        return new Some<>(field, false);
    }

    /**
     * The <code>SOME(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#some">documentation</a>
     */
    @NotNull
    public static <T> AggregateFunction<T> someDistinct(@NotNull Field<T> field) {
        return new Some<>(field, true);
    }

    /**
     * The <code>CountDistinctEstimate</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#countdistinctestimate">documentation</a>
     */
    @NotNull
    public static AggregateFunction<ULong> countDistinctEstimate(@NotNull Field<?> field) {
        return new CountDistinctEstimate(field);
    }

    /**
     * The <code>HyperLogLog</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#countdistinctestimate">documentation</a>
     */
    @NotNull
    public static AggregateFunction<ULong> hyperLogLog(@NotNull Field<?> field) {
        return new HyperLogLog(field);
    }

    /**
     * The <code>HLL</code> aggregate function (alias for <code>HyperLogLog</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#countdistinctestimate">documentation</a>
     */
    @NotNull
    public static AggregateFunction<ULong> hll(@NotNull Field<?> field) {
        return new HyperLogLog(field);
    }

    /**
     * The <code>MAX_BY</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#max-min-by">documentation</a>
     */
    @NotNull
    public static <T> AggregateFunction<T> maxBy(@NotNull Field<T> field,
                                                 @NotNull Field<?> cmp) {
        return new MaxBy<>(field, cmp);
    }

    /**
     * The <code>MIN_BY</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#max-min-by">documentation</a>
     */
    @NotNull
    public static <T> AggregateFunction<T> minBy(@NotNull Field<T> field,
                                                 @NotNull Field<?> cmp) {
        return new MinBy<>(field, cmp);
    }

    /**
     * The <code>STDDEV</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> stdDev(@NotNull Field<Double> field) {
        return new StdDev(field, false);
    }

    /**
     * The <code>STDDEV_POPULATION</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> stdDevPopulation(@NotNull Field<Double> field) {
        return new StdDevPopulation(field, false);
    }

    /**
     * The <code>POPULATION_STDDEV</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> populationStdDev(@NotNull Field<Double> field) {
        return new PopulationStdDev(field, false);
    }

    /**
     * The <code>STDDEV_SAMPLE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> stdDevSample(@NotNull Field<Double> field) {
        return new StdDevSample(field, false);
    }

    /**
     * The <code>STDDEVSAMP</code> aggregate function (alias for <code>STDDEV_SAMPLE</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> stdDevSamp(@NotNull Field<Double> field) {
        return new StdDevSample(field, false);
    }

    /**
     * The <code>STDDEV(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> stdDevDistinct(@NotNull Field<Double> field) {
        return new StdDev(field, true);
    }

    /**
     * The <code>STDDEV_POPULATION(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> stdDevPopulationDistinct(@NotNull Field<Double> field) {
        return new StdDevPopulation(field, true);
    }

    /**
     * The <code>POPULATION_STDDEV(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> populationStdDevDistinct(@NotNull Field<Double> field) {
        return new PopulationStdDev(field, true);
    }

    /**
     * The <code>STDDEV_SAMPLE(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> stdDevSampleDistinct(@NotNull Field<Double> field) {
        return new StdDevSample(field, true);
    }

    /**
     * The <code>STDDEVSAMP(DISTINCT field)</code> aggregate function (alias for <code>STDDEV_SAMPLE</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> stdDevSampDistinct(@NotNull Field<Double> field) {
        return new StdDevSample(field, true);
    }

    /**
     * The <code>VARIANCE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> variance(@NotNull Field<Double> field) {
        return new Variance(field, false);
    }

    /**
     * The <code>VARIANCE_POPULATION</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> variancePopulation(@NotNull Field<Double> field) {
        return new VariancePopulation(field, false);
    }

    /**
     * The <code>POPULATION_VARIANCE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> populationVariance(@NotNull Field<Double> field) {
        return new PopulationVariance(field, false);
    }

    /**
     * The <code>VARIANCE_SAMPLE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> varianceSample(@NotNull Field<Double> field) {
        return new VarianceSample(field, false);
    }

    /**
     * The <code>VARPOP</code> aggregate function (alias for <code>VARIANCE_POPULATION</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> varPop(@NotNull Field<Double> field) {
        return new VariancePopulation(field, false);
    }

    /**
     * The <code>VARIANCE(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> varianceDistinct(@NotNull Field<Double> field) {
        return new Variance(field, true);
    }

    /**
     * The <code>VARIANCE_POPULATION(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> variancePopulationDistinct(@NotNull Field<Double> field) {
        return new VariancePopulation(field, true);
    }

    /**
     * The <code>POPULATION_VARIANCE(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> populationVarianceDistinct(@NotNull Field<Double> field) {
        return new PopulationVariance(field, true);
    }

    /**
     * The <code>VARIANCE_SAMPLE(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> varianceSampleDistinct(@NotNull Field<Double> field) {
        return new VarianceSample(field, true);
    }

    /**
     * The <code>VARIANCE_SAMP(DISTINCT field)</code> aggregate function (alias for <code>VARIANCE_POPULATION</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> varianceSampDistinct(@NotNull Field<Double> field) {
        return new VarianceSample(field, true);
    }

    /**
     * The <code>CORRELATION</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#correlation-covariance">documentation</a>
     */
    @NotNull
    public static AggregateFunction<Double> correlation(@NotNull Field<Double> field1,
                                                        @NotNull Field<Double> field2) {
        return new Correlation(field1, field2, false);
    }

    /**
     * The <code>COVARIANCE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#correlation-covariance">documentation</a>
     */
    public static AggregateFunction<Double> covariance(@NotNull Field<Double> field1,
                                                       @NotNull Field<Double> field2) {
        return new Covariance(field1, field2, false);
    }

    /**
     * The <code>COVARIANCE_SAMPLE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#correlation-covariance">documentation</a>
     */
    public static AggregateFunction<Double> covarianceSample(@NotNull Field<Double> field1,
                                                             @NotNull Field<Double> field2) {
        return new CovarianceSample(field1, field2, false);
    }

    /**
     * The <code>COVARIANCE_POPULATION</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#correlation-covariance">documentation</a>
     */
    public static AggregateFunction<Double> covariancePopulation(@NotNull Field<Double> field1,
                                                                 @NotNull Field<Double> field2) {
        return new CovariancePopulation(field1, field2, false);
    }

    /**
     * The <code>CORR</code> aggregate function (alias for <code>CORRELATION</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#correlation-covariance">documentation</a>
     */
    public static AggregateFunction<Double> corr(@NotNull Field<Double> field1,
                                                 @NotNull Field<Double> field2) {
        return new Correlation(field1, field2, false);
    }

    /**
     * The <code>COVAR</code> aggregate function (alias for <code>COVARIANCE</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#correlation-covariance">documentation</a>
     */
    public static AggregateFunction<Double> covar(@NotNull Field<Double> field1,
                                                  @NotNull Field<Double> field2) {
        return new Covariance(field1, field2, false);
    }

    /**
     * The <code>PERCENTILE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> percentile(@NotNull Field<T> field,
                                                      double percent) {
        return percentile(field, val(percent));
    }

    /**
     * The <code>PERCENTILE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> percentile(@NotNull Field<T> field,
                                                      @NotNull Field<Double> percent) {
        return new Percentile<>(field, percent, false);
    }

    /**
     * The <code>PERCENTILE(DISTINCT field, percent)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> percentileDistinct(@NotNull Field<T> field,
                                                              double percent) {
        return percentileDistinct(field, val(percent));
    }

    /**
     * The <code>PERCENTILE(DISTINCT field, percent)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> percentileDistinct(@NotNull Field<T> field,
                                                              @NotNull Field<Double> percent) {
        return new Percentile<>(field, percent, true);
    }

    /**
     * The <code>MEDIAN</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> median(@NotNull Field<T> field) {
        return median(field, null);
    }

    /**
     * The <code>MEDIAN</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> median(@NotNull Field<T> field,
                                                  double percent) {
        return median(field, val(percent));
    }

    /**
     * The <code>MEDIAN</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> median(@NotNull Field<T> field,
                                                  @Nullable Field<Double> percent) {
        return new Median<>(field, percent, false);
    }

    /**
     * The <code>MEDIAN(DISTINCT field, percent)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> medianDistinct(@NotNull Field<T> field) {
        return medianDistinct(field, null);
    }

    /**
     * The <code>MEDIAN(DISTINCT field, percent)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> medianDistinct(@NotNull Field<T> field,
                                                          double percent) {
        return medianDistinct(field, val(percent));
    }

    /**
     * The <code>MEDIAN(DISTINCT field, percent)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> medianDistinct(@NotNull Field<T> field,
                                                          @Nullable Field<Double> percent) {
        return new Median<>(field, percent, true);
    }

    /**
     * The <code>BOOL_AND</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#bool-and-or-xor">documentation</a>
     */
    public static AggregateFunction<Boolean> boolAnd(@NotNull Field<Boolean> field) {
        return new BoolAnd(field);
    }

    /**
     * The <code>BOOL_OR</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#bool-and-or-xor">documentation</a>
     */
    public static AggregateFunction<Boolean> boolOr(@NotNull Field<Boolean> field) {
        return new BoolOr(field);
    }

    /**
     * The <code>BOOL_XOR</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#bool-and-or-xor">documentation</a>
     */
    public static AggregateFunction<Boolean> boolXor(@NotNull Field<Boolean> field) {
        return new BoolXor(field);
    }

    /**
     * The <code>BIT_AND</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#bit-and-or-xor">documentation</a>
     */
    public static <T extends UNumber> AggregateFunction<T> bitAnd(@NotNull Field<T> field) {
        return new BitAnd<>(field);
    }

    /**
     * The <code>BIT_OR</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#bit-and-or-xor">documentation</a>
     */
    public static <T extends UNumber> AggregateFunction<T> bitOr(@NotNull Field<T> field) {
        return new BitOr<>(field);
    }

    /**
     * The <code>BIT_XOR</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#bit-and-or-xor">documentation</a>
     */
    public static <T extends UNumber> AggregateFunction<T> bitXor(@NotNull Field<T> field) {
        return new BitXor<>(field);
    }
}
