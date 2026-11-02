package com.quietgrid.app.ui.journey.map

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val SCENE_SPOTS = listOf(
    40f to 770f, 86f to 742f, 160f to 776f, 322f to 752f, 352f to 682f, 340f to 610f, 30f to 640f, 42f to 560f, 330f to 530f, 356f to 488f,
    32f to 440f, 350f to 440f, 336f to 380f, 50f to 336f, 40f to 288f, 340f to 236f, 300f to 170f, 332f to 112f, 56f to 136f, 250f to 128f,
)

internal fun scenePage(kind: JourneySceneKind, frame: SceneFrame): List<List<SceneItem>> = when (kind) {
    JourneySceneKind.SPACE -> spacePage(frame)
    JourneySceneKind.SKY -> skyPage(frame)
    JourneySceneKind.SKETCH -> sketchPage(frame)
    JourneySceneKind.ZEN -> zenPage(frame)
    JourneySceneKind.ARCTIC -> arcticPage(frame)
    JourneySceneKind.CAFE -> cafePage(frame)
    JourneySceneKind.GOTHIC -> gothicPage(frame)
}

private fun SceneFrame.path(block: ScenePath.() -> Unit): ScenePath = ScenePath(this).apply(block)

private fun ScenePath.pine(x: Float, y: Float, h: Float) = apply {
    polygon(x, y - h, x - h * 0.45f, y, x + h * 0.45f, y)
    move(x, y)
    line(x, y + h * 0.25f)
}

private fun ScenePath.bird(x: Float, y: Float) = apply {
    move(x - 7, y - 3)
    quad(x - 3, y - 4, x, y)
    quad(x + 3, y - 4, x + 7, y - 3)
}

private fun ScenePath.cloud(x: Float, y: Float, w: Float) = apply {
    move(x, y)
    line(x + w, y)
    quad(x + 1.12f * w, y, x + 1.1f * w, y - 0.14f * w)
    quad(x + 1.06f * w, y - 0.30f * w, x + 0.86f * w, y - 0.26f * w)
    quad(x + 0.78f * w, y - 0.48f * w, x + 0.56f * w, y - 0.42f * w)
    quad(x + 0.36f * w, y - 0.42f * w, x + 0.34f * w, y - 0.26f * w)
    quad(x + 0.14f * w, y - 0.28f * w, x + 0.12f * w, y - 0.12f * w)
    quad(x + 0.08f * w, y, x + 0.2f * w, y)
    close()
}

private fun ScenePath.balloon(x: Float, y: Float, r: Float) = apply {
    move(x, y + r * 1.25f)
    cubic(x - r * 1.5f, y + r * 0.2f, x - r, y - r * 1.1f, x, y - r * 1.1f)
    cubic(x + r, y - r * 1.1f, x + r * 1.5f, y + r * 0.2f, x, y + r * 1.25f)
    close()
    move(x - r * 0.3f, y + r * 1.15f)
    line(x - r * 0.2f, y + r * 1.7f)
    move(x + r * 0.3f, y + r * 1.15f)
    line(x + r * 0.2f, y + r * 1.7f)
    polygon(x - r * 0.25f, y + r * 1.7f, x + r * 0.25f, y + r * 1.7f, x + r * 0.25f, y + r * 2.05f, x - r * 0.25f, y + r * 2.05f)
    move(x, y - r * 1.1f)
    cubic(x - r * 0.4f, y - r * 0.4f, x - r * 0.4f, y + r * 0.6f, x, y + r * 1.25f)
}

private fun ScenePath.flake(x: Float, y: Float, r: Float) = apply {
    move(x - r, y)
    line(x + r, y)
    move(x - r / 2, y - r * 0.87f)
    line(x + r / 2, y + r * 0.87f)
    move(x - r / 2, y + r * 0.87f)
    line(x + r / 2, y - r * 0.87f)
}

