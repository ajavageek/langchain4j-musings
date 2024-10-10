package ch.frankel.blog.langchain4j

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.StreamingResponseHandler
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.store.memory.chat.ChatMemoryStore
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore
import kotlinx.coroutines.reactive.asFlow
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.support.beans
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.coRouter
import reactor.core.publisher.Sinks

@SpringBootApplication
class Langchain4jMusingsApplication

data class StructuredMessage(val sessionId: String, val message: String)

class AppStreamingResponseHandler(
    private val sink: Sinks.Many<String>,
    private val store: ChatMemoryStore,
    private val sessionId: String
) : StreamingResponseHandler<AiMessage> {

    override fun onNext(token: String) {
        sink.tryEmitNext(token)
    }

    override fun onError(error: Throwable) {
        sink.tryEmitError(error)
    }

    override fun onComplete(response: Response<AiMessage>) {
        val message = response.content()?.text()
        if (message != null) {
            store.getMessages(sessionId).add(AiMessage(message))
        }
        sink.tryEmitComplete()
    }
}

class PromptHandler(private val model: StreamingChatLanguageModel, private val store: ChatMemoryStore) {

    suspend fun handle(req: ServerRequest): ServerResponse {
        val message = req.awaitBody<StructuredMessage>()
        val prompt = UserMessage(message.text)
        val messages = store.getMessages(message.sessionId)
        messages.add(prompt)
        val sink = Sinks.many().unicast().onBackpressureBuffer<String>()
        model.generate(messages, AppStreamingResponseHandler(sink, store, message.sessionId))
        return ServerResponse.ok().bodyAndAwait(sink.asFlux().asFlow())
    }
}

fun beans() = beans {
    bean {
        coRouter {
            POST("/")(PromptHandler(ref<StreamingChatLanguageModel>(), InMemoryChatMemoryStore())::handle)
        }
    }
}

fun main(args: Array<String>) {
    runApplication<Langchain4jMusingsApplication>(*args) {
        addInitializers(beans())
    }
}
