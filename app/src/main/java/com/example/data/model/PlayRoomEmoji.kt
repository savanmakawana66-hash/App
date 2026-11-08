package com.example.data.model

import java.util.concurrent.ConcurrentHashMap

enum class EmojiCategory(val id: String, val title: String, val icon: String) {
  HAPPY("happy", "Happy", "😄"),
  LOVE("love", "Love", "💖"),
  SAD("sad", "Sad", "🥺"),
  FUNNY("funny", "Funny", "🤣"),
  ANGRY("angry", "Angry", "🔥"),
  GAMING("gaming", "Gaming", "🎮"),
  REACTIONS("reactions", "Reactions", "⚡"),
  SPECIAL("special", "Special", "👑")
}

data class PlayRoomEmoji(
  val code: String,
  val name: String,
  val category: EmojiCategory,
  val visualSymbol: String,
  val secondarySymbol: String = "",
  val accentColor: String = "#8B5CF6",
  val isAnimated: Boolean = false,
  val isExclusive: Boolean = false,
  val unlockLevel: Int = 1,
  val packName: String = "PlayRoom Originals"
)

data class PlayRoomEmojiPack(
  val id: String,
  val name: String,
  val icon: String,
  val isPremium: Boolean = false,
  val emojis: List<PlayRoomEmoji>
)

object PlayRoomEmojiRegistry {
  private val emojiMap = ConcurrentHashMap<String, PlayRoomEmoji>()
  private val packMap = ConcurrentHashMap<String, PlayRoomEmojiPack>()

  init {
    loadDefaultPlayRoomEmojis()
  }

