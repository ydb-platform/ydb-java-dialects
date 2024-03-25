package tech.ydb.liquibase.lockservice;

import java.util.concurrent.ThreadLocalRandom;
import liquibase.Scope;
import liquibase.exception.DatabaseException;
import liquibase.exception.LockException;
import liquibase.lockservice.StandardLockService;

/**
 * @author Kirill Kurdyukov
 */
public class StandardLockServiceYdb extends StandardLockService {

    private static final int RELEASE_MAX_ATTEMPT = 10;

    @Override
    public void waitForLock() throws LockException {
        try {
            database.getConnection().setAutoCommit(false);
        } catch (DatabaseException e) {
            throw new LockException(e);
        }

        super.waitForLock();
    }

    @Override
    public boolean acquireLock() {
        try {
            return super.acquireLock();
        } catch (LockException e) {
            return false;
        }
    }

    @Override
    public void releaseLock() throws LockException {
        for (int i = 0; i < RELEASE_MAX_ATTEMPT; i++) {
            try {
                super.releaseLock();

                return;
            } catch (LockException e) {
                if (i == RELEASE_MAX_ATTEMPT - 1) {
                    throw e;
                }

                Scope.getCurrentScope().getLog(StandardLockServiceYdb.class).info("Retry release lock!");

                try {
                    Thread.sleep(ThreadLocalRandom.current().nextLong(1000));
                } catch (InterruptedException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }
    }
}
