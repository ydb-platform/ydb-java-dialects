package tech.ydb.liquibase.snapshot;

import java.util.ArrayList;
import java.util.List;
import liquibase.database.Database;
import liquibase.snapshot.CachedRow;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.SnapshotGenerator;
import liquibase.snapshot.jvm.UniqueConstraintSnapshotGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Table;
import tech.ydb.liquibase.database.YdbDatabase;

/**
 * @author Kirill Kurdyukov
 */
public class UniqueConstraintSnapshotGeneratorYdb extends UniqueConstraintSnapshotGenerator {

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (database instanceof YdbDatabase) {
            return PRIORITY_DATABASE;
        }

        return PRIORITY_NONE;
    }

    @Override
    public Class<? extends SnapshotGenerator>[] replaces() {
        return new Class[]{UniqueConstraintSnapshotGenerator.class};
    }

    protected List<CachedRow> listConstraints(Table table, DatabaseSnapshot snapshot, Schema schema)  {
        return new ArrayList<>();
    }
}