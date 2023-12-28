package tech.ydb.hibernate.dialect.exporter;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Kirill Kurdyukov
 */
public class EmptyExporter<T extends Exportable> implements Exporter<T> {

    @Override
    public String[] getSqlCreateStrings(T exportable, Metadata metadata) {
        return NO_COMMANDS;
    }

    @Override
    public String[] getSqlDropStrings(T exportable, Metadata metadata) {
        return NO_COMMANDS;
    }
}
