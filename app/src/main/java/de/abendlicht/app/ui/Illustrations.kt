package de.abendlicht.app.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import de.abendlicht.app.Scene
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/*
 * Handgezeichnete kleine Welten für jede Karte.
 * Alles ist in Einheiten der Kartenhöhe gebaut und am rechten Rand verankert,
 * damit links Platz für den Titel bleibt. `t` läuft von 0 bis 1 und wiederholt sich –
 * jede Bewegung ist so gewählt, dass sie nahtlos in sich zurückläuft.
 */

private const val TAU = (2 * PI).toFloat()
private fun osc(t: Float, k: Int, phase: Float = 0f) = sin(TAU * (k * t + phase))

private val Hair = Color(0xFF2F2A28)
private val Skin = Color(0xFFF2C6A2)
private val Bark = Color(0xFF8A6A4A)
private val White = Color.White

fun DrawScope.drawScene(scene: Scene, c: SceneColors, t: Float, width: Float = size.width) {
    when (scene) {
        Scene.WALK -> walk(c, t, width)
        Scene.READ -> read(c, t, width)
        Scene.JOURNAL -> journal(c, t, width)
        Scene.STRETCH -> stretch(c, t, width)
        Scene.CALM -> calm(c, t, width)
        Scene.GARDEN -> garden(c, t, width)
    }
}

/** Für die kleine Vorschau in den Einstellungen: Szene in Kartenproportion, rechter Teil sichtbar. */
fun DrawScope.drawScenePreview(scene: Scene, c: SceneColors, t: Float) {
    val h = size.height
    val vw = h * 2.3f
    drawRect(Brush.linearGradient(listOf(c.top, c.bottom), Offset.Zero, Offset(size.width, h)))
    translate(left = size.width - vw) {
        drawScene(scene, c, t, vw)
    }
}

// ── Helfer ────────────────────────────────────────────────────

private fun DrawScope.cloud(center: Offset, s: Float, color: Color) {
    drawCircle(color, s * 0.55f, center + Offset(-s * 0.55f, s * 0.12f))
    drawCircle(color, s * 0.75f, center + Offset(0f, -s * 0.1f))
    drawCircle(color, s * 0.5f, center + Offset(s * 0.62f, s * 0.15f))
    drawRoundRect(
        color, topLeft = center + Offset(-s * 1.05f, s * 0.05f), size = Size(s * 2.1f, s * 0.6f),
        cornerRadius = CornerRadius(s * 0.3f)
    )
}

private fun DrawScope.sparkle(center: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(center.x, center.y - r)
        quadraticTo(center.x, center.y, center.x + r, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + r)
        quadraticTo(center.x, center.y, center.x - r, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - r)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.glow(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)), center, radius),
        radius, center
    )
}

/** Blatt mit Spitze in Richtung `angle` (Grad, 0 = nach oben). */
private fun DrawScope.leaf(base: Offset, length: Float, width: Float, angle: Float, color: Color, vein: Color? = null) {
    rotate(angle, pivot = base) {
        val tip = Offset(base.x, base.y - length)
        val p = Path().apply {
            moveTo(base.x, base.y)
            quadraticTo(base.x + width, base.y - length * 0.55f, tip.x, tip.y)
            quadraticTo(base.x - width, base.y - length * 0.55f, base.x, base.y)
            close()
        }
        drawPath(p, color)
        if (vein != null) drawLine(vein, base, Offset(tip.x, tip.y + length * 0.2f), width * 0.12f, StrokeCap.Round)
    }
}

// ── Spazieren ─────────────────────────────────────────────────

