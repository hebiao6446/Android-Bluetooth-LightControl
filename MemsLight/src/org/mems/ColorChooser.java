package org.mems;import android.content.Context;import android.graphics.Bitmap;import android.graphics.Canvas;import android.graphics.Color;import android.graphics.Paint;import android.graphics.drawable.BitmapDrawable;import android.util.AttributeSet;import android.util.Log;import android.view.MotionEvent;import android.widget.ImageView;public class ColorChooser extends ImageView {    private final int mBitmapWidth;    private final int  mBitmapHeight;    private Bitmap bitmap;    private ImageRGBDelegate delegate;    private int currentX, currentY;    private int mCurrentColor = 0;    private Paint mPaint, mYinPaint;    public ColorChooser(Context context, AttributeSet attrs) {        super(context, attrs);        BitmapDrawable bd = (BitmapDrawable) getDrawable();        bitmap = bd.getBitmap();        mBitmapWidth = bitmap.getWidth();        mBitmapHeight = bitmap.getHeight();        Log.d("temp", "w="+mBitmapWidth+".h="+mBitmapHeight);        setClickable(true);        mPaint = new Paint();        mPaint.setAntiAlias(true);        mPaint.setStrokeWidth(50f);        /*        mPaint.setStyle(Paint.Style.STROKE);        mYinPaint = new Paint();        mYinPaint.setAntiAlias(true);        mYinPaint.setStrokeWidth(10f);        mYinPaint.setStyle(Paint.Style.STROKE);        mYinPaint.setColor(context.getResources().getColor(R.color.stroke));*/        mCurrentColor = SharedPrefManager.getColor(getContext());        Log.d("temp", "ColorChooser().mCurrentColor="+mCurrentColor);        mPaint.setColor(mCurrentColor);        invalidate();    }    public void setDelegate(ImageRGBDelegate delegate) {        this.delegate = delegate;    }    @Override    public boolean performClick() {        return super.performClick();    }    @Override    public boolean onTouchEvent(MotionEvent event) {        currentX = (int) (event.getX() * mBitmapWidth / getWidth());        currentY = (int) (event.getY() * mBitmapHeight / getHeight());//        Log.d("temp", "onTouchEvent:w="+getWidth()+".h="+getHeight());        mCurrentColor = getColor(currentX, currentY);        mPaint.setColor(mCurrentColor);        final int action = event.getAction();        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {            //            invalidate();        }        else if (action == MotionEvent.ACTION_UP) {            delegate.imageColor(mCurrentColor);            SharedPrefManager.saveColor(getContext(), mCurrentColor);            Log.d("temp", "onTouchEvent-->save.mCurrentColor="+mCurrentColor);            performClick();        }        return super.onTouchEvent(event);    }    @Override    protected void onDraw(Canvas canvas) {        Log.d("temp", "onDraw-->in.mCurrentColor="+mCurrentColor);        final int w = getWidth();//        canvas.drawCircle(w / 2, w / 2, w / 2-40, mYinPaint);        if (mCurrentColor != 0) {            canvas.drawCircle(w/2, w/2, w/2 - 10, mPaint);        }        super.onDraw(canvas);    }    private int getColor(int x, int y) {        try {            int color = bitmap.getPixel(x, y);            if (color != 0) {                return color;            } else {                return Color.WHITE;            }        }catch (IllegalArgumentException e) {            return Color.WHITE;        }    }    public void recycle() {        this.bitmap.recycle();    }    public interface ImageRGBDelegate {        void imageColor(int color);    }}