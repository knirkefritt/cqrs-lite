package cqrslite.spring.config

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
@ConfigurationPropertiesBinding
class ClassNameToClassTypeConfigurationPropertiesBinding : Converter<String, Class<*>> {
    override fun convert(source: String): Class<*>? = Class.forName(source)
}
