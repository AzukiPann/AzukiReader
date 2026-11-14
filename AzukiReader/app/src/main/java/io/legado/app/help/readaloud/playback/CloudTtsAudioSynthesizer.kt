package io.legado.app.help.readaloud.playback

import io.legado.app.domain.gateway.CloudTtsEngineGateway
import io.legado.app.domain.model.readaloud.CloudTtsSynthesisRequest
import io.legado.app.domain.model.readaloud.CloudTtsEngine
import io.legado.app.domain.model.readaloud.CloudTtsVoiceConfig
import io.legado.app.domain.model.readaloud.CloudTtsVoiceDescriptor
import io.legado.app.domain.model.readaloud.CharacterPerformanceProfile
import io.legado.app.domain.model.readaloud.ReadAloudVoice
import io.legado.app.domain.model.readaloud.SpeechRoleType
import io.legado.app.utils.GSON
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runInterruptible

class CloudTtsAudioSynthesizer(
    private val engineGateway: CloudTtsEngineGateway,
) {
    private val registry = CloudTtsProviderRegistry()

    suspend fun fetchVoices(engineId: String): List<CloudTtsVoiceDescriptor> =
        withContext(Dispatchers.IO) {
            val engine = engineGateway.get(engineId) ?: error("云 TTS 引擎不存在")
            require(engine.enabled) { "云 TTS 引擎已停用" }
            registry.get(engine.provider).fetchVoices(engine)
        }

    suspend fun fetchVoices(engine: CloudTtsEngine): List<CloudTtsVoiceDescriptor> =
        withContext(Dispatchers.IO) {
            registry.get(engine.provider).fetchVoices(engine)
        }

    suspend fun synthesize(
        engine: CloudTtsEngine,
        request: CloudTtsSynthesisRequest,
        output: File,
    ): Boolean = withContext(Dispatchers.IO) {
        val audio = runInterruptible { synthesizeAudio(engine, request) }
        output.parentFile?.mkdirs()
        output.writeBytes(audio.bytes)
        output.length() > 0
    }

    suspend fun synthesize(
        voice: ReadAloudVoice,
        text: String,
        output: File,
        styleOverride: String = "",
        characterPerformance: CharacterPerformanceProfile? = null,
        roleType: SpeechRoleType = SpeechRoleType.Unknown,
    ): Boolean = withContext(Dispatchers.IO) {
        val engine = engineGateway.get(voice.engineId) ?: return@withContext false
        if (!engine.enabled) return@withContext false
        val config = runCatching {
            GSON.fromJson(voice.traitsJson, CloudTtsVoiceConfig::class.java)
        }.getOrNull() ?: CloudTtsVoiceConfig()
        val supportedStyles = runCatching {
            GSON.fromJson(voice.emotionCatalogJson, Array<String>::class.java).toSet()
        }.getOrDefault(emptySet())
        val emotionControl = if (config.automaticEmotion != false) {
            CloudTtsEmotionMapper.map(engine.provider, styleOverride)
        } else {
            CloudTtsEmotionControl()
        }
        val mappedStyle = emotionControl.style.takeIf { it in supportedStyles }
        val characterInstruction = if (config.characterPersonality != false) {
            CharacterPerformanceInstructionBuilder.build(engine.provider, characterPerformance)
        } else ""
        val roleInstruction = if (config.thoughtPerformance != false) {
            CloudTtsRoleInstructionMapper.map(engine.provider, roleType)
        } else ""
        val instructions = listOfNotNull(
            config.instructions.takeIf(String::isNotBlank),
            characterInstruction.takeIf(String::isNotBlank),
            roleInstruction.takeIf(String::isNotBlank),
            emotionControl.instruction.takeIf(String::isNotBlank),
        ).joinToString(" ")
        val audio = runInterruptible { synthesizeAudio(
            engine = engine,
            request = CloudTtsSynthesisRequest(
                text = text,
                voiceId = voice.speakerId,
                locale = config.locale,
                style = mappedStyle ?: config.style,
                role = config.role,
                instructions = instructions,
                speed = config.speed,
                pitch = config.pitch,
                volume = config.volume,
                format = config.format,
                sampleRate = config.sampleRate,
            ),
        ) }
        output.parentFile?.mkdirs()
        output.writeBytes(audio.bytes)
        output.length() > 0
    }
    private fun synthesizeAudio(engine: CloudTtsEngine, request: CloudTtsSynthesisRequest): io.legado.app.domain.model.readaloud.CloudTtsAudio {
        val provider = registry.get(engine.provider)
        try { return provider.synthesize(engine, request) }
        catch (failure: Wenku8SpeechFailure) {
            if (!failure.contentFailure || request.text.length <= 100) throw failure
            // Independent MP3 frames can be concatenated; retain the original queue/UTF-16 cursor.
            val parts = io.legado.app.domain.model.readaloud.wenku8SpeechSegments(request.text, 80)
            if (parts.size <= 1) throw failure
            val result = java.io.ByteArrayOutputStream()
            for (part in parts) {
                val audio = provider.synthesize(engine, request.copy(text = part.text))
                if (result.size() + audio.bytes.size > 8 * 1024 * 1024) throw failure
                result.write(audio.bytes)
            }
            return io.legado.app.domain.model.readaloud.CloudTtsAudio(result.toByteArray(), "mp3")
        }
    }

}
