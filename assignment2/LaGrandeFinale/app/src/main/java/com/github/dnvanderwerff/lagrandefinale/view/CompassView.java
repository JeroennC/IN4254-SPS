package com.github.dnvanderwerff.lagrandefinale.view;

/**
 * Displays a compass so that the user can see where the measured magnetic north lies
 */
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CompassView extends View {

    private float directionNorth, directionMe;

    public CompassView(Context context) {
        super(context);
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CompassView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        int r;
        if(w > h){
            r = h/2;
        }else{
            r = w/2;
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(w/2, h/2, r, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.WHITE);

        canvas.drawCircle(w/2, h/2, r, paint);

        canvas.drawLine(
                w/2,
                h/2,
                (float)(w/2 + (r-3) * Math.sin(-directionNorth)),
                (float)(h/2 - (r-3) * Math.cos(-directionNorth)),
                paint);

        paint.setColor(Color.rgb(204,0,0));
        canvas.drawLine(
                w/2,
                h/2,
                (float)(w/2 + (r-3) * -1 * Math.sin(-directionNorth)),
                (float)(h/2 - (r-3) * -1 * Math.cos(-directionNorth)),
                paint);

        paint.setColor(Color.rgb(0,255,255));

        canvas.drawLine(
                w/2,
                h/2,
                (float)(w/2 + (r-10) * Math.sin(-directionMe)),
                (float)(h/2 - (r-10) * Math.cos(-directionMe)),
                paint);
    }

    public void update(float dirNorth, float dirMe){
        directionNorth = dirNorth;
        directionMe = dirMe;
        invalidate();
    }

}