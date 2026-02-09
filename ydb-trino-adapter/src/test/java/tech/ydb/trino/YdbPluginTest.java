package tech.ydb.trino;

import io.trino.spi.connector.ConnectorFactory;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class YdbPluginTest
{
    @Test
    void exposesSingleConnectorFactory()
    {
        YdbPlugin plugin = new YdbPlugin();
        Iterator<ConnectorFactory> factories = plugin.getConnectorFactories().iterator();
        assertTrue(factories.hasNext(), "Expected a connector factory");
        ConnectorFactory factory = factories.next();
        assertEquals("ydb", factory.getName());
        assertFalse(factories.hasNext(), "Expected only one connector factory");
    }
}
