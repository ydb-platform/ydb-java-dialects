package tech.ydb.trino;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntPredicate;

import tech.ydb.test.integration.utils.PortsGenerator;

import static java.util.Objects.requireNonNull;

final class NonEphemeralPortsGenerator
        extends PortsGenerator
{
    static final int MIN_PORT = 20_000;
    static final int MAX_PORT = 29_999;

    private static final int PORT_COUNT = MAX_PORT - MIN_PORT + 1;

    private final IntPredicate availablePort;
    private int nextPort;

    NonEphemeralPortsGenerator()
    {
        this(ThreadLocalRandom.current().nextInt(MIN_PORT, MAX_PORT + 1));
    }

    NonEphemeralPortsGenerator(int startingPort)
    {
        this(startingPort, NonEphemeralPortsGenerator::canBind);
    }

    NonEphemeralPortsGenerator(int startingPort, IntPredicate availablePort)
    {
        if (startingPort < MIN_PORT || startingPort > MAX_PORT) {
            throw new IllegalArgumentException("Starting port must be between %s and %s: %s"
                    .formatted(MIN_PORT, MAX_PORT, startingPort));
        }
        this.nextPort = startingPort;
        this.availablePort = requireNonNull(availablePort, "availablePort is null");
    }

    @Override
    public synchronized int findAvailablePort()
    {
        for (int checked = 0; checked < PORT_COUNT; checked++) {
            int candidate = nextPort;
            nextPort = candidate == MAX_PORT ? MIN_PORT : candidate + 1;
            if (availablePort.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No wildcard TCP port is available in test range %s-%s; stop conflicting services"
                .formatted(MIN_PORT, MAX_PORT));
    }

    private static boolean canBind(int port)
    {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress((InetAddress) null, port), 1);
            return true;
        }
        catch (IOException ignored) {
            return false;
        }
    }
}
