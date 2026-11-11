package com.example.ui.screens.store

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.data.model.*
import com.example.ui.theme.*

enum class StoreSection(val title: String, val icon: String) {
  COINS("Coins", "🪙"),
  NAME_TAGS("Tags", "🏷️"),
  RINGS("Rings", "💍"),
  CHARMS("Charms", "✨"),
  INVENTORY("Inventory", "🎒")
}

@Composable
fun StoreScreen(
  currentUser: User,
  onPurchaseCoinPackage: (packageId: String) -> Unit,
  onOpenMysteryBox: () -> Unit,
  onEquipRing: (ringId: String?) -> Unit,
  onEquipNameTag: (tagLabel: String?) -> Unit,
  onEquipProfileFrame: (frameName: String) -> Unit,
  onEquipCharmBadge: (badge: String?) -> Unit
) {
  var selectedSection by remember { mutableStateOf(StoreSection.COINS) }
  val wallet = remember(currentUser.coins) {
    com.example.network.security.ServerCoinManager.getWalletState(currentUser.id)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    // Top Bar with Wallet Balance
    Surface(
      color = DarkSurfaceCard,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Virtual Store 💎",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
          )
          Text(
            text = "Official Play Room Store & Item Inventory",
            fontSize = 11.sp,
            color = TextSecondary
          )
        }

        // Live Coin Balance Pill
        Surface(
          color = Color(0x33F59E0B),
          shape = RoundedCornerShape(20.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text("🪙", fontSize = 15.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "%,d".format(wallet.balance),
              fontSize = 13.sp,
              fontWeight = FontWeight.ExtraBold,
              color = GoldAccent
            )
          }
        }
      }
    }

    // Category Tabs
    ScrollableTabRow(
      selectedTabIndex = selectedSection.ordinal,
      containerColor = DarkSurfaceElevated,
      contentColor = Color.White,
      edgePadding = 12.dp,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSection.ordinal]),
          height = 2.5.dp,
          color = GoldAccent
        )
      },
      divider = {}
    ) {
      StoreSection.values().forEach { section ->
        Tab(
          selected = selectedSection == section,
          onClick = { selectedSection = section },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(section.icon, fontSize = 13.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                section.title,
                fontSize = 13.sp,
                fontWeight = if (selectedSection == section) FontWeight.Bold else FontWeight.Medium,
                color = if (selectedSection == section) GoldAccent else TextSecondary
              )
            }
          },
          modifier = Modifier.testTag("tab_store_${section.name.lowercase()}")
        )
      }
    }

    // Tab Contents
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
      when (selectedSection) {
        StoreSection.COINS -> CoinStoreTab(
          onPurchase = onPurchaseCoinPackage
        )
        StoreSection.NAME_TAGS -> NameTagsTab(
          currentUser = currentUser,
          onEquip = onEquipNameTag
        )
        StoreSection.RINGS -> RingsTab(
          currentUser = currentUser,
          onEquip = onEquipRing
        )
        StoreSection.CHARMS -> CharmsTab(
          currentUser = currentUser,
          wallet = wallet,
          onOpenMysteryBox = onOpenMysteryBox,
          onEquipCharmBadge = onEquipCharmBadge
        )
        StoreSection.INVENTORY -> InventoryTab(
          currentUser = currentUser,
          onEquipRing = onEquipRing,
          onEquipNameTag = onEquipNameTag,
          onEquipFrame = onEquipProfileFrame,
          onEquipCharm = onEquipCharmBadge
        )
      }
    }
  }
}

