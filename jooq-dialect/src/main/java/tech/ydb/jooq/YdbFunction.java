package tech.ydb.jooq;

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
    public static <T> Field<T> coalesce(Field<T> field, T value) {
        return coalesce(field, DSL.val(value, field.getDataType()));
    }

    /**
     * The <code>COALESCE</code> function for multiple fields.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#coalesce">documentation</a>
     */
    @SafeVarargs
    public static <T> Field<T> coalesce(Field<T> field, Field<T>... fields) {
        return new Coalesce<>(combineTyped(field, fields));
    }


    /**
     * The <code>LENGTH</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#length">documentation</a>
     */
    public static Field<UInteger> length(Field<?> value) {
        return new Length(value);
    }

    /**
     * The <code>LEN</code> function (alias for <code>Length</code>)
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#length">documentation</a>
     */
    public static Field<UInteger> len(Field<?> value) {
        return length(value);
    }


    /**
     * The <code>SUBSTRING</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#substring">documentation</a>
     */
    public static Field<byte[]> substring(Field<byte[]> source, int startingPosition) {
        return substring(source, UInteger.valueOf(startingPosition));
    }

    /**
     * The <code>SUBSTRING</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#substring">documentation</a>
     */
    public static Field<byte[]> substring(Field<byte[]> source, int startingPosition, int length) {
        return substring(source, UInteger.valueOf(startingPosition), UInteger.valueOf(length));
    }

    /**
     * The <code>SUBSTRING</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#substring">documentation</a>
     */
    public static Field<byte[]> substring(Field<byte[]> source,
                                          UInteger startingPosition) {
        return substring(source, val(startingPosition), null);
    }

    /**
     * The <code>SUBSTRING</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#substring">documentation</a>
     */
    public static Field<byte[]> substring(Field<byte[]> source,
                                          UInteger startingPosition,
                                          UInteger length) {
        return substring(source, val(startingPosition), val(length));
    }

    /**
     * The <code>SUBSTRING</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#substring">documentation</a>
     */
    public static Field<byte[]> substring(Field<byte[]> source,
                                          Field<UInteger> startingPosition,
                                          Field<UInteger> length) {
        return new Substring(source, startingPosition, length);
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    public static Field<UInteger> find(Field<byte[]> source,
                                       byte[] substring) {
        return find(source, val(substring));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    public static Field<UInteger> find(Field<byte[]> source,
                                       byte[] substring,
                                       int startingPosition) {
        return find(source, substring, UInteger.valueOf(startingPosition));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    public static Field<UInteger> find(Field<byte[]> source,
                                       byte[] substring,
                                       UInteger startingPosition) {
        return find(source, val(substring), val(startingPosition));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    public static Field<UInteger> find(Field<String> source,
                                       String substring) {
        return findUtf8(source, val(substring));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    public static Field<UInteger> find(Field<String> source,
                                       String substring,
                                       int startingPosition) {
        return find(source, substring, UInteger.valueOf(startingPosition));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    public static Field<UInteger> find(Field<String> source,
                                       String substring,
                                       UInteger startingPosition) {
        return findUtf8(source, val(substring), val(startingPosition));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    public static Field<UInteger> find(Field<byte[]> source,
                                       Field<byte[]> substring) {
        return find(source, substring, (Field<UInteger>) null);
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    public static Field<UInteger> findUtf8(Field<String> source,
                                           Field<String> substring) {
        return findUtf8(source, substring, (Field<UInteger>) null);
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    public static Field<UInteger> find(Field<byte[]> source,
                                       Field<byte[]> substring,
                                       UInteger startingPosition) {
        return find(source, substring, val(startingPosition));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    public static Field<UInteger> findUtf8(Field<String> source,
                                           Field<String> substring,
                                           UInteger startingPosition) {
        return findUtf8(source, substring, val(startingPosition));
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    public static Field<UInteger> find(Field<byte[]> source,
                                       Field<byte[]> substring,
                                       Field<UInteger> startingPosition) {
        return new Find<>(source, substring, startingPosition);
    }

    /**
     * The <code>FIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#find">documentation</a>
     */
    public static Field<UInteger> findUtf8(Field<String> source,
                                           Field<String> substring,
                                           Field<UInteger> startingPosition) {
        return new Find<>(source, substring, startingPosition);
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    public static Field<UInteger> rFind(Field<byte[]> source,
                                        byte[] substring) {
        return rFind(source, val(substring));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    public static Field<UInteger> rFind(Field<byte[]> source,
                                        byte[] substring,
                                        int startingPosition) {
        return rFind(source, substring, UInteger.valueOf(startingPosition));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    public static Field<UInteger> rFind(Field<byte[]> source,
                                        byte[] substring,
                                        UInteger startingPosition) {
        return rFind(source, val(substring), val(startingPosition));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    public static Field<UInteger> rFind(Field<String> source,
                                        String substring) {
        return rFindUtf8(source, val(substring));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    public static Field<UInteger> rFind(Field<String> source,
                                        String substring,
                                        int startingPosition) {
        return rFind(source, substring, UInteger.valueOf(startingPosition));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    public static Field<UInteger> rFind(Field<String> source,
                                        String substring,
                                        UInteger startingPosition) {
        return rFindUtf8(source, val(substring), val(startingPosition));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    public static Field<UInteger> rFind(Field<byte[]> source,
                                        Field<byte[]> substring) {
        return rFind(source, substring, (Field<UInteger>) null);
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    public static Field<UInteger> rFindUtf8(Field<String> source,
                                            Field<String> substring) {
        return rFindUtf8(source, substring, (Field<UInteger>) null);
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    public static Field<UInteger> rFind(Field<byte[]> source,
                                        Field<byte[]> substring,
                                        UInteger startingPosition) {
        return rFind(source, substring, val(startingPosition));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    public static Field<UInteger> rFindUtf8(Field<String> source,
                                            Field<String> substring,
                                            UInteger startingPosition) {
        return rFindUtf8(source, substring, val(startingPosition));
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    public static Field<UInteger> rFind(Field<byte[]> source,
                                        Field<byte[]> substring,
                                        Field<UInteger> startingPosition) {
        return new RFind<>(source, substring, startingPosition);
    }

    /**
     * The <code>RFIND</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#rfind">documentation</a>
     */
    public static Field<UInteger> rFindUtf8(Field<String> source,
                                            Field<String> substring,
                                            Field<UInteger> startingPosition) {
        return new RFind<>(source, substring, startingPosition);
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition startsWith(Field<byte[]> source,
                                       byte[] substring) {
        return startsWith(source, val(substring));
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition startsWithUtf8(Field<String> source,
                                           byte[] substring) {
        return startsWith(source, val(substring));
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition startsWith(Field<byte[]> source,
                                       String substring) {
        return startsWith(source, val(substring));
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition startsWithUtf8(Field<String> source,
                                           String substring) {
        return startsWith(source, val(substring));
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition startsWith(byte[] source,
                                       Field<byte[]> substring) {
        return startsWith(val(source), substring);
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition startsWithUtf8(byte[] source,
                                           Field<String> substring) {
        return startsWith(val(source), substring);
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition startsWith(String source,
                                       Field<byte[]> substring) {
        return startsWith(val(source), substring);
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition startsWithUtf8(String source,
                                           Field<String> substring) {
        return startsWith(val(source), substring);
    }

    /**
     * The <code>StarsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition startsWith(Field<?> source,
                                       Field<?> substring) {
        return new StartsWith(source, substring);
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition endsWith(Field<byte[]> source,
                                     byte[] substring) {
        return endsWith(source, val(substring));
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition endsWithUtf8(Field<String> source,
                                         byte[] substring) {
        return endsWith(source, val(substring));
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition endsWith(Field<byte[]> source,
                                     String substring) {
        return endsWith(source, val(substring));
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition endsWithUtf8(Field<String> source,
                                         String substring) {
        return endsWith(source, val(substring));
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition endsWith(byte[] source,
                                     Field<byte[]> substring) {
        return endsWith(val(source), substring);
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition endsWithUtf8(byte[] source,
                                         Field<String> substring) {
        return endsWith(val(source), substring);
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition endsWith(String source,
                                     Field<byte[]> substring) {
        return endsWith(val(source), substring);
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition endsWithUtf8(String source,
                                         Field<String> substring) {
        return endsWith(val(source), substring);
    }

    /**
     * The <code>EndsWith</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#starts_ends_with">documentation</a>
     */
    public static Condition endsWith(Field<?> source,
                                     Field<?> substring) {
        return new EndsWith(source, substring);
    }

    /**
     * The <code>IF</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#if">documentation</a>
     */
    public static <T> Field<T> if_(Condition condition,
                                   T ifTrue) {
        return if_(condition, (Field<T>) val(ifTrue), (Field<T>) null);
    }

    /**
     * The <code>IF</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#if">documentation</a>
     */
    public static <T> Field<T> if_(Condition condition,
                                   Field<T> ifTrue) {
        return if_(condition, ifTrue, (Field<T>) null);
    }

    /**
     * The <code>IF</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#if">documentation</a>
     */
    public static <T> Field<T> if_(Condition condition,
                                   T ifTrue,
                                   T ifFalse) {
        return if_(condition, val(ifTrue), ifFalse);
    }

    /**
     * The <code>IF</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#if">documentation</a>
     */
    public static <T> Field<T> if_(Condition condition,
                                   T ifTrue,
                                   Field<T> ifFalse) {
        return if_(condition, DSL.val(ifTrue, ifFalse.getDataType()), ifFalse);
    }

    /**
     * The <code>IF</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#if">documentation</a>
     */
    public static <T> Field<T> if_(Condition condition,
                                   Field<T> ifTrue,
                                   T ifFalse) {
        return if_(condition, ifTrue, DSL.val(ifFalse, ifTrue.getDataType()));
    }

    /**
     * The <code>IF</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#if">documentation</a>
     */
    public static <T> Field<T> if_(Condition condition,
                                   Field<T> ifTrue,
                                   Field<T> ifFalse) {
        return new If<>(condition, ifTrue, ifFalse);
    }

    /**
     * The <code>NANVL</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#nanvl">documentation</a>
     */
    public static Field<Float> nanvl(Field<Float> expression,
                                     Float replacement) {
        return nanvl(expression, val(replacement));
    }

    /**
     * The <code>NANVL</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#nanvl">documentation</a>
     */
    public static Field<Double> nanvl(Field<Double> expression,
                                      Double replacement) {
        return nanvl(expression, val(replacement));
    }

    /**
     * The <code>NANVL</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#nanvl">documentation</a>
     */
    public static <T> Field<T> nanvl(Field<T> condition,
                                     Field<T> replacement) {
        return new NaNvl<>(condition, replacement);
    }

    /**
     * The <code>Random</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    public static Field<Double> random(Object value,
                                       Object... values) {
        return new Random(combine(val(value), fieldsArray(values)));
    }

    /**
     * The <code>Random</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    public static Field<Double> random(Field<?> field,
                                       Object value) {
        return new Random(new Field[]{field, val(value)});
    }

    /**
     * The <code>Random</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    public static Field<Double> random(Field<?> field,
                                       Field<?>... fields) {
        return new Random(combine(field, fields));
    }

    /**
     * The <code>RandomNumber</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    public static Field<ULong> randomNumber(Object value,
                                            Object... values) {
        return new RandomNumber(combine(val(value), fieldsArray(values)));
    }

    /**
     * The <code>RandomNumber</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    public static Field<ULong> randomNumber(Field<?> field,
                                            Object value) {
        return new RandomNumber(new Field[]{field, val(value)});
    }

    /**
     * The <code>RandomNumber</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    public static Field<ULong> randomNumber(Field<?> field,
                                            Field<?>... fields) {
        return new RandomNumber(combine(field, fields));
    }

    /**
     * The <code>RandomUuid</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    public static Field<UUID> randomUuid(Object value,
                                         Object... values) {
        return new RandomUuid(combine(val(value), fieldsArray(values)));
    }

    /**
     * The <code>RandomUuid</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    public static Field<UUID> randomUuid(Field<?> field,
                                         Object value) {
        return new RandomUuid(new Field[]{field, val(value)});
    }

    /**
     * The <code>RandomUuid</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#random">documentation</a>
     */
    public static Field<UUID> randomUuid(Field<?> field,
                                         Field<?>... fields) {
        return new RandomUuid(combine(field, fields));
    }

    /**
     * The <code>CurrentUtcDate</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-utc">documentation</a>
     */
    public static Field<LocalDate> currentUtcDate(Object... values) {
        return currentUtcDate(fieldsArray(values));
    }

    /**
     * The <code>CurrentUtcDate</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-utc">documentation</a>
     */
    public static Field<LocalDate> currentUtcDate(Field<?>... fields) {
        return new CurrentUtcDate(fields);
    }

    /**
     * The <code>CurrentUtcDatetime</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-utc">documentation</a>
     */
    public static Field<LocalDateTime> currentUtcDatetime(Object... values) {
        return currentUtcDatetime(fieldsArray(values));
    }

    /**
     * The <code>CurrentUtcDatetime</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-utc">documentation</a>
     */
    public static Field<LocalDateTime> currentUtcDatetime(Field<?>... fields) {
        return new CurrentUtcDatetime(fields);
    }

    /**
     * The <code>CurrentUtcTimestamp</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-utc">documentation</a>
     */
    public static Field<Instant> currentUtcTimestamp(Object... values) {
        return currentUtcTimestamp(fieldsArray(values));
    }

    /**
     * The <code>CurrentUtcTimestamp</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-utc">documentation</a>
     */
    public static Field<Instant> currentUtcTimestamp(Field<?>... fields) {
        return new CurrentUtcTimestamp(fields);
    }

    /**
     * The <code>CurrentTzDate</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    public static Field<ZonedDateTime> currentTzDate(ZoneId timeZone,
                                                     Object... values) {
        return currentTzDate(val(timeZone.toString().getBytes()), fieldsArray(values));
    }

    /**
     * The <code>CurrentTzDate</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    public static Field<ZonedDateTime> currentTzDate(String timeZone,
                                                     Object... values) {
        return currentTzDate(val(timeZone.getBytes()), fieldsArray(values));
    }

    /**
     * The <code>CurrentTzDate</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    public static Field<ZonedDateTime> currentTzDate(Field<byte[]> timeZone,
                                                     Field<?>... fields) {
        return new CurrentTzDate(timeZone, fields);
    }

    /**
     * The <code>CurrentTzDatetime</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    public static Field<ZonedDateTime> currentTzDatetime(ZoneId timeZone,
                                                         Object... values) {
        return currentTzDatetime(val(timeZone.toString().getBytes()), fieldsArray(values));
    }

    /**
     * The <code>CurrentTzDatetime</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    public static Field<ZonedDateTime> currentTzDatetime(String timeZone,
                                                         Object... values) {
        return currentTzDatetime(val(timeZone.getBytes()), fieldsArray(values));
    }

    /**
     * The <code>CurrentTzDatetime</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    public static Field<ZonedDateTime> currentTzDatetime(Field<byte[]> timeZone,
                                                         Field<?>... fields) {
        return new CurrentTzDatetime(timeZone, fields);
    }

    /**
     * The <code>CurrentTzTimestamp</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    public static Field<ZonedDateTime> currentTzTimestamp(ZoneId timeZone,
                                                          Object... values) {
        return currentTzTimestamp(timeZone.toString(), values);
    }

    /**
     * The <code>CurrentTzTimestamp</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    public static Field<ZonedDateTime> currentTzTimestamp(String timeZone,
                                                          Object... values) {
        return currentTzTimestamp(val(timeZone.getBytes()), fieldsArray(values));
    }

    /**
     * The <code>CurrentTzTimestamp</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#current-tz">documentation</a>
     */
    public static Field<ZonedDateTime> currentTzTimestamp(Field<byte[]> timeZone,
                                                          Field<?>... fields) {
        return new CurrentTzTimestamp(timeZone, fields);
    }

    /**
     * The <code>AddTimezone</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#addtimezone">documentation</a>
     */
    public static Field<ZonedDateTime> addTimezone(LocalDate date,
                                                   ZoneId timeZone) {
        return addTimezone(val(date), timeZone);
    }

    /**
     * The <code>AddTimezone</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#addtimezone">documentation</a>
     */
    public static Field<ZonedDateTime> addTimezone(LocalDateTime date,
                                                   ZoneId timeZone) {
        return addTimezone(val(date), timeZone);
    }

    /**
     * The <code>AddTimezone</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#addtimezone">documentation</a>
     */
    public static Field<ZonedDateTime> addTimezone(Instant date,
                                                   ZoneId timeZone) {
        return addTimezone(val(date), timeZone);
    }

    /**
     * The <code>AddTimezone</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#addtimezone">documentation</a>
     */
    public static Field<ZonedDateTime> addTimezone(Field<?> date,
                                                   ZoneId timeZone) {
        return addTimezone(date, val(timeZone.toString().getBytes()));
    }

    /**
     * The <code>AddTimezone</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#addtimezone">documentation</a>
     */
    public static Field<ZonedDateTime> addTimezone(Field<?> date,
                                                   Field<byte[]> timeZone) {
        return new AddTimezone(date, timeZone);
    }

    /**
     * The <code>RemoveTimezone</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#removetimezone">documentation</a>
     */
    public static <T> Field<T> removeTimezone(Field<ZonedDateTime> date,
                                              DataType<T> type) {
        return new RemoveTimezone<>(date, type);
    }

    /**
     * The <code>MaxOf</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    public static <T> Field<T> maxOf(Field<T> field, T value) {
        return maxOf(field, DSL.val(value, field.getDataType()));
    }

    /**
     * The <code>MaxOf</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    @SafeVarargs
    public static <T> Field<T> maxOf(Field<T> field, Field<T>... fields) {
        return new MaxOf<>(combineTyped(field, fields));
    }

    /**
     * The <code>MinOf</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    public static <T> Field<T> minOf(Field<T> field, T value) {
        return minOf(field, DSL.val(value, field.getDataType()));
    }

    /**
     * The <code>MinOf</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    @SafeVarargs
    public static <T> Field<T> minOf(Field<T> field, Field<T>... fields) {
        return new MinOf<>(combineTyped(field, fields));
    }

    /**
     * The <code>Greatest</code> function (alias for <code>MaxOf</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    public static <T> Field<T> greatest(Field<T> field, T value) {
        return maxOf(field, value);
    }

    /**
     * The <code>Greatest</code> function (alias for <code>MaxOf</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    @SafeVarargs
    public static <T> Field<T> greatest(Field<T> field, Field<T>... fields) {
        return maxOf(field, fields);
    }

    /**
     * The <code>Least</code> function (alias for <code>MinOf</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    public static <T> Field<T> least(Field<T> field, T value) {
        return minOf(field, value);
    }

    /**
     * The <code>Least</code> function (alias for <code>MinOf</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#max-min">documentation</a>
     */
    @SafeVarargs
    public static <T> Field<T> least(Field<T> field, Field<T>... fields) {
        return minOf(field, fields);
    }

    /**
     * The <code>TableRow</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#tablerow">documentation</a>
     */
    public static Field<?> tableRow() {
        return new TableRow();
    }

    /**
     * The <code>JoinTableRow</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#tablerow">documentation</a>
     */
    public static Field<?> joinTableRow() {
        return new JoinTableRow();
    }

    /**
     * The <code>Ensure</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#ensure">documentation</a>
     */
    public static <T> Field<T> ensure(Field<T> value,
                                      Condition condition) {
        return ensure(value, condition, (Field<byte[]>) null);
    }

    /**
     * The <code>Ensure</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#ensure">documentation</a>
     */
    public static <T> Field<T> ensure(Field<T> value,
                                      Condition condition,
                                      String message) {
        return ensure(value, condition, message != null ? val(message.getBytes()) : null);
    }

    /**
     * The <code>Ensure</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#ensure">documentation</a>
     */
    public static <T> Field<T> ensure(Field<T> value,
                                      Condition condition,
                                      Field<byte[]> message) {
        return new Ensure<>(value, condition, message);
    }

    /**
     * The <code>AssumeStrict</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#assumestrict">documentation</a>
     */
    public static <T> Field<T> assumeStrict(Field<T> value) {
        return new AssumeStrict<>(value);
    }

    /**
     * The <code>Likely</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/basic#likely">documentation</a>
     */
    public static Condition likely(Condition condition) {
        return new Likely(condition);
    }

    /**
     * The <code>ToBytes</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#to-from-bytes">documentation</a>
     */
    public static Field<byte[]> toBytes(Field<?> value) {
        return new ToBytes(value);
    }

    /**
     * The <code>FromBytes</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#to-from-bytes">documentation</a>
     */
    public static <T> Field<T> fromBytes(Field<byte[]> bytes,
                                         DataType<T> type) {
        return new FromBytes<>(bytes, type);
    }

    /**
     * The <code>ByteAt</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#byteat">documentation</a>
     */
    public static Field<UByte> byteAt(Field<byte[]> source,
                                      int index) {
        return byteAt(source, UInteger.valueOf(index));
    }

    /**
     * The <code>ByteAt</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#byteat">documentation</a>
     */
    public static Field<UByte> byteAt(Field<byte[]> source,
                                      UInteger index) {
        return byteAt(source, val(index));
    }

    /**
     * The <code>ByteAt</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#byteat">documentation</a>
     */
    public static Field<UByte> byteAt(Field<byte[]> source,
                                      Field<UInteger> index) {
        return new ByteAt(source, index);
    }

    /**
     * The <code>ByteAt</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#byteat">documentation</a>
     */
    public static Field<UByte> byteAtUtf8(Field<String> source,
                                          int index) {
        return byteAtUtf8(source, UInteger.valueOf(index));
    }

    /**
     * The <code>ByteAt</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#byteat">documentation</a>
     */
    public static Field<UByte> byteAtUtf8(Field<String> source,
                                          UInteger index) {
        return byteAtUtf8(source, val(index));
    }

    /**
     * The <code>ByteAt</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#byteat">documentation</a>
     */
    public static Field<UByte> byteAtUtf8(Field<String> source,
                                          Field<UInteger> index) {
        return new ByteAt(source, index);
    }

    /**
     * The <code>TestBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    public static Condition testBit(Field<? extends UNumber> source,
                                    int index) {
        return testBit(source, UByte.valueOf(index));
    }

    /**
     * The <code>TestBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    public static Condition testBit(Field<? extends UNumber> source,
                                    UByte index) {
        return testBit(source, val(index));
    }

    /**
     * The <code>TestBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    public static Condition testBit(Field<? extends UNumber> source,
                                    Field<UByte> index) {
        return new TestBit(source, index);
    }

    /**
     * The <code>ClearBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    public static <T extends UNumber> Field<T> clearBit(Field<T> source,
                                                        int index) {
        return clearBit(source, UByte.valueOf(index));
    }

    /**
     * The <code>ClearBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    public static <T extends UNumber> Field<T> clearBit(Field<T> source,
                                                        UByte index) {
        return clearBit(source, val(index));
    }

    public static <T extends UNumber> Field<T> clearBit(Field<T> source,
                                                        Field<UByte> index) {
        return new ClearBit<>(source, index);
    }

    /**
     * The <code>SetBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    public static <T extends UNumber> Field<T> setBit(Field<T> source,
                                                      int index) {
        return setBit(source, UByte.valueOf(index));
    }

    /**
     * The <code>SetBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    public static <T extends UNumber> Field<T> setBit(Field<T> source,
                                                      UByte index) {
        return setBit(source, val(index));
    }

    /**
     * The <code>SetBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    public static <T extends UNumber> Field<T> setBit(Field<T> source,
                                                      Field<UByte> index) {
        return new SetBit<>(source, index);
    }

    /**
     * The <code>FlipBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    public static <T extends UNumber> Field<T> flipBit(Field<T> source,
                                                       int index) {
        return flipBit(source, UByte.valueOf(index));
    }

    /**
     * The <code>FlipBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    public static <T extends UNumber> Field<T> flipBit(Field<T> source,
                                                       UByte index) {
        return flipBit(source, val(index));
    }

    /**
     * The <code>FlipBit</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#bitops">documentation</a>
     */
    public static <T extends UNumber> Field<T> flipBit(Field<T> source,
                                                       Field<UByte> index) {
        return new FlipBit<>(source, index);
    }

    /**
     * The <code>Abs</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#abs">documentation</a>
     */
    public static <T extends Number> Field<T> abs(Field<T> value) {
        return new Abs<>(value);
    }

    /**
     * The <code>Just</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#optional-ops">documentation</a>
     */
    public static <T> Field<T> just(Field<T> value) {
        return new Just<>(value);
    }

    /**
     * The <code>Unwrap</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#optional-ops">documentation</a>
     */
    public static <T> Field<T> unwrap(Field<T> value) {
        return unwrap(value, (Field<byte[]>) null);
    }

    /**
     * The <code>Unwrap</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#optional-ops">documentation</a>
     */
    public static <T> Field<T> unwrap(Field<T> value, String message) {
        return unwrap(value, val(message.getBytes()));
    }

    /**
     * The <code>Unwrap</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#optional-ops">documentation</a>
     */
    public static <T> Field<T> unwrap(Field<T> value,
                                      Field<byte[]> message) {
        return new Unwrap<>(value, message);
    }

    /**
     * The <code>Nothing</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#optional-ops">documentation</a>
     */
    public static <T> Field<T> nothing(DataType<T> type) {
        return new Nothing<>(type);
    }

    /**
     * The <code>Pickle</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#pickle">documentation</a>
     */
    public static Field<byte[]> pickle(Field<?> value) {
        return new Pickle(value);
    }

    /**
     * The <code>StablePickle</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#pickle">documentation</a>
     */
    public static Field<byte[]> stablePickle(Field<?> value) {
        return new StablePickle(value);
    }

    /**
     * The <code>Unpickle</code> function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/basic#pickle">documentation</a>
     */
    public static <T> Field<T> unpickle(DataType<T> type,
                                        Field<byte[]> bytes) {
        return new Unpickle<>(type, bytes);
    }

    /**
     * The <code>COUNT</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#count">documentation</a>
     */
    public static AggregateFunction<ULong> count(Field<?> field) {
        return new Count(field, false);
    }

    /**
     * The <code>COUNT</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#count">documentation</a>
     */
    public static AggregateFunction<ULong> count(SelectFieldOrAsterisk field) {
        return new Count(DSL.field("{0}", field), false);
    }

    /**
     * The <code>COUNT(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#count">documentation</a>
     */
    public static AggregateFunction<ULong> countDistinct(Field<?> field) {
        return new Count(field, true);
    }

    /**
     * The <code>MIN</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#min-max">documentation</a>
     */
    public static <T> AggregateFunction<T> min(Field<T> field) {
        return new Min<>(field, false);
    }

    /**
     * The <code>MIN(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#min-max">documentation</a>
     */
    public static <T> AggregateFunction<T> minDistinct(Field<T> field) {
        return new Min<>(field, true);
    }

    /**
     * The <code>MAX</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#min-max">documentation</a>
     */
    public static <T> AggregateFunction<T> max(Field<T> field) {
        return new Max<>(field, false);
    }

    /**
     * The <code>MAX(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#min-max">documentation</a>
     */
    public static <T> AggregateFunction<T> maxDistinct(Field<T> field) {
        return new Max<>(field, true);
    }

    /**
     * The <code>SUM</code> aggregate function for unsigned types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    public static AggregateFunction<ULong> sumUnsigned(Field<? extends UNumber> field) {
        return new Sum<>(field, false, YdbTypes.UINT64);
    }

    /**
     * The <code>SUM</code> aggregate function for signed types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    public static AggregateFunction<Long> sumSigned(Field<? extends Number> field) {
        return new Sum<>(field, false, YdbTypes.INT64);
    }

    /**
     * The <code>SUM</code> aggregate function for INTERVAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    public static AggregateFunction<Duration> sumInterval(Field<Duration> field) {
        return new Sum<>(field, false, YdbTypes.INTERVAL);
    }

    /**
     * The <code>SUM</code> aggregate function for DECIMAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    public static AggregateFunction<BigDecimal> sumDecimal(Field<BigDecimal> field) {
        return new Sum<>(field, false, YdbTypes.DECIMAL);
    }

    /**
     * The <code>SUM(DISTINCT field)</code> aggregate function for unsigned types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    public static AggregateFunction<ULong> sumUnsignedDistinct(Field<? extends UNumber> field) {
        return new Sum<>(field, true, YdbTypes.UINT64);
    }

    /**
     * The <code>SUM(DISTINCT field)</code> aggregate function for signed types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    public static AggregateFunction<Long> sumSignedDistinct(Field<? extends Number> field) {
        return new Sum<>(field, true, YdbTypes.INT64);
    }

    /**
     * The <code>SUM(DISTINCT field)</code> aggregate function for INTERVAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    public static AggregateFunction<Duration> sumIntervalDistinct(Field<Duration> field) {
        return new Sum<>(field, true, YdbTypes.INTERVAL);
    }

    /**
     * The <code>SUM(DISTINCT field)</code> aggregate function for DECIMAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum">documentation</a>
     */
    public static AggregateFunction<BigDecimal> sumDecimalDistinct(Field<BigDecimal> field) {
        return new Sum<>(field, true, YdbTypes.DECIMAL);
    }

    /**
     * The <code>AVG</code> aggregate function for DOUBLE.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#avg">documentation</a>
     */
    public static AggregateFunction<Double> avgDouble(Field<Double> field) {
        return new Avg<>(field, false, YdbTypes.DOUBLE);
    }

    /**
     * The <code>AVG</code> aggregate function for INTERVAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#avg">documentation</a>
     */
    public static AggregateFunction<Duration> avgInterval(Field<Duration> field) {
        return new Avg<>(field, false, YdbTypes.INTERVAL);
    }

    /**
     * The <code>AVG</code> aggregate function for DECIMAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#avg">documentation</a>
     */
    public static AggregateFunction<BigDecimal> avgDecimal(Field<BigDecimal> field) {
        return new Avg<>(field, false, YdbTypes.DECIMAL);
    }

    /**
     * The <code>AVG(DISTINCT field)</code> aggregate function for DOUBLE.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#avg">documentation</a>
     */
    public static AggregateFunction<Double> avgDoubleDistinct(Field<Double> field) {
        return new Avg<>(field, false, YdbTypes.DOUBLE);
    }

    /**
     * The <code>AVG(DISTINCT field)</code> aggregate function for INTERVAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#avg">documentation</a>
     */
    public static AggregateFunction<Duration> avgIntervalDistinct(Field<Duration> field) {
        return new Avg<>(field, false, YdbTypes.INTERVAL);
    }

    /**
     * The <code>AVG(DISTINCT field)</code> aggregate function for DECIMAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#avg">documentation</a>
     */
    public static AggregateFunction<BigDecimal> avgDecimalDistinct(Field<BigDecimal> field) {
        return new Avg<>(field, false, YdbTypes.DECIMAL);
    }

    /**
     * The <code>COUNT_IF</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#count-if">documentation</a>
     */
    public static AggregateFunction<ULong> countIf(Condition condition) {
        return new CountIf(condition);
    }

    /**
     * The <code>SUM_IF</code> aggregate function for unsigned types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    public static AggregateFunction<ULong> sumIfUnsigned(Field<? extends UNumber> field,
                                                         Condition condition) {
        return new SumIf<>(field, condition, false, YdbTypes.UINT64);
    }

    /**
     * The <code>SUM_IF</code> aggregate function for signed types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    public static AggregateFunction<Long> sumIfSigned(Field<? extends Number> field,
                                                      Condition condition) {
        return new SumIf<>(field, condition, false, YdbTypes.INT64);
    }

    /**
     * The <code>SUM_IF</code> aggregate function for INTERVAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    public static AggregateFunction<Duration> sumIfInterval(Field<Duration> field,
                                                            Condition condition) {
        return new SumIf<>(field, condition, false, YdbTypes.INTERVAL);
    }

    /**
     * The <code>SUM_IF(DISTINCT field)</code> aggregate function for unsigned types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    public static AggregateFunction<ULong> sumIfUnsignedDistinct(Field<? extends UNumber> field,
                                                                 Condition condition) {
        return new SumIf<>(field, condition, true, YdbTypes.UINT64);
    }

    /**
     * The <code>SUM_IF(DISTINCT field)</code> aggregate function for signed types.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    public static AggregateFunction<Long> sumIfSignedDistinct(Field<? extends Number> field,
                                                              Condition condition) {
        return new SumIf<>(field, condition, true, YdbTypes.INT64);
    }

    /**
     * The <code>SUM_IF(DISTINCT field)</code> aggregate function for INTERVAL.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    public static AggregateFunction<Duration> sumIfIntervalDistinct(Field<Duration> field,
                                                                    Condition condition) {
        return new SumIf<>(field, condition, true, YdbTypes.INTERVAL);
    }

    /**
     * The <code>AVG_IF</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    public static AggregateFunction<Double> avgIf(Field<Double> field,
                                                  Condition condition) {
        return new AvgIf(field, condition, false);
    }

    /**
     * The <code>AVG_IF(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#sum-if">documentation</a>
     */
    public static AggregateFunction<Double> avgIfDistinct(Field<Double> field,
                                                          Condition condition) {
        return new AvgIf(field, condition, true);
    }

    /**
     * The <code>SOME</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#some">documentation</a>
     */
    public static <T> AggregateFunction<T> some(Field<T> field) {
        return new Some<>(field, false);
    }

    /**
     * The <code>SOME(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#some">documentation</a>
     */
    public static <T> AggregateFunction<T> someDistinct(Field<T> field) {
        return new Some<>(field, true);
    }

    /**
     * The <code>CountDistinctEstimate</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#countdistinctestimate">documentation</a>
     */
    public static AggregateFunction<ULong> countDistinctEstimate(Field<?> field) {
        return new CountDistinctEstimate(field);
    }

    /**
     * The <code>HyperLogLog</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#countdistinctestimate">documentation</a>
     */
    public static AggregateFunction<ULong> hyperLogLog(Field<?> field) {
        return new HyperLogLog(field);
    }

    /**
     * The <code>HLL</code> aggregate function (alias for <code>HyperLogLog</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/en/yql/reference/builtins/aggregation#countdistinctestimate">documentation</a>
     */
    public static AggregateFunction<ULong> hll(Field<?> field) {
        return new HyperLogLog(field);
    }

    /**
     * The <code>MAX_BY</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#max-min-by">documentation</a>
     */
    public static <T> AggregateFunction<T> maxBy(Field<T> field,
                                                 Field<?> cmp) {
        return new MaxBy<>(field, cmp);
    }

    /**
     * The <code>MIN_BY</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#max-min-by">documentation</a>
     */
    public static <T> AggregateFunction<T> minBy(Field<T> field,
                                                 Field<?> cmp) {
        return new MinBy<>(field, cmp);
    }

    /**
     * The <code>STDDEV</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> stdDev(Field<Double> field) {
        return new StdDev(field, false);
    }

    /**
     * The <code>STDDEV_POPULATION</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> stdDevPopulation(Field<Double> field) {
        return new StdDevPopulation(field, false);
    }

    /**
     * The <code>POPULATION_STDDEV</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> populationStdDev(Field<Double> field) {
        return new PopulationStdDev(field, false);
    }

    /**
     * The <code>STDDEV_SAMPLE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> stdDevSample(Field<Double> field) {
        return new StdDevSample(field, false);
    }

    /**
     * The <code>STDDEVSAMP</code> aggregate function (alias for <code>STDDEV_SAMPLE</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> stdDevSamp(Field<Double> field) {
        return new StdDevSample(field, false);
    }

    /**
     * The <code>STDDEV(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> stdDevDistinct(Field<Double> field) {
        return new StdDev(field, true);
    }

    /**
     * The <code>STDDEV_POPULATION(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> stdDevPopulationDistinct(Field<Double> field) {
        return new StdDevPopulation(field, true);
    }

    /**
     * The <code>POPULATION_STDDEV(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> populationStdDevDistinct(Field<Double> field) {
        return new PopulationStdDev(field, true);
    }

    /**
     * The <code>STDDEV_SAMPLE(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> stdDevSampleDistinct(Field<Double> field) {
        return new StdDevSample(field, true);
    }

    /**
     * The <code>STDDEVSAMP(DISTINCT field)</code> aggregate function (alias for <code>STDDEV_SAMPLE</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> stdDevSampDistinct(Field<Double> field) {
        return new StdDevSample(field, true);
    }

    /**
     * The <code>VARIANCE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> variance(Field<Double> field) {
        return new Variance(field, false);
    }

    /**
     * The <code>VARIANCE_POPULATION</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> variancePopulation(Field<Double> field) {
        return new VariancePopulation(field, false);
    }

    /**
     * The <code>POPULATION_VARIANCE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> populationVariance(Field<Double> field) {
        return new PopulationVariance(field, false);
    }

    /**
     * The <code>VARIANCE_SAMPLE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> varianceSample(Field<Double> field) {
        return new VarianceSample(field, false);
    }

    /**
     * The <code>VARPOP</code> aggregate function (alias for <code>VARIANCE_POPULATION</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> varPop(Field<Double> field) {
        return new VariancePopulation(field, false);
    }

    /**
     * The <code>VARIANCE(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> varianceDistinct(Field<Double> field) {
        return new Variance(field, true);
    }

    /**
     * The <code>VARIANCE_POPULATION(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> variancePopulationDistinct(Field<Double> field) {
        return new VariancePopulation(field, true);
    }

    /**
     * The <code>POPULATION_VARIANCE(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> populationVarianceDistinct(Field<Double> field) {
        return new PopulationVariance(field, true);
    }

    /**
     * The <code>VARIANCE_SAMPLE(DISTINCT field)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> varianceSampleDistinct(Field<Double> field) {
        return new VarianceSample(field, true);
    }

    /**
     * The <code>VARIANCE_SAMP(DISTINCT field)</code> aggregate function (alias for <code>VARIANCE_POPULATION</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#stddev-variance">documentation</a>
     */
    public static AggregateFunction<Double> varianceSampDistinct(Field<Double> field) {
        return new VarianceSample(field, true);
    }

    /**
     * The <code>CORRELATION</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#correlation-covariance">documentation</a>
     */
    public static AggregateFunction<Double> correlation(Field<Double> field1,
                                                        Field<Double> field2) {
        return new Correlation(field1, field2, false);
    }

    /**
     * The <code>COVARIANCE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#correlation-covariance">documentation</a>
     */
    public static AggregateFunction<Double> covariance(Field<Double> field1,
                                                       Field<Double> field2) {
        return new Covariance(field1, field2, false);
    }

    /**
     * The <code>COVARIANCE_SAMPLE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#correlation-covariance">documentation</a>
     */
    public static AggregateFunction<Double> covarianceSample(Field<Double> field1,
                                                             Field<Double> field2) {
        return new CovarianceSample(field1, field2, false);
    }

    /**
     * The <code>COVARIANCE_POPULATION</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#correlation-covariance">documentation</a>
     */
    public static AggregateFunction<Double> covariancePopulation(Field<Double> field1,
                                                                 Field<Double> field2) {
        return new CovariancePopulation(field1, field2, false);
    }

    /**
     * The <code>CORR</code> aggregate function (alias for <code>CORRELATION</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#correlation-covariance">documentation</a>
     */
    public static AggregateFunction<Double> corr(Field<Double> field1,
                                                 Field<Double> field2) {
        return new Correlation(field1, field2, false);
    }

    /**
     * The <code>COVAR</code> aggregate function (alias for <code>COVARIANCE</code>).
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#correlation-covariance">documentation</a>
     */
    public static AggregateFunction<Double> covar(Field<Double> field1,
                                                  Field<Double> field2) {
        return new Covariance(field1, field2, false);
    }

    /**
     * The <code>PERCENTILE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> percentile(Field<T> field,
                                                      double percent) {
        return percentile(field, val(percent));
    }

    /**
     * The <code>PERCENTILE</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> percentile(Field<T> field,
                                                      Field<Double> percent) {
        return new Percentile<>(field, percent, false);
    }

    /**
     * The <code>PERCENTILE(DISTINCT field, percent)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> percentileDistinct(Field<T> field,
                                                              double percent) {
        return percentileDistinct(field, val(percent));
    }

    /**
     * The <code>PERCENTILE(DISTINCT field, percent)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> percentileDistinct(Field<T> field,
                                                              Field<Double> percent) {
        return new Percentile<>(field, percent, true);
    }

    /**
     * The <code>MEDIAN</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> median(Field<T> field) {
        return median(field, null);
    }

    /**
     * The <code>MEDIAN</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> median(Field<T> field,
                                                  double percent) {
        return median(field, val(percent));
    }

    /**
     * The <code>MEDIAN</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> median(Field<T> field,
                                                  Field<Double> percent) {
        return new Median<>(field, percent, false);
    }

    /**
     * The <code>MEDIAN(DISTINCT field, percent)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> medianDistinct(Field<T> field) {
        return medianDistinct(field, null);
    }

    /**
     * The <code>MEDIAN(DISTINCT field, percent)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> medianDistinct(Field<T> field,
                                                          double percent) {
        return medianDistinct(field, val(percent));
    }

    /**
     * The <code>MEDIAN(DISTINCT field, percent)</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#percentile-median">documentation</a>
     */
    public static <T> AggregateFunction<T> medianDistinct(Field<T> field,
                                                          Field<Double> percent) {
        return new Median<>(field, percent, true);
    }

    /**
     * The <code>BOOL_AND</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#bool-and-or-xor">documentation</a>
     */
    public static AggregateFunction<Boolean> boolAnd(Field<Boolean> field) {
        return new BoolAnd(field);
    }

    /**
     * The <code>BOOL_OR</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#bool-and-or-xor">documentation</a>
     */
    public static AggregateFunction<Boolean> boolOr(Field<Boolean> field) {
        return new BoolOr(field);
    }

    /**
     * The <code>BOOL_XOR</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#bool-and-or-xor">documentation</a>
     */
    public static AggregateFunction<Boolean> boolXor(Field<Boolean> field) {
        return new BoolXor(field);
    }

    /**
     * The <code>BIT_AND</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#bit-and-or-xor">documentation</a>
     */
    public static <T extends UNumber> AggregateFunction<T> bitAnd(Field<T> field) {
        return new BitAnd<>(field);
    }

    /**
     * The <code>BIT_OR</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#bit-and-or-xor">documentation</a>
     */
    public static <T extends UNumber> AggregateFunction<T> bitOr(Field<T> field) {
        return new BitOr<>(field);
    }

    /**
     * The <code>BIT_XOR</code> aggregate function.
     * <p>
     * For details, read the <a href="https://ydb.tech/docs/ru/yql/reference/builtins/aggregation#bit-and-or-xor">documentation</a>
     */
    public static <T extends UNumber> AggregateFunction<T> bitXor(Field<T> field) {
        return new BitXor<>(field);
    }
}
