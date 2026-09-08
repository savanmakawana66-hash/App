package com.example

import com.example.data.model.RoomCategory
import com.example.ui.viewmodel.BottomNavTab
import com.example.ui.viewmodel.MainViewModel
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testGuestUserCreation() {
    val viewModel = MainViewModel()
    val user = viewModel.currentUser.value
    assertTrue("User should default to guest", user.isGuest)
    assertTrue("User ID should start with USR-", user.id.startsWith("USR-"))
    assertTrue("Username should start with Guest_", user.username.startsWith("Guest_"))
  }

  @Test
  fun testRoomListSeedingAndFiltering() {
    val viewModel = MainViewModel()
    val rooms = viewModel.rooms.value
    assertTrue("Should have initial rooms seeded", rooms.isNotEmpty())
    assertEquals("Should have 5 rooms seeded", 5, rooms.size)

    // Filter category
    viewModel.selectCategory(RoomCategory.WATCH_TOGETHER)
    val filtered = viewModel.filteredRooms.value
    assertTrue("Filtered rooms should match category", filtered.all { it.category == RoomCategory.WATCH_TOGETHER })

    // Reset
    viewModel.selectCategory(RoomCategory.ALL)
  }

  @Test
  fun testBottomNavigationTabs() {
    val viewModel = MainViewModel()
    assertEquals(BottomNavTab.ROOM, viewModel.currentTab.value)

    viewModel.selectTab(BottomNavTab.DM)
    assertEquals(BottomNavTab.DM, viewModel.currentTab.value)

    viewModel.selectTab(BottomNavTab.PROFILE)
    assertEquals(BottomNavTab.PROFILE, viewModel.currentTab.value)
  }

  @Test
  fun testRoomJoiningAndEightVoiceSeats() {
    val viewModel = MainViewModel()

    // Join LOFI882 where seats 2-7 are open and video control is EVERYONE
    val joined = viewModel.joinRoom("LOFI882")
    assertTrue("Should successfully join room", joined)
    assertEquals("Current active room should be LOFI882", "LOFI882", viewModel.currentActiveRoom.value?.id)

    // Verify 8 voice seats exist
    val seats = viewModel.roomSeats.value
    assertEquals("Room must strictly have 8 seats", 8, seats.size)

    // User is automatically seated in seat 1
    assertEquals(viewModel.currentUser.value.id, viewModel.roomSeats.value[1].userId)

    // Toggle mic
    viewModel.toggleMicMute()
    assertTrue("Seat mic state should be toggled", viewModel.roomSeats.value[1].isMuted)

    // Leave seat
    viewModel.leaveSeat()
    assertNull("Seat 1 should be vacant after leaving", viewModel.roomSeats.value[1].userId)

    // Take seat 2 (which is vacant)
    viewModel.takeSeat(2)
    assertEquals(viewModel.currentUser.value.id, viewModel.roomSeats.value[2].userId)

    // Leave room
    viewModel.leaveRoom()
    assertNull("Current active room should be null after leaving", viewModel.currentActiveRoom.value)
  }

  @Test
  fun testSynchronizedVideoControls() {
    val viewModel = MainViewModel()
    // Join LOFI882 which has VideoControlMode.EVERYONE
    viewModel.joinRoom("LOFI882")
    assertTrue("User should be able to control video in EVERYONE mode", viewModel.canControlVideo())

    val initialPlaying = viewModel.currentActiveRoom.value?.isVideoPlaying ?: false
    viewModel.togglePlayPause()
    assertEquals(!initialPlaying, viewModel.currentActiveRoom.value?.isVideoPlaying)

    viewModel.seekVideo(120f)
    assertEquals(120f, viewModel.currentActiveRoom.value?.activeVideoPosSec ?: 0f, 0.01f)

    viewModel.forward10()
    assertEquals(130f, viewModel.currentActiveRoom.value?.activeVideoPosSec ?: 0f, 0.01f)

    viewModel.rewind10()
    assertEquals(120f, viewModel.currentActiveRoom.value?.activeVideoPosSec ?: 0f, 0.01f)

    viewModel.leaveRoom()
  }

  @Test
  fun testOtpGuestConversion() {
    val viewModel = MainViewModel()
    viewModel.verifyOtp("+1 555 123 4567", "123456")

    val updatedUser = viewModel.currentUser.value
    assertFalse("User should no longer be a guest after verification", updatedUser.isGuest)
    assertTrue("User should be verified", updatedUser.isVerified)
    assertEquals("+1 555 123 4567", updatedUser.phoneNumber)
  }
}
