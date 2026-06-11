package com.xinto.aliuplugins

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.aliucord.patcher.InsteadHook
import com.aliucord.patcher.after
import com.aliucord.patcher.before
import com.aliucord.patcher.PreHook
import com.xinto.aliuplugins.nitrospoof.*
import com.discord.app.AppFragment
import com.discord.models.domain.emoji.ModelEmojiCustom
import com.discord.models.message.Message
import com.discord.restapi.RestAPIParams
import com.discord.widgets.chat.list.actions.`WidgetChatListActions$onViewCreated$2`
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter
import com.discord.api.message.embed.MessageEmbed
import com.discord.stores.StoreStream
import de.robv.android.xposed.XC_MethodHook
import java.lang.reflect.Field
import java.net.URL

// Savior of this project: https://gitdab.com/Juby210/discord-jadx/

@AliucordPlugin
class NitroSpoof : Plugin() {

    private val reflectionCache = HashMap<String, Field>()
    private var reactionsListOpen = false

    override fun start(context: Context) {
        patcher.before<ModelEmojiCustom>("isUsable") { param ->
            if (!reactionsListOpen) {
                param.result = true
            }
        }

        patcher.before<ModelEmojiCustom>("isAvailable") { param ->
            if (!reactionsListOpen) {
                param.result = true
            }
        }

        patcher.before<`WidgetChatListActions$onViewCreated$2`>(
            "invoke"
        ) { _ ->
            reactionsListOpen = true
        }

        patcher.before<WidgetChatListAdapter>(
            "onQuickAddReactionClicked", Long::class.java
        ) { _ ->
            reactionsListOpen = true
        }

        patcher.after<AppFragment>(
            "onDetach"
        ) { _ ->
            reactionsListOpen = false
        }

        val messageCtor = Message::class.java.declaredConstructors.firstOrNull {
            !it.isSynthetic
        } ?: throw IllegalStateException("Didn't find Message ctor")

        patcher.patch(messageCtor, PreHook { param ->
            if (settings.getBool(REALMOJI_KEY, REALMOJI_DEFAULT) && param.args[4] != null) {
                var markdownRegex: Regex
                var directURLRegex: Regex

                if (settings.getBool(COMPOUND_SENTENCES_KEY, COMPOUND_SENTENCES_DEFAULT)) {
                    markdownRegex = Regex("""\[(?:[a-zA-Z0-9_~]+?|\u2236[a-zA-Z0-9_~]+?\u2236|.|\u200b|\u180c)\]\((https:\/\/cdn\.discordapp\.com\/emojis\/(\d+)\.([a-z]{3,4})?[^\)\(\[\]]*?)\)""")
                    directURLRegex = Regex("""(https:\/\/cdn\.discordapp\.com\/emojis\/(\d+)\.([a-z]{3,4})[A-Za-z=\d&%_\?]*)""")
                } else {
                    markdownRegex = Regex("""^\[(?:[a-zA-Z0-9_~]+?|\u2236[a-zA-Z0-9_~]+?\u2236|.|\u200b|\u180c)\]\((https:\/\/cdn\.discordapp\.com\/emojis\/(\d+)\.([a-z]{3,4})?[^\)\(\[\]]*?)\)$""")
                    directURLRegex = Regex("""^(https:\/\/cdn\.discordapp\.com\/emojis\/(\d+)\.([a-z]{3,4})[A-Za-z=\d&%_\?]*)$""")
                }

                val oldEmbeds = param.args[12] as List<MessageEmbed>
                val newEmbeds = ArrayList<MessageEmbed>(oldEmbeds)

                fun replace(match: MatchResult): CharSequence {
                    val url = match.groupValues[1]
                    val emojiId = match.groupValues[2]
                    val extension = match.groupValues[3]

                    var animated = if (extension == "gif") "a" else ""
                    var emojiName = "UNKNOWN_FAKE_EMOJI"

                    URL(url).query?.split("&")?.forEach {
                        val pair = it.split("=")
                        if (extension == "webp" && pair[0] == "animated" && pair[1] == "true") {
                            animated = "a"
                        }
                        if (pair[0] == "name") {
                            emojiName = pair[1].takeWhile { it.isLetterOrDigit() || it == '_' }
                        }
                    }

                    newEmbeds.removeIf {
                        it.l().startsWith("https://cdn.discordapp.com/emojis/$emojiId")
                    }

                    return "<$animated:$emojiName:$emojiId>"
                }

                param.args[4] = markdownRegex.replace(param.args[4] as String) { replace (it) }
                param.args[4] = directURLRegex.replace(param.args[4] as String) { replace (it) }

                param.args[12] = newEmbeds
            }
        })

        patcher.before<ModelEmojiCustom>("getMessageContentReplacement") { param ->
            val isUsable = getCachedField<Boolean>("isUsable")
            val available = getCachedField<Boolean>("available")

            if (isUsable && available) {
                return@before
            }

            val emojiId = getCachedField<String>("idStr")
            val isAnimated = getCachedField<Boolean>("isAnimated")
            val emojiName = getCachedField<String>("name")

            if (settings.getBool(REALMOJI_KEY, REALMOJI_DEFAULT)) {
                val animated = if (isAnimated) "a" else ""
                param.result = "<$animated:FAKE_$emojiName:$emojiId>"
            } else {
                param.result = getMarkdown(emojiName, emojiId, isAnimated)
            }
        }

        val restApiMessageCtor = RestAPIParams.Message::class.java.declaredConstructors.firstOrNull {
            !it.isSynthetic
        } ?: throw IllegalStateException("Didn't find RestAPIParams.Message ctor")
        val restApiMessageContent = RestAPIParams.Message::class.java.getDeclaredField("content")
        restApiMessageContent.isAccessible = true

        patcher.patch(restApiMessageCtor, Hook { param ->
            val emojiRegex = Regex("""<(a)?:(FAKE_)?([a-zA-Z0-9_]+):(\d+)>""")
            var content = restApiMessageContent.get(param.thisObject)

            content = emojiRegex.replace(content as String) {
                val isFake = it.groupValues[2] == "FAKE_"
                if (!isFake) return@replace it.value
 
                val emojiName = it.groupValues[3]
                val emojiId = it.groupValues[4]

                return@replace getMarkdown(emojiName, emojiId, it.groupValues[1] == "a")
            }

            restApiMessageContent.set(param.thisObject, content)
        })

        val experiments = StoreStream.getExperiments()
        experiments.setOverride("2021-03_nitro_emoji_autocomplete_upsell_android", 1)
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
        val experiments = StoreStream.getExperiments()
        experiments.setOverride("2021-03_nitro_emoji_autocomplete_upsell_android", 0)
    }

