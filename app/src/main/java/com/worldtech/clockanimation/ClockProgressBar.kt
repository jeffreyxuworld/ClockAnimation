package com.worldtech.clockanimation

import android.content.Context
import android.graphics.*
import android.graphics.BlurMaskFilter.Blur
import android.graphics.Paint.Cap
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.annotation.IntDef
import com.worldtech.clockanimation.utils.CommonUtil.dip2px
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

class ClockProgressBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {
    private val mProgressRectF = RectF()
    private val mBoundsRectF = RectF()
    private val mProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mProgressBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    //    private final Paint mProgressTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private val mCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mClockHandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mRadius = 0f
    private var mCenterX = 0f
    private var mCenterY = 0f
    private var mProgress = 0
    private var mMax = DEFAULT_MAX

    //Only work well in the Line Style, represents the line count of the rings included
    private var mLineCount = 0

    //Only work well in the Line Style, Height of the line of the progress bar
    private var mLineWidth = 0f

    //Stroke width of the progress of the progress bar
    private var mProgressStrokeWidth = 0f

    //Text size of the progress of the progress bar
    //    private float mProgressTextSize;
    //Start color of the progress of the progress bar
    private var mProgressStartColor = 0

    //End color of the progress of the progress bar
    private var mProgressEndColor = 0

    //Color of the progress value of the progress bar
    private var mProgressTextColor = 0

    //Background color of the progress of the progress bar
    private var mProgressBackgroundColor = 0

    //the rotate degree of the canvas, default is -90.
    private var mStartDegree = 0

    // whether draw the background only outside the progress area or not
    private var mDrawBackgroundOutsideProgress = false

    // Format the current progress value to the specified format
    private var mProgressFormatter: ProgressFormatter? = DefaultProgressFormatter()

    // The style of the progress color
    @Style
    private var mStyle = 0

    // The Shader of mProgressPaint
    @ShaderMode
    private var mShader = 0

    // The Stroke Cap of mProgressPaint and mProgressBackgroundPaint
    private var mCap: Cap? = null

    // The blur radius of mProgressPaint
    private var mBlurRadius = 0

    // The blur style of mProgressPaint
    private var mBlurStyle: Blur? = null
    private val mWidth = dip2px(48f)
    private val mHeight = dip2px(48f)

