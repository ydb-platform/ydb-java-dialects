package tech.ydb.retry.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tech.ydb.core.StatusCode.ABORTED;
import static tech.ydb.core.StatusCode.CLIENT_CANCELLED;

class DeterministicErrorChannelTest {

    @Test
    void shouldAcceptProtobufResponseStatus() {
        assertDoesNotThrow(() -> DeterministicErrorChannel.configure().onError("executeQuery", 1, ABORTED));
    }

    @Test
    void shouldRejectClientSideStatusAtConfigurationTime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DeterministicErrorChannel.configure().onError("executeQuery", 1, CLIENT_CANCELLED)
        );
    }
}
