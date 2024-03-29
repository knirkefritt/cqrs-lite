package cqrslite.spring.config

import cqrslite.core.Repository
import cqrslite.core.SessionImpl
import cqrslite.core.asCoroutineContext
import kotlinx.coroutines.withContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter

/**
 * If this is a webflux project, the session is available on the current coroutine context, example:
 * val session = SessionManager.session()
 */
@Configuration
@ConditionalOnClass(name = ["org.springframework.web.server.CoWebFilter"])
class CqrsConfigWebflux {
    @Bean
    fun sessionWebFlux(repository: Repository): WebFilter = object : CoWebFilter() {
        override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
            withContext(SessionImpl(repository).asCoroutineContext()) {
                chain.filter(exchange)
            }
        }
    }
}