private fun DrawScope.walk(c: SceneColors, t: Float, w: Float) {
    val h = size.height
    fun x(f: Float) = w - f * h

    // Sonne
    val sun = Offset(x(0.62f), h * 0.30f)
    glow(sun, h * 0.30f * (1f + 0.05f * osc(t, 2)), c.glow, 0.9f)
    drawCircle(c.c, h * 0.12f, sun)

    // Wolken ziehen langsam vorbei
    val span = w + h * 0.8f
    val c1 = (t + 0.25f) % 1f
    val c2 = (t * 2 + 0.7f) % 1f
    cloud(Offset(w + h * 0.4f - c1 * span, h * 0.2f), h * 0.07f, White.copy(alpha = 0.85f))
    cloud(Offset(w + h * 0.4f - c2 * span, h * 0.36f), h * 0.05f, White.copy(alpha = 0.7f))

    // Hügel
    val back = Path().apply {
        moveTo(0f, h); lineTo(0f, h * 0.8f)
        cubicTo(w * 0.3f, h * 0.64f, w * 0.52f, h * 0.86f, w * 0.74f, h * 0.62f)
        cubicTo(w * 0.86f, h * 0.5f, w * 0.94f, h * 0.56f, w, h * 0.52f)
        lineTo(w, h); close()
    }
    drawPath(back, c.a.copy(alpha = 0.75f))
    val front = Path().apply {
        moveTo(0f, h); lineTo(0f, h * 0.94f)
        cubicTo(w * 0.35f, h * 0.8f, w * 0.62f, h * 0.98f, w, h * 0.74f)
        lineTo(w, h); close()
    }
    drawPath(front, c.b)

    // Weg
    val path = Path().apply {
        moveTo(w * 0.5f, h * 1.05f)
        cubicTo(w * 0.72f, h * 0.95f, x(0.9f), h * 0.84f, x(0.48f), h * 0.74f)
    }
    drawPath(path, Color(0xFFF6E8C3), style = Stroke(h * 0.07f, cap = StrokeCap.Round))

    // Baum, der sich sanft im Wind wiegt
    val trunkBase = Offset(x(0.2f), h * 0.8f)
    drawLine(Bark, trunkBase, Offset(trunkBase.x, h * 0.48f), h * 0.05f, StrokeCap.Round)
    rotate(osc(t, 3) * 2.5f, pivot = trunkBase) {
        drawCircle(c.b, h * 0.13f, Offset(x(0.3f), h * 0.47f))
        drawCircle(c.b, h * 0.13f, Offset(x(0.1f), h * 0.47f))
        drawCircle(c.a, h * 0.17f, Offset(x(0.2f), h * 0.36f))
        drawCircle(Color.White.copy(alpha = 0.18f), h * 0.07f, Offset(x(0.25f), h * 0.3f))
        listOf(0.14f to 0.3f, 0.27f to 0.42f, 0.17f to 0.48f, 0.08f to 0.4f).forEach { (fx, fy) ->
            drawCircle(c.c, h * 0.018f, Offset(x(fx), h * fy))
        }
    }

    // Spaziergängerin
    val px = x(0.72f)
    val stepT = osc(t, 12)
    val bob = abs(stepT) * h * 0.012f
    translate(top = -bob) {
        val hip = Offset(px, h * 0.655f)
        val legLen = h * 0.13f
        for (side in listOf(1f, -1f)) {
            val a = side * stepT * 0.38f
            drawLine(
                Color(0xFF4F6B3A), hip, hip + Offset(sin(a) * legLen, cos(a) * legLen),
                h * 0.035f, StrokeCap.Round
            )
        }
        val shoulder = Offset(px, h * 0.535f)
        for (side in listOf(1f, -1f)) {
            val a = -side * stepT * 0.45f
            drawLine(
                Color(0xFFE79A4C), shoulder, shoulder + Offset(sin(a) * h * 0.1f, cos(a) * h * 0.1f),
                h * 0.03f, StrokeCap.Round
            )
        }
        drawRoundRect(
            Color(0xFFF4B04F), topLeft = Offset(px - h * 0.045f, h * 0.505f), size = Size(h * 0.09f, h * 0.16f),
            cornerRadius = CornerRadius(h * 0.04f)
        )
        drawCircle(Skin, h * 0.038f, Offset(px + h * 0.006f, h * 0.46f))
        drawCircle(Hair, h * 0.04f, Offset(px - h * 0.004f, h * 0.447f))
    }
}

