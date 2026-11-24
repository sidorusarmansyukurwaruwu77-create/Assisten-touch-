package com.example

import com.example.touchflow.minimax.models.MiniWindowPosition
import com.example.touchflow.minimax.models.MiniWindowSize
import com.example.touchflow.minimax.models.OemSkinType
import com.example.touchflow.models.ActionItem
import org.junit.Assert.*
import org.junit.Test

/**
 * Local JVM unit tests for TouchFlow and MiniMax Assistant models.
 */
class ExampleUnitTest {
  @Test
  fun miniWindowEnums_resolveCorrectly() {
    assertEquals(MiniWindowPosition.TOP_LEFT, MiniWindowPosition.fromId("top_left"))
    assertEquals(MiniWindowPosition.TOP_RIGHT, MiniWindowPosition.fromId("unknown_pos"))
    assertEquals(MiniWindowSize.COMPACT, MiniWindowSize.fromId("compact"))
    assertEquals(0.70f, MiniWindowSize.BALANCED.scaleFraction, 0.001f)
  }

  @Test
  fun oemSkinAndActionIntegration_exist() {
    assertEquals("TECNO HiOS", OemSkinType.TECNO_HIOS.displayName)
    assertEquals(
      ActionItem.MINIMAX_ASSISTANT,
      ActionItem.fromId("minimax_assistant", ActionItem.NONE)
    )
    assertTrue(ActionItem.getGestureAssignableActions().contains(ActionItem.MINIMAX_ASSISTANT))
  }
}
