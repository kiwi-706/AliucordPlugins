package com.xinto.aliuplugins.nitrospoof

import android.annotation.SuppressLint
import android.text.InputType
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.content.Context
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.views.Button
import com.aliucord.views.TextInput
import com.aliucord.views.Divider
import com.discord.views.CheckedSetting
import com.lytefast.flexinput.R

class PluginSettings(
    private val settingsAPI: SettingsAPI
) : SettingsPage() {

    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View) {
        super.onViewBound(view)

        val context = requireContext()

        setActionBarTitle("NitroSpoof")

        addHeader(context, "Emote Size")

        val emoteSizes = listOf(16, 20, 22, 24, 28, 32, 40, 44, 48, 56, 60, 64, 80, 96, 128)
        val emoteSizeSlider = SeekBar(context)
        emoteSizeSlider.setMin(0)
        emoteSizeSlider.setMax(emoteSizes.size - 1)

        var currentIndex = emoteSizes.indexOf(
            try { settingsAPI.getInt(EMOTE_SIZE_KEY, EMOTE_SIZE_DEFAULT) } catch (_: Exception) { EMOTE_SIZE_DEFAULT }
        )
        if (currentIndex == -1) {
            settingsAPI.setInt(EMOTE_SIZE_KEY, EMOTE_SIZE_DEFAULT)
            currentIndex = emoteSizes.indexOf(EMOTE_SIZE_DEFAULT)
        }
        emoteSizeSlider.setProgress(currentIndex)

        addView(emoteSizeSlider)

        val emoteSizeValue = addText(context, "")
        fun updateEmoteSizeValueText(index: Int) {
            emoteSizeValue.setText("Selected Size: ${emoteSizes[index]} px\nDefault Size: $EMOTE_SIZE_DEFAULT px")
        }
        updateEmoteSizeValueText(currentIndex)

        emoteSizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateEmoteSizeValueText(progress)
                settingsAPI.setInt(EMOTE_SIZE_KEY, emoteSizes[progress])
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        addView(Divider(context).apply {})

        addHeader(context, "Realmoji")
        addText(context, "Note: Restart Aliucord to apply Realmoji settings\nDefaults:\n- Realmoji: ON\n- Transform Compound Sentences: OFF")

        val compoundSentencesSetting = Utils.createCheckedSetting(
            context, CheckedSetting.ViewType.SWITCH, "Transform Compound Sentences",
            "Transform fake emojis in compound sentences (messages that aren't just one emoji)"
        ).apply {
            isChecked = settingsAPI.getBool(COMPOUND_SENTENCES_KEY, COMPOUND_SENTENCES_DEFAULT)
            setOnCheckedListener {
                if (settingsAPI.getBool(REALMOJI_KEY, REALMOJI_DEFAULT)) {
                    settingsAPI.setBool(COMPOUND_SENTENCES_KEY, it)
                } else {
                    setChecked(false)
                }
            }
        }

        addView(Utils.createCheckedSetting(
            context, CheckedSetting.ViewType.SWITCH, "Transform Emojis (Realmoji)",
            "Transform fake emojis into real ones, but only on your side"
        ).apply {
            isChecked = settingsAPI.getBool(REALMOJI_KEY, REALMOJI_DEFAULT)
            setOnCheckedListener {
                settingsAPI.setBool(REALMOJI_KEY, it)
                if (!it) {
                    compoundSentencesSetting.setChecked(it)
                    settingsAPI.setBool(COMPOUND_SENTENCES_KEY, it)
                }
            }
        })
        addView(compoundSentencesSetting)

        addView(Divider(context).apply {})

        addHeader(context, "Markdown Options")
        addText(context, "Default: Markdown")

        // Format type selection - credit @nyxiereal - https://github.com/nyxiereal/AliucordPlugins/
        val formatOptions = listOf(
            Triple(FORMAT_DIRECT_URL, "Direct URL", "https://cdn.discordapp.com/emojis/..."),
            Triple(FORMAT_MARKDOWN, "Markdown", "[emoji](https://cdn.discordapp...)"),
            Triple(FORMAT_ZERO_WIDTH_JOINER, "ZWJ (Zero Width Joiner)", "[\u180c](https://cdn.discordapp...)"),
            Triple(FORMAT_EXTENDED_MD, "\"Extended\" Markdown", "[\u2236emoji\u2236](https://cdn.discordapp...)"),
        )

        var selectedFormat = settingsAPI.getString(FORMAT_KEY, FORMAT_DEFAULT)

        val radioButtons = mutableListOf<CheckedSetting>()

        formatOptions.forEach { (value, label, subtext) ->
            val radio = Utils.createCheckedSetting(
                context,
                CheckedSetting.ViewType.RADIO,
                label,
                subtext
            ).apply {
                isChecked = value == selectedFormat
            }

            radio.setOnCheckedListener { isChecked ->
                if (isChecked) {
                    selectedFormat = value
                    settingsAPI.setString(FORMAT_KEY, selectedFormat)
                    // Uncheck all other radio buttons
                    radioButtons.forEach { button ->
                        if (button != radio) {
                            button.isChecked = false
                        }
                    }
                }
            }

            radioButtons.add(radio)
            addView(radio)
        }

        addView(Divider(context).apply {})

        addHeader(context, "WebP Options")
        addText(context, "Default: Never use WebP")

        val radioEnableWebp = Utils.createCheckedSetting(
            context, CheckedSetting.ViewType.RADIO, "Always use WebP",
            "All official clients will see the animated version of all animated emojis you send.\n\nHowever, Aliucord users that do not have \"Realmoji\" option enabled in either this fork of NitroSpoof or nyxiereal's FreeNitroEmojis will only see a non-animated version of any animated emojis you send."
        )

        val radioDisableWebp = Utils.createCheckedSetting(
            context, CheckedSetting.ViewType.RADIO, "Never use WebP",
            "Ensures maximum compatibility with Aliucord users. However, some animated emojis may only be available as WebP, and will not be visible at all on any client."
        )

        addView(radioDisableWebp.apply {
            isChecked = !settingsAPI.getBool(FORCE_WEBP_KEY, FORCE_WEBP_DEFAULT)
            setOnCheckedListener {
                settingsAPI.setBool(FORCE_WEBP_KEY, false)
                radioEnableWebp.isChecked = false
            }
        })

        addView(radioEnableWebp.apply {
            isChecked = settingsAPI.getBool(FORCE_WEBP_KEY, FORCE_WEBP_DEFAULT)
            setOnCheckedListener {
                settingsAPI.setBool(FORCE_WEBP_KEY, true)
                radioDisableWebp.isChecked = false
            }
        })
    }

    fun addText(context: Context, text: String): TextView {
        val textView = TextView(context, null, 0, R.i.UiKit_Settings_Item_SubText)
        textView.setText(text)
        addView(textView)
        return textView
    }
}