// ── Lesen ─────────────────────────────────────────────────────

private fun DrawScope.read(c: SceneColors, t: Float, w: Float) {
    val h = size.height
    fun x(f: Float) = w - f * h

    // Warmes Lampenlicht
    val lampHead = Offset(x(1.02f), h * 0.3f)
    glow(Offset(x(0.85f), h * 0.6f), h * 0.75f, c.glow, 0.75f + 0.08f * osc(t, 2))

    // Sterne vor dem Fenster
    listOf(Triple(0.3f, 0.16f, 0f), Triple(0.55f, 0.1f, 0.3f), Triple(0.12f, 0.36f, 0.6f), Triple(0.68f, 0.26f, 0.15f)).forEach { (fx, fy, ph) ->
        val a = 0.5f + 0.5f * osc(t, 4, ph)
        sparkle(Offset(x(fx), h * fy), h * (0.025f + 0.015f * a), White.copy(alpha = 0.45f + 0.5f * a))
    }

    // Stehlampe
    drawLine(c.c, Offset(x(1.2f), h * 0.95f), Offset(x(1.2f), h * 0.22f), h * 0.025f, StrokeCap.Round)
    drawLine(c.c, Offset(x(1.2f), h * 0.22f), Offset(lampHead.x, h * 0.2f), h * 0.025f, StrokeCap.Round)
    val shade = Path().apply {
        moveTo(lampHead.x - h * 0.06f, h * 0.18f)
        lineTo(lampHead.x + h * 0.06f, h * 0.18f)
        lineTo(lampHead.x + h * 0.11f, h * 0.33f)
        lineTo(lampHead.x - h * 0.11f, h * 0.33f)
        close()
    }
    drawPath(shade, c.a)
    drawCircle(c.glow, h * 0.025f, Offset(lampHead.x, h * 0.34f))

    // Aufgeschlagenes Buch
    val cx = x(0.68f)
    fun page(dir: Float, lift: Float = 0f) = Path().apply {
        moveTo(cx, h * 0.62f - lift)
        quadraticTo(cx + dir * h * 0.16f, h * 0.55f - lift, cx + dir * h * 0.34f, h * 0.6f - lift)
        lineTo(cx + dir * h * 0.34f, h * 0.86f)
        quadraticTo(cx + dir * h * 0.16f, h * 0.81f, cx, h * 0.88f)
        close()
    }
    val cover = Path().apply {
        moveTo(cx, h * 0.66f)
        lineTo(cx - h * 0.37f, h * 0.63f); lineTo(cx - h * 0.37f, h * 0.9f)
        quadraticTo(cx - h * 0.16f, h * 0.86f, cx, h * 0.93f)
        quadraticTo(cx + h * 0.16f, h * 0.86f, cx + h * 0.37f, h * 0.9f)
        lineTo(cx + h * 0.37f, h * 0.63f); close()
    }
    drawPath(cover, c.a)
    drawPath(page(-1f), c.b)
    drawPath(page(1f), c.b)
    val lineCol = c.title.copy(alpha = 0.16f)
    for (i in 0..3) {
        val y = h * (0.66f + i * 0.045f)
        drawLine(lineCol, Offset(cx - h * 0.29f, y), Offset(cx - h * 0.05f, y + h * 0.01f), h * 0.012f, StrokeCap.Round)
        drawLine(lineCol, Offset(cx + h * 0.05f, y + h * 0.01f), Offset(cx + h * 0.29f, y), h * 0.012f, StrokeCap.Round)
    }
    // Eine Seite blättert um
    val ph = (t * 3f) % 1f
    if (ph < 0.32f) {
        val f = ph / 0.32f
        val e = f * f * (3 - 2 * f)
        scale(scaleX = 1f - 2f * e, scaleY = 1f, pivot = Offset(cx, h * 0.75f)) {
            drawPath(page(1f, lift = sin(PI.toFloat() * e) * h * 0.04f), Color(0xFFFFF4DC))
        }
    }

    // Tasse mit Dampf
    val mx = x(0.2f)
    for (i in 0..2) {
        val p = (t * 3f + i / 3f) % 1f
        val alpha = sin(PI.toFloat() * p) * 0.75f
        val baseY = h * 0.66f - p * h * 0.28f
        val sx = mx - h * 0.035f + i * h * 0.035f
        val steam = Path().apply {
            moveTo(sx, baseY)
            quadraticTo(sx + h * 0.03f, baseY - h * 0.04f, sx, baseY - h * 0.08f)
            quadraticTo(sx - h * 0.03f, baseY - h * 0.12f, sx, baseY - h * 0.16f)
        }
        drawPath(steam, White.copy(alpha = alpha), style = Stroke(h * 0.016f, cap = StrokeCap.Round))
    }
    drawRoundRect(c.a, topLeft = Offset(mx - h * 0.08f, h * 0.7f), size = Size(h * 0.16f, h * 0.18f), cornerRadius = CornerRadius(h * 0.04f))
    drawCircle(c.a, h * 0.045f, Offset(mx + h * 0.085f, h * 0.78f), style = Stroke(h * 0.022f))
    drawRoundRect(White.copy(alpha = 0.35f), topLeft = Offset(mx - h * 0.06f, h * 0.73f), size = Size(h * 0.025f, h * 0.1f), cornerRadius = CornerRadius(h * 0.012f))
}

