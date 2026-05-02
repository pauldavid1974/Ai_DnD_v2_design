package com.pauldavid74.ai_dnd.core.network.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class LLMProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    val icon: ImageVector,
    val description: String,
    val isCustom: Boolean = false
) {
    companion object {
        val ALL_PROVIDERS = listOf(
            LLMProvider("openai", "OpenAI", "https://api.openai.com/v1/", "gpt-4.5-preview", Icons.Default.AutoAwesome, "Industry standard high-performance models."),
            LLMProvider("gemini", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta/openai/", "gemini-1.5-pro", Icons.Default.Language, "Multimodal excellence from Google."),
            LLMProvider("anthropic", "Anthropic Claude", "https://api.anthropic.com/v1/", "claude-3-7-sonnet", Icons.Default.AutoStories, "Balanced, safe, and highly articulate."),
            LLMProvider("mistral", "Mistral AI", "https://api.mistral.ai/v1/", "mistral-large-latest", Icons.Default.Air, "Open-weight models with premium performance."),
            LLMProvider("groq", "Groq", "https://api.groq.com/openai/v1/", "llama-3.1-70b-versatile", Icons.Default.Speed, "Blazing fast inference speeds."),
            LLMProvider("perplexity", "Perplexity", "https://api.perplexity.ai/", "sonar-pro", Icons.Default.Search, "Optimized for search and retrieval."),
            LLMProvider("together", "Together AI", "https://api.together.xyz/v1/", "meta-llama/Llama-3.1-405B-Instruct-Turbo", Icons.Default.Hub, "Massive scale open-source infrastructure."),
            LLMProvider("deepseek", "DeepSeek", "https://api.deepseek.com/", "deepseek-chat", Icons.Default.Animation, "High-efficiency coding and chat models."),
            LLMProvider("openrouter", "OpenRouter", "https://openrouter.ai/api/v1/", "openrouter/auto", Icons.Default.Router, "Unified aggregator for hundreds of models."),
            LLMProvider("ollama_cloud", "Ollama Cloud", "https://ollama.com/v1", "gpt-oss:120b-cloud", Icons.Default.Cloud, "Decentralized cloud compute."),
            LLMProvider("huggingface", "Hugging Face", "https://api-inference.huggingface.co/v1/", "meta-llama/Llama-3.3-70B-Instruct", Icons.Default.Face, "The community hub for open source AI."),
            LLMProvider("siliconflow", "SiliconFlow", "https://api.siliconflow.cn/v1/", "deepseek-v3", Icons.Default.Memory, "Scalable AI infrastructure in China."),
            LLMProvider("custom", "Custom / Self-Hosted", "", "", Icons.Default.SettingsInputComponent, "LM Studio, Local Ollama, or private endpoints.", isCustom = true)
        )
    }
}
