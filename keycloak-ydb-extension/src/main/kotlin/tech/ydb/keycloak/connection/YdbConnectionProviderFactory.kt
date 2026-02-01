package tech.ydb.keycloak.connection

import org.keycloak.provider.ProviderFactory

interface YdbConnectionProviderFactory<T : YdbConnectionProvider> : ProviderFactory<T>
