package views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.widget.GridView;

import com.siqi.bits.app.R;

/**
 * Created by me on 4/18/14.
 */
public class TimeLine extends GridView {
    private static final float MARGIN_Y = 3;
    private static final float MARGIN_X = 2;
    // Minimum rect width
    private final float MIN_RECT_WIDTH = 20;
    private final int RECT_PER_ROW = 15;

    private boolean mShowCounts;
    private int mLabelPos;
    private int mDoneColor, mSkipColor, mLateColor, mTextColor, mNoneColor;
    private float mRectW, mRectH;
    private float mCountRadius;
    private float mTimeLineX, mTimeLineY;

    private Paint mTextPaint, mDonePaint, mSkipPaint, mLatePaint, mNonePaint;

    private float mBottompad, mCountsHeight;

    private String mData = new String();

    public TimeLine(Context context, String data) {
        super(context);

        mData = data;

        TypedArray attributes = context.getTheme().obtainStyledAttributes(
                null,
                R.styleable.TimeLine,
                0, 0);

        try {
            mShowCounts = attributes.getBoolean(R.styleable.TimeLine_showCounts, true);
            mLabelPos = attributes.getInteger(R.styleable.TimeLine_labelPosition, 0);
            mDoneColor = attributes.getColor(R.styleable.TimeLine_doneColor, getResources().getColor(R.color.doneColor));
            mSkipColor = attributes.getColor(R.styleable.TimeLine_skipColor, getResources().getColor(R.color.skipColor));
            mLateColor = attributes.getColor(R.styleable.TimeLine_lateColor, getResources().getColor(R.color.lateColor));
            mNoneColor = attributes.getColor(R.styleable.TimeLine_noneColor, getResources().getColor(R.color.noneColor));
            mTextColor = attributes.getColor(R.styleable.TimeLine_textColor, getResources().getColor(android.R.color.black));
        } finally {
            attributes.recycle();
        }

        init();
    }

    private void init() {
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mTextColor);
        mCountsHeight = mTextPaint.getTextSize();

        mDonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDonePaint.setStyle(Paint.Style.FILL);
        mDonePaint.setColor(mDoneColor);

        mSkipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSkipPaint.setStyle(Paint.Style.FILL);
        mSkipPaint.setColor(mSkipColor);

        mLatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLatePaint.setStyle(Paint.Style.FILL);
        mLatePaint.setColor(mLateColor);

        mNonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLatePaint.setStyle(Paint.Style.FILL);
        mLatePaint.setColor(mNoneColor);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Account for padding
        float xpad = (float) (getPaddingLeft() + getPaddingRight());
        float ypad = (float) (getPaddingTop() + getPaddingBottom());

        mBottompad = ypad;
        if (mShowCounts) {
            mBottompad += mCountsHeight;
        }

        mRectW = Math.max((w - 2 * xpad) / RECT_PER_ROW, MIN_RECT_WIDTH);
        mRectH = mRectW;

        mTimeLineX = w / 2 - mRectW * RECT_PER_ROW/2;
        mTimeLineY = ypad;

        mCountRadius = mCountsHeight / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) throws IllegalStateException {
        super.onDraw(canvas);

        float startX = mTimeLineX;
        float startY = mTimeLineY;

        int countDone = 0, countSkip = 0, countLate = 0;

        int count = 0;

        for (char c : mData.toCharArray()) {
            ++count;

            switch (c) {
                case 'd':
                    canvas.drawRoundRect(new RectF(startX, startY, startX+mRectW, startY+mRectH), 3, 3, mDonePaint);
                    ++countDone;
                    break;
                case 's':
                    canvas.drawRoundRect(new RectF(startX, startY, startX+mRectW, startY+mRectH), 3, 3, mSkipPaint);
                    ++countSkip;
                    break;
                case 'l':
                    canvas.drawRoundRect(new RectF(startX, startY, startX+mRectW, startY+mRectH), 3, 3, mLatePaint);
                    ++countLate;
                    break;
                default:
                    throw new IllegalStateException("Unkown state char, only 'd', 's', 'l' are allowed in the string");
            }

            startX += mRectW + MARGIN_X;

            if (count >= RECT_PER_ROW) {
                startX = mTimeLineX;
                startY += mRectH + MARGIN_Y;
                count = 0;
            }
        }

        count = RECT_PER_ROW - count;

        while (count > 0) {
            canvas.drawRoundRect(new RectF(startX, startY, startX+mRectW, startY+mRectH), 3, 3, mNonePaint);
            startX += mRectW + MARGIN_X;
            --count;
        }


        /**
         * Draw counts here
         */
        Log.d("TimeLine", "Done: "+countDone + " Late: "+countLate + " Skip: "+countSkip);
    }


    public boolean isShowCounts() {
        return mShowCounts;
    }

    public void setShowCounts(boolean mShowCounts) {
        this.mShowCounts = mShowCounts;
        invalidate();
        requestLayout();
    }

    public int getLabelPos() {
        return mLabelPos;
    }

    public void setLabelPos(int mLabelPos) {
        this.mLabelPos = mLabelPos;
        invalidate();
        requestLayout();
    }

    public int getDoneColor() {
        return mDoneColor;
    }

    public void setDoneColor(int mDoneColor) {
        this.mDoneColor = mDoneColor;
        invalidate();
        requestLayout();
    }

    public int getSkipColor() {
        return mSkipColor;
    }

    public void setSkipColor(int mSkipColor) {
        this.mSkipColor = mSkipColor;
        invalidate();
        requestLayout();
    }

    public int getLateColor() {
        return mLateColor;
    }

    public void setLateColor(int mLateColor) {
        this.mLateColor = mLateColor;
        invalidate();
        requestLayout();
    }

    public int getTextColor() {
        return mTextColor;
    }

    public void setTextColor(int mTextColor) {
        this.mTextColor = mTextColor;
        invalidate();
        requestLayout();
    }

}