// ── Journaling ────────────────────────────────────────────────

private fun DrawScope.journal(c: SceneColors, t: Float, w: Float) {
    val h = size.height
    fun x(f: Float) = w - f * h

    listOf(Triple(0.95f, 0.18f, 0f), Triple(0.4f, 0.12f, 0.4f), Triple(1.25f, 0.4f, 0.7f), Triple(0.1f, 0.3f, 0.2f), Triple(0.7f, 0.08f, 0.85f)).forEach { (fx, fy, ph) ->
        val a = 0.5f + 0.5f * osc(t, 3, ph)
        sparkle(Offset(x(fx), h * fy), h * (0.022f + 0.016f * a), White.copy(alpha = 0.5f + 0.5f * a))
    }

    // Notizbuch, leicht schräg
    val nb = Offset(x(0.78f), h * 0.66f)
    rotate(-7f, pivot = nb) {
        val bw = h * 0.66f; val bh = h * 0.5f
        val tl = Offset(nb.x - bw / 2, nb.y - bh / 2)
        drawRoundRect(c.a, topLeft = tl + Offset(h * 0.02f, h * 0.025f), size = Size(bw, bh), cornerRadius = CornerRadius(h * 0.05f))
        drawRoundRect(c.b, topLeft = tl, size = Size(bw, bh), cornerRadius = CornerRadius(h * 0.05f))
        for (i in 0..3) {
            val y = tl.y + bh * (0.28f + i * 0.18f)
            drawLine(c.a.copy(alpha = 0.22f), Offset(tl.x + bw * 0.14f, y), Offset(tl.x + bw * 0.92f, y), h * 0.01f)
        }
        for (i in 0..4) {
            drawCircle(c.a, h * 0.018f, Offset(tl.x + h * 0.035f, tl.y + bh * (0.14f + i * 0.18f)))
        }
        // Handschrift entsteht
        val ph = (t * 2f) % 1f
        val prog = min(ph / 0.7f, 1f)
        val fade = if (ph > 0.75f) 1f - (ph - 0.75f) / 0.25f else 1f
        val lineY = tl.y + bh * 0.46f - h * 0.025f
        val x0 = tl.x + bw * 0.16f
        val len = bw * 0.68f
        val ink = Path()
        val n = 60
        var tip = Offset(x0, lineY)
        for (i in 0..(n * prog).toInt()) {
            val u = i / n.toFloat()
            val px = x0 + u * len + sin(u * 38f) * h * 0.012f
            val py = lineY - abs(sin(u * 19f)) * h * 0.05f
            if (i == 0) ink.moveTo(px, py) else ink.lineTo(px, py)
            tip = Offset(px, py)
        }
        drawPath(ink, c.title.copy(alpha = 0.55f * fade), style = Stroke(h * 0.012f, cap = StrokeCap.Round))
        // Stift
        val penEnd = tip + Offset(h * 0.13f, -h * 0.24f)
        drawLine(c.c, tip + Offset(h * 0.02f, -h * 0.035f), penEnd, h * 0.04f, StrokeCap.Round)
        drawLine(Hair, tip, tip + Offset(h * 0.02f, -h * 0.035f), h * 0.02f, StrokeCap.Round)
    }

    // Kerze
    val kx = x(0.2f)
    glow(Offset(kx, h * 0.5f), h * 0.32f, Color(0xFFFFE2A6), 0.8f + 0.1f * osc(t, 7))
    drawRoundRect(c.b, topLeft = Offset(kx - h * 0.05f, h * 0.6f), size = Size(h * 0.1f, h * 0.3f), cornerRadius = CornerRadius(h * 0.03f))
    drawLine(Hair, Offset(kx, h * 0.6f), Offset(kx, h * 0.56f), h * 0.01f, StrokeCap.Round)
    val flick = 1f + 0.08f * osc(t, 9) + 0.04f * osc(t, 13, 0.3f)
    rotate(osc(t, 5) * 4f, pivot = Offset(kx, h * 0.57f)) {
        val fh = h * 0.12f * flick
        val flame = Path().apply {
            moveTo(kx, h * 0.57f - fh)
            quadraticTo(kx + h * 0.05f, h * 0.53f, kx, h * 0.575f)
            quadraticTo(kx - h * 0.05f, h * 0.53f, kx, h * 0.57f - fh)
            close()
        }
        drawPath(flame, c.c)
        drawCircle(Color(0xFFFFF3C4), h * 0.015f, Offset(kx, h * 0.54f))
    }
}