  private fun loadDefaultPlayRoomEmojis() {
    val defaults = listOf(
      // 1. HAPPY
      PlayRoomEmoji(":pr_cyber_smile:", "Cyber Smile", EmojiCategory.HAPPY, "✨🤖", "🤖", "#3B82F6", isAnimated = true),
      PlayRoomEmoji(":pr_star_beam:", "Star Beam", EmojiCategory.HAPPY, "⭐💫", "⭐", "#F59E0B"),
      PlayRoomEmoji(":pr_neon_joy:", "Neon Joy", EmojiCategory.HAPPY, "🌈✨", "🌈", "#10B981"),
      PlayRoomEmoji(":pr_golden_grin:", "Golden Grin", EmojiCategory.HAPPY, "👑😁", "👑", "#FBBF24"),

      // 2. LOVE
      PlayRoomEmoji(":pr_heart_flame:", "Heart Flame", EmojiCategory.LOVE, "❤️‍🔥💎", "❤️‍🔥", "#EC4899", isAnimated = true),
      PlayRoomEmoji(":pr_cosmic_kiss:", "Cosmic Kiss", EmojiCategory.LOVE, "💋🌌", "💋", "#9333EA"),
      PlayRoomEmoji(":pr_sparkle_love:", "Sparkle Love", EmojiCategory.LOVE, "💖✨", "💖", "#F43F5E"),
      PlayRoomEmoji(":pr_angel_heart:", "Angel Heart", EmojiCategory.LOVE, "🤍🪽", "🤍", "#E2E8F0"),

      // 3. SAD
      PlayRoomEmoji(":pr_rain_tear:", "Rain Tear", EmojiCategory.SAD, "🌧️💧", "🌧️", "#60A5FA"),
      PlayRoomEmoji(":pr_broken_heart:", "Neon Shatter", EmojiCategory.SAD, "💔⚡", "💔", "#94A3B8"),
      PlayRoomEmoji(":pr_cloudy_sigh:", "Cloudy Sigh", EmojiCategory.SAD, "☁️🥺", "☁️", "#A1A1AA"),

      // 4. FUNNY
      PlayRoomEmoji(":pr_laugh_burst:", "Laugh Burst", EmojiCategory.FUNNY, "🤣💥", "🤣", "#EAB308", isAnimated = true),
      PlayRoomEmoji(":pr_clown_party:", "Clown Party", EmojiCategory.FUNNY, "🤡🎈", "🤡", "#F97316"),
      PlayRoomEmoji(":pr_rofl_neon:", "ROFL Neon", EmojiCategory.FUNNY, "💀🤣", "💀", "#A855F7"),

      // 5. ANGRY
      PlayRoomEmoji(":pr_dragon_fury:", "Dragon Fury", EmojiCategory.ANGRY, "🐲🔥", "🐲", "#EF4444", isAnimated = true),
      PlayRoomEmoji(":pr_rage_skull:", "Rage Skull", EmojiCategory.ANGRY, "💀⚡", "💀", "#DC2626"),
      PlayRoomEmoji(":pr_volcano_blast:", "Volcano Blast", EmojiCategory.ANGRY, "🌋💥", "🌋", "#B91C1C"),

      // 6. GAMING
      PlayRoomEmoji(":pr_game_win:", "Victory Royale", EmojiCategory.GAMING, "🏆🎮", "🏆", "#10B981", isAnimated = true),
      PlayRoomEmoji(":pr_chess_mate:", "Checkmate King", EmojiCategory.GAMING, "♟️👑", "♟️", "#3B82F6"),
      PlayRoomEmoji(":pr_dice_lucky:", "Lucky Sixes", EmojiCategory.GAMING, "🎲✨", "🎲", "#F59E0B"),
      PlayRoomEmoji(":pr_game_over:", "GG Play", EmojiCategory.GAMING, "👾🕹️", "👾", "#6366F1"),

      // 7. REACTIONS
      PlayRoomEmoji(":pr_hype_rocket:", "Hype Rocket", EmojiCategory.REACTIONS, "🚀🔥", "🚀", "#F97316", isAnimated = true),
      PlayRoomEmoji(":pr_mind_blown:", "Cosmic Galaxy", EmojiCategory.REACTIONS, "🤯🌌", "🤯", "#8B5CF6"),
      PlayRoomEmoji(":pr_fire_claps:", "Fire Claps", EmojiCategory.REACTIONS, "👏🔥", "👏", "#F59E0B"),
      PlayRoomEmoji(":pr_shield_vip:", "VIP Verified", EmojiCategory.REACTIONS, "🛡️💎", "🛡️", "#06B6D4"),

      // 8. SPECIAL (VIP & Exclusive)
      PlayRoomEmoji(":pr_royal_crown:", "Imperial Crown", EmojiCategory.SPECIAL, "👑✨", "👑", "#F59E0B", isAnimated = true, isExclusive = true, unlockLevel = 5),
      PlayRoomEmoji(":pr_galaxy_ring:", "Eternal Galaxy", EmojiCategory.SPECIAL, "💍🌌", "💍", "#8B5CF6", isAnimated = true, isExclusive = true, unlockLevel = 10),
      PlayRoomEmoji(":pr_gold_phoenix:", "Golden Phoenix", EmojiCategory.SPECIAL, "🦅🔥", "🦅", "#EAB308", isExclusive = true, unlockLevel = 15)
    )

    defaults.forEach { emojiMap[it.code] = it }

    val defaultPack = PlayRoomEmojiPack(
      id = "pack_originals",
      name = "PlayRoom Originals",
      icon = "✨",
      isPremium = false,
      emojis = defaults
    )
    packMap[defaultPack.id] = defaultPack
  }

  fun getAllEmojis(): List<PlayRoomEmoji> = emojiMap.values.toList()

  fun getByCategory(category: EmojiCategory): List<PlayRoomEmoji> =
    emojiMap.values.filter { it.category == category }

  fun findByCode(code: String): PlayRoomEmoji? = emojiMap[code]

  fun getPacks(): List<PlayRoomEmojiPack> = packMap.values.toList()

  fun registerEmojiPack(pack: PlayRoomEmojiPack) {
    packMap[pack.id] = pack
    pack.emojis.forEach { emojiMap[it.code] = it }
  }

  fun addEmoji(emoji: PlayRoomEmoji) {
    emojiMap[emoji.code] = emoji
  }

  /**
   * Helper that checks if a message contains any custom Play Room emojis.
   */
  fun containsCustomEmoji(text: String): Boolean {
    return emojiMap.keys.any { text.contains(it) }
  }

  /**
   * Extracts all custom emoji codes present in the text.
   */
  fun extractEmojiCodes(text: String): List<String> {
    return emojiMap.keys.filter { text.contains(it) }
  }
}