    /**
     * Get a reflected field from cache or compute it if cache is absent
     * @param V type of the field value
     */
    private inline fun <reified V> Any.getCachedField(
        name: String,
        instance: Any? = this,
    ): V {
        val clazz = this::class.java
        return reflectionCache.computeIfAbsent(clazz.name + name) {
            clazz.getDeclaredField(name).also {
                it.isAccessible = true
            }
        }.get(instance) as V
    }

    init {
        settingsTab = SettingsTab(
            PluginSettings::class.java,
            SettingsTab.Type.PAGE
        ).withArgs(settings)
    }

    private fun getMarkdown(emojiName: String, emojiId: String, animated: Boolean): String {
        val emoteSize = try { settings.getInt(EMOTE_SIZE_KEY, EMOTE_SIZE_DEFAULT) } catch (_: Exception) { EMOTE_SIZE_DEFAULT }
        val animatedQuery = if (animated) "animated=true&" else ""
        var url = "https://cdn.discordapp.com/emojis/$emojiId.webp?${animatedQuery}quality=lossless&name=$emojiName&size=$emoteSize"

        // Format type selection - credit @nyxiereal - https://github.com/nyxiereal/AliucordPlugins/
        return when (settings.getString(FORMAT_KEY, FORMAT_DEFAULT)) {
            FORMAT_EXTENDED_MD -> "[\u2236$emojiName\u2236]($url)"
            FORMAT_MARKDOWN -> "[$emojiName]($url)"
            FORMAT_ZERO_WIDTH_JOINER -> "[\u180c]($url)"
            else -> url
        }
    }
}
