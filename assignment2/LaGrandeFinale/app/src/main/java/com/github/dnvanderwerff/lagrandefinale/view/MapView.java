package com.github.dnvanderwerff.lagrandefinale.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.github.dnvanderwerff.lagrandefinale.particle.Cell;
import com.github.dnvanderwerff.lagrandefinale.particle.CollisionMap;
import com.github.dnvanderwerff.lagrandefinale.particle.Particle;
import com.github.dnvanderwerff.lagrandefinale.particle.ParticleController;

/**
 * Created by Jeroen on 21/05/2016.
 */
public class MapView extends View {
    private CollisionMap collisionMap;
    private ParticleController particleController;
    private Bitmap background;

    public MapView(Context context) {
        super(context);
    }

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MapView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void initialize(CollisionMap m, ParticleController pc) {
        collisionMap = m;
        particleController = pc;
    }

    private void createBackground(int w, int h) {
        if (collisionMap == null) return;
        // Create bitmap of background
        float tileW = ((float)w) / collisionMap.width;
        float tileH = ((float)h) / collisionMap.height;

        background = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(background);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(1.0f);

        // Draw background
        int[][] map = collisionMap.getMap();
        for (int y = 0; y < collisionMap.height; y++) {
            for (int x = 0; x < collisionMap.width; x++) {
                if (map[y][x] != 0)
                    paint.setColor(Color.WHITE);
                else
                    paint.setColor(Color.BLACK);

                canvas.drawRect(x * tileW, y * tileH, x * tileW + tileW, y * tileH + tileH, paint);
            }
        }

        // Draw cell numbers
        paint.setTextSize(48);
        float meterW = tileW * 10;
        float meterH = tileH * 10;
        float tpX, tpY;
        String ctxt;
        Cell[] cells = collisionMap.getCells();
        paint.setTextAlign(Paint.Align.CENTER);
        for (Cell c : cells) {
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(
                    c.x * meterW,
                    c.y * meterH,
                    (c.x + c.width) * meterW,
                    (c.y + c.height) * meterH,
                    paint);
            paint.setStyle(Paint.Style.FILL);
            ctxt = "C" + c.cellNo;
            tpX = (c.x + c.width / 2) * meterW;
            tpY = (c.y + c.height / 2) * meterH - ((paint.descent() + paint.ascent()) / 2.0f);
            canvas.drawText(ctxt,
                    tpX,
                    tpY,
                    paint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        createBackground(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();

        if (collisionMap == null) return;

        float tileW = ((float)w) / collisionMap.width;
        float tileH = ((float)h) / collisionMap.height;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        // Draw background
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(background,0,0, null);

        // Draw particools
        Particle[] particools = particleController.getParticles();
        paint.setColor(Color.RED);
        float meterToCanvasW = tileW * 10;
        float meterToCanvasH = tileH * 10;
        for (Particle particool : particools) {
            if (!particool.valid) continue;
            canvas.drawCircle(
                    (float)particool.x * meterToCanvasW,
                    (float)particool.y * meterToCanvasH, 2, paint);
        }
    }

    public void update() {
        invalidate();
    }
}
