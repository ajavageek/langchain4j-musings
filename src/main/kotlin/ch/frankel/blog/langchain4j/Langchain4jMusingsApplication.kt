package ch.frankel.blog.langchain4j

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.StreamingResponseHandler
import dev.langchain4j.model.chat.StreamingChatLanguageModel
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

class AppStreamingResponseHandler(private val sink: Sinks.Many<String>) : StreamingResponseHandler<AiMessage> {

    override fun onNext(token: String) {
        sink.tryEmitNext(token)
    }

    override fun onError(error: Throwable) {
        sink.tryEmitError(error)
    }
}

class PromptHandler(private val model: StreamingChatLanguageModel) {

    suspend fun handle(req: ServerRequest): ServerResponse {
        val prompt = req.awaitBody<String>()
        val sink = Sinks.many().unicast().onBackpressureBuffer<String>()
        model.generate(prompt, AppStreamingResponseHandler(sink))
        return ServerResponse.ok().bodyAndAwait(sink.asFlux().asFlow())
    }
}

fun beans() = beans {
    bean {
        coRouter {
            POST("/")(PromptHandler(ref<StreamingChatLanguageModel>())::handle)
        }
    }
}

fun main(args: Array<String>) {
    runApplication<Langchain4jMusingsApplication>(*args) {
        addInitializers(beans())
    }
}
