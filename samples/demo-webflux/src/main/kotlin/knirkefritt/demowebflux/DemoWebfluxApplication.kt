package knirkefritt.demowebflux

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["cqrslite", "knirkefritt.demowebflux"])
class DemoWebfluxApplication

fun main(args: Array<String>) {
	runApplication<DemoWebfluxApplication>(*args)
}
