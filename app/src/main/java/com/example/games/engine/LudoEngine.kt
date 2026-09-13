package com.example.games.engine

import com.example.data.model.games.*

object LudoEngine {

  val SAFE_POSITIONS = setOf(0, 8, 13, 21, 26, 34, 39, 47)

  fun createInitialState(players: List<GamePlayer>): LudoState {
    val ludoColors = listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE)
    val ludoPlayers = players.mapIndexed { index, p ->
      val color = ludoColors[index % ludoColors.size]
      LudoPlayer(
        userId = p.userId,
        username = p.username,
        color = color,
        tokens = List(4) { tIdx -> LudoToken(id = tIdx, color = color) }
      )
    }

    val firstPlayer = ludoPlayers.firstOrNull()

    return LudoState(
      players = ludoPlayers,
      currentTurnColor = firstPlayer?.color ?: LudoColor.RED,
      currentTurnPlayerId = firstPlayer?.userId ?: "",
      diceValue = 0,
      hasRolledDice = false,
      consecutiveSixes = 0,
      canRollDice = true,
      movableTokenIds = emptyList(),
      lastActionText = "Game ready. ${firstPlayer?.username ?: "Player"} to roll."
    )
  }

  fun rollDice(currentState: LudoState): Pair<LudoState, Int> {
    if (!currentState.canRollDice || currentState.hasRolledDice) {
      return Pair(currentState, currentState.diceValue)
    }

    val roll = (1..6).random()
    val consecutive = if (roll == 6) currentState.consecutiveSixes + 1 else 0

    // 3 consecutive sixes forfeits turn
    if (consecutive >= 3) {
      val nextState = passTurn(currentState).copy(
        diceValue = roll,
        lastActionText = "Rolled three 6s in a row! Turn skipped."
      )
      return Pair(nextState, roll)
    }

    val currentPlayer = currentState.players.firstOrNull { it.color == currentState.currentTurnColor }
    val movableTokenIds = if (currentPlayer != null) {
      getMovableTokens(currentPlayer, roll)
    } else emptyList()

    if (movableTokenIds.isEmpty()) {
      // No legal moves possible, pass turn after showing dice
      val afterNoMoves = passTurn(currentState.copy(
        diceValue = roll,
        hasRolledDice = true,
        consecutiveSixes = consecutive
      )).copy(
        lastActionText = "${currentPlayer?.username ?: "Player"} rolled $roll (No moves available)"
      )
      return Pair(afterNoMoves, roll)
    }

    val stateAfterRoll = currentState.copy(
      diceValue = roll,
      hasRolledDice = true,
      consecutiveSixes = consecutive,
      canRollDice = false,
      movableTokenIds = movableTokenIds,
      lastActionText = "${currentPlayer?.username ?: "Player"} rolled a $roll! Select a token."
    )

    return Pair(stateAfterRoll, roll)
  }

  private fun getMovableTokens(player: LudoPlayer, dice: Int): List<Int> {
    val movable = mutableListOf<Int>()
    for (token in player.tokens) {
      when (token.state) {
        LudoTokenState.BASE -> {
          if (dice == 6) movable.add(token.id)
        }
        LudoTokenState.TRACK -> {
          // Can always advance on track or enter home path
          movable.add(token.id)
        }
        LudoTokenState.HOME_PATH -> {
          // Can move only if not overshooting Home (step 6)
          if (token.homeSteps + dice <= 6) {
            movable.add(token.id)
          }
        }
        LudoTokenState.HOME -> {
          // Already completed
        }
      }
    }
    return movable
  }

  fun moveToken(
    currentState: LudoState,
    tokenId: Int
  ): LudoState {
    if (!currentState.hasRolledDice || tokenId !in currentState.movableTokenIds) {
      return currentState
    }

    val currentPlayer = currentState.players.firstOrNull { it.color == currentState.currentTurnColor }
      ?: return currentState

    val token = currentPlayer.tokens.firstOrNull { it.id == tokenId } ?: return currentState
    val dice = currentState.diceValue

    var capturedOpponent = false
    var actionNote = ""

    val updatedTokens = currentPlayer.tokens.map { t ->
      if (t.id != tokenId) t
      else {
        when (t.state) {
          LudoTokenState.BASE -> {
            actionNote = "${currentPlayer.username} brought token ${tokenId + 1} onto the board!"
            t.copy(
              state = LudoTokenState.TRACK,
              trackPosition = currentPlayer.color.startTrackPos,
              homeSteps = 0
            )
          }

          LudoTokenState.TRACK -> {
            val startPos = currentPlayer.color.startTrackPos
            // Calculate total steps moved from start
            val currentStepsFromStart = (t.trackPosition - startPos + 52) % 52
            val newStepsFromStart = currentStepsFromStart + dice

            if (newStepsFromStart >= 51) {
              // Enters Home Path
              val homePathStep = newStepsFromStart - 51
              if (homePathStep >= 6) {
                actionNote = "🎉 ${currentPlayer.username}'s token reached HOME!"
                t.copy(state = LudoTokenState.HOME, homeSteps = 6, trackPosition = -1)
              } else {
                actionNote = "${currentPlayer.username} entered the home straight!"
                t.copy(state = LudoTokenState.HOME_PATH, homeSteps = homePathStep, trackPosition = -1)
              }
            } else {
              val newTrackPos = (t.trackPosition + dice) % 52
              actionNote = "${currentPlayer.username} moved token to square $newTrackPos."
              t.copy(trackPosition = newTrackPos)
            }
          }

          LudoTokenState.HOME_PATH -> {
            val newStep = t.homeSteps + dice
            if (newStep == 6) {
              actionNote = "🎉 ${currentPlayer.username}'s token reached HOME!"
              t.copy(state = LudoTokenState.HOME, homeSteps = 6)
            } else if (newStep < 6) {
              actionNote = "${currentPlayer.username} advanced to step $newStep in home straight."
              t.copy(homeSteps = newStep)
            } else {
              t // Overshoot not permitted
            }
          }

          LudoTokenState.HOME -> t
        }
      }
    }

    val movedToken = updatedTokens.first { it.id == tokenId }

    // Check if movedToken captured any opponent token on track
    val updatedOtherPlayers = currentState.players.map { otherPlayer ->
      if (otherPlayer.color == currentPlayer.color) {
        otherPlayer.copy(tokens = updatedTokens)
      } else {
        if (movedToken.state == LudoTokenState.TRACK && movedToken.trackPosition !in SAFE_POSITIONS) {
          val newOtherTokens = otherPlayer.tokens.map { ot ->
            if (ot.state == LudoTokenState.TRACK && ot.trackPosition == movedToken.trackPosition) {
              capturedOpponent = true
              ot.copy(state = LudoTokenState.BASE, trackPosition = -1, homeSteps = 0)
            } else ot
          }
          otherPlayer.copy(tokens = newOtherTokens)
        } else {
          otherPlayer
        }
      }
    }

    val playerAfterMove = updatedOtherPlayers.first { it.color == currentPlayer.color }
    val isWinner = playerAfterMove.hasWon

    if (isWinner) {
      return currentState.copy(
        players = updatedOtherPlayers,
        winnerId = currentPlayer.userId,
        winnerName = currentPlayer.username,
        canRollDice = false,
        hasRolledDice = false,
        movableTokenIds = emptyList(),
        lastActionText = "🏆 Winner: ${currentPlayer.username} has finished all 4 tokens!"
      )
    }

    // Bonus roll if rolled a 6 or captured an opponent
    val hasBonusRoll = (dice == 6 || capturedOpponent)
    if (capturedOpponent) {
      actionNote += " 💥 Captured opponent token! Bonus roll awarded!"
    } else if (dice == 6) {
      actionNote += " 🎲 Rolled 6! Bonus roll awarded!"
    }

    return if (hasBonusRoll) {
      currentState.copy(
        players = updatedOtherPlayers,
        hasRolledDice = false,
        canRollDice = true,
        movableTokenIds = emptyList(),
        lastActionText = actionNote
      )
    } else {
      passTurn(currentState.copy(
        players = updatedOtherPlayers,
        lastActionText = actionNote
      ))
    }
  }

  fun passTurn(state: LudoState): LudoState {
    val players = state.players
    if (players.isEmpty()) return state

    val currentIndex = players.indexOfFirst { it.color == state.currentTurnColor }
    val nextIndex = (currentIndex + 1) % players.size
    val nextPlayer = players[nextIndex]

    return state.copy(
      currentTurnColor = nextPlayer.color,
      currentTurnPlayerId = nextPlayer.userId,
      diceValue = 0,
      hasRolledDice = false,
      consecutiveSixes = 0,
      canRollDice = true,
      movableTokenIds = emptyList(),
      lastActionText = "${nextPlayer.username}'s turn to roll."
    )
  }
}
