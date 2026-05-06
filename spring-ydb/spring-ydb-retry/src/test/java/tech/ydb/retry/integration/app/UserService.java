package tech.ydb.retry.integration.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ydb.retry.YdbTransactional;

@Service
public class UserService {

    private final SimpleUserRepository userRepository;

    public UserService(SimpleUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void saveRaw(User user) {
        userRepository.save(user);
    }

    @YdbTransactional
    public void save(User user) {
        userRepository.save(user);
    }

    @YdbTransactional(maxRetries = 3)
    public void saveWithMaxRetries3(User user) {
        userRepository.save(user);
    }

    @YdbTransactional(idempotent = 1)
    public void saveIdempotent(User user) {
        userRepository.save(user);
    }

    @YdbTransactional(maxRetries = 50, idempotent = 1)
    public void updateFirstname(Long id, String firstname) {
        userRepository.findById(id);
        userRepository.updateFirstnameById(id, firstname);
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteAll() {
        userRepository.deleteAll();
    }
}
