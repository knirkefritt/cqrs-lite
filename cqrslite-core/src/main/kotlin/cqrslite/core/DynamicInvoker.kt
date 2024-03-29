package cqrslite.core

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredFunctions

inline fun <reified T : Any> T.invokeMethod(methodName: String, vararg args: Any?): Any? {
    val key = MethodCacheKey(
        // BUGFIX: use this::class instead of T::class, since T::class will give you the AggregateRoot, not the
        // derived type
        this::class.java.name,
        methodName,
        DynamicInvoker.combineArgumentTypes(args),
    )

    DynamicInvoker.cachedMembers[key]?.let { return it.call(this, *args) }

    synchronized(DynamicInvoker.lockObj) {
        val argTypes = args.map { it?.let { it::class } ?: Unit::class }.toTypedArray()
        val method = DynamicInvoker.findMethod(this::class, methodName, *argTypes)
        DynamicInvoker.cachedMembers[key] = method
        return method?.call(this, *args)
    }
}

data class MethodCacheKey(
    val className: String,
    val methodName: String,
    val argumentTypes: String,
)

class DynamicInvoker {
    companion object {
        val cachedMembers = mutableMapOf<MethodCacheKey, KFunction<*>?>()
        val lockObj = Any()

        fun combineArgumentTypes(vararg args: Any?): String {
            val builder = StringBuilder()
            args.forEach {
                val childArr = it as? Array<*>
                if (childArr != null) {
                    childArr.forEach { item -> item?.let { builder.append(item::class.java.name) } }
                    builder.append("|")
                } else if (it != null) {
                    builder.append(it::class.java.name)
                    builder.append("|")
                }
            }
            return builder.toString()
        }

        fun findMethod(type: KClass<*>, name: String, vararg argTypes: KClass<*>): KFunction<*>? {
            val matchingMethod = type.declaredFunctions
                .map { src ->
                    object {
                        val name = src.name
                        val function = src
                        val parameters = src.parameters.drop(1) // the first parameter is the instance itself aka "this"
                    }
                }
                .filter { it.name == name && it.parameters.size == argTypes.size }
                .firstOrNull { function ->
                    function.parameters.allIndexed { index, parameter ->
                        parameter.type == argTypes[index].createType()
                    }
                }

            return matchingMethod?.function
        }

        private inline fun <T> Iterable<T>.allIndexed(predicate: (index: Int, T) -> Boolean): Boolean {
            for ((index, element) in this.withIndex()) {
                if (!predicate(index, element)) return false
            }
            return true
        }
    }
}
