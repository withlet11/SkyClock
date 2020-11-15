package io.github.withlet11.skyclock.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import io.github.withlet11.skyclock.R
import java.time.LocalDate
import kotlin.math.PI

class ClockBasePanel(context: Context) : AbstractPanel() {
    var paint = Paint().apply { isAntiAlias = true }
    private val bezelColor = context.getColor(R.color.darkBlue)
    private val minuteGridColor = context.getColor(R.color.gray)
    private val datePanelColor = context.getColor(R.color.lightGray)
    private val todayGridColor = context.getColor(R.color.red)
    private val dayGridColor = context.getColor(R.color.black)
    private val monthBorderColor = context.getColor(R.color.darkGray)
    private val monthNameColor = context.getColor(R.color.black)
    private val skyBackGroundColor = context.getColor(R.color.midnightBlue)

    var currentDate: LocalDate = LocalDate.now()
    private var offset = 0f
    private var direction = false

    fun draw() {
        draw(Canvas(bmp))
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.run {
            drawBackPanel()
            drawGrid()
            drawDate()
        }
    }

    private fun Canvas.drawBackPanel() {
        listOf(
            BEZEL_RADIUS to bezelColor,
            DATE_PANEL_RADIUS to datePanelColor,
            SKY_BACKGROUND_RADIUS to skyBackGroundColor
        ).forEach { (r, color) ->
            paint.color = color
            drawCircle(0f, 0f, r, paint)
        }
    }

    private fun Canvas.drawGrid() {
        val rectangleSize = 22f
        val dot1radius = 8f
        val dot2radius = 4f
        val intervalOfDoubleRect = 8f
        val doubleRect1OffsetX = -rectangleSize - intervalOfDoubleRect * 0.5f
        val doubleRect2OffsetX = intervalOfDoubleRect * 0.5f
        val singleRectOffsetX = rectangleSize * 0.5f
        val offsetY =
            -io.github.withlet11.skyclock.view.AbstractPanel.DATE_PANEL_RADIUS - rectangleSize
        val dot1OffsetY = offsetY + rectangleSize * 0.5f
        val dot2OffsetY = offsetY + rectangleSize * 0.5f

        paint.color = minuteGridColor
        paint.style = Paint.Style.FILL
        for (i in 0..59) {
            save()
            rotate(i * 6f) // 6 = 360 / 60
            when {
                i == 0 -> {
                    drawRect(
                        doubleRect1OffsetX,
                        offsetY,
                        doubleRect1OffsetX + rectangleSize,
                        offsetY + rectangleSize,
                        paint
                    )
                    drawRect(
                        doubleRect2OffsetX,
                        offsetY,
                        doubleRect2OffsetX + rectangleSize,
                        offsetY + rectangleSize,
                        paint
                    )
                }
                i % 15 == 0 ->
                    drawRect(
                        -singleRectOffsetX,
                        offsetY,
                        singleRectOffsetX,
                        offsetY + rectangleSize,
                        paint
                    )
                i % 5 == 0 ->
                    drawCircle(0f, dot1OffsetY, dot1radius, paint)
                else ->
                    drawCircle(0f, dot2OffsetY, dot2radius, paint)
            }
            restore()
        }
    }

    private fun Canvas.drawDate() {
        val circleY = 341f
        val lengthOfYear = currentDate.lengthOfYear()
        for (dayOfYear in 1 until lengthOfYear) {
            val date = LocalDate.ofYearDay(currentDate.year, dayOfYear)
            save()
            // angle + 180 because text is drawn at opposite side
            rotate((-360f / lengthOfYear * dayOfYear + offset + 180f) * if (direction) -1f else 1f)

            val (color, r) = when {
                date == currentDate -> todayGridColor to 4f
                date.dayOfMonth % 10 == 0 -> dayGridColor to 3f
                date.dayOfMonth % 5 == 0 -> dayGridColor to 2f
                else -> dayGridColor to 1f
            }

            paint.style = Paint.Style.FILL
            paint.color = color
            drawCircle(0f, circleY, r, paint)

            drawMonth(date)
            restore()
        }
    }

    private fun Canvas.drawMonth(date: LocalDate) {
        paint.textSize = 24f
        when (date.dayOfMonth) {
            1 -> {
                paint.color = monthBorderColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                val startY = io.github.withlet11.skyclock.view.AbstractPanel.SKY_BACKGROUND_RADIUS
                val stopY = io.github.withlet11.skyclock.view.AbstractPanel.DATE_PANEL_RADIUS
                val offsetRate = PI.toFloat() / if (direction) 365f else -365f
                drawLine(
                    startY * offsetRate,
                    startY,
                    stopY * offsetRate,
                    stopY,
                    paint
                )
            }
            15 -> {
                paint.color = monthNameColor
                paint.style = Paint.Style.FILL
                val monthName = date.month.name
                val fontMetrics = paint.fontMetrics
                val r = 356f - (fontMetrics.ascent + fontMetrics.descent) * 0.5f
                val ratio = 180f / PI.toFloat() / r
                val start = ratio * paint.measureText(monthName) * 0.5f
                save()
                rotate(start)
                monthName.forEach { name ->
                    drawText(name.toString(), 0f, r, paint)
                    val step = -ratio * paint.measureText(name.toString())
                    rotate(step)
                }
                restore()
            }
        }
    }

    fun set(offset: Float, direction: Boolean) {
        this.offset = offset
        this.direction = direction
    }
}