package cqrslite.spring.harness

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties

@EnableConfigurationProperties
@SpringBootApplication(scanBasePackages = ["cqrslite.spring", "harness"])
class TestContext
