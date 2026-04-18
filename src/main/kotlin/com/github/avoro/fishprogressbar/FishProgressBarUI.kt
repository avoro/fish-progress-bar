package com.github.avoro.fishprogressbar

import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.LinearGradientPaint
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import javax.swing.JComponent
import javax.swing.Timer
import javax.swing.plaf.basic.BasicProgressBarUI

class FishProgressBarUI : BasicProgressBarUI() {

    companion object {
        @JvmStatic
        fun createUI(c: JComponent): FishProgressBarUI = FishProgressBarUI()

        private const val NATIVE_W = 400.0
        private const val NATIVE_H = 22.0

        // Water
        private val WATER_TOP  = Color(0x0d3b4f)
        private val WATER_MID  = Color(0x0a2e40)
        private val WATER_BOT  = Color(0x041c29)
        private val SEAWEED    = Color(0x072530)
        private val SHAFT_COL  = Color(0xffffff)
        private val PLANKTON   = Color(0xa8e8ff)
        private val BUBBLE_COL = Color(0xe8f4ff)
        private val BUBBLE_HI  = Color(0xffffff)

        // Fish — leader
        private val LEADER_BODY   = Color(0x6b8fa8)
        private val LEADER_BELLY  = Color(0xa8c4d6)
        private val LEADER_DORSAL = Color(0x3d5a70)
        private val LEADER_IRIS   = Color(0xf4e4a8)

        // Fish — medium
        private val MED_BODY   = Color(0x7a9bb0)
        private val MED_BELLY  = Color(0xb4ccdc)
        private val MED_DORSAL = Color(0x4a6a80)

        // Fish — small / tiny
        private val SML_BODY   = Color(0x8aacc0)
        private val SML_BELLY  = Color(0xbcd2e0)
        private val SML_DORSAL = Color(0x557386)

        // Eye
        private val EYE_PUPIL = Color(0x1a0f0a)
        private val EYE_HI    = Color(0xffffff)
    }

    private var frame = 0
    private var timer: Timer? = null
    private var prevProgress = 0.0
    private var facingRight = true

    override fun installUI(c: JComponent) {
        super.installUI(c)
        progressBar.isOpaque = false
        timer = Timer(33) {
            frame = (frame + 1) % 3600
            progressBar?.repaint()
        }
        timer?.start()
    }

    override fun uninstallUI(c: JComponent) {
        timer?.stop()
        timer = null
        super.uninstallUI(c)
    }

    override fun getPreferredSize(c: JComponent): Dimension =
        Dimension(super.getPreferredSize(c).width, (NATIVE_H * 2).toInt())

    override fun paintString(g: Graphics, x: Int, y: Int, w: Int, h: Int, amountFull: Int, d: Insets) {
        // suppress percentage text over the animation
    }

    private fun paintScene(g2: Graphics2D, width: Int, height: Int, progress: Double, goingRight: Boolean) {
        val w = width.toDouble()
        val h = height.toDouble()
        val ux = w / NATIVE_W   // horizontal unit scale
        val ps = h / NATIVE_H   // pixel scale (uniform for fish + vertical elements)

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        // Background gradient
        g2.paint = LinearGradientPaint(
            0f, 0f, 0f, height.toFloat(),
            floatArrayOf(0f, 0.5f, 1f),
            arrayOf(WATER_TOP, WATER_MID, WATER_BOT)
        )
        g2.fillRect(0, 0, width, height)
        g2.paint = null

        // Light shafts — faint diagonal beams (x1top, x2top, x1bot, x2bot in NATIVE_W coords)
        paintShaft(g2, 30.0, 36.0, 28.0, 34.0, 0.04f, ux, h)
        paintShaft(g2, 180.0, 188.0, 176.0, 184.0, 0.03f, ux, h)
        paintShaft(g2, 290.0, 296.0, 288.0, 294.0, 0.04f, ux, h)

        // Seaweed silhouettes at left and right edges
        paintSeaweedLeft(g2, 0.0, h, ps)
        paintSeaweedRight(g2, w - 8.0 * ps, h, ps)

        // Plankton twinkle — (nativeX, nativeY, phaseOffset)
        val plankton = arrayOf(
            intArrayOf(60, 6, 0),
            intArrayOf(140, 17, 31),
            intArrayOf(230, 4, 57),
            intArrayOf(300, 15, 83),
            intArrayOf(360, 9, 107),
        )
        for (p in plankton) {
            val bright = ((frame + p[2]) % 120) < 80
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, if (bright) 0.8f else 0.2f)
            g2.color = PLANKTON
            val cx = p[0] * ux; val cy = p[1] * ps
            g2.fill(Ellipse2D.Double(cx - 0.6 * ps, cy - 0.6 * ps, 1.2 * ps, 1.2 * ps))
        }
        g2.composite = AlphaComposite.SrcOver