private fun ScenePath.star(x: Float, y: Float, r: Float) = apply {
    val points = (0 until 10).flatMap { i ->
        val a = -Math.PI.toFloat() / 2 + i * Math.PI.toFloat() / 5
        val rr = if (i % 2 == 1) r * 0.45f else r
        listOf(x + rr * cos(a), y + rr * sin(a))
    }
    polygon(*points.toFloatArray())
}

private fun ScenePath.blob(cx: Float, cy: Float, r: Float, k: Float, squash: Float) = closedSmooth(blobPoints(cx, cy, r, k, squash))

private fun ScenePath.rect(x1: Float, y1: Float, x2: Float, y2: Float) = polygon(x1, y1, x2, y1, x2, y2, x1, y2)

private fun ScenePath.satellite(x: Float, y: Float) = apply {
    rect(x - 4, y - 4, x + 4, y + 4)
    rect(x - 18, y - 3, x - 6, y + 3)
    rect(x + 6, y - 3, x + 18, y + 3)
    move(x, y - 4)
    line(x, y - 10)
}

private fun ScenePath.ringedPlanetRing() = apply {
    move(276f, 612f)
    cubic(290f, 590f, 350f, 580f, 362f, 590f)
    cubic(372f, 600f, 330f, 622f, 300f, 628f)
    cubic(280f, 632f, 268f, 624f, 276f, 612f)
    close()
}

private fun starField(frame: SceneFrame, count: Int, top: Float, span: Int, big: (Int) -> Boolean, mid: (Int) -> Boolean, bigR: Float, midR: Float, smallR: Float) =
    (0 until count).map { i ->
        val x = ((i * 97 + 31) % 390).toFloat()
        val y = top + (i * 173 + 59) % span
        val radius = if (big(i)) bigR else if (mid(i)) midR else smallR
        val paint = if (big(i)) ScenePaint.INK_STRONG else ScenePaint.INK
        listOf(frame.path { circle(x, y, radius) }.item(fill = paint, alpha = if (big(i) || mid(i)) 1f else 0.6f))
    }

