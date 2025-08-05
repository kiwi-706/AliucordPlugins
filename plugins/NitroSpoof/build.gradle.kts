description = "Use all emotes in any server without Nitro."
version = "1.1.0"

aliucord.changelog.set("""
# 1.1.0
* (Experimental) Added option to "transform compound sentences"
* Reworked message replacement patches - this should reduce flashing
* Links are no longer inserted directly into the textbox. Emojis now only convert to links right before message is sent.
* `2021-03_nitro_emoji_autocomplete_upsell_android` experiment is now enabled

# 1.0.14
* Reaction emoji picker now only shows valid emojis

# 1.0.13
* (poorly) Fixed usable emojis being spoofed

# 1.0.12
* Make spoofed emojis display as normal emoji
* Use markdown hyperlinks instead of raw emoji URLs
* The troll is gone - saving over 4 kB of your precious storage
  
Note: Originally developed by Xinto
""".trimIndent())

aliucord {
  author("Xinto", 423915768191647755L)
}