        // Rising bubbles — (nativeX, radiusInNativeUnits, phaseOffset)
        val bubbles = arrayOf(
            Triple(80.0, 0.9, 0),
            Triple(250.0, 1.1, 67),
            Triple(330.0, 0.8, 133),
        )
        for ((nx, nr, ph) in bubbles) {
            val riseT = ((frame + ph) % 200) / 200.0
            val bcx = nx * ux
            val bcy = h * (1.0 - riseT)
            val br = nr * ps
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f)
            g2.color = BUBBLE_COL
            g2.fill(Ellipse2D.Double(bcx - br, bcy - br, br * 2.0, br * 2.0))
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f)
            g2.color = BUBBLE_HI
            val hiR = br * 0.35
            g2.fill(Ellipse2D.Double(bcx - br * 0.3 - hiR, bcy - br * 0.3 - hiR, hiR * 2.0, hiR * 2.0))
        }
        g2.composite = AlphaComposite.SrcOver

        // Fish school — moves left→right with progress; direction set by caller
        val margin = 28.0 * ux
        val schoolX = margin + progress * (w - 2.0 * margin)

        val wiggleA = (frame / 8) % 2 == 0   // tail wiggle ~250 ms at 30 fps
        paintSchool(g2, schoolX, h / 2.0, ps, goingRight, wiggleA)
    }

    // --- Scene helpers ---

    private fun paintShaft(g: Graphics2D, x1t: Double, x2t: Double, x1b: Double, x2b: Double,
                            alpha: Float, ux: Double, h: Double) {
        val g2 = g.create() as Graphics2D
        try {
            val path = Path2D.Double()
            path.moveTo(x1t * ux, 0.0); path.lineTo(x2t * ux, 0.0)
            path.lineTo(x2b * ux, h);   path.lineTo(x1b * ux, h)
            path.closePath()
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
            g2.color = SHAFT_COL
            g2.fill(path)
        } finally {
            g2.dispose()
        }
    }

    private fun paintSeaweedLeft(g: Graphics2D, startX: Double, h: Double, ps: Double) {
        val g2 = g.create() as Graphics2D
        try {
            g2.translate(startX, 0.0)
            g2.scale(ps, ps)
            g2.color = SEAWEED
            val p1 = Path2D.Double()
            p1.moveTo(1.0, 22.0); p1.quadTo(3.0, 16.0, 1.0, 10.0); p1.quadTo(-1.0, 6.0, 3.0, 2.0)
            p1.lineTo(4.0, 22.0); p1.closePath()
            g2.fill(p1)
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f)
            val p2 = Path2D.Double()
            p2.moveTo(5.0, 22.0); p2.quadTo(7.0, 17.0, 5.0, 12.0); p2.quadTo(4.0, 8.0, 6.0, 5.0)
            p2.lineTo(7.0, 22.0); p2.closePath()
            g2.fill(p2)
        } finally {
            g2.dispose()
        }
    }

    private fun paintSeaweedRight(g: Graphics2D, startX: Double, h: Double, ps: Double) {
        val g2 = g.create() as Graphics2D
        try {
            g2.translate(startX, 0.0)
            g2.scale(ps, ps)
            g2.color = SEAWEED
            val p1 = Path2D.Double()
            p1.moveTo(7.0, 22.0); p1.quadTo(5.0, 16.0, 7.0, 10.0); p1.quadTo(9.0, 6.0, 5.0, 2.0)
            p1.lineTo(4.0, 22.0); p1.closePath()
            g2.fill(p1)
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f)
            val p2 = Path2D.Double()
            p2.moveTo(3.0, 22.0); p2.quadTo(1.0, 17.0, 3.0, 12.0); p2.quadTo(4.0, 8.0, 2.0, 5.0)
            p2.lineTo(1.0, 22.0); p2.closePath()
            g2.fill(p2)
        } finally {
            g2.dispose()
        }
    }

    // --- School ---

    private fun paintSchool(g: Graphics2D, cx: Double, cy: Double, ps: Double,
                             facingRight: Boolean, wiggleA: Boolean) {
        val g2 = g.create() as Graphics2D
        try {
            g2.translate(cx, cy)
            g2.scale(ps, ps)
            if (facingRight) g2.scale(-1.0, 1.0)

            paintLeader(g2, -15.0, 0.0, wiggleA)
            paintMedium(g2, -5.0, -5.0, wiggleA)
            paintMedium(g2, -2.0,  5.0, wiggleA)
            paintSmall(g2,   5.0, -2.0, wiggleA)
            paintSmall(g2,   8.0,  3.0, wiggleA)
            paintTiny(g2,   13.0, -4.0, wiggleA)
            paintTiny(g2,   14.0,  4.0, wiggleA)
        } finally {
            g2.dispose()
        }
    }

    // --- Fish painters (native 400×22 coordinate space, centered at 0,0) ---

    private fun paintLeader(g: Graphics2D, tx: Double, ty: Double, wiggleA: Boolean) {
        val g2 = g.create() as Graphics2D
        try {
            g2.translate(tx, ty)
            // Body
            g2.color = LEADER_BODY;   g2.fill(Ellipse2D.Double(-5.0, -2.2, 10.0, 4.4))
            // Belly
            g2.color = LEADER_BELLY;  g2.fill(Ellipse2D.Double(-4.5, -0.2,  9.0, 2.4))
            // Dorsal shadow
            g2.color = LEADER_DORSAL; g2.fill(Ellipse2D.Double(-3.5, -1.9,  8.0, 1.8))
            // Tail outer
            g2.color = LEADER_DORSAL
            val tailOut = Path2D.Double()
            if (wiggleA) { tailOut.moveTo(4.5, 0.0); tailOut.lineTo(8.0, -2.8); tailOut.lineTo(7.0, 0.0); tailOut.lineTo(8.0,  2.8) }
            else         { tailOut.moveTo(4.5, 0.0); tailOut.lineTo(8.0,  2.8); tailOut.lineTo(7.0, 0.0); tailOut.lineTo(8.0, -2.8) }
            tailOut.closePath(); g2.fill(tailOut)
            // Tail inner
            g2.color = LEADER_BODY
            val tailIn = Path2D.Double()
            if (wiggleA) { tailIn.moveTo(4.5, 0.0); tailIn.lineTo(7.0, -1.8); tailIn.lineTo(6.5, 0.0); tailIn.lineTo(7.0,  1.8) }
            else         { tailIn.moveTo(4.5, 0.0); tailIn.lineTo(7.0,  1.8); tailIn.lineTo(6.5, 0.0); tailIn.lineTo(7.0, -1.8) }
            tailIn.closePath(); g2.fill(tailIn)
            // Dorsal fin
            g2.color = LEADER_DORSAL
            val dFin = Path2D.Double()
            dFin.moveTo(-1.0, -2.0); dFin.lineTo(1.5, -3.2); dFin.lineTo(2.0, -2.0); dFin.closePath(); g2.fill(dFin)
            // Pectoral fin
            val pFin = Path2D.Double()
            pFin.moveTo(-1.0, 2.0); pFin.lineTo(0.5, 3.0); pFin.lineTo(1.5, 2.0); pFin.closePath(); g2.fill(pFin)
            // Gill line
            g2.stroke = BasicStroke(0.4f)
            g2.draw(Line2D.Double(-2.8, -1.2, -2.8, 1.2))
            // Eye
            g2.color = LEADER_IRIS; g2.fill(Ellipse2D.Double(-4.2, -1.0,  1.4, 1.4))
            g2.color = EYE_PUPIL;   g2.fill(Ellipse2D.Double(-4.1, -0.7,  0.8, 0.8))
            g2.color = EYE_HI;      g2.fill(Ellipse2D.Double(-4.0, -0.65, 0.3, 0.3))
        } finally {
            g2.dispose()
        }
    }

    private fun paintMedium(g: Graphics2D, tx: Double, ty: Double, wiggleA: Boolean) {
        val g2 = g.create() as Graphics2D
        try {
            g2.translate(tx, ty)
            g2.color = MED_BODY;   g2.fill(Ellipse2D.Double(-3.8, -1.7, 7.6, 3.4))
            g2.color = MED_BELLY;  g2.fill(Ellipse2D.Double(-3.3, -0.2, 6.6, 1.8))
            g2.color = MED_DORSAL; g2.fill(Ellipse2D.Double(-2.7, -1.4, 6.0, 1.4))
            g2.color = MED_DORSAL
            val tail = Path2D.Double()
            if (wiggleA) { tail.moveTo(3.3, 0.0); tail.lineTo(6.0, -2.0); tail.lineTo(5.3, 0.0); tail.lineTo(6.0,  2.0) }
            else         { tail.moveTo(3.3, 0.0); tail.lineTo(6.0,  2.0); tail.lineTo(5.3, 0.0); tail.lineTo(6.0, -2.0) }
            tail.closePath(); g2.fill(tail)
            val dFin = Path2D.Double()
            dFin.moveTo(-0.5, -1.6); dFin.lineTo(1.0, -2.4); dFin.lineTo(1.5, -1.6); dFin.closePath(); g2.fill(dFin)
            val pFin = Path2D.Double()
            pFin.moveTo(-0.5, 1.6); pFin.lineTo(0.5, 2.3); pFin.lineTo(1.2, 1.6); pFin.closePath(); g2.fill(pFin)
            g2.color = EYE_PUPIL; g2.fill(Ellipse2D.Double(-3.0, -0.7,  1.0,  1.0))
            g2.color = EYE_HI;    g2.fill(Ellipse2D.Double(-2.72, -0.47, 0.24, 0.24))
        } finally {
            g2.dispose()
        }
    }

    private fun paintSmall(g: Graphics2D, tx: Double, ty: Double, wiggleA: Boolean) {
        val g2 = g.create() as Graphics2D
        try {
            g2.translate(tx, ty)
            g2.color = SML_BODY;   g2.fill(Ellipse2D.Double(-2.8, -1.3, 5.6, 2.6))
            g2.color = SML_BELLY;  g2.fill(Ellipse2D.Double(-2.4, -0.2, 4.8, 1.4))
            g2.color = SML_DORSAL; g2.fill(Ellipse2D.Double(-2.0, -1.0, 4.4, 1.0))
            g2.color = SML_DORSAL
            val tail = Path2D.Double()
            if (wiggleA) { tail.moveTo(2.3, 0.0); tail.lineTo(4.5, -1.5); tail.lineTo(4.0, 0.0); tail.lineTo(4.5,  1.5) }
            else         { tail.moveTo(2.3, 0.0); tail.lineTo(4.5,  1.5); tail.lineTo(4.0, 0.0); tail.lineTo(4.5, -1.5) }
            tail.closePath(); g2.fill(tail)
            val dFin = Path2D.Double()
            dFin.moveTo(-0.3, -1.2); dFin.lineTo(0.8, -1.8); dFin.lineTo(1.1, -1.2); dFin.closePath(); g2.fill(dFin)
            g2.color = EYE_PUPIL; g2.fill(Ellipse2D.Double(-2.15, -0.55, 0.7, 0.7))
        } finally {
            g2.dispose()
        }
    }

    private fun paintTiny(g: Graphics2D, tx: Double, ty: Double, wiggleA: Boolean) {
        val g2 = g.create() as Graphics2D
        try {
            g2.translate(tx, ty)
            g2.color = SML_BODY;   g2.fill(Ellipse2D.Double(-2.2, -1.0, 4.4, 2.0))
            g2.color = SML_BELLY;  g2.fill(Ellipse2D.Double(-1.8, -0.1, 3.6, 1.0))
            g2.color = SML_DORSAL
            val tail = Path2D.Double()
            if (wiggleA) { tail.moveTo(1.8, 0.0); tail.lineTo(3.5, -1.2); tail.lineTo(3.2, 0.0); tail.lineTo(3.5,  1.2) }
            else         { tail.moveTo(1.8, 0.0); tail.lineTo(3.5,  1.2); tail.lineTo(3.2, 0.0); tail.lineTo(3.5, -1.2) }
            tail.closePath(); g2.fill(tail)
            g2.color = EYE_PUPIL; g2.fill(Ellipse2D.Double(-1.7, -0.4, 0.6, 0.6))
        } finally {
            g2.dispose()
        }
    }

    // --- Progress bar paint overrides ---

    override fun paintDeterminate(g: Graphics, c: JComponent) {
        val g2 = g.create() as Graphics2D
        try {
            val progress = progressBar.percentComplete
            // only update direction when progress actually moves — avoids flicker from
            // multiple paint calls per frame where progress == prevProgress
            if (progress > prevProgress) facingRight = true
            else if (progress < prevProgress) facingRight = false
            prevProgress = progress
            paintScene(g2, c.width, c.height, progress, facingRight)
        } finally {
            g2.dispose()
        }
    }

    override fun paintIndeterminate(g: Graphics, c: JComponent) {
        val g2 = g.create() as Graphics2D
        try {
            // direction comes from phase, not value comparison — no flicker at turnaround
            val t = (frame % 240) / 240.0
            val pingpong = if (t < 0.5) t * 2.0 else (1.0 - t) * 2.0
            paintScene(g2, c.width, c.height, pingpong, t < 0.5)
        } finally {
            g2.dispose()
        }
    }
}