private fun spacePage(frame: SceneFrame): List<List<SceneItem>> = buildList {
    add(listOf(frame.path { blob(90f, 600f, 120f, 1f, 0.7f) }.item(fill = ScenePaint.FILL_ACCENT, alpha = 0.55f, wash = true)))
    add(listOf(frame.path { blob(60f, 640f, 70f, 3f, 0.8f) }.item(fill = ScenePaint.FILL_ACCENT, alpha = 0.45f, wash = true)))
    add(listOf(frame.path { blob(320f, 300f, 110f, 2f, 0.6f) }.item(fill = ScenePaint.FILL_SOFT, alpha = 0.6f, wash = true)))
    add(listOf(frame.path { blob(340f, 280f, 55f, 4f, 0.7f) }.item(fill = ScenePaint.FILL_SOFT, alpha = 0.45f, wash = true)))
    addAll(starField(frame, 110, 70f, 780, { it % 11 == 0 }, { it % 4 == 0 }, 1.8f, 1.1f, 0.7f))
    listOf(300f to 720f, 50f to 380f, 230f to 150f).forEach { (x, y) ->
        add(listOf(frame.path { move(x - 6, y); line(x + 6, y); move(x, y - 6); line(x, y + 6) }.item(stroke = ScenePaint.INK_STRONG, width = 1.2f)))
    }
    add(
        listOf(
            frame.path { circle(318f, 600f, 26f) }.item(fill = ScenePaint.FILL_SOFT, stroke = ScenePaint.INK, width = 1.5f),
            frame.path { ringedPlanetRing() }.item(stroke = ScenePaint.INK_STRONG, width = 2f),
            frame.path { move(300f, 586f); quad(310f, 582f, 322f, 584f); move(296f, 600f); quad(312f, 594f, 336f, 598f) }
                .item(stroke = ScenePaint.INK, width = 1.5f),
        ),
    )
    add(
        listOf(
            frame.path { circle(52f, 210f, 14f) }.item(fill = ScenePaint.FILL_ACCENT, stroke = ScenePaint.INK, width = 1.5f),
            frame.path { circle(46f, 206f, 3f); circle(56f, 216f, 2f) }.item(stroke = ScenePaint.INK, width = 1.2f),
        ),
    )
    add(
        listOf(
            frame.path { circle(340f, 120f, 34f) }.item(fill = ScenePaint.FILL_SOFT, stroke = ScenePaint.INK, width = 1.5f),
            frame.path {
                move(318f, 104f); quad(329f, 112f, 362f, 104f)
                move(310f, 122f); quad(325f, 132f, 370f, 122f)
                move(316f, 140f); quad(328f, 148f, 364f, 140f)
            }.item(stroke = ScenePaint.INK, width = 2f),
        ),
    )
    add(
        listOf(
            frame.path { move(40f, 760f); line(120f, 720f) }.item(stroke = ScenePaint.INK_STRONG, width = 1.2f, alpha = 0.7f),
            frame.path { circle(122f, 719f, 2.4f) }.item(fill = ScenePaint.GLOW),
        ),
    )
    add(
        listOf(
            frame.path {
                (0 until 40).forEach { k ->
                    val a = k * 0.35f
                    val r = 2 + k * 0.55f
                    val x = 330 + r * cos(a)
                    val y = 440 + r * sin(a) * 0.5f
                    if (k == 0) move(x, y) else line(x, y)
                }
            }.item(stroke = ScenePaint.INK, width = 1.4f),
        ),
    )
    add(
        listOf(
            frame.path {
                polygon(60f, 470f, 70f, 466f, 76f, 472f, 72f, 482f, 64f, 480f)
                polygon(84f, 488f, 90f, 486f, 93f, 490f, 90f, 495f)
            }.item(fill = ScenePaint.FILL_SOFT, stroke = ScenePaint.INK, width = 1f),
        ),
    )
    add(listOf(frame.path { satellite(150f, 300f) }.item(stroke = ScenePaint.INK_STRONG, width = 1.3f)))
}

private fun skyPage(frame: SceneFrame): List<List<SceneItem>> = buildList {
    add(
        listOf(
            frame.path { circle(330f, 112f, 22f) }.item(stroke = ScenePaint.GLOW, width = 2f, alpha = 0.5f),
            frame.path { circle(330f, 112f, 32f) }.item(stroke = ScenePaint.GLOW, width = 1.5f, dash = listOf(3f, 7f), alpha = 0.35f),
        ),
    )
    listOf(Triple(20f, 220f, 90f), Triple(250f, 330f, 110f), Triple(10f, 500f, 80f), Triple(280f, 600f, 90f), Triple(30f, 700f, 70f), Triple(150f, 140f, 70f))
        .forEach { (x, y, w) -> add(listOf(frame.path { cloud(x, y, w) }.item(fill = ScenePaint.SURFACE, stroke = ScenePaint.INK, width = 1.5f))) }
    add(listOf(frame.path { balloon(345f, 470f, 15f) }.item(fill = ScenePaint.FILL_ACCENT, stroke = ScenePaint.INK_STRONG, width = 1.5f)))
    add(listOf(frame.path { balloon(52f, 390f, 11f) }.item(fill = ScenePaint.FILL_ACCENT, stroke = ScenePaint.INK_STRONG, width = 1.5f)))
    add(
        listOf(
            frame.path { polygon(318f, 760f, 352f, 746f, 340f, 768f, 334f, 760f); move(334f, 768f); line(340f, 760f) }
                .item(fill = ScenePaint.SURFACE, stroke = ScenePaint.INK, width = 1.5f),
        ),
    )
    listOf(90f to 280f, 108f to 270f, 270f to 200f, 290f to 210f, 60f to 600f)
        .forEach { (x, y) -> add(listOf(frame.path { bird(x, y) }.item(stroke = ScenePaint.INK_STRONG))) }
}

