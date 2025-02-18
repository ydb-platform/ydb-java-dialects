package tech.ydb.data.repository.config;

import java.lang.reflect.Field;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.util.ReflectionUtils;

import tech.ydb.data.YdbBaseTest;
import tech.ydb.data.repository.ViewIndex;

/**
 * Tests for {@link JdbcRepositoryBeanPostProcessor}.
 *
 * @author Mikhail Polivakha
 */
class JdbcRepositoryBeanPostProcessorTest extends YdbBaseTest {

    @Autowired
    private RepositoryFactoryBeanSupport<UserRepository, User, Long> userFactoryBeanSupport;

    @Autowired
    private RepositoryFactoryBeanSupport<AddressRepository, Address, Long> addressFactoryBeanSupport;

    @Test
    void shouldExposeMetadataOnlyForRepositoriesWithViewIndexMethods() {

        // given.
        Field exposeMetadataField = ReflectionUtils.findField(RepositoryFactoryBeanSupport.class, "exposeMetadata");
        exposeMetadataField.setAccessible(true);

        // when.
        Object userFactoryExposedMetadata = ReflectionUtils.getField(exposeMetadataField, userFactoryBeanSupport);
        Object addressFactoryExposedMetadata = ReflectionUtils.getField(exposeMetadataField, addressFactoryBeanSupport);

        // then.
        Assertions.assertThat(userFactoryExposedMetadata).isEqualTo(true);
        Assertions.assertThat(addressFactoryExposedMetadata).isEqualTo(false);
    }

    @Table
    static class User {

        @Id
        private Long id;

        private String name;
    }

    @Table
    static class Address {

        @Id
        private Long id;
    }

    interface UserRepository extends CrudRepository<User, Long> {

        @ViewIndex(indexName = "name_authors_index", tableName = "authors")
        Optional<User> findUserByName(String name);
    }

    interface AddressRepository extends CrudRepository<Address, Long> {

    }
}