// -------------------------------------------------------------
// 1. COIN PACKAGES TAB
// -------------------------------------------------------------
@Composable
private fun CoinStoreTab(
  onPurchase: (String) -> Unit
) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    item {
      Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(Color(0x33F59E0B)),
            contentAlignment = Alignment.Center
          ) {
            Text("🛡️", fontSize = 22.sp)
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = "Official Play Room Coin Store",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "Server-Authoritative • Instant Credit • Anti-Duplicate Guard",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }
      }
    }

    items(CoinPackage.values().toList()) { pkg ->
      Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          if (pkg.tag.isNotBlank()) GoldAccent.copy(alpha = 0.5f) else DarkCardBorderSubtle
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0x22F59E0B)),
              contentAlignment = Alignment.Center
            ) {
              Text("🪙", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "${pkg.title} Package",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                if (pkg.tag.isNotBlank()) {
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    color = GoldAccent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f))
                  ) {
                    Text(
                      text = pkg.tag,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = GoldAccent,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = "+%,d In-App Coins".format(pkg.coins),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = GoldAccent
              )
            }
          }

          Button(
            onClick = { onPurchase(pkg.id) },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (pkg.tag.isNotBlank()) GoldAccent else IndigoPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.testTag("btn_buy_${pkg.id}")
          ) {
            Text(
              text = "₹${pkg.priceInr}",
              fontSize = 13.sp,
              fontWeight = FontWeight.ExtraBold,
              color = if (pkg.tag.isNotBlank()) Color.Black else Color.White
            )
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 2. NAME TAGS TAB
// -------------------------------------------------------------
@Composable
private fun NameTagsTab(
  currentUser: User,
  onEquip: (String?) -> Unit
) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    item {
      Text(
        text = "Social Relationship Name Tags (2,000 Coins)",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
      )
    }

    items(RelationshipTagType.values().toList()) { tag ->
      val isEquipped = currentUser.equippedNameTag == tag.label
      Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
          1.5.dp,
          if (isEquipped) GoldAccent else Color(android.graphics.Color.parseColor(tag.badgeColor)).copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tag.emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = tag.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(android.graphics.Color.parseColor(tag.badgeColor))
              )
              Text(
                text = "Displayed next to your name in rooms & chat",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }

          Button(
            onClick = { onEquip(if (isEquipped) null else tag.label) },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isEquipped) Color(0x33EF4444) else DarkSurfaceElevated
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
          ) {
            Text(
              text = if (isEquipped) "Unequip" else "Equip",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (isEquipped) Color(0xFFF87171) else Color.White
            )
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 3. PROFILE RINGS TAB
// -------------------------------------------------------------
@Composable
private fun RingsTab(
  currentUser: User,
  onEquip: (String?) -> Unit
) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    item {
      Text(
        text = "Couple Rings & Profile Glow Effects",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
      )
    }

    items(VirtualRing.ALL_RINGS) { ring ->
      val isEquipped = currentUser.equippedRingId == ring.id
      Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
          1.5.dp,
          if (isEquipped) GoldAccent else Color(android.graphics.Color.parseColor(ring.effectColor)).copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ring.emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = ring.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "${ring.animationDescription} • %,d Coins".format(ring.priceCoins),
                fontSize = 11.sp,
                color = GoldAccent
              )
            }
          }

          Button(
            onClick = { onEquip(if (isEquipped) null else ring.id) },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isEquipped) Color(0x33EF4444) else DarkSurfaceElevated
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
          ) {
            Text(
              text = if (isEquipped) "Unequip" else "Equip",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (isEquipped) Color(0xFFF87171) else Color.White
            )
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 4. CHARMS & MYSTERY BOX TAB
// -------------------------------------------------------------
@Composable
private fun CharmsTab(
  currentUser: User,
  wallet: CoinWalletState,
  onOpenMysteryBox: () -> Unit,
  onEquipCharmBadge: (String?) -> Unit
) {
  val charmInfo = remember(wallet.lifetimeCharmPoints) {
    CharmLevelInfo.calculateLevel(wallet.lifetimeCharmPoints)
  }

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    // Charm Status Card
    item {
      Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(charmInfo.tierBadge, fontSize = 32.sp)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Charm Level ${charmInfo.level} • ${charmInfo.tierName}",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color.White
                )
                Text(
                  text = "${wallet.lifetimeCharmPoints} Lifetime Points",
                  fontSize = 12.sp,
                  color = GoldAccent
                )
              }
            }

            val isEquipped = currentUser.equippedCharmBadge == charmInfo.tierBadge
            Button(
              onClick = { onEquipCharmBadge(if (isEquipped) null else charmInfo.tierBadge) },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isEquipped) Color(0x33EF4444) else GoldAccent
              ),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text(
                text = if (isEquipped) "Unequip" else "Equip Badge",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEquipped) Color(0xFFF87171) else Color.Black
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          val progress = ((wallet.lifetimeCharmPoints % 50).toFloat() / 50f).coerceIn(0f, 1f)
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = GoldAccent,
            trackColor = DarkSurfaceElevated
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "${50 - (wallet.lifetimeCharmPoints % 50)} points to Level ${charmInfo.level + 1}",
            fontSize = 11.sp,
            color = TextSecondary
          )
        }
      }
    }

    // Mystery Box Section
    item {
      Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, IndigoPrimary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("🎁", fontSize = 48.sp)
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Lucky Mystery Box",
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
          )
          Text(
            text = "Guaranteed virtual gift inside! (Costs 100 Coins)",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(16.dp))
          Button(
            onClick = onOpenMysteryBox,
            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_open_mystery_box")
          ) {
            Text(
              text = "Open Mystery Box (100 Coins) 🎁",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 5. USER INVENTORY TAB
// -------------------------------------------------------------
@Composable
private fun InventoryTab(
  currentUser: User,
  onEquipRing: (String?) -> Unit,
  onEquipNameTag: (String?) -> Unit,
  onEquipFrame: (String) -> Unit,
  onEquipCharm: (String?) -> Unit
) {
  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    // Currently Equipped Section
    item {
      Text(
        text = "Currently Equipped Virtual Items",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
      Spacer(modifier = Modifier.height(8.dp))
      Surface(
        color = DarkSurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          InventoryStatusRow(
            label = "Profile Frame",
            value = currentUser.profileFrame,
            icon = "🖼️"
          )
          InventoryStatusRow(
            label = "Couple Ring",
            value = currentUser.equippedRingId?.let { VirtualRing.findById(it)?.name } ?: "None",
            icon = "💍"
          )
          InventoryStatusRow(
            label = "Name Tag",
            value = currentUser.equippedNameTag ?: "None",
            icon = "🏷️"
          )
          InventoryStatusRow(
            label = "Charm Badge",
            value = currentUser.equippedCharmBadge ?: "None",
            icon = "✨"
          )
        }
      }
    }

    // Profile Frames
    item {
      Text(
        text = "Available Profile Frames",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
      Spacer(modifier = Modifier.height(8.dp))
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        currentUser.unlockedFrames.forEach { frame ->
          val isEquipped = currentUser.profileFrame == frame
          Surface(
            color = DarkSurfaceCard,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isEquipped) GoldAccent else DarkCardBorderSubtle
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(frame, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
              Button(
                onClick = { onEquipFrame(frame) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isEquipped) Color(0x33F59E0B) else DarkSurfaceElevated
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
              ) {
                Text(
                  text = if (isEquipped) "Equipped ✓" else "Equip",
                  fontSize = 11.sp,
                  color = if (isEquipped) GoldAccent else Color.White
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun InventoryStatusRow(label: String, value: String, icon: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(icon, fontSize = 16.sp)
      Spacer(modifier = Modifier.width(8.dp))
      Text(label, fontSize = 13.sp, color = TextSecondary)
    }
    Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
  }
}