// ── Dehnen ────────────────────────────────────────────────────

private fun DrawScope.stretch(c: SceneColors, t: Float, w: Float) {
    val h = size.height
    fun x(f: Float) = w - f * h
    val breath = 0.5f - 0.5f * cos(TAU * 2 * t) // 0..1, zwei ruhige Atemzüge pro Zyklus

    // Atmende Sonne mit Ringen
    val sun = Offset(x(0.62f), h * 0.42f)
    for (i in 2 downTo 0) {
        drawCircle(White.copy(alpha = 0.16f + i * 0.05f), h * (0.2f + i * 0.08f + breath * 0.04f), sun, style = Stroke(h * 0.012f))
    }
    drawCircle(c.glow.copy(alpha = 0.85f), h * (0.2f + breath * 0.03f), sun)

    // Pflanze am Rand
    val pb = Offset(x(0.08f), h * 0.9f)
    leaf(pb, h * 0.32f, h * 0.07f, -12f + osc(t, 3) * 3f, c.c, Color.White.copy(alpha = 0.3f))
    leaf(pb, h * 0.24f, h * 0.06f, -42f + osc(t, 3, 0.2f) * 3f, c.c.copy(alpha = 0.85f))
    leaf(pb, h * 0.2f, h * 0.05f, 22f + osc(t, 3, 0.4f) * 3f, c.c.copy(alpha = 0.9f))

    // Matte
    drawRoundRect(c.a, topLeft = Offset(x(1.22f), h * 0.86f), size = Size(h * 1.08f, h * 0.07f), cornerRadius = CornerRadius(h * 0.035f))
    drawLine(White.copy(alpha = 0.35f), Offset(x(1.17f), h * 0.875f), Offset(x(0.2f), h * 0.875f), h * 0.008f, StrokeCap.Round)

    // Figur in der Vorbeuge
    val hip = Offset(x(0.96f), h * 0.8f)
    val toe = Offset(x(0.38f), h * 0.8f)
    drawLine(c.c.copy(red = c.c.red * 0.8f, green = c.c.green * 0.8f), hip, toe, h * 0.085f, StrokeCap.Round)
    drawLine(Hair, toe + Offset(h * 0.01f, 0f), toe + Offset(h * 0.02f, -h * 0.075f), h * 0.04f, StrokeCap.Round)

    val lean = (0.45f + 0.55f * breath) * 1.05f // Radiant aus der Senkrechten
    val dir = Offset(sin(lean), -cos(lean))
    val torsoLen = h * 0.25f
    val shoulder = hip + dir * torsoLen
    // Arme greifen nach den Zehen
    val armDir = Offset(sin(lean + 1.05f), -cos(lean + 1.05f))
    val hand = shoulder + armDir * (h * 0.2f + breath * h * 0.05f)
    drawLine(Color(0xFFF2B21F), shoulder, hand, h * 0.04f, StrokeCap.Round)
    drawCircle(Skin, h * 0.026f, hand)
    drawLine(c.b, hip, shoulder, h * 0.11f, StrokeCap.Round)
    val head = shoulder + dir * (h * 0.085f)
    drawCircle(Skin, h * 0.05f, head + Offset(dir.y * -h * 0.005f, 0f))
    drawCircle(Hair, h * 0.052f, head - Offset(dir.x * h * 0.012f, dir.y * h * 0.012f) + Offset(-h * 0.008f, -h * 0.004f))
}

