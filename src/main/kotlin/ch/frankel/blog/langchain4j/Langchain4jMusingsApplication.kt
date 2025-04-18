package ch.frankel.blog.langchain4j

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.TokenStream
import dev.langchain4j.service.UserMessage
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import kotlinx.coroutines.reactive.asFlow
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.support.beans
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter
import reactor.core.publisher.Sinks

@SpringBootApplication
class Langchain4jMusingsApplication

interface ChatBot {
    fun talk(@MemoryId sessionId: String, @UserMessage message: String): TokenStream
}

data class StructuredMessage(val sessionId: String, val text: String)

class PromptHandler(private val chatBot: ChatBot) {

    suspend fun handle(req: ServerRequest) = try {
        val message = req.awaitBody<StructuredMessage>()
        val sink = Sinks.many().unicast().onBackpressureBuffer<String>()
        chatBot.talk(message.sessionId, message.text)
            .onPartialResponse(sink::tryEmitNext)
            .onError(sink::tryEmitError)
            .onCompleteResponse { sink.tryEmitComplete() }
            .start()
        ServerResponse.ok().bodyAndAwait(sink.asFlux().asFlow())
    } catch (e: Exception) {
        ServerResponse.badRequest().bodyValueAndAwait(e.message ?: "Unknown error")
    }
}

fun beans() = beans {
    bean<EmbeddingStore<TextSegment>> {
        InMemoryEmbeddingStore()
    }
    bean {
        BlogDataLoader(ref<EmbeddingStore<TextSegment>>())
    }
    bean {
        coRouter {
            val chatBot = AiServices
                .builder(ChatBot::class.java)
                .streamingChatLanguageModel(ref<StreamingChatLanguageModel>())
                .chatMemoryProvider { MessageWindowChatMemory.withMaxMessages(40) }
                .contentRetriever(EmbeddingStoreContentRetriever.from(ref<EmbeddingStore<TextSegment>>()))
                .build()
            POST("/")(PromptHandler(chatBot)::handle)
        }
    }
}

fun main(args: Array<String>) {
    runApplication<Langchain4jMusingsApplication>(*args) {
        addInitializers(beans())
    }
}
