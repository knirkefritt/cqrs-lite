package cqrslite.core

import kotlinx.coroutines.currentCoroutineContext
import java.util.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

suspend fun <T : AggregateRoot> Session.find(id: UUID, expectedVersion: Int? = null, clazz: Class<T>): T? {
    return try {
        this.get(id, expectedVersion, clazz)
    } catch (_: Repository.AggregateNotFoundException) {
        null
    }
}

class SessionContextElement(
    val session: Session,
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<SessionContextElement>
}

class SessionManager {
    companion object {
        suspend fun session(): Session {
            return currentCoroutineContext()[SessionContextElement]?.session
                ?: throw Error("Session has not been initialized!")
        }
    }
}

fun Session.asCoroutineContext(): SessionContextElement {
    return SessionContextElement(this)
}