private fun sketchPage(frame: SceneFrame): List<List<SceneItem>> = buildList {
    fun ink(path: ScenePath) = path.item(stroke = ScenePaint.INK_STRONG, width = 1.6f)
    add(
        listOf(
            ink(
                frame.path {
                    circle(318f, 600f, 26f)
                    var k = -22f + 4
                    while (k < 22f) {
                        val h = sqrt(22f * 22f - k * k) * 0.8f
                        move(318 + k - h * 0.4f, 600 + h)
                        line(318 + k + h * 0.4f, 600 - h)
                        k += 5
                    }
                },
            ),
            ink(frame.path { ringedPlanetRing() }),
        ),
    )
    add(listOf(ink(frame.path { circle(340f, 120f, 30f); move(316f, 108f); quad(328f, 116f, 364f, 108f); move(312f, 124f); quad(326f, 133f, 368f, 124f) })))
    add(listOf(ink(frame.path { circle(52f, 210f, 13f); circle(47f, 206f, 3f); circle(56f, 216f, 2f) })))
    add(
        listOf(
            frame.path { move(40f, 760f); line(118f, 722f) }.item(stroke = ScenePaint.INK, width = 1.2f, dash = listOf(5f, 4f)),
            ink(frame.path { star(122f, 719f, 6f) }),
        ),
    )
    SCENE_SPOTS.forEachIndexed { i, (x, y) ->
        if (i % 3 == 1) add(listOf(frame.path { flake(x, y, if (i % 2 == 1) 4f else 6f) }.item(stroke = ScenePaint.INK, width = 1.3f)))
    }
    add(listOf(ink(frame.path { satellite(150f, 300f) })))
}

private fun zenPage(frame: SceneFrame): List<List<SceneItem>> = buildList {
    var y = 90f
    while (y < 860f) {
        val lineY = y
        add(
            listOf(
                frame.path {
                    move(-10f, lineY)
                    var x = 0
                    while (x <= 400) {
                        quad(x + 10f, lineY + if (x % 80 != 0) 3f else -3f, x + 20f, lineY)
                        x += 40
                    }
                }.item(stroke = ScenePaint.INK, width = 1.4f, alpha = 0.5f, wash = true),
            ),
        )
        y += 14f
    }
    listOf(Triple(60f, 690f, 18f), Triple(330f, 520f, 22f), Triple(50f, 330f, 16f), Triple(320f, 200f, 20f)).forEachIndexed { k, (x, cy, r) ->
        val kf = k.toFloat()
        add(
            buildList {
                add(frame.path { blob(x, cy, r * 3.2f, kf, 0.75f) }.item(fill = ScenePaint.BACKDROP, wash = true))
                for (i in 3 downTo 1) add(frame.path { blob(x, cy, r + i * 11, kf + i * 0.2f, 0.75f) }.item(stroke = ScenePaint.INK, width = 1.4f))
                add(frame.path { blob(x, cy, r, kf * 2, 0.8f) }.item(fill = ScenePaint.FILL_SOFT, stroke = ScenePaint.INK_STRONG, width = 1.2f))
            },
        )
    }
    add(
        listOf(
            frame.path {
                move(300f, 760f)
                quad(314f, 740f, 334f, 752f)
                quad(320f, 756f, 316f, 770f)
                quad(308f, 764f, 300f, 760f)
                close()
                move(318f, 752f)
                line(326f, 770f)
            }.item(fill = ScenePaint.FILL_SOFT, stroke = ScenePaint.INK_STRONG, width = 1.2f),
        ),
    )
}