// ── Ruhe ──────────────────────────────────────────────────────

private fun DrawScope.calm(c: SceneColors, t: Float, w: Float) {
    val h = size.height
    fun x(f: Float) = w - f * h

    val moon = Offset(x(0.55f), h * 0.33f)
    glow(moon, h * 0.4f, c.glow, 0.6f)
    val cut = Path().apply { addOval(Rect(moon + Offset(h * 0.07f, -h * 0.045f), h * 0.13f)) }
    clipPath(cut, clipOp = ClipOp.Difference) { drawCircle(c.b, h * 0.15f, moon) }

    listOf(Triple(1.1f, 0.2f, 0f), Triple(0.2f, 0.14f, 0.3f), Triple(0.85f, 0.1f, 0.6f), Triple(0.3f, 0.42f, 0.8f)).forEach { (fx, fy, ph) ->
        val a = 0.5f + 0.5f * osc(t, 3, ph)
        sparkle(Offset(x(fx), h * fy), h * (0.02f + 0.016f * a), White.copy(alpha = 0.5f + 0.5f * a))
    }

    fun wave(y: Float, amp: Float, len: Float, k: Int, phase: Float, color: Color) {
        val p = Path()
        p.moveTo(0f, h)
        var xx = 0f
        val shift = TAU * (k * t + phase)
        while (xx <= w + 4f) {
            p.lineTo(xx, y + sin(xx / len * TAU - shift) * amp)
            xx += 6f
        }
        p.lineTo(w, h); p.close()
        drawPath(p, color)
    }
    wave(h * 0.66f, h * 0.025f, h * 0.55f, 1, 0f, c.c.copy(alpha = 0.55f))

    // Papierboot schaukelt
    val bx = x(0.95f)
    val by = h * 0.7f + sin(bx / (h * 0.55f) * TAU - TAU * (2 * t + 0.3f)) * h * 0.02f
    rotate(osc(t, 2, 0.05f) * 6f, pivot = Offset(bx, by)) {
        val hull = Path().apply {
            moveTo(bx - h * 0.12f, by - h * 0.02f); lineTo(bx + h * 0.12f, by - h * 0.02f)
            lineTo(bx + h * 0.08f, by + h * 0.04f); lineTo(bx - h * 0.08f, by + h * 0.04f); close()
        }
        val sail = Path().apply {
            moveTo(bx, by - h * 0.17f); lineTo(bx + h * 0.08f, by - h * 0.025f); lineTo(bx - h * 0.04f, by - h * 0.025f); close()
        }
        drawPath(sail, White)
        drawPath(hull, c.b)
    }
    wave(h * 0.74f, h * 0.022f, h * 0.55f, 2, 0.3f, c.a.copy(alpha = 0.7f))
    wave(h * 0.84f, h * 0.02f, h * 0.42f, 1, 0.6f, c.a)
}

