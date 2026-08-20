package tech.ydb.trino;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestNonEphemeralPortsGenerator
{
    @Test
    public void testSkipsOccupiedStartingPort()
            throws IOException
    {
        try (ServerSocket occupied = reservePortInRange()) {
            NonEphemeralPortsGenerator ports = new NonEphemeralPortsGenerator(occupied.getLocalPort());

            int available = ports.findAvailablePort();

            assertThat(available)
                    .isBetween(NonEphemeralPortsGenerator.MIN_PORT, NonEphemeralPortsGenerator.MAX_PORT)
                    .isNotEqualTo(occupied.getLocalPort());
        }
    }

    @Test
    public void testReturnsUniquePortsAcrossRangeBoundary()
    {
        NonEphemeralPortsGenerator ports = new NonEphemeralPortsGenerator(
                NonEphemeralPortsGenerator.MAX_PORT,
                ignored -> true);

        List<Integer> allocated = List.of(
                ports.findAvailablePort(),
                ports.findAvailablePort(),
                ports.findAvailablePort());

        assertThat(allocated)
                .containsExactly(
                        NonEphemeralPortsGenerator.MAX_PORT,
                        NonEphemeralPortsGenerator.MIN_PORT,
                        NonEphemeralPortsGenerator.MIN_PORT + 1)
                .doesNotHaveDuplicates()
                .allSatisfy(port -> assertThat(port)
                        .isBetween(NonEphemeralPortsGenerator.MIN_PORT, NonEphemeralPortsGenerator.MAX_PORT));
    }

    @Test
    public void testReportsExhaustedRange()
    {
        NonEphemeralPortsGenerator ports = new NonEphemeralPortsGenerator(
                NonEphemeralPortsGenerator.MIN_PORT,
                ignored -> false);

        assertThatThrownBy(ports::findAvailablePort)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No wildcard TCP port is available in test range 20000-29999; stop conflicting services");
    }

    private static ServerSocket reservePortInRange()
            throws IOException
    {
        for (int port = NonEphemeralPortsGenerator.MIN_PORT;
                port <= NonEphemeralPortsGenerator.MAX_PORT;
                port++) {
            ServerSocket socket = new ServerSocket();
            socket.setReuseAddress(false);
            try {
                socket.bind(new InetSocketAddress((InetAddress) null, port), 1);
                return socket;
            }
            catch (IOException unavailable) {
                socket.close();
            }
        }
        throw new IOException("No port can be reserved in non-ephemeral test range");
    }
}
