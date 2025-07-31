package com.xinto.aliuplugins

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.aliucord.patcher.InsteadHook
import com.aliucord.patcher.after
import com.xinto.aliuplugins.nitrospoof.EMOTE_SIZE_DEFAULT
import com.xinto.aliuplugins.nitrospoof.EMOTE_SIZE_KEY
import com.xinto.aliuplugins.nitrospoof.PluginSettings
import com.discord.models.domain.emoji.ModelEmojiCustom
import com.discord.models.message.Message
import com.discord.restapi.RestAPIParams
import de.robv.android.xposed.XC_MethodHook
import java.lang.reflect.Field

// Savior of this project: https://gitdab.com/Juby210/discord-jadx/

@AliucordPlugin
class NitroSpoof : Plugin() {

    private val reflectionCache = HashMap<String, Field>()

    override fun start(context: Context) {
        patcher.patch(
            ModelEmojiCustom::class.java.getDeclaredMethod("getChatInputText"),
            Hook { getChatReplacement(it) }
        )
        patcher.patch(
            ModelEmojiCustom::class.java.getDeclaredMethod("getMessageContentReplacement"),
            Hook { getChatReplacement(it) }
        )
        patcher.patch(
            ModelEmojiCustom::class.java.getDeclaredMethod("isUsable"),
            InsteadHook { true }
        )
        patcher.patch(
            ModelEmojiCustom::class.java.getDeclaredMethod("isAvailable"),
            InsteadHook { true }
        )

        patcher.after<Message>("getContent") { param ->
            val markdownRegex = Regex("""^\[[a-zA-Z0-9_~]+?\]\(https:\/\/cdn\.discordapp\.com\/emojis\/(\d+)\.([a-z]{3,4})?[^\)\(\[\]]*?name=([a-zA-Z0-9_]+)[^\)\(\[\]]*?\)$""")
            val markdownMatch = markdownRegex.find(param.result as String, 0)

            markdownMatch?.let {
                val emojiId = it.groupValues[1]
                val animated = if (it.groupValues[2] == "gif") "a" else ""
                val emojiName = it.groupValues[3]
                param.result = "<$animated:$emojiName:$emojiId>"
                embeds.clear();
            }
        }

        patcher.after<RestAPIParams.Message>("getContent") { param ->
            val emojiRegex = Regex("""^<(a)?:([a-zA-Z0-9_]+):(\d+)>$""")
            val emojiMatch = emojiRegex.find(param.result as String, 0)

            emojiMatch?.let {
                val emojiId = it.groupValues[3]
                val emojiName = it.groupValues[2]
                val emojiExtension = if (it.groupValues[1] == "a") "gif" else "png"
                val emoteSize = settings.getString(EMOTE_SIZE_KEY, EMOTE_SIZE_DEFAULT).toIntOrNull()
                param.result = "[$emojiName](https://cdn.discordapp.com/emojis/$emojiId.$emojiExtension?quality=lossless&name=$emojiName&size=$emoteSize)"
            }
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }

    private fun getChatReplacement(callFrame: XC_MethodHook.MethodHookParam) {
        val thisObject = callFrame.thisObject as ModelEmojiCustom
        val isUsable = thisObject.getCachedField<Boolean>("isUsable")
        val available = thisObject.getCachedField<Boolean>("available")

        if (isUsable && available) {
            callFrame.result = callFrame.result
            return
        }

        var finalUrl = "https://cdn.discordapp.com/emojis/"

        val idStr = thisObject.getCachedField<String>("idStr")
        val isAnimated = thisObject.getCachedField<Boolean>("isAnimated")
        val emoteName = thisObject.getCachedField<String>("name")

        finalUrl += idStr
        val emoteSize = settings.getString(EMOTE_SIZE_KEY, EMOTE_SIZE_DEFAULT).toIntOrNull()

        finalUrl += (if (isAnimated) ".gif" else ".png") + "?quality=lossless&name=" + emoteName

        if (emoteSize != null) {
            finalUrl += "&size=${emoteSize}"
        }
        
        callFrame.result = "[" + emoteName + "]" + "(" + finalUrl + ")"
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
}
