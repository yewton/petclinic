package net.yewton.petclinic

import io.r2dbc.spi.ConnectionFactory
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.RenderQuotedNames
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.connection.TransactionAwareConnectionFactoryProxy

@Configuration
class JooqConfig {
  // https://github.com/spring-projects/spring-boot/issues/30760#issuecomment-1670644545
  @Bean
  fun dslContext(connectionFactory: ConnectionFactory): DSLContext {
    val settings =
      Settings().apply {
        withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED)
      }
    return DSL.using(TransactionAwareConnectionFactoryProxy(connectionFactory), SQLDialect.POSTGRES, settings)
  }
}
