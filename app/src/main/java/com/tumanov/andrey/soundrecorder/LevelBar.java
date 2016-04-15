package com.tumanov.andrey.soundrecorder;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * !!! This view doesn't support WRAP_CONTENT
 */
public class LevelBar extends View {

    private List<Short> samples = new ArrayList<>();
    private float[] points;
    private Paint painter;

    public LevelBar(Context context) {
        this(context, null);
    }

    public LevelBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LevelBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LevelBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        painter = new Paint();
        painter.setColor(Color.CYAN);
        recalcPoints();
    }

    public void addSamples(Short ... samples) {
        this.samples.addAll(Arrays.asList(samples));
        recalcPoints();
        invalidate();
    }

    public void clear() {
        samples.clear();
        invalidate();
    }

    private void recalcPoints() {
        if (samples.size() < 1) {
            points = new float[0];
            return;
        }

        int w = getWidth();
        int h = getHeight();
        points = new float[samples.size() * 4];
        float step = w / samples.size();


        painter.setStrokeWidth(step);
        double scaleFactor = 10. * h / ((int)Short.MAX_VALUE - (int)Short.MIN_VALUE);
        for (int i = 0, samplesSize = samples.size(); i < samplesSize; i++) {
            short sample = samples.get(i);
            float x = step * i + step / 2;
            float y = (float) (sample * scaleFactor);
            points[i * 4] = x;
            points[i * 4 + 1] = 0;
            points[i * 4 + 2] = x;
            points[i * 4 + 3] = y;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (points.length < 4)
            return;
        canvas.translate(0, getHeight() / 2);
        canvas.drawLines(points, painter);
    }
}