private fun arcticPage(frame: SceneFrame): List<List<SceneItem>> = buildList {
    fun band(y: Float, amp: Float, phase: Float, h: Float): ScenePath = frame.path {
        val xs = (-10..400 step 30).map { it.toFloat() }
        val points = xs.flatMap { x -> listOf(x, y + amp * sin(x / 60 + phase)) } +
            xs.reversed().flatMap { x -> listOf(x, y + h + amp * sin(x / 60 + phase + 0.6f)) }
        polygon(*points.toFloatArray())
    }
    add(listOf(band(110f, 16f, 0f, 26f).item(fill = ScenePaint.AURORA_1, alpha = 0.18f, wash = true)))
    add(listOf(band(140f, 14f, 1.4f, 22f).item(fill = ScenePaint.AURORA_2, alpha = 0.16f, wash = true)))
    add(listOf(band(168f, 12f, 2.6f, 18f).item(fill = ScenePaint.AURORA_3, alpha = 0.14f, wash = true)))
    listOf(listOf(60f, 290f, 150f, 70f), listOf(190f, 290f, 130f, 54f), listOf(330f, 290f, 170f, 84f)).forEach { (x, y, w, h) ->
        add(
            listOf(
                frame.path { polygon(x - w / 2, y, x, y - h, x + w / 2, y) }.item(fill = ScenePaint.FILL_SOFT, stroke = ScenePaint.INK, width = 1.5f, wash = true),
                frame.path {
                    polygon(x - w * 0.14f, y - h * 0.72f, x, y - h, x + w * 0.14f, y - h * 0.72f, x + w * 0.07f, y - h * 0.65f, x, y - h * 0.72f, x - w * 0.07f, y - h * 0.65f)
                }.item(fill = ScenePaint.SURFACE, stroke = ScenePaint.INK, width = 1f, wash = true),
            ),
        )
    }
    listOf(
        Triple(40f, 772f, 30f), Triple(72f, 756f, 24f), Triple(322f, 760f, 26f), Triple(350f, 682f, 22f), Triple(36f, 640f, 22f),
        Triple(340f, 540f, 24f), Triple(360f, 520f, 18f), Triple(44f, 440f, 20f), Triple(340f, 400f, 18f),
    ).forEach { (x, y, h) -> add(listOf(frame.path { pine(x, y, h) }.item(fill = ScenePaint.FILL_SOFT, stroke = ScenePaint.INK, width = 1.2f))) }
    SCENE_SPOTS.forEachIndexed { i, (x, y) ->
        if (i % 2 == 0) add(listOf(frame.path { flake(x + 14, y - 30, if (i % 4 != 0) 3f else 5f) }.item(stroke = ScenePaint.INK, width = 1.2f)))
    }
}

private fun cafePage(frame: SceneFrame): List<List<SceneItem>> = buildList {
    SCENE_SPOTS.forEachIndexed { i, (x, y) ->
        when (i % 3) {
            0 -> add(
                listOf(
                    frame.path { ellipse(x, y, 6f, 9f); move(x, y - 8); quad(x - 3, y, x, y + 8) }
                        .item(fill = ScenePaint.FILL_SOFT, stroke = ScenePaint.INK, width = 1.4f),
                ),
            )
            1 -> add(listOf(frame.path { move(x, y + 12); quad(x - 6, y + 6, x, y); quad(x + 6, y - 6, x, y - 12) }.item(stroke = ScenePaint.INK, width = 1.8f)))
            else -> add(
                listOf(
                    frame.path { circle(x, y, 7f) }.item(fill = ScenePaint.GLOW, alpha = 0.16f),
                    frame.path { circle(x, y, 2f) }.item(fill = ScenePaint.GLOW, alpha = 0.8f),
                ),
            )
        }
    }
    add(
        listOf(
            frame.path {
                move(300f, 128f)
                line(340f, 128f)
                line(340f, 144f)
                quad(340f, 158f, 320f, 158f)
                quad(300f, 158f, 300f, 144f)
                close()
                move(340f, 132f)
                quad(351f, 132f, 351f, 139f)
                quad(351f, 146f, 340f, 146f)
                move(296f, 162f)
                line(344f, 162f)
            }.item(fill = ScenePaint.FILL_SOFT, stroke = ScenePaint.INK_STRONG, width = 1.6f),
            frame.path {
                listOf(312f, 327f).forEach { x ->
                    move(x, 118f)
                    quad(x - 5, 110f, x, 102f)
                    quad(x + 5, 94f, x, 86f)
                }
            }.item(stroke = ScenePaint.INK_STRONG, width = 1.6f),
        ),
    )
}

