package ydb.jimmer.dialect.model.type.ydbJson;

import org.babyfish.jimmer.sql.Serialized;

@Serialized
public class Json {
    private Integer a;
    private Integer b;

    public Integer getA() {
        return a;
    }

    public Integer getB() {
        return b;
    }

    public void setA(Integer a) {
        this.a = a;
    }

    public void setB(Integer b) {
        this.b = b;
    }
}
