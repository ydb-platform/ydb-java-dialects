package tech.ydb.jooq.value;

import java.io.Serializable;
import java.util.Arrays;

public final class YSON implements Serializable {

    private final byte[] data;
    private String value;

    private YSON(byte[] data) {
        this.data = Arrays.copyOf(data, data.length);
    }

    public byte[] data() {
        return data;
    }

    public String string() {
        if (value == null) {
            value = new String(data);
        }

        return value;
    }

    public static YSON valueOf(byte[] data) {
        return new YSON(data);
    }

    public static YSON valueOf(String string) {
        return new YSON(string.getBytes());
    }

    public static YSON yson(byte[] data) {
        return new YSON(data);
    }

    public static YSON yson(String string) {
        return new YSON(string.getBytes());
    }

    public static YSON ysonOrNull(byte[] data) {
        return data == null ? null : yson(data);
    }

    public static YSON ysonOrNull(String string) {
        return string == null ? null : yson(string);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof YSON) {
            YSON yson = (YSON) obj;
            return Arrays.equals(data, yson.data);
        }
        return false;
    }

    @Override
    public String toString() {
        return string();
    }
}
