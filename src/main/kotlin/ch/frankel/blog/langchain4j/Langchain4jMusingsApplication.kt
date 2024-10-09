package ch.frankel.blog.langchain4j

import dev.langchain4j.model.chat.ChatLanguageModel
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.support.beans
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.ServerResponse.ok
import org.springframework.web.servlet.function.router

@SpringBootApplication
class Langchain4jMusingsApplication

class PromptHandler(private val model: ChatLanguageModel) {

    fun handle(req: ServerRequest): ServerResponse {
        val prompt = req.body<String>(String::class.java)
        val result = model.generate(prompt)
        return ok().body(result)
    }
}

fun beans() = beans {
    bean {
        router {
            POST("/")(PromptHandler(ref<ChatLanguageModel>())::handle)
        }
    }
}


fun main(args: Array<String>) {
    runApplication<Langchain4jMusingsApplication>(*args) {
        addInitializers(beans())
    }
}