// ── Garten ────────────────────────────────────────────────────

private fun DrawScope.garden(c: SceneColors, t: Float, w: Float) {
    val h = size.height
    fun x(f: Float) = w - f * h

    glow(Offset(x(0.4f), h * 0.2f), h * 0.45f, c.glow, 0.8f)
    drawRoundRect(c.a.copy(alpha = 0.35f), topLeft = Offset(0f, h * 0.88f), size = Size(w, h * 0.3f), cornerRadius = CornerRadius(h * 0.1f))

    fun pot(cx: Float, top: Float, width: Float, color: Color) {
        val p = Path().apply {
            moveTo(cx - width / 2, top); lineTo(cx + width / 2, top)
            lineTo(cx + width * 0.38f, h * 0.92f); lineTo(cx - width * 0.38f, h * 0.92f); close()
        }
        drawPath(p, color)
        drawRoundRect(color, topLeft = Offset(cx - width * 0.56f, top - h * 0.03f), size = Size(width * 1.12f, h * 0.06f), cornerRadius = CornerRadius(h * 0.02f))
    }

    // Monstera-artige Pflanze
    val p1 = Offset(x(0.95f), h * 0.66f)
    val sway = osc(t, 3) * 3f
    leaf(p1, h * 0.4f, h * 0.11f, -30f + sway, c.a, Color.White.copy(alpha = 0.3f))
    leaf(p1, h * 0.46f, h * 0.12f, 5f + sway, c.a.copy(alpha = 0.92f), Color.White.copy(alpha = 0.3f))
    leaf(p1, h * 0.36f, h * 0.1f, 38f + sway, c.a, Color.White.copy(alpha = 0.3f))
    pot(p1.x, h * 0.68f, h * 0.24f, c.b)

    // Blume
    val p2 = Offset(x(0.5f), h * 0.7f)
    val fs = osc(t, 3, 0.25f) * 4f
    rotate(fs, pivot = p2) {
        val head = Offset(p2.x, h * 0.3f)
        drawLine(c.a, p2, head, h * 0.025f, StrokeCap.Round)
        leaf(Offset(p2.x, h * 0.56f), h * 0.14f, h * 0.05f, 55f, c.a)
        leaf(Offset(p2.x, h * 0.48f), h * 0.12f, h * 0.045f, -55f, c.a)
        for (i in 0 until 6) {
            val a = TAU * i / 6f
            drawCircle(c.c, h * 0.05f, head + Offset(cos(a), sin(a)) * (h * 0.055f))
        }
        drawCircle(Color(0xFFFFD45C), h * 0.04f, head)
    }
    pot(p2.x, h * 0.72f, h * 0.18f, Color(0xFFF2A06E))

    // Gießkanne mit Tropfen
    val can = Offset(x(0.15f), h * 0.32f)
    rotate(-18f + osc(t, 2) * 3f, pivot = can) {
        drawRoundRect(c.b, topLeft = can - Offset(h * 0.09f, h * 0.07f), size = Size(h * 0.18f, h * 0.14f), cornerRadius = CornerRadius(h * 0.04f))
        drawLine(c.b, can + Offset(-h * 0.08f, 0f), can + Offset(-h * 0.22f, -h * 0.06f), h * 0.03f, StrokeCap.Round)
        drawCircle(c.b, h * 0.05f, can + Offset(h * 0.1f, -h * 0.01f), style = Stroke(h * 0.022f))
    }
    for (i in 0..3) {
        val p = (t * 4f + i / 4f) % 1f
        val dx = can.x - h * 0.24f + (i - 1.5f) * h * 0.02f
        val dy = can.y + p * h * 0.28f
        drawCircle(Color(0xFF7FC3E8).copy(alpha = 1f - p), h * 0.014f, Offset(dx - p * h * 0.04f, dy))
    }
}
