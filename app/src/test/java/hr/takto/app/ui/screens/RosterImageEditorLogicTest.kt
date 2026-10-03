package hr.takto.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RosterImageEditorLogicTest {
    @Test
    fun moveKeepsCropInsideImageBounds() {
        val start = CropSelection(left = 0.10f, top = 0.20f, right = 0.90f, bottom = 0.60f)
        val moved = updateCrop(start, CropDragMode.MOVE, 0.50f, 0.60f)

        assertEquals(0.20f, moved.left, 0.0001f)
        assertEquals(1.00f, moved.right, 0.0001f)
        assertEquals(0.60f, moved.top, 0.0001f)
        assertEquals(1.00f, moved.bottom, 0.0001f)
    }

    @Test
    fun leftEdgeCanResizeWithoutMovingOppositeEdge() {
        val start = CropSelection(left = 0.10f, top = 0.20f, right = 0.90f, bottom = 0.60f)
        val resized = updateCrop(start, CropDragMode.LEFT, 0.15f, 0f)

        assertEquals(0.25f, resized.left, 0.0001f)
        assertEquals(0.90f, resized.right, 0.0001f)
        assertEquals(start.top, resized.top, 0.0001f)
        assertEquals(start.bottom, resized.bottom, 0.0001f)
    }

    @Test
    fun topAndBottomEdgesRespectMinimumHeight() {
        val start = CropSelection(left = 0.10f, top = 0.20f, right = 0.90f, bottom = 0.60f)
        val top = updateCrop(start, CropDragMode.TOP, 0f, 0.50f)
        val bottom = updateCrop(start, CropDragMode.BOTTOM, 0f, -0.50f)

        assertTrue(top.height >= 0.08f)
        assertTrue(bottom.height >= 0.08f)
    }
}
