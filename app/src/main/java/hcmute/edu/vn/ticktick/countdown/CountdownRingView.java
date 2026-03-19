package hcmute.edu.vn.ticktick.countdown;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import hcmute.edu.vn.ticktick.R;

public class CountdownRingView extends View {

    private final RectF arcBounds = new RectF();
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float progressFraction = 0f;

    public CountdownRingView(Context context) {
        this(context, null);
    }

    public CountdownRingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CountdownRingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        float strokeWidth = dp(10f);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setColor(ContextCompat.getColor(context, R.color.countdown_ring_track));
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setColor(ContextCompat.getColor(context, R.color.countdown_ring_progress));
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setProgressFraction(float progressFraction) {
        float clamped = Math.max(0f, Math.min(1f, progressFraction));
        if (this.progressFraction == clamped) {
            return;
        }
        this.progressFraction = clamped;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float strokeInset = progressPaint.getStrokeWidth() / 2f + dp(2f);
        arcBounds.set(strokeInset, strokeInset, w - strokeInset, h - strokeInset);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawArc(arcBounds, -90f, 360f, false, trackPaint);
        canvas.drawArc(arcBounds, -90f, 360f * progressFraction, false, progressPaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
