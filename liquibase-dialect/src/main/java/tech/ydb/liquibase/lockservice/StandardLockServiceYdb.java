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
    public boolean acquireLock() throws LockException {
        try {
            boolean originalAutoCommit = database.getConnection().getAutoCommit();

            try {
                database.getConnection().setAutoCommit(false);

                return super.acquireLock();
            } catch (LockException e) {
                return false;
            } finally {
                database.getConnection().setAutoCommit(originalAutoCommit);
            }
        } catch (DatabaseException e) { // getAutoCommit / setAutoCommit throws this exception
            throw new LockException(e);
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
                    throw new LockException(exception);
                }
            }
        }
    }
}
