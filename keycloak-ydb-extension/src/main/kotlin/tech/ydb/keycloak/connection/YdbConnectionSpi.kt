package tech.ydb.keycloak.connection

import org.keycloak.provider.Spi

class YdbConnectionSpi : Spi {
  override fun isInternal(): Boolean = true

  override fun getName() = NAME

  override fun getProviderClass() = YdbConnectionProvider::class.java

  override fun getProviderFactoryClass() = YdbConnectionProviderFactory::class.java

  companion object {
    private const val NAME: String = "ydbConnection"
  }
}
