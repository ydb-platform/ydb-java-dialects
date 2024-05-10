package tech.ydb.jooq.codegen;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.meta.*;
import org.jooq.meta.jaxb.ForcedType;
import org.jooq.tools.JooqLogger;
import org.jooq.types.UByte;
import org.jooq.types.UInteger;
import org.jooq.types.ULong;
import org.jooq.types.UShort;
import tech.ydb.jdbc.YdbConnection;
import tech.ydb.jdbc.context.YdbContext;
import tech.ydb.jooq.YDB;
import tech.ydb.jooq.YdbTypes;
import tech.ydb.jooq.binding.*;
import tech.ydb.jooq.value.YSON;
import tech.ydb.proto.scheme.SchemeOperationProtos;
import tech.ydb.proto.scheme.SchemeOperationProtos.Entry.Type;
import tech.ydb.scheme.SchemeClient;
import tech.ydb.scheme.description.ListDirectoryResult;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.description.TableIndex;
import tech.ydb.table.settings.DescribeTableSettings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class YdbDatabase extends AbstractDatabase implements ResultQueryDatabase {
    private static final JooqLogger log = JooqLogger.getLogger(YdbDatabase.class);

    public YdbDatabase() {
        addForcedTypeForUnsignedTypes();
        addForcedTypeForStringTypes();
    }

    private void addForcedTypeForUnsignedTypes() {
        ForcedType uint8 = new ForcedType()
                .withName("Uint8")
                .withExpression(".*")
                .withTypes(YdbTypes.UINT8.getTypeName())
                .withBinding(Uint8Binding.class.getName())
                .withUserType(UByte.class.getName());

        ForcedType uint16 = new ForcedType()
                .withName("Uint16")
                .withExpression(".*")
                .withTypes(YdbTypes.UINT16.getTypeName())
                .withBinding(Uint16Binding.class.getName())
                .withUserType(UShort.class.getName());

        ForcedType uint32 = new ForcedType()
                .withName("Uint32")
                .withExpression(".*")
                .withTypes(YdbTypes.UINT32.getTypeName())
                .withBinding(Uint32Binding.class.getName())
                .withUserType(UInteger.class.getName());

        ForcedType uint64 = new ForcedType()
                .withName("Uint64")
                .withExpression(".*")
                .withTypes(YdbTypes.UINT64.getTypeName())
                .withBinding(Uint64Binding.class.getName())
                .withUserType(ULong.class.getName());

        setConfiguredForcedTypes(List.of(uint8, uint16, uint32, uint64));
    }

    private void addForcedTypeForStringTypes() {
        ForcedType json = new ForcedType()
                .withName("Json")
                .withExpression(".*")
                .withTypes(YdbTypes.JSON.getTypeName())
                .withBinding(JsonBinding.class.getName())
                .withUserType(JSON.class.getName());

        ForcedType jsonDoc = new ForcedType()
                .withName("JsonDocument")
                .withExpression(".*")
                .withTypes(YdbTypes.JSONDOCUMENT.getTypeName())
                .withBinding(JsonDocumentBinding.class.getName())
                .withUserType(JSONB.class.getName());

        ForcedType yson = new ForcedType()
                .withName("Yson")
                .withExpression(".*")
                .withTypes(YdbTypes.YSON.getTypeName())
                .withBinding(YsonBinding.class.getName())
                .withUserType(YSON.class.getName());

        setConfiguredForcedTypes(List.of(json, jsonDoc, yson));
    }

    @Override
    protected DSLContext create0() {
        return DSL.using(getConnection(), YDB.DIALECT);
    }

    private YdbContext getContext() {
        return ((YdbConnection) getConnection()).getCtx();
    }

    @Override
    protected void loadPrimaryKeys(DefaultRelations r) {
        for (TableDefinition tableDefinition : getTables0()) {
            TableDescription tableDescription = ((YdbTableDefinition) tableDefinition).getTableDescription();
            List<String> primaryKeys = tableDescription.getPrimaryKeys();

            String pkName = "pk_" + tableDefinition.getName();

            for (String column : primaryKeys) {
                r.addPrimaryKey(pkName, tableDefinition, tableDefinition.getColumn(column));
            }
        }
    }

    @Override
    protected void loadUniqueKeys(DefaultRelations r) {
        // Unique keys doesn't exist
    }

    @Override
    protected void loadForeignKeys(DefaultRelations r) {
        // Foreign keys doesn't exist
    }

    @Override
    protected void loadCheckConstraints(DefaultRelations r) {
        // Check constraints doesn't exist
    }

    @Override
    protected List<CatalogDefinition> getCatalogs0() {
        List<CatalogDefinition> result = new ArrayList<>();
        result.add(new CatalogDefinition(this, "", ""));
        return result;
    }

    @Override
    protected List<SchemaDefinition> getSchemata0() {
        String database = getContext().getDatabase();
        List<String> schemas = schemas();

        int databasePathSize = database.length() + 1;

        List<SchemaDefinition> result = new ArrayList<>();
        for (String path : schemas) {
            String catalogPath = path.equals(database) ? "" : path.substring(databasePathSize);
            SchemaDefinition schemaDefinition = new SchemaDefinition(this, catalogPath, "");
            result.add(schemaDefinition);
        }

        return result;
    }

    @Override
    protected List<SequenceDefinition> getSequences0() {
        return Collections.emptyList();
    }

    @Override
    protected List<TableDefinition> getTables0() {
        List<TableDefinition> result = new ArrayList<>();

        YdbContext context = getContext();
        SchemeClient client = context.getSchemeClient();
        DescribeTableSettings settings = context.withDefaultTimeout(new DescribeTableSettings());

        List<String> schemas = schemas();

        for (String dirPath : schemas) {
            ListDirectoryResult listDirectory = client.listDirectory(dirPath).join().getValue();

            for (SchemeOperationProtos.Entry entry : listDirectory.getChildren()) {
                if (entry.getType() == Type.TABLE || entry.getType() == Type.COLUMN_TABLE) {
                    String tableName = entry.getName();
                    String fullPath = dirPath + "/" + tableName;

                    TableDescription tableDescription = context.describeTable(fullPath, settings).join().getValue();
                    TableDefinition tableDefinition = getTableDefinition(tableName, dirPath, fullPath, tableDescription);

                    result.add(tableDefinition);
                }
            }
        }

        return result;
    }

    private TableDefinition getTableDefinition(String tableName, String dirPathFull, String fullPath, TableDescription tableDescription) {
        int databasePathSize = getContext().getDatabase().length() + 1;

        String tablePath = fullPath.length() < databasePathSize ? "" : fullPath.substring(databasePathSize);
        String dirPath = dirPathFull.length() < databasePathSize ? "" : dirPathFull.substring(databasePathSize);

        SchemaDefinition scheme = new SchemaDefinition(
                this,
                dirPath,
                ""
        );

        return new YdbTableDefinition(
                scheme,
                tableName,
                "",
                tableDescription,
                tablePath
        );
    }

    private List<String> schemas() {
        List<String> schemas = new ArrayList<>();

        SchemeClient client = getContext().getSchemeClient();
        String database = getContext().getDatabase();
        schemas.add(database);

        for (int i = 0; i < schemas.size(); i++) {
            String pathPrefix = schemas.get(i);
            ListDirectoryResult listDirectory = client.listDirectory(pathPrefix).join().getValue();
            pathPrefix += "/";

            for (SchemeOperationProtos.Entry entry : listDirectory.getChildren()) {
                String name = entry.getName();
                String fullPath = pathPrefix + name;
                if (entry.getType() == Type.DIRECTORY) {
                    schemas.add(fullPath);
                }
            }
        }

        return schemas;
    }

    @Override
    protected List<RoutineDefinition> getRoutines0() {
        return Collections.emptyList();
    }

    @Override
    protected List<PackageDefinition> getPackages0() {
        return Collections.emptyList();
    }

    @Override
    protected List<EnumDefinition> getEnums0() {
        return Collections.emptyList();
    }

    @Override
    protected List<DomainDefinition> getDomains0() {
        return Collections.emptyList();
    }

    @Override
    protected List<IndexDefinition> getIndexes0() {
        List<IndexDefinition> result = new ArrayList<>();

        for (TableDefinition tableDefinition : getTables0()) {
            TableDescription tableDescription = ((YdbTableDefinition) tableDefinition).getTableDescription();
            List<TableIndex> tableIndexes = tableDescription.getIndexes();

            for (TableIndex tableIndex : tableIndexes) {
                IndexDefinition indexDefinition = new YdbIndexDefinition(
                        tableDefinition.getSchema(),
                        tableIndex.getName(),
                        tableDefinition,
                        tableIndex
                );

                result.add(indexDefinition);
            }
        }

        return result;
    }

    @Override
    protected List<XMLSchemaCollectionDefinition> getXMLSchemaCollections0() {
        return Collections.emptyList();
    }

    @Override
    protected List<UDTDefinition> getUDTs0() {
        return Collections.emptyList();
    }

    @Override
    protected List<ArrayDefinition> getArrays0() {
        return Collections.emptyList();
    }

    @Override
    public ResultQuery<Record6<String, String, String, String, String, Integer>> primaryKeys(List<String> schemas) {
        return null;
    }

    @Override
    public ResultQuery<Record6<String, String, String, String, String, Integer>> uniqueKeys(List<String> schemas) {
        return null;
    }

    @Override
    public ResultQuery<Record12<String, String, String, String, Integer, Integer, Long, Long, BigDecimal, BigDecimal, Boolean, Long>> sequences(List<String> schemas) {
        return null;
    }

    @Override
    public ResultQuery<Record6<String, String, String, String, String, Integer>> enums(List<String> schemas) {
        return null;
    }

    @Override
    public ResultQuery<Record4<String, String, String, String>> sources(List<String> schemas) {
        return null;
    }

    @Override
    public ResultQuery<Record5<String, String, String, String, String>> comments(List<String> schemas) {
        return null;
    }
}
