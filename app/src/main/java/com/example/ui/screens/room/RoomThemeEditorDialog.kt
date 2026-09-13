package com.example.ui.screens.room

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.RoomBackgroundEffect
import com.example.data.model.RoomThemeConfig
import com.example.ui.theme.RoomThemeConstants
import com.example.ui.theme.RoomThemeEngine
import com.example.ui.theme.RoomThemePresets

val ACCENT_PALETTES = listOf(
  "#06B6D4" to "Cyan",
  "#8B5CF6" to "Purple",
  "#F43F5E" to "Rose",
  "#F59E0B" to "Gold",
  "#10B981" to "Emerald",
  "#3B82F6" to "Electric Blue",
  "#D946EF" to "Magenta",
  "#14B8A6" to "Teal",
  "#F97316" to "Sunset Amber"
)

@Composable
fun RoomThemeEditorDialog(
  currentThemeId: String,
  currentConfig: RoomThemeConfig?,
  roomLevel: Int,
  canEdit: Boolean = true,
  onDismiss: () -> Unit,
  onSaveTheme: (themeId: String, config: RoomThemeConfig) -> Unit
) {
  var selectedThemeId by remember { mutableStateOf(currentThemeId) }
  val initialPreset = RoomThemePresets.getPreset(selectedThemeId)

  var primaryHex by remember {
    mutableStateOf(currentConfig?.primaryAccentHex ?: String.format("#%06X", (0xFFFFFF and initialPreset.primaryAccent.value.toInt())))
  }
  var secondaryHex by remember {
    mutableStateOf(currentConfig?.secondaryAccentHex ?: String.format("#%06X", (0xFFFFFF and initialPreset.secondaryAccent.value.toInt())))
  }
  var bgIntensity by remember { mutableStateOf(currentConfig?.backgroundIntensity ?: 0.85f) }
  var gradIntensity by remember { mutableStateOf(currentConfig?.gradientIntensity ?: 0.70f) }
  var glowIntensity by remember { mutableStateOf(currentConfig?.glowIntensity ?: 0.80f) }
  var glassTransp by remember { mutableStateOf(currentConfig?.glassTransparency ?: 0.65f) }
  var borderBright by remember { mutableStateOf(currentConfig?.borderBrightness ?: 0.50f) }
  var ambientParticles by remember { mutableStateOf(currentConfig?.ambientParticles ?: true) }
  var backgroundEffect by remember { mutableStateOf(currentConfig?.backgroundEffect ?: RoomBackgroundEffect.AURORA) }

  // Compute live preview theme
  val livePreviewConfig = RoomThemeConfig(
    themeId = selectedThemeId,
    primaryAccentHex = primaryHex,
    secondaryAccentHex = secondaryHex,
    backgroundIntensity = bgIntensity,
    gradientIntensity = gradIntensity,
    glowIntensity = glowIntensity,
    glassTransparency = glassTransp,
    borderBrightness = borderBright,
    ambientParticles = ambientParticles,
    backgroundEffect = backgroundEffect
  )
  val previewResolved = RoomThemeEngine.resolveTheme(selectedThemeId, livePreviewConfig)

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0E1A)),
      border = androidx.compose.foundation.BorderStroke(1.dp, previewResolved.primaryAccent.copy(alpha = 0.5f)),
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.92f)
        .testTag("room_theme_editor_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        // Header
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎨", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = if (canEdit) "Voice Room Theme Editor" else "Voice Room Theme (View Only)",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = if (canEdit) "Custom theme for this voice room only" else "Only Room Owner & Admins can change theme",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f)
              )
            }
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // LIVE MINI PREVIEW
        Surface(
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, previewResolved.surfaceGlassBorder),
          modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .testTag("theme_live_preview_card")
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(previewResolved.backgroundBrush)
              .padding(12.dp)
          ) {
            Column(
              verticalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxSize()
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Surface(
                  color = previewResolved.primaryAccent.copy(alpha = 0.25f),
                  shape = RoundedCornerShape(8.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, previewResolved.primaryAccent)
                ) {
                  Text(
                    text = "LIVE PREVIEW: ${previewResolved.preset.name}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = previewResolved.primaryAccent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                  )
                }

                Text(
                  text = "Effect: ${backgroundEffect.name}",
                  fontSize = 10.sp,
                  color = Color.White.copy(alpha = 0.8f)
                )
              }

              // Mini voice seats row preview
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
              ) {
                // Speaking User Mini Avatar
                Box(contentAlignment = Alignment.Center) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(CircleShape)
                      .border(2.dp, previewResolved.speakingRingColor, CircleShape)
                      .background(previewResolved.surfaceGlass)
                  ) {
                    Text(
                      text = "🎙️",
                      fontSize = 18.sp,
                      modifier = Modifier.align(Alignment.Center)
                    )
                  }
                }

                // Empty Seat Mini Placeholder
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(previewResolved.seatItemEmptyBg)
                    .border(1.dp, previewResolved.seatItemEmptyBorder, CircleShape)
                ) {
                  Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = previewResolved.primaryAccent,
                    modifier = Modifier.size(16.dp)
                  )
                }

                // Mini Preview Action Pill
                Surface(
                  color = previewResolved.buttonActiveBg,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.padding(start = 8.dp)
                ) {
                  Text(
                    text = "Active Button",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SCROLLABLE CONFIGURATION CONTROLS
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
        ) {
          // 1. SELECT BASE PRESET
          Text(
            text = "1. Base Room Theme Presets",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(6.dp))

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(RoomThemePresets.PRESETS, key = { it.id }) { preset ->
              val isUnlocked = preset.isUnlocked(roomLevel)
              val isSelected = selectedThemeId == preset.id

              Surface(
                color = if (isSelected) preset.primaryAccent.copy(alpha = 0.2f) else Color(0xFF141724),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                  width = if (isSelected) 2.dp else 1.dp,
                  color = if (isSelected) preset.primaryAccent else Color.White.copy(alpha = 0.1f)
                ),
                modifier = Modifier
                  .width(130.dp)
                  .clickable {
                    if (isUnlocked) {
                      selectedThemeId = preset.id
                      primaryHex = String.format("#%06X", (0xFFFFFF and preset.primaryAccent.value.toInt()))
                      secondaryHex = String.format("#%06X", (0xFFFFFF and preset.secondaryAccent.value.toInt()))
                    }
                  }
                  .testTag("theme_preset_${preset.id}")
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Box(
                      modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(preset.primaryAccent)
                    )
                    if (!isUnlocked) {
                      Text("🔒 LV.${preset.requiredLevel}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    } else if (isSelected) {
                      Icon(Icons.Default.Check, contentDescription = null, tint = preset.primaryAccent, modifier = Modifier.size(14.dp))
                    }
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = preset.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.4f),
                    maxLines = 1
                  )
                  Text(
                    text = if (preset.requiredLevel == 1) "Free" else "Room Level ${preset.requiredLevel}",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.5f)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            color = Color(0x18FFFFFF),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
              Text(
                text = "⭐ Current Room Level: $roomLevel • Themes 1-3 are Free; Level 2 unlocks Cyber Violet; Level 3 unlocks Cosmic Gold.",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.75f)
              )
            }
          }

          if (!canEdit) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
              color = Color(0x1FFFFFFF),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text("🔒", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Theme Customization Locked",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                  )
                  Text(
                    text = "Only the room owner and appointed administrators can customize or save themes for this room. All participants experience the theme set by the room host.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                  )
                }
              }
            }
          }

          if (canEdit) {
            Spacer(modifier = Modifier.height(16.dp))

            // 2. PRIMARY ACCENT COLOR CHIPS
            Text(
              text = "2. Primary Accent Color",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          Spacer(modifier = Modifier.height(6.dp))
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(ACCENT_PALETTES) { (hex, name) ->
              val color = RoomThemeEngine.safeParseColor(hex, Color.Cyan)
              val isSelected = primaryHex.equals(hex, ignoreCase = true)
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(color)
                  .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                  .clickable { primaryHex = hex }
              ) {
                if (isSelected) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 3. SECONDARY ACCENT COLOR CHIPS
          Text(
            text = "3. Secondary Accent Color",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(6.dp))
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            items(ACCENT_PALETTES) { (hex, name) ->
              val color = RoomThemeEngine.safeParseColor(hex, Color.Magenta)
              val isSelected = secondaryHex.equals(hex, ignoreCase = true)
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(color)
                  .border(if (isSelected) 2.5.dp else 1.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                  .clickable { secondaryHex = hex }
              ) {
                if (isSelected) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // 4. BACKGROUND EFFECT
          Text(
            text = "4. Background Ambient Effect",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            RoomBackgroundEffect.values().forEach { eff ->
              FilterChip(
                selected = backgroundEffect == eff,
                onClick = { backgroundEffect = eff },
                label = { Text(eff.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = previewResolved.primaryAccent,
                  selectedLabelColor = Color.White
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // 5. SLIDERS
          Text(
            text = "5. Fine-Tuning & Glow",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Glow Intensity
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Speaking Glow Intensity", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
            Text("${(glowIntensity * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = previewResolved.primaryAccent)
          }
          Slider(
            value = glowIntensity,
            onValueChange = { glowIntensity = it },
            valueRange = 0.1f..1.0f,
            colors = SliderDefaults.colors(
              thumbColor = previewResolved.primaryAccent,
              activeTrackColor = previewResolved.primaryAccent
            )
          )

          // Glass Transparency
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Glass Opacity", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
            Text("${(glassTransp * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = previewResolved.primaryAccent)
          }
          Slider(
            value = glassTransp,
            onValueChange = { glassTransp = it },
            valueRange = 0.2f..0.95f,
            colors = SliderDefaults.colors(
              thumbColor = previewResolved.primaryAccent,
              activeTrackColor = previewResolved.primaryAccent
            )
          )

          // Border Brightness
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Border Brightness", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
            Text("${(borderBright * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = previewResolved.primaryAccent)
          }
          Slider(
            value = borderBright,
            onValueChange = { borderBright = it },
            valueRange = 0.1f..1.0f,
            colors = SliderDefaults.colors(
              thumbColor = previewResolved.primaryAccent,
              activeTrackColor = previewResolved.primaryAccent
            )
          )

          // Background Intensity
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Background Intensity", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
            Text("${(bgIntensity * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = previewResolved.primaryAccent)
          }
          Slider(
            value = bgIntensity,
            onValueChange = { bgIntensity = it },
            valueRange = 0.4f..1.0f,
            colors = SliderDefaults.colors(
              thumbColor = previewResolved.primaryAccent,
              activeTrackColor = previewResolved.primaryAccent
            )
          )

            // Ambient Particles Switch
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column {
                Text("Ambient Particle Stars", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text("Subtle floating ambient stars in room background", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
              }
              Switch(
                checked = ambientParticles,
                onCheckedChange = { ambientParticles = it }
              )
            }

            Spacer(modifier = Modifier.height(16.dp))
          }
        }

        // FOOTER ACTIONS
        if (!canEdit) {
          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = previewResolved.primaryAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("close_theme_btn")
          ) {
            Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
          }
        } else {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            // Reset to Preset
            OutlinedButton(
              onClick = {
                val preset = RoomThemePresets.getPreset(selectedThemeId)
                primaryHex = String.format("#%06X", (0xFFFFFF and preset.primaryAccent.value.toInt()))
                secondaryHex = String.format("#%06X", (0xFFFFFF and preset.secondaryAccent.value.toInt()))
                bgIntensity = 0.85f
                gradIntensity = 0.70f
                glowIntensity = 0.80f
                glassTransp = 0.65f
                borderBright = 0.50f
                ambientParticles = true
                backgroundEffect = RoomBackgroundEffect.AURORA
              },
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Reset Preset", fontSize = 11.sp)
            }

            Row {
              TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
              }

              Spacer(modifier = Modifier.width(8.dp))

              Button(
                onClick = {
                  onSaveTheme(selectedThemeId, livePreviewConfig)
                },
                colors = ButtonDefaults.buttonColors(containerColor = previewResolved.primaryAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_theme_btn")
              ) {
                Text("Save Theme ✨", fontWeight = FontWeight.Bold, color = Color.White)
              }
            }
          }
        }
      }
    }
  }
}
