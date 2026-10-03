package ydb.jimmer.dialect.scalar;

import java.time.Duration;

public class DurationProvider extends DumbYdbScalarProvider<Duration> {
    public DurationProvider() {
        super(Duration.class);
    }
}
