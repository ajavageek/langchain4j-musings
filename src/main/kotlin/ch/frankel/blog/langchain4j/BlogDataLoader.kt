package ch.frankel.blog.langchain4j

import dev.langchain4j.data.document.loader.UrlDocumentLoader
import dev.langchain4j.data.document.parser.TextDocumentParser
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener

class BlogDataLoader(private val embeddingStore: EmbeddingStore<TextSegment>) {

    val logger = LoggerFactory.getLogger(BlogDataLoader::class.java)

    private val urls = arrayOf(
        "https://blog.frankel.ch/ajax-ssr/5/",
        "https://blog.frankel.ch/ajax-ssr/4/",
        "https://blog.frankel.ch/ajax-ssr/3/",
        "https://blog.frankel.ch/ajax-ssr/2/",
        "https://blog.frankel.ch/ajax-ssr/1/",
        "https://blog.frankel.ch/ajax-ssr/",
        "https://blog.frankel.ch/kotlin-coroutines-otel-tracing/",
        "https://blog.frankel.ch/default-map-value/",
        "https://blog.frankel.ch/opentelemetry-tracing-spring-boot/",
        "https://blog.frankel.ch/free-tier-api-apisix/",
        "https://blog.frankel.ch/dynamic-watermarking/1/",
        "https://blog.frankel.ch/renovate-for-everything/",
        "https://blog.frankel.ch/refresher-github-pages/",
        "https://blog.frankel.ch/even-more-opentelemetry/",
        "https://blog.frankel.ch/structured-env-vars-rust/",
        "https://blog.frankel.ch/worfklow-stateless-stateful/",
        "https://blog.frankel.ch/opinion-tauri/",
        "https://blog.frankel.ch/vary-http-header/",
        "https://blog.frankel.ch/try-block-rust/",
        "https://blog.frankel.ch/dissolving-design-patterns/",
        "https://blog.frankel.ch/fix-duplicate-api-requests/",
        "https://blog.frankel.ch/pitfall-implicit-returns/",
        "https://blog.frankel.ch/raspberry-pi-github-action/",
        "https://blog.frankel.ch/kotlin-scripting-to-python/",
        "https://blog.frankel.ch/error-management-rust-libs/",
        "https://blog.frankel.ch/improve-otel-demo/",
        "https://blog.frankel.ch/fonts-embedded-svg/",
        "https://blog.frankel.ch/kicking-tires-docker-scout/",
        "https://blog.frankel.ch/chopping-monolith-smarter-way/",
        "https://blog.frankel.ch/retrospective-error-management/",
        "https://blog.frankel.ch/opentelemetry-collector/",
        "https://blog.frankel.ch/api-versioning/",
        "https://blog.frankel.ch/feedback-rust-from-python/",
        "https://blog.frankel.ch/python-magic-methods/2/",
        "https://blog.frankel.ch/python-magic-methods/1/",
        "https://blog.frankel.ch/rust-from-python/",
        "https://blog.frankel.ch/resize-images-on-the-fly/",
        "https://blog.frankel.ch/monkeypatching-java/",
        "https://blog.frankel.ch/problem-details-http-apis/",
        "https://blog.frankel.ch/my-blog-new-authors/",
        "https://blog.frankel.ch/me/",
        "https://blog.frankel.ch/authors/",
        "https://blog.frankel.ch/authors/stefanofago/",
        "https://blog.frankel.ch/mentions/",
        "https://blog.frankel.ch/books/",
        "https://blog.frankel.ch/speaking/",
    )

    @EventListener(ApplicationStartedEvent::class)
    fun onApplicationStarted() {
        val parser = TextDocumentParser()
        logger.info("Starting to load documents")
        val documents = urls.map { UrlDocumentLoader.load(it, parser) }
        logger.info("Finished loading documents")
        logger.info("Starting to ingest documents")
        EmbeddingStoreIngestor.ingest(documents, embeddingStore)
        logger.info("Finished ingesting documents")
    }
}
