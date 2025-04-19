package ch.frankel.blog.langchain4j

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("application")
data class ApplicationProperties(val mcp: Mcp)
data class Mcp(val url: String)