private fun gothicPage(frame: SceneFrame): List<List<SceneItem>> = buildList {
    addAll(starField(frame, 50, 80f, 760, { it % 9 == 0 }, { false }, 1.4f, 0.8f, 0.8f))
    add(
        listOf(
            frame.path { circle(320f, 130f, 38f) }.item(fill = ScenePaint.FILL_SOFT),
            frame.path { circle(306f, 120f, 6f); circle(330f, 146f, 4f); circle(334f, 116f, 3f) }.item(fill = ScenePaint.INK),
        ),
    )
    listOf(262f to 190f, 290f to 214f, 60f to 470f, 340f to 410f).forEach { (x, y) ->
        add(
            listOf(
                frame.path {
                    move(x, y)
                    quad(x - 6, y - 6, x - 14, y - 2)
                    quad(x - 10, y, x - 10, y + 4)
                    quad(x - 6, y + 2, x - 4, y + 6)
                    quad(x - 2, y + 2, x, y + 2)
                    quad(x + 2, y + 2, x + 4, y + 6)
                    quad(x + 6, y + 2, x + 10, y + 4)
                    quad(x + 10, y, x + 14, y - 2)
                    quad(x + 6, y - 6, x, y)
                    close()
                }.item(fill = ScenePaint.INK_STRONG),
            ),
        )
    }
    add(
        listOf(
            frame.path {
                polygon(10f, 260f, 10f, 200f, 24f, 200f, 24f, 192f, 30f, 192f, 30f, 200f, 36f, 200f, 36f, 192f, 42f, 192f, 42f, 200f, 48f, 200f, 48f, 260f)
                polygon(44f, 260f, 44f, 176f, 54f, 176f, 54f, 168f, 59f, 168f, 59f, 176f, 64f, 176f, 64f, 168f, 69f, 168f, 69f, 176f, 74f, 176f, 74f, 260f)
                polygon(76f, 260f, 76f, 208f, 98f, 208f, 98f, 260f)
                polygon(60f, 150f, 69f, 132f, 78f, 150f)
            }.item(fill = ScenePaint.FILL_SOFT, stroke = ScenePaint.INK, width = 1f),
            frame.path { rect(58f, 220f, 63f, 228f); rect(20f, 226f, 24f, 232f) }.item(fill = ScenePaint.GLOW, alpha = 0.5f),
        ),
    )
    listOf(40f to 790f, 330f to 770f, 350f to 560f, 30f to 640f, 340f to 330f).forEach { (x, y) ->
        add(
            listOf(
                frame.path {
                    move(x, y); line(x, y - 42)
                    move(x, y - 24); line(x - 14, y - 36); line(x - 20, y - 34)
                    move(x, y - 30); line(x + 12, y - 46)
                    move(x + 6, y - 38); line(x + 14, y - 36)
                }.item(stroke = ScenePaint.INK_STRONG, width = 2.2f),
            ),
        )
    }
    listOf(Triple(300f, 700f, 10f), Triple(60f, 560f, 8f)).forEach { (x, y, half) ->
        add(
            listOf(
                frame.path {
                    move(x, y)
                    quad(x, y - half * 1.2f, x + half, y - half * 1.2f)
                    quad(x + half * 2, y - half * 1.2f, x + half * 2, y)
                    line(x + half * 2, y + half)
                    line(x, y + half)
                    close()
                }.item(fill = ScenePaint.FILL_SOFT, stroke = ScenePaint.INK, width = 1.2f),
            ),
        )
    }
}
