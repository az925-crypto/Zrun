package com.zaaam.Zmusic.ui.zrun

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Ikon ZRun sebagai ImageVector, viewport 24x24.
 * Ikon garis: stroke 1.8, cap & join round, tanpa fill.
 * Ikon isi: fill penuh, tanpa stroke.
 * Warna di-set via tint saat dipakai (dibangun dengan hitam).
 */
object ZRIcons {
    private fun outline(vararg d: String): ImageVector {
        val b = ImageVector.Builder("zr", 24.dp, 24.dp, 24f, 24f)
        d.forEach {
            b.addPath(
                pathData = addPathNodes(it),
                pathFillType = PathFillType.NonZero,
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        return b.build()
    }

    private fun solid(vararg d: String): ImageVector {
        val b = ImageVector.Builder("zr", 24.dp, 24.dp, 24f, 24f)
        d.forEach {
            b.addPath(
                pathData = addPathNodes(it),
                pathFillType = PathFillType.NonZero,
                fill = SolidColor(Color.Black)
            )
        }
        return b.build()
    }

    val Home: ImageVector = outline("M3 11l9-8 9 8v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z")

    val Music: ImageVector = outline(
        "M9 18V5l11-2v13",
        "M3 18a3 3 0 1 0 6 0a3 3 0 1 0-6 0",
        "M14 16a3 3 0 1 0 6 0a3 3 0 1 0-6 0"
    )

    val Flame: ImageVector = outline(
        "M12 2c.6 3.6 5.6 5.4 5.6 11a5.6 5.6 0 0 1-11.2 0c0-2.6 1.6-4 2.6-6.2.9 1 1.8 1.6 2.4 1.6-.4-2.4-.2-4.4.6-6.4z"
    )

    val Feed: ImageVector = outline("M4 6h16M4 12h16M4 18h9")

    val User: ImageVector = outline(
        "M8 8a4 4 0 1 0 8 0a4 4 0 1 0-8 0",
        "M4 21c0-4 3.6-6 8-6s8 2 8 6"
    )

    val Search: ImageVector = outline(
        "M4 11a7 7 0 1 0 14 0a7 7 0 1 0-14 0",
        "M20 20l-4-4"
    )

    val ChevronDown: ImageVector = outline("M6 9l6 6 6-6")

    val Play: ImageVector = solid("M7 4.5v15l12.5-7.5z")

    val Pause: ImageVector = solid("M7 4h3.5v16H7zM13.5 4H17v16h-3.5z")

    val Next: ImageVector = solid("M5 5v14l10-7zM17 5h3v14h-3z")

    val Prev: ImageVector = solid("M19 5v14L9 12zM4 5h3v14H4z")
}
