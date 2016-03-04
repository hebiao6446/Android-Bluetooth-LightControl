package org.mems;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerView extends View {

	private Context mContext;
	
	private Paint mRightPaint;
	
	private int mHeight;//real view height
	private int mWidth;//real view width
	
	private int[] mRightColors;
	
	private int RIGHT_WIDTH;
	private int LEFT_WIDTH;
	
	private Bitmap mLeftBitmap;
	
	private Bitmap mLeftBitmap2;
	
	private Bitmap mRightBitmap;

	private Bitmap mRightBitmap2;
	
	private Paint mBitmapPaint;
	
	private final int SPLIT_WIDTH, SPLIT_WIDTH_2, SPLIT_WIDTH_3;
	
	private boolean downInLeft = false;
	
	private boolean downInRight = false;
	
	private PointF mLeftSelectPoint; 
	
	private PointF mRightSelectPoint;
	
	private OnColorChangedListener mChangedListener;
	
	private boolean mLeftMove = false;
	
	private boolean mRightMove = false;
	
	private float mLeftBitmapRadius;
	
	private Bitmap mGradualChangeBitmap;
	
	private float mRightBitmapHalfHeight;
	
	private float mRightBitmapQuarterWidth;
	
	private int mCallBackColor = Integer.MAX_VALUE;
    private Paint mSidePaint;
    private float mRgbCenterX = -1;
    private float mRgbCenterY = -1;
    private float mRgbRadio = -1;
    private Rect mLeftBitmapRect = null;

    public ColorPickerView(Context context) {
		this(context, null);
	}
	
	@SuppressLint("NewApi")
	public ColorPickerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		SPLIT_WIDTH = (int) (context.getResources().getDisplayMetrics().scaledDensity*24);
        SPLIT_WIDTH_2 = SPLIT_WIDTH*2;
        SPLIT_WIDTH_3 = SPLIT_WIDTH*3;
		Log.d("temp", "SPLIT_WID="+SPLIT_WIDTH);
		init();
	}

	public void setOnColorChangedListenner(OnColorChangedListener listener) {
		mChangedListener = listener;
	}
	
	private void init() {
		mSidePaint = new Paint();
        mSidePaint.setStyle(Paint.Style.STROKE);
        mSidePaint.setStrokeWidth(10);

        mRightPaint = new Paint();
        mRightPaint.setStyle(Paint.Style.FILL);
        mRightPaint.setStrokeWidth(1);

		mRightColors = new int[3];
		mRightColors[0] = Color.BLACK;
		mRightColors[2] = Color.WHITE;
		
		mBitmapPaint = new Paint();
		
		mLeftBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.reading__color_view__button);
		mLeftBitmap2 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.reading__color_view__button_press);
		mLeftBitmapRadius = mLeftBitmap.getWidth() / 2;
		
		mRightBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.reading__color_view__saturation);
		mRightBitmap2 = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.reading__color_view__saturation_press);
		
		mRightBitmapHalfHeight = mRightBitmap.getHeight();
		mRightBitmapQuarterWidth = mRightBitmap.getWidth() / 2;
		RIGHT_WIDTH = mRightBitmap.getWidth() / 2;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// 左边
        if (mLeftBitmapRect == null) {
            mLeftBitmapRect = new Rect(SPLIT_WIDTH, SPLIT_WIDTH_3, LEFT_WIDTH + SPLIT_WIDTH, SPLIT_WIDTH_3 + LEFT_WIDTH);
        }
        canvas.drawBitmap(getGradual(), null, mLeftBitmapRect, mBitmapPaint);
        canvas.drawCircle(getWidth()/2, SPLIT_WIDTH_3 + LEFT_WIDTH/2, getWidth()/2-SPLIT_WIDTH, mSidePaint);

		// 右边
		mRightColors[1] = mRightPaint.getColor();
		Shader rightShader = new LinearGradient(SPLIT_WIDTH, SPLIT_WIDTH, mWidth-SPLIT_WIDTH, SPLIT_WIDTH_2, mRightColors, null, TileMode.MIRROR);
		mRightPaint.setShader(rightShader);
		canvas.drawRect(new Rect(SPLIT_WIDTH, SPLIT_WIDTH, mWidth-SPLIT_WIDTH, SPLIT_WIDTH_2), mRightPaint);

		if (null == mLeftSelectPoint) {
            mRgbCenterX = getWidth()/2;
            mRgbCenterY = SPLIT_WIDTH_3 + LEFT_WIDTH/2;
            mRgbRadio = LEFT_WIDTH/2 - 3;
			mLeftSelectPoint = new PointF(mRgbCenterX, mRgbCenterY);
		}
		if (null == mRightSelectPoint) {
			mRightSelectPoint = new PointF(SPLIT_WIDTH, SPLIT_WIDTH);
		}
		
		// 两个图标
		if (mLeftMove) {
			canvas.drawBitmap(mLeftBitmap, mLeftSelectPoint.x - mLeftBitmapRadius, mLeftSelectPoint.y - mLeftBitmapRadius, mBitmapPaint);
		} else {
			canvas.drawBitmap(mLeftBitmap2, mLeftSelectPoint.x - mLeftBitmapRadius, mLeftSelectPoint.y - mLeftBitmapRadius, mBitmapPaint);
		}
		
		if (mRightMove) {
			canvas.drawBitmap(mRightBitmap, mRightSelectPoint.x - mRightBitmapQuarterWidth, SPLIT_WIDTH - (mRightBitmapHalfHeight-SPLIT_WIDTH)/2, mBitmapPaint);
		} else {
			canvas.drawBitmap(mRightBitmap2, mRightSelectPoint.x - mRightBitmapQuarterWidth, SPLIT_WIDTH - (mRightBitmapHalfHeight-SPLIT_WIDTH)/2, mBitmapPaint);
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);
		if (widthMode == MeasureSpec.EXACTLY) {
			mWidth = width;
		} else {
			mWidth = 480;
		}

        LEFT_WIDTH = mWidth - SPLIT_WIDTH * 2;
		if (heightMode == MeasureSpec.EXACTLY) {
			mHeight = height;
		} else {
			mHeight = LEFT_WIDTH + SPLIT_WIDTH * 4;//350;
		}

		setMeasuredDimension(mWidth, mHeight);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			downInRight = inRightPanel(x, y);
			if (downInRight) {
				mRightMove = true;
				proofRight(x, y);
			} else {
                downInLeft = inLeftPanel(x, y);
                if (downInLeft) {
                    mLeftMove = true;
                    proofLeft(x, y);
                    int leftColor = getLeftColor(mLeftSelectPoint.x - SPLIT_WIDTH, mLeftSelectPoint.y - SPLIT_WIDTH * 3);
                    mRightPaint.setColor(leftColor);
                    mSidePaint.setColor(leftColor);
                } else {
                    break;
                }
            }
			
			invalidate();
			int rightColor = getRightColor(mRightSelectPoint.x - SPLIT_WIDTH);
			if (mCallBackColor == Integer.MAX_VALUE || mCallBackColor != rightColor) {
				mCallBackColor = rightColor;
			} else {
//				break;
			}

            if (downInLeft) {
                if (mChangedListener != null)  mChangedListener.onSingleColorChanged(mRightPaint.getColor());
            } else if (downInRight) {
                int saturate = (int) ((mRightSelectPoint.x - SPLIT_WIDTH)*100 / (mWidth - 2 * SPLIT_WIDTH));
                Log.d("temp", "saturate="+saturate);
                if (mChangedListener != null)  mChangedListener.onBrightnessChanged(mRightPaint.getColor(), saturate);
            }
			break;
		case MotionEvent.ACTION_UP:
			if (downInLeft) {
                if (mChangedListener != null)  mChangedListener.onSingleColorChanged(mRightPaint.getColor());
				downInLeft = false;
			} else if (downInRight) {
                int saturate = (int) ((mRightSelectPoint.x - SPLIT_WIDTH)*100 / (mWidth - 2 * SPLIT_WIDTH));
                Log.d("temp", "saturate="+saturate);
                if (mChangedListener != null)  mChangedListener.onBrightnessChanged(mRightPaint.getColor(), saturate);
				downInRight = false;
			} else {
                break;
            }

            invalidate();

            mLeftMove = false;
            mRightMove = false;
		}
		return true;
	}
	
	@Override
	protected void onDetachedFromWindow() {
		if (mGradualChangeBitmap != null && mGradualChangeBitmap.isRecycled() == false) {
			mGradualChangeBitmap.recycle();
		}
		if (mLeftBitmap != null && mLeftBitmap.isRecycled() == false) {
			mLeftBitmap.recycle();
		}
		if (mLeftBitmap2 != null && mLeftBitmap2.isRecycled() == false) {
			mLeftBitmap2.recycle();
		}
		if (mRightBitmap != null && mRightBitmap.isRecycled() == false) {
			mRightBitmap.recycle();
		}
		if (mRightBitmap2 != null && mRightBitmap2.isRecycled() == false) {
			mRightBitmap2.recycle();
		}
		super.onDetachedFromWindow();
	}
	
	private Bitmap getGradual() {
		if (mGradualChangeBitmap == null) {
			mGradualChangeBitmap = BitmapFactory.decodeStream(mContext.getResources()
					.openRawResource(R.drawable.rgb));
			
			mGradualChangeBitmap = Bitmap.createScaledBitmap(mGradualChangeBitmap, LEFT_WIDTH, LEFT_WIDTH, true);
		}
		return mGradualChangeBitmap;
	}
	
	private Bitmap getGradual2() {
		if (mGradualChangeBitmap == null) {
			Paint leftPaint = new Paint();
			leftPaint.setStrokeWidth(1);
			mGradualChangeBitmap = Bitmap.createBitmap(LEFT_WIDTH, mHeight - 2 * SPLIT_WIDTH, Config.RGB_565);
			Canvas canvas = new Canvas(mGradualChangeBitmap);
			int bitmapWidth = mGradualChangeBitmap.getWidth();
			LEFT_WIDTH = bitmapWidth;
			int bitmapHeight = mGradualChangeBitmap.getHeight();
			int[] leftColors = new int[] {Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA};
			Shader leftShader = new LinearGradient(0, bitmapHeight / 2, bitmapWidth, bitmapHeight / 2, leftColors, null, TileMode.REPEAT);
			LinearGradient shadowShader = new LinearGradient(bitmapWidth / 2, 0, bitmapWidth / 2, bitmapHeight,
					Color.WHITE, Color.BLACK, TileMode.CLAMP);
			ComposeShader shader = new ComposeShader(leftShader, shadowShader, PorterDuff.Mode.SCREEN);
			leftPaint.setShader(shader);
			canvas.drawRect(0, 0, bitmapWidth, bitmapHeight, leftPaint);
		}
		return mGradualChangeBitmap;
	}
	
	private boolean inLeftPanel(float x, float y) {
//		if ( SPLIT_WIDTH < x && x < SPLIT_WIDTH + LEFT_WIDTH && SPLIT_WIDTH_3 < y && y < SPLIT_WIDTH_3 + LEFT_WIDTH)
        float absX = Math.abs(mRgbCenterX-x);
        float absY = Math.abs(mRgbCenterY-y);
        if (Math.sqrt(absX * absX + absY*absY) < mRgbRadio) {
			return true;
		} else {
			return false;
		}
	}

	private boolean inRightPanel(float x, float y) {
		if (SPLIT_WIDTH < x && x < mWidth-SPLIT_WIDTH && SPLIT_WIDTH < y && y < SPLIT_WIDTH_2) {
			return true;
		} else {
			return false;
		}
	}
	
	// 校正xy
	private void proofLeft(float x, float y) {
		if (x < SPLIT_WIDTH) {
			mLeftSelectPoint.x = SPLIT_WIDTH;
		} else if (x > (SPLIT_WIDTH + LEFT_WIDTH)) {
			mLeftSelectPoint.x = SPLIT_WIDTH + LEFT_WIDTH;
		} else {
			mLeftSelectPoint.x = x;
		}
		if (y < SPLIT_WIDTH_3) {
			mLeftSelectPoint.y = SPLIT_WIDTH;
		} else if (y > (LEFT_WIDTH + SPLIT_WIDTH_3)) {
			mLeftSelectPoint.y = mHeight - SPLIT_WIDTH;
		} else {
			mLeftSelectPoint.y = y;
		}
	}
	
	private void proofRight(float x, float y) {
		if (x < SPLIT_WIDTH) {
			mRightSelectPoint.x = SPLIT_WIDTH;
		} else if (x > (mWidth - SPLIT_WIDTH )) {
			mRightSelectPoint.x = mWidth - SPLIT_WIDTH;
		} else {
			mRightSelectPoint.x = x;
		}
		if (y < SPLIT_WIDTH) {
			mRightSelectPoint.y = SPLIT_WIDTH;
		} else if (y > (SPLIT_WIDTH_2)) {
			mRightSelectPoint.y = SPLIT_WIDTH_2;
		} else {
			mRightSelectPoint.y = y;
		}
	}
	
	private int getLeftColor(float x, float y) {
		Bitmap temp = getGradual();
		// 为了防止越界
		int intX = (int) x;
		int intY = (int) y;
		if (intX >= temp.getWidth()) {
			intX = temp.getWidth() - 1;
		} else if (intX < 0) {
            intX = 0;
        }

		if (intY >= temp.getHeight()) {
			intY = temp.getHeight() - 1;
		} else if (intY < 0) {
            intY = 0;
        }
        return temp.getPixel(intX, intY);
	}
	
	private int getRightColor(float x) {
		int a, r, g, b, so, dst;  
		float p;  
		
		float rightHalfHeight = (mWidth - (float)SPLIT_WIDTH * 2) / 2;
		if (x < rightHalfHeight) {
			so = mRightColors[0];   
			dst = mRightColors[1];
			p =  x / rightHalfHeight;
		} else {
			so = mRightColors[1];
			dst = mRightColors[2];
			p = (x - rightHalfHeight) / rightHalfHeight;
		}
  
        a = ave(Color.alpha(so), Color.alpha(dst), p);  
        r = ave(Color.red(so), Color.red(dst), p);  
        g = ave(Color.green(so), Color.green(dst), p);  
        b = ave(Color.blue(so), Color.blue(dst), p);  
        return Color.argb(a, r, g, b);
	}
	
	private int ave(int s, int d, float p) {
		return s + Math.round(p * (d - s));
	}
	
	// ### 内部类 ###
	public interface OnColorChangedListener {
		void onColorChanged(int color, int originalColor, float saturation);
        void onSingleColorChanged(int originalColor);
        void onBrightnessChanged(int color, float saturation);
	}
}
