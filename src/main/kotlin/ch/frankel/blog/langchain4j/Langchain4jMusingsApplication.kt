package ch.frankel.blog.langchain4j

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.mcp.McpToolProvider
import dev.langchain4j.mcp.client.DefaultMcpClient
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.UserMessage
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import kotlinx.coroutines.reactive.asFlow
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.support.beans
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter
import reactor.core.publisher.Flux

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties::class)
class Langchain4jMusingsApplication

interface ChatBot {
    fun talk(@MemoryId sessionId: String, @UserMessage message: String): Flux<String>
}

data class StructuredMessage(val sessionId: String, val text: String)

class PromptHandler(private val chatBot: ChatBot) {

    suspend fun handle(req: ServerRequest) = try {
        val message = req.awaitBody<StructuredMessage>()
        val flux = chatBot.talk(message.sessionId, message.text)
        ServerResponse.ok().bodyAndAwait(flux.asFlow())
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
        val transport = HttpMcpTransport.Builder()
            .sseUrl(ref<ApplicationProperties>().mcp.url)
            .logRequests(true)
            .logResponses(true)
            .build()
        val mcpClient = DefaultMcpClient.Builder()
            .transport(transport)
            .build()
        mcpClient.listTools().forEach { println(it) }
        McpToolProvider.builder()
            .mcpClients(listOf(mcpClient))
            .build()
    }
    bean {
        coRouter {
            val chatBot = AiServices
                .builder(ChatBot::class.java)
                .streamingChatLanguageModel(ref<StreamingChatLanguageModel>())
                .chatMemoryProvider { MessageWindowChatMemory.withMaxMessages(40) }
                .contentRetriever(EmbeddingStoreContentRetriever.from(ref<EmbeddingStore<TextSegment>>()))
                .toolProvider(ref<McpToolProvider>())
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
