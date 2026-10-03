package io.github.shmemcat.waterminder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.sin

private val outline = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private fun DrawScope.filled(path: Path, color: Color, width: Float = 4.5f) {
    drawPath(path, color)
    drawPath(path, Palette.Ink, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
private fun path(block: Path.() -> Unit) = Path().apply(block)
private fun DrawScope.line(a: Offset, b: Offset, width: Float = 4.5f, color: Color = Palette.Ink) = drawLine(color, a, b, width, StrokeCap.Round)

/** Original vector artwork, sharing shmemplay's pastel fills and rounded charcoal outlines. */
@Composable
fun PlantIllustration(watering: Float, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Canvas(modifier.semantics {
        contentDescription = if (watering > 0f && watering < 1f) "A happy little plant being watered" else "A smiling little plant in a peach pot"
    }) {
        val unit = minOf(size.width / 340f, size.height / 280f)
        translate((size.width - 340 * unit) / 2f, (size.height - 280 * unit) / 2f) {
            scale(unit, unit, Offset.Zero) {
                drawOval(palette.Floor, Offset(91f, 241f), Size(160f, 18f))
                drawCircle(palette.SagePale, 87f, Offset(170f, 144f))
                drawCircle(Palette.Lavender.copy(alpha = .55f), 18f, Offset(75f, 99f))
                line(Offset(263f, 144f), Offset(263f, 158f), 3f, Palette.Peach)
                line(Offset(256f, 151f), Offset(270f, 151f), 3f, Palette.Peach)
                drawCircle(Palette.Blue, 4f, Offset(97f, 183f))
                val wave = if (watering in 0.15f..0.98f) sin(watering * PI.toFloat() * 5) * 5f else 0f
                rotate(wave, Offset(171f, 194f)) {
                    val stem = path { moveTo(171f, 194f); cubicTo(160f, 155f, 178f, 111f, 169f, 83f) }
                    drawPath(stem, palette.Ink, style = outline)
                    filled(path {
                        moveTo(169f, 103f); cubicTo(148f, 104f, 132f, 81f, 141f, 58f)
                        cubicTo(169f, 58f, 186f, 81f, 169f, 103f); close()
                    }, Palette.Sage)
                    drawPath(path { moveTo(169f, 103f); quadraticTo(156f, 81f, 149f, 72f) }, Palette.Ink, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    filled(path {
                        moveTo(170f, 133f); cubicTo(169f, 109f, 194f, 92f, 218f, 101f)
                        cubicTo(218f, 125f, 195f, 142f, 170f, 133f); close()
                    }, Color(0xFFC7DCAA))
                    drawPath(path { moveTo(170f, 133f); quadraticTo(191f, 116f, 206f, 111f) }, Palette.Ink, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    filled(path {
                        moveTo(167f, 166f); cubicTo(141f, 175f, 118f, 157f, 119f, 135f)
                        cubicTo(146f, 129f, 167f, 145f, 167f, 166f); close()
                    }, Palette.Sage)
                    drawPath(path { moveTo(167f, 166f); quadraticTo(147f, 150f, 132f, 146f) }, Palette.Ink, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                filled(path {
                    moveTo(127f, 190f); lineTo(213f, 190f); lineTo(204f, 239f)
                    quadraticTo(203f, 247f, 194f, 247f); lineTo(147f, 247f)
                    quadraticTo(139f, 247f, 138f, 239f); close()
                }, Palette.Peach)
                drawRoundRect(Palette.Peach, Offset(122f, 183f), Size(97f, 18f), CornerRadius(6f))
                drawRoundRect(Palette.Ink, Offset(122f, 183f), Size(97f, 18f), CornerRadius(6f), style = outline)
                drawCircle(Palette.Ink, 3f, Offset(158f, 220f))
                drawCircle(Palette.Ink, 3f, Offset(183f, 220f))
                drawCircle(Color(0xFFDDA18D), 5f, Offset(151f, 228f))
                drawCircle(Color(0xFFDDA18D), 5f, Offset(190f, 228f))
                drawPath(path { moveTo(165f, 226f); quadraticTo(171f, 232f, 177f, 226f) }, Palette.Ink, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                if (watering > .03f && watering < .88f) {
                    val fade = minOf(watering / .13f, (.88f - watering) / .13f, 1f).coerceAtLeast(0f)
                    rotate(-15f, Offset(249f, 66f)) {
                        filled(path { moveTo(232f, 57f); lineTo(263f, 57f); lineTo(267f, 86f); quadraticTo(248f, 94f, 231f, 86f); close() }, Palette.Lavender.copy(alpha = fade), 3.5f)
                        drawPath(path { moveTo(263f, 62f); cubicTo(285f, 52f, 288f, 87f, 268f, 82f) }, palette.Ink.copy(alpha = fade), style = outline)
                        filled(path { moveTo(232f, 66f); lineTo(214f, 57f); lineTo(209f, 66f); lineTo(233f, 80f); close() }, Palette.Lavender.copy(alpha = fade), 3.5f)
                    }
                    repeat(5) { i ->
                        val fall = ((watering * 4 + i * .19f) % 1f)
                        val x = 204f - fall * 34f + (i % 2) * 8f
                        val y = 80f + fall * 99f
                        line(Offset(x, y), Offset(x - 2f, y + 6f), 3.5f, Palette.Blue.copy(alpha = fade))
                    }
                }
                if (watering > .48f && watering < 1f) {
                    val rise = (watering - .48f) / .52f
                    val alpha = sin(rise * PI.toFloat()).coerceAtLeast(0f)
                    listOf(Offset(109f, 116f), Offset(225f, 154f), Offset(187f, 57f)).forEach { point ->
                        translate(point.x, point.y - rise * 24f) {
                            drawPath(path {
                                moveTo(0f, 3f); cubicTo(-11f, -5f, -7f, -12f, 0f, -7f)
                                cubicTo(7f, -12f, 11f, -5f, 0f, 3f); close()
                            }, Palette.Peach.copy(alpha = alpha))
                        }
                    }
                }
            }
        }
    }
}

enum class Doodle { Drop, Settings, Glass, Clock, Moon, Sun, Back, Bell }

@Composable
fun DoodleIcon(icon: Doodle, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val ink = if (icon == Doodle.Settings || icon == Doodle.Back) palette.Ink else Palette.Ink
    Canvas(modifier) {
        val unit = minOf(size.width, size.height) / 24f
        translate((size.width - 24 * unit) / 2, (size.height - 24 * unit) / 2) {
            scale(unit, unit, Offset.Zero) {
                val pen = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                fun draw(p: Path, fill: Color? = null) {
                    if (fill != null) drawPath(p, fill)
                    drawPath(p, ink, style = pen)
                }
                when (icon) {
                    Doodle.Drop -> draw(path { moveTo(12f, 3f); cubicTo(10f, 6f, 5f, 10f, 5f, 14f); cubicTo(5f, 23f, 19f, 23f, 19f, 14f); cubicTo(19f, 10f, 14f, 6f, 12f, 3f); close() }, Palette.Blue)
                    Doodle.Glass -> {
                        draw(path { moveTo(6f, 3f); lineTo(18f, 3f); lineTo(16f, 21f); lineTo(8f, 21f); close() }, Palette.Blue)
                        drawPath(path { moveTo(7f, 11f); quadraticTo(10f, 8f, 12f, 11f); quadraticTo(15f, 13f, 17f, 10f) }, Palette.Ink, style = pen)
                    }
                    Doodle.Settings -> {
                        listOf(6f, 12f, 18f).forEach { y -> line(Offset(3f, y), Offset(21f, y), 1.8f, ink) }
                        listOf(Offset(9f, 6f), Offset(16f, 12f), Offset(7f, 18f)).forEach {
                            drawCircle(palette.SettingsSurface, 2.4f, it); drawCircle(ink, 2.4f, it, style = pen)
                        }
                    }
                    Doodle.Clock -> {
                        drawCircle(Palette.Lavender, 8.5f, Offset(12f, 12f)); drawCircle(Palette.Ink, 8.5f, Offset(12f, 12f), style = pen)
                        drawPath(path { moveTo(12f, 7f); lineTo(12f, 12f); lineTo(15.5f, 14f) }, Palette.Ink, style = pen)
                    }
                    Doodle.Moon -> draw(path { moveTo(15f, 3f); cubicTo(2f, 0f, 0f, 20f, 13f, 21f); quadraticTo(19f, 22f, 22f, 15f); cubicTo(12f, 18f, 9f, 8f, 15f, 3f); close() }, Palette.Lavender)
                    Doodle.Sun -> {
                        drawCircle(Palette.Peach, 5f, Offset(12f, 12f)); drawCircle(Palette.Ink, 5f, Offset(12f, 12f), style = pen)
                        repeat(8) { i -> rotate(i * 45f, Offset(12f, 12f)) { line(Offset(12f, 1f), Offset(12f, 3f), 1.8f) } }
                    }
                    Doodle.Back -> draw(path { moveTo(14f, 5f); lineTo(7f, 12f); lineTo(14f, 19f); moveTo(7f, 12f); lineTo(21f, 12f) })
                    Doodle.Bell -> {
                        draw(path { moveTo(4f, 17f); lineTo(6f, 14f); lineTo(6f, 9f); cubicTo(6f, 1f, 18f, 1f, 18f, 9f); lineTo(18f, 14f); lineTo(20f, 17f); close() }, Palette.Peach)
                        drawPath(path { moveTo(9f, 21f); quadraticTo(12f, 23f, 15f, 21f) }, Palette.Ink, style = pen)
                    }
                }
            }
        }
    }
}