    /**
     * Basic data initialization
     */
    private fun initFromAttributes(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CircleProgressBar)
        mLineCount = a.getInt(R.styleable.CircleProgressBar_line_count, DEFAULT_LINE_COUNT)
        mStyle = a.getInt(R.styleable.CircleProgressBar_progress_style, LINE)
        mShader = a.getInt(R.styleable.CircleProgressBar_progress_shader, LINEAR)
        mCap =
            if (a.hasValue(R.styleable.CircleProgressBar_progress_stroke_cap)) Cap.values()[a.getInt(
                R.styleable.CircleProgressBar_progress_stroke_cap,
                0
            )] else Cap.BUTT
        mLineWidth = a.getDimensionPixelSize(
            R.styleable.CircleProgressBar_line_width,
            dip2px(getContext(), DEFAULT_LINE_WIDTH)
        ).toFloat()
        //        mProgressTextSize = a.getDimensionPixelSize(R.styleable.CircleProgressBar_progress_text_size, dip2px(getContext(), DEFAULT_PROGRESS_TEXT_SIZE));
        mProgressStrokeWidth = a.getDimensionPixelSize(
            R.styleable.CircleProgressBar_progress_stroke_width,
            dip2px(getContext(), DEFAULT_PROGRESS_STROKE_WIDTH)
        ).toFloat()
        mProgressStartColor = a.getColor(
            R.styleable.CircleProgressBar_progress_start_color, Color.parseColor(
                COLOR_FFF2A670
            )
        )
        mProgressEndColor = a.getColor(
            R.styleable.CircleProgressBar_progress_end_color, Color.parseColor(
                COLOR_FFF2A670
            )
        )
        mProgressTextColor = a.getColor(
            R.styleable.CircleProgressBar_progress_text_color, Color.parseColor(
                COLOR_FFF2A670
            )
        )
        mProgressBackgroundColor = a.getColor(
            R.styleable.CircleProgressBar_progress_background_color, Color.parseColor(
                COLOR_FFD3D3D5
            )
        )
        mStartDegree =
            a.getInt(R.styleable.CircleProgressBar_progress_start_degree, DEFAULT_START_DEGREE)
        mDrawBackgroundOutsideProgress =
            a.getBoolean(R.styleable.CircleProgressBar_drawBackgroundOutsideProgress, false)
        mBlurRadius = a.getDimensionPixelSize(R.styleable.CircleProgressBar_progress_blur_radius, 0)
        val blurStyle = a.getInt(R.styleable.CircleProgressBar_progress_blur_style, 0)
        mBlurStyle = when (blurStyle) {
            1 -> Blur.SOLID
            2 -> Blur.OUTER
            3 -> Blur.INNER
            else -> Blur.NORMAL
        }
        a.recycle()
    }

    /**
     * Paint initialization
     */
    private fun initPaint() {
//        mProgressTextPaint.setTextAlign(Paint.Align.CENTER);
//        mProgressTextPaint.setTextSize(mProgressTextSize);
        mProgressPaint.style =
            if (mStyle == SOLID) Paint.Style.FILL else Paint.Style.STROKE
        mProgressPaint.strokeWidth = mProgressStrokeWidth
        mProgressPaint.color = mProgressStartColor
        mProgressPaint.strokeCap = mCap
        updateMaskBlurFilter()
        mProgressBackgroundPaint.style =
            if (mStyle == SOLID) Paint.Style.FILL else Paint.Style.STROKE
        mProgressBackgroundPaint.strokeWidth = mProgressStrokeWidth
        mProgressBackgroundPaint.color = mProgressBackgroundColor
        mProgressBackgroundPaint.strokeCap = mCap
    }

    private fun updateMaskBlurFilter() {
        if (mBlurStyle != null && mBlurRadius > 0) {
            setLayerType(LAYER_TYPE_SOFTWARE, mProgressPaint)
            mProgressPaint.maskFilter = BlurMaskFilter(mBlurRadius.toFloat(), mBlurStyle)
        } else {
            mProgressPaint.maskFilter = null
        }
    }

    /**
     * The progress bar color gradient,
     * need to be invoked in the [.onSizeChanged]
     */
    private fun updateProgressShader() {
        if (mProgressStartColor != mProgressEndColor) {
            var shader: Shader? = null
            when (mShader) {
                LINEAR -> {
                    shader = LinearGradient(
                        mProgressRectF.left, mProgressRectF.top,
                        mProgressRectF.left, mProgressRectF.bottom,
                        mProgressStartColor, mProgressEndColor, Shader.TileMode.CLAMP
                    )
                    val matrix = Matrix()
                    matrix.setRotate(LINEAR_START_DEGREE, mCenterX, mCenterY)
                    shader.setLocalMatrix(matrix)
                }
                RADIAL -> {
                    shader = RadialGradient(
                        mCenterX, mCenterY, mRadius,
                        mProgressStartColor, mProgressEndColor, Shader.TileMode.CLAMP
                    )
                }
                SWEEP -> {

                    //arc = radian * radius
                    val radian = (mProgressStrokeWidth / Math.PI * 2.0f / mRadius).toFloat()
                    val rotateDegrees = if (mCap == Cap.BUTT && mStyle == SOLID_LINE) 0 else -(Math.toDegrees(radian.toDouble()).toFloat())
                    shader = SweepGradient(
                        mCenterX,
                        mCenterY,
                        intArrayOf(mProgressStartColor, mProgressEndColor),
                        floatArrayOf(0.0f, 1.0f)
                    )
                    val matrix = Matrix()
                    matrix.setRotate(rotateDegrees as Float, mCenterX, mCenterY)
                    shader.setLocalMatrix(matrix)
                }
                else -> {
                }
            }
            mProgressPaint.shader = shader
        } else {
            mProgressPaint.shader = null
            mProgressPaint.color = mProgressStartColor
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.rotate(mStartDegree.toFloat(), mCenterX, mCenterY)
        drawProgress(canvas)
        canvas.restore()
        /**
         * 画指针
         */
        val degreesH = mProgress * 3.6f
        canvas.rotate(degreesH, (mWidth / 2).toFloat(), (mHeight / 2).toFloat())
        //小圆和指针颜色
        mClockHandPaint.color = resources.getColor(R.color.color_c0c0c0)
        mClockHandPaint.strokeWidth = dip2px(2f).toFloat()
        val lengthH = (mHeight / 2 - dip2px(8f)).toFloat()
        //第四个参数的解释：因为半径是 height/2 或 width/2，线是从圆心开始的，所以结束的Y坐标是：height - height/2 - length
        canvas.drawLine(
            (mWidth / 2).toFloat(),
            (mHeight / 2).toFloat(),
            (mWidth / 2).toFloat(),
            mHeight - mHeight / 2 - lengthH,
            mClockHandPaint
        )
        /**
         * 画指针围绕的镂空圆心
         */
        mCirclePaint.color = resources.getColor(R.color.color_c0c0c0)
        canvas.drawCircle(mCenterX, mCenterY, dip2px(3f).toFloat(), mCirclePaint)
        drawProgressText(canvas)
    }

    /**
     * Just draw arc
     */
    private fun drawSolidProgress(canvas: Canvas) {
        if (mDrawBackgroundOutsideProgress) {
            val startAngle = MAX_DEGREE * mProgress / mMax
            val sweepAngle = MAX_DEGREE - startAngle
            canvas.drawArc(mProgressRectF, startAngle, sweepAngle, true, mProgressBackgroundPaint)
        } else {
            canvas.drawArc(mProgressRectF, 0.0f, MAX_DEGREE, true, mProgressBackgroundPaint)
        }
        canvas.drawArc(mProgressRectF, 0.0f, MAX_DEGREE * mProgress / mMax, true, mProgressPaint)
    }

    private fun drawProgressText(canvas: Canvas) {
        if (mProgressFormatter == null) {
            return
        }
        val progressText = mProgressFormatter!!.format(mProgress, mMax)
        if (TextUtils.isEmpty(progressText)) {
            return
        }

//        mProgressTextPaint.setTextSize(mProgressTextSize);
//        mProgressTextPaint.setColor(mProgressTextColor);

//        mProgressTextPaint.getTextBounds(String.valueOf(progressText), 0, progressText.length(), mProgressTextRect);
//        canvas.drawText(progressText, 0, progressText.length(), mCenterX, mCenterY + mProgressTextRect.height() / 2, mProgressTextPaint);
    }

    private fun drawProgress(canvas: Canvas) {
        when (mStyle) {
            SOLID -> drawSolidProgress(canvas)
            SOLID_LINE -> drawSolidLineProgress(canvas)
            LINE -> drawLineProgress(canvas)
            else -> drawLineProgress(canvas)
        }
    }

    /**
     * In the center of the drawing area as a reference point , rotate the canvas
     */
    private fun drawLineProgress(canvas: Canvas) {
        val unitDegrees = (2.0f * Math.PI / mLineCount).toFloat()
        val outerCircleRadius = mRadius
        val interCircleRadius = mRadius - mLineWidth
        val progressLineCount = (mProgress.toFloat() / mMax.toFloat() * mLineCount).toInt()
        for (i in 0 until mLineCount) {
            val rotateDegrees = i * -unitDegrees
            val startX = mCenterX + Math.cos(rotateDegrees.toDouble()).toFloat() * interCircleRadius
            val startY = mCenterY - Math.sin(rotateDegrees.toDouble()).toFloat() * interCircleRadius
            val stopX = mCenterX + Math.cos(rotateDegrees.toDouble()).toFloat() * outerCircleRadius
            val stopY = mCenterY - Math.sin(rotateDegrees.toDouble()).toFloat() * outerCircleRadius
            if (mDrawBackgroundOutsideProgress) {
                if (i >= progressLineCount) {
                    canvas.drawLine(startX, startY, stopX, stopY, mProgressBackgroundPaint)
                }
            } else {
                canvas.drawLine(startX, startY, stopX, stopY, mProgressBackgroundPaint)
            }
            if (i < progressLineCount) {
                canvas.drawLine(startX, startY, stopX, stopY, mProgressPaint)
            }
        }
    }

    /**
     * Just draw arc
     */
    private fun drawSolidLineProgress(canvas: Canvas) {
        if (mDrawBackgroundOutsideProgress) {
            val startAngle = MAX_DEGREE * mProgress / mMax
            val sweepAngle = MAX_DEGREE - startAngle
            canvas.drawArc(mProgressRectF, startAngle, sweepAngle, false, mProgressBackgroundPaint)
        } else {
            canvas.drawArc(mProgressRectF, 0.0f, MAX_DEGREE, false, mProgressBackgroundPaint)
        }
        canvas.drawArc(mProgressRectF, 0.0f, MAX_DEGREE * mProgress / mMax, false, mProgressPaint)
    }

    /**
     * When the size of CircleProgressBar changed, need to re-adjust the drawing area
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mBoundsRectF.left = paddingLeft.toFloat()
        mBoundsRectF.top = paddingTop.toFloat()
        mBoundsRectF.right = (w - paddingRight).toFloat()
        mBoundsRectF.bottom = (h - paddingBottom).toFloat()
        mCenterX = mBoundsRectF.centerX()
        mCenterY = mBoundsRectF.centerY()
        mRadius = Math.min(mBoundsRectF.width(), mBoundsRectF.height()) / 2
        mProgressRectF.set(mBoundsRectF)
        updateProgressShader()
        mProgressRectF.inset(mProgressStrokeWidth / 2, mProgressStrokeWidth / 2)
    }

    public override fun onSaveInstanceState(): Parcelable? {
        // Force our ancestor class to save its state
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.progress = mProgress
        return ss
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        progress = ss.progress
    }

    fun setProgressFormatter(progressFormatter: ProgressFormatter?) {
        mProgressFormatter = progressFormatter
        invalidate()
    }

    fun setProgressStrokeWidth(progressStrokeWidth: Float) {
        mProgressStrokeWidth = progressStrokeWidth
        mProgressRectF.set(mBoundsRectF)
        updateProgressShader()
        mProgressRectF.inset(mProgressStrokeWidth / 2, mProgressStrokeWidth / 2)
        invalidate()
    }

    //    public void setProgressTextSize(float progressTextSize) {
    //        mProgressTextSize = progressTextSize;
    //        invalidate();
    //    }
    fun setProgressStartColor(progressStartColor: Int) {
        mProgressStartColor = progressStartColor
        updateProgressShader()
        invalidate()
    }

    fun setProgressEndColor(progressEndColor: Int) {
        mProgressEndColor = progressEndColor
        updateProgressShader()
        invalidate()
    }

    fun setProgressTextColor(progressTextColor: Int) {
        mProgressTextColor = progressTextColor
        invalidate()
    }

    fun setProgressBackgroundColor(progressBackgroundColor: Int) {
        mProgressBackgroundColor = progressBackgroundColor
        mProgressBackgroundPaint.color = mProgressBackgroundColor
        invalidate()
    }

    fun setLineCount(lineCount: Int) {
        mLineCount = lineCount
        invalidate()
    }

    fun setLineWidth(lineWidth: Float) {
        mLineWidth = lineWidth
        invalidate()
    }

    fun setStyle(@Style style: Int) {
        mStyle = style
        mProgressPaint.style =
            if (mStyle == SOLID) Paint.Style.FILL else Paint.Style.STROKE
        mProgressBackgroundPaint.style =
            if (mStyle == SOLID) Paint.Style.FILL else Paint.Style.STROKE
        invalidate()
    }

    fun setBlurRadius(blurRadius: Int) {
        mBlurRadius = blurRadius
        updateMaskBlurFilter()
        invalidate()
    }

    fun setBlurStyle(blurStyle: Blur?) {
        mBlurStyle = blurStyle
        updateMaskBlurFilter()
        invalidate()
    }

    fun setShader(@ShaderMode shader: Int) {
        mShader = shader
        updateProgressShader()
        invalidate()
    }

    fun setCap(cap: Cap?) {
        mCap = cap
        mProgressPaint.strokeCap = cap
        mProgressBackgroundPaint.strokeCap = cap
        invalidate()
    }

    fun setStartDegree(startDegree: Int) {
        mStartDegree = startDegree
        invalidate()
    }

    fun setDrawBackgroundOutsideProgress(drawBackgroundOutsideProgress: Boolean) {
        mDrawBackgroundOutsideProgress = drawBackgroundOutsideProgress
        invalidate()
    }

    var progress: Int
        get() = mProgress
        set(progress) {
            mProgress = progress
            invalidate()
        }
    var max: Int
        get() = mMax
        set(max) {
            mMax = max
            invalidate()
        }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(LINE, SOLID, SOLID_LINE)
    private annotation class Style

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(LINEAR, RADIAL, SWEEP)
    private annotation class ShaderMode
    interface ProgressFormatter {
        fun format(progress: Int, max: Int): CharSequence
    }

    private class DefaultProgressFormatter : ProgressFormatter {
        override fun format(progress: Int, max: Int): CharSequence {
            return String.format(
                DEFAULT_PATTERN,
                (progress.toFloat() / max.toFloat() * 100).toInt()
            )
        }

        companion object {
            private const val DEFAULT_PATTERN = "%d%%"
        }
    }

    private class SavedState : BaseSavedState {
        var progress = 0

        internal constructor(superState: Parcelable?) : super(superState) {}
        private constructor(`in`: Parcel) : super(`in`) {
            progress = `in`.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(progress)
        }

        companion object {
            @JvmField
            val CREATOR: Creator<SavedState?> = object : Creator<SavedState?> {
                override fun createFromParcel(`in`: Parcel): SavedState? {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {
        const val LINE = 0
        const val SOLID = 1
        const val SOLID_LINE = 2
        const val LINEAR = 0
        const val RADIAL = 1
        const val SWEEP = 2
        private const val DEFAULT_MAX = 100
        private const val MAX_DEGREE = 360.0f
        private const val LINEAR_START_DEGREE = 90.0f
        private const val DEFAULT_START_DEGREE = -90
        private const val DEFAULT_LINE_COUNT = 45
        private const val DEFAULT_LINE_WIDTH = 4.0f
        private const val DEFAULT_PROGRESS_TEXT_SIZE = 11.0f
        private const val DEFAULT_PROGRESS_STROKE_WIDTH = 1.0f
        private const val COLOR_FFF2A670 = "#fff2a670"
        private const val COLOR_FFD3D3D5 = "#ffe3e3e5"
        private fun dip2px(context: Context, dpValue: Float): Int {
            val scale = context.resources.displayMetrics.density
            return (dpValue * scale + 0.5f).toInt()
        }
    }

    init {
        initFromAttributes(context, attrs)
        initPaint()
    }
}