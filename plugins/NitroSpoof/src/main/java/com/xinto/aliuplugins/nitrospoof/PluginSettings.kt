package com.xinto.aliuplugins.nitrospoof

import android.annotation.SuppressLint
import android.text.InputType
import android.view.View
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.views.Button
import com.aliucord.views.TextInput
import com.aliucord.views.Divider
import com.discord.views.CheckedSetting

class PluginSettings(
    private val settingsAPI: SettingsAPI
) : SettingsPage() {

    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View) {
        super.onViewBound(view)

        val context = requireContext()

        setActionBarTitle("NitroSpoof")

        val textInput = TextInput(context).apply {
            setHint("Emote Size (leave empty for discord default)")
            editText.setText(settingsAPI.getString(EMOTE_SIZE_KEY, EMOTE_SIZE_DEFAULT).toString())
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.maxLines = 1
        }

        val saveButton = Button(context).apply {
            text = "Save"
            setOnClickListener {
                settingsAPI.setString(EMOTE_SIZE_KEY, textInput.editText.text.toString())
                Utils.showToast("Successfully saved!")
            }
        }

        addView(textInput)
        addView(saveButton)

        addView(Divider(context).apply {})

        addView(Utils.createCheckedSetting(
            context, CheckedSetting.ViewType.SWITCH, "Transform Compound Sentences",
            "[Restart Recommended]\nTransform fake emojis in compound sentences (messages that aren't just one emoji)"
        ).apply {
            isChecked = settingsAPI.getBool(COMPOUND_SENTENCES_KEY, COMPOUND_SENTENCES_DEFAULT)
            setOnCheckedListener {
                settingsAPI.setBool(COMPOUND_SENTENCES_KEY, it)
            }
        })

        addView(Divider(context).apply {})

        addHeader(context, "WebP Options")

        val radioEnableWebp = Utils.createCheckedSetting(
            context, CheckedSetting.ViewType.RADIO, "Always use WebP",
            "All official clients will see the animated version of all animated emojis you send.\n\nHowever, Aliucord users that do not have this version of NitroSpoof or nyxiereal's FreeNitroEmojis with \"Realmojis\" option enabled will only see a non-animated version of any animated emojis you send."
        )

        val radioDisableWebp = Utils.createCheckedSetting(
            context, CheckedSetting.ViewType.RADIO, "Never use WebP",
            "Ensures maximum compatibility with Aliucord users. However, some animated emojis are only available as WebP, and will not be visible at all on any client."
        )
        
        addView(radioEnableWebp.apply {
            isChecked = settingsAPI.getBool(FORCE_WEBP_KEY, FORCE_WEBP_DEFAULT)
            setOnCheckedListener {
                settingsAPI.setBool(FORCE_WEBP_KEY, true)
                radioDisableWebp.isChecked = false
            }
        })
        
        addView(radioDisableWebp.apply {
            isChecked = !settingsAPI.getBool(FORCE_WEBP_KEY, FORCE_WEBP_DEFAULT)
            setOnCheckedListener {
                settingsAPI.setBool(FORCE_WEBP_KEY, false)
                radioEnableWebp.isChecked = false
            }
        })
    }
}