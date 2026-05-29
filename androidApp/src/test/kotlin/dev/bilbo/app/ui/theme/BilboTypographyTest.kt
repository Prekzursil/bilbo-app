package dev.bilbo.app.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Pure-JVM smoke test for the [BilboTypography] table — exercises the file's
 * top-level property initialiser so Kover counts each TextStyle line.
 */
class BilboTypographyTest {
    @Test
    fun `displayLarge is bold default sans 57sp`() {
        val s = BilboTypography.displayLarge
        assertEquals(FontFamily.Default, s.fontFamily)
        assertEquals(FontWeight.Bold, s.fontWeight)
        assertEquals(57.sp, s.fontSize)
    }

    @Test
    fun `headline and title styles use semibold or medium`() {
        assertEquals(FontWeight.SemiBold, BilboTypography.headlineLarge.fontWeight)
        assertEquals(FontWeight.SemiBold, BilboTypography.headlineMedium.fontWeight)
        assertEquals(FontWeight.SemiBold, BilboTypography.headlineSmall.fontWeight)
        assertEquals(FontWeight.SemiBold, BilboTypography.titleLarge.fontWeight)
        assertEquals(FontWeight.Medium, BilboTypography.titleMedium.fontWeight)
        assertEquals(FontWeight.Medium, BilboTypography.titleSmall.fontWeight)
    }

    @Test
    fun `body and label styles are configured`() {
        assertEquals(16.sp, BilboTypography.bodyLarge.fontSize)
        assertEquals(14.sp, BilboTypography.bodyMedium.fontSize)
        assertEquals(12.sp, BilboTypography.bodySmall.fontSize)
        assertEquals(14.sp, BilboTypography.labelLarge.fontSize)
        assertEquals(12.sp, BilboTypography.labelMedium.fontSize)
        assertEquals(11.sp, BilboTypography.labelSmall.fontSize)
    }

    @Test
    fun `display sizes form a descending sequence`() {
        val sizes =
            listOf(
                BilboTypography.displayLarge.fontSize.value,
                BilboTypography.displayMedium.fontSize.value,
                BilboTypography.displaySmall.fontSize.value,
            )
        assertEquals(sizes.sortedDescending(), sizes)
    }
}
