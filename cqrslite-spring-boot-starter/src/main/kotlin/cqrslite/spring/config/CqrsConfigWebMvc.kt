package cqrslite.spring.config

import cqrslite.core.Repository
import cqrslite.core.Session
import cqrslite.core.SessionImpl
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.context.annotation.RequestScope

/**
 * Configures a session pr web request, does not work with WebFlux, because it relies on ThreadLocal
 */
@Configuration
@ConditionalOnClass(name = ["org.springframework.web.servlet.DispatcherServlet"])
class CqrsConfigWebMvc {

    @Bean
    @RequestScope
    fun sessionMvc(repository: Repository): Session {
        return SessionImpl(repository)
    }
}
