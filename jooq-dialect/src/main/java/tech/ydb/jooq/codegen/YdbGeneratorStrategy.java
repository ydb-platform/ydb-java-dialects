package tech.ydb.jooq.codegen;

import org.jooq.codegen.DefaultGeneratorStrategy;
import org.jooq.impl.YdbTableImpl;
import org.jooq.meta.Definition;
import org.jooq.meta.TableDefinition;
import org.jooq.tools.StringUtils;

public class YdbGeneratorStrategy extends DefaultGeneratorStrategy {
    @Override
    public String getJavaClassName(Definition definition, Mode mode) {
        return getJavaClassName0(definition.getInputName(), mode);
    }

    private static String getJavaClassName0(String outputName, Mode mode) {
        StringBuilder result = new StringBuilder();

        result.append(StringUtils.toCamelCase(
                outputName.replace(' ', '_')
                        .replace('-', '_')
                        .replace('.', '_')
        ));

        if (mode == Mode.RECORD) {
            result.append("Record");
        } else if (mode == Mode.DAO) {
            result.append("Dao");
        } else if (mode == Mode.INTERFACE) {
            result.insert(0, "I");
        } else if (mode == Mode.PATH) {
            result.append("Path");
        }

        return result.toString();
    }

    @Override
    public String getJavaIdentifier(Definition definition) {
        return definition.getInputName().toUpperCase(getTargetLocale());
    }

    @Override
    public String getJavaClassExtends(Definition definition, Mode mode) {
        if (definition instanceof TableDefinition && YdbGeneratorStrategy.Mode.DEFAULT.equals(mode)) {
            return YdbTableImpl.class.getName();
        }
        return super.getJavaClassExtends(definition, mode);
    }
}
