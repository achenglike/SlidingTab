package com.github.achenglike.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;



public class SlidingTabStrip extends LinearLayout {

    private static final float SELECTED_INDICATOR_THICKNESS_DIPS = 1.5f;
    private static final int DEFAULT_SELECTED_INDICATOR_COLOR = 0xFFD82A3E;

    private static final int DEFAULT_DIVIDER_THICKNESS_DIPS = 1;
    private static final byte DEFAULT_DIVIDER_COLOR_ALPHA = 0x20;
    private static final float DEFAULT_DIVIDER_HEIGHT = 0.5f;

    /**
     * 为蓝色的标志线
     */
    private boolean mHasBottomIndicator;

    private float mSelectedIndicatorThickness;
    private Paint mSelectedIndicatorPaint;

    private int mSelectedPosition;
    private float mSelectionOffset;

    /**
     * 标题文字
     */
    private int mDefaultTextColor = 0xFF000000;

    private static int INDICATOR_WIDTH;

    /**
     * 底部滑动条颜色
     */
    private int mBottomIndicatorColor = 0;

    /**
     * 是标题之间的分割线
     */
    private boolean mHasTitleDivider;
    private final Paint mDividerPaint;
    private final float mDividerHeight;

    private SlidingTabLayout.TabColorizer mCustomTabColorizer;
    private final SimpleTabColorizer mDefaultTabColorizer;

    private float density;

    private int mLastIndex = -1;
    private int mCurrentIndex = 0;
    private RectF mRectF;
    private float radius;
    private float mLeftPadding; //整个slidingTabLayout左边padding值
    private float mRightPadding; //整个slidingTabLayout右边padding值

    private boolean mTitleTabHorizontalMatch;

    SlidingTabStrip(Context context) {
        this(context, null);
    }

    SlidingTabStrip(Context context, AttributeSet attrs) {
        super(context, attrs);

        density = getResources().getDisplayMetrics().density;

        INDICATOR_WIDTH = (int) (16 * density + .5f);

        // 获取自定义属性
        TypedArray styled = context.obtainStyledAttributes(attrs, R.styleable.SlidingTab, 0, 0);
        mHasBottomIndicator = styled.getBoolean(R.styleable.SlidingTab_has_bottom_indicator, true);
        mSelectedIndicatorThickness = styled.getDimension(R.styleable.SlidingTab_indicator_thickness,
                SELECTED_INDICATOR_THICKNESS_DIPS * density);
        mHasTitleDivider = styled.getBoolean(R.styleable.SlidingTab_has_title_divider, false);
        float mDividerThickness = styled.getDimension(R.styleable.SlidingTab_divider_thickness,
                DEFAULT_DIVIDER_THICKNESS_DIPS * density);
        mTitleTabHorizontalMatch = styled.getBoolean(R.styleable.SlidingTab_title_tab_horizontal_match, true);
        mLeftPadding = styled.getDimension(R.styleable.SlidingTab_left_padding, 0);
        mRightPadding = styled.getDimension(R.styleable.SlidingTab_right_padding, 0);
        styled.recycle();

        // 默认情况下ViewGroup的onDraw方法是不会被调用的
        // 设置为false后，就告诉需进行绘制操作
        setWillNotDraw(false);

        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorForeground, outValue, true);
        final int themeForegroundColor = outValue.data;

        mDefaultTabColorizer = new SimpleTabColorizer();
        mDefaultTabColorizer.setIndicatorColors(DEFAULT_SELECTED_INDICATOR_COLOR);
        mDefaultTabColorizer.setDividerColors(setColorAlpha(themeForegroundColor, DEFAULT_DIVIDER_COLOR_ALPHA));

        mSelectedIndicatorPaint = new Paint();

        mDividerHeight = DEFAULT_DIVIDER_HEIGHT;
        mDividerPaint = new Paint();
        mDividerPaint.setStrokeWidth(mDividerThickness);
        mRectF = new RectF();
        radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,getResources().getDisplayMetrics());
    }

    void setCustomTabColorizer(SlidingTabLayout.TabColorizer customTabColorizer) {
        mCustomTabColorizer = customTabColorizer;
        invalidate();
    }

    void setSelectedIndicatorColors(int... colors) {
        // Make sure that the custom colorizer is removed
        mCustomTabColorizer = null;
        mDefaultTabColorizer.setIndicatorColors(colors);
        invalidate();

        // 设置为可以实时刷新颜色
        int childCount = getChildCount();
        for (int i=0;i<childCount;i++) {
            if (i == mCurrentIndex) {
                TextView textView = (TextView) getChildAt(i);
                int newColor = colors[mCurrentIndex % colors.length];
                textView.setTextColor(newColor);
                textView.invalidate();
            }
        }

    }

    void setDividerColors(int... colors) {
        // Make sure that the custom colorizer is removed
        mCustomTabColorizer = null;
        mDefaultTabColorizer.setDividerColors(colors);
        invalidate();
    }

    void setDefaultTextColor(int colors) {
        mDefaultTextColor = colors;

        // 设置为可以实时刷新颜色
        int childCount = getChildCount();
        for (int i=0;i<childCount;i++) {
            if (i != mCurrentIndex) {
                TextView textView = (TextView) getChildAt(i);
                textView.setTextColor(mDefaultTextColor);
                textView.invalidate();
            }
        }
    }

    void setBottomIndicatorColor(int color) {
        mBottomIndicatorColor = color;
    }

    void onViewPagerPageChanged(int position, float positionOffset) {
        mSelectedPosition = position;
        mSelectionOffset = positionOffset;
        invalidate();
    }

    void onViewPagerSelected(int position) {
        mCurrentIndex = position;
        invalidate();
    }

    AccelerateInterpolator interpolator = new AccelerateInterpolator();
    DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
    /**
     * 在onDraw方法当中主要是对底部蓝色标志条进行绘制操作
     */
    @Override
    protected void onDraw(Canvas canvas) {
        final int height = getHeight();
        final int childCount = getChildCount();
        final int dividerHeightPx = (int) (Math.min(Math.max(0f, mDividerHeight), 1f) * height);
        final SlidingTabLayout.TabColorizer tabColorizer = mCustomTabColorizer != null ? mCustomTabColorizer
                : mDefaultTabColorizer;

        // Thick colored underline below the current selection
        if (childCount > 0) {
            TextView selectedTitle = (TextView) getChildAt(mSelectedPosition);


            /*int offset = getIndicatorOffset(selectedTitle);
            int left = selectedTitle.getLeft() + offset;
            int right = selectedTitle.getRight() - offset;*/
            int left;
            int right;

            if(mLeftPadding > 0 || mRightPadding > 0) {
                //by tiancuicui 2017.08.24 目前免费课程分类页面设置了左右的padding
                //如果给MCSlidingTabLayout设置了左右的padding， 第一个和最后一个tab的红色标志条要做相应的位移，以防止这两个地方的标志条没有相对于文字居中
                int leftPadding = (int) mLeftPadding;
                int rightPadding = (int) mRightPadding;
                if(mSelectedPosition == 0) {
                    //第一个tab的标志条要右移半个leftPadding, 以相对于文字居中
                    left = selectedTitle.getRight() - (selectedTitle.getWidth() + INDICATOR_WIDTH) / 2 + leftPadding/2;
                    right = selectedTitle.getRight() - (selectedTitle.getWidth() - INDICATOR_WIDTH) / 2 + leftPadding/2;
                } else if(mSelectedPosition == childCount - 1) {
                    //最后一个tab的标志条要左移半个rightPadding, 以相对于文字居中
                    left = selectedTitle.getRight() - (selectedTitle.getWidth() + INDICATOR_WIDTH) / 2 - rightPadding/2;
                    right = selectedTitle.getRight() - (selectedTitle.getWidth() - INDICATOR_WIDTH) / 2 - rightPadding/2;
                } else {
                    left = selectedTitle.getRight() - (selectedTitle.getWidth() + INDICATOR_WIDTH) / 2;
                    right = selectedTitle.getRight() - (selectedTitle.getWidth() - INDICATOR_WIDTH) / 2;
                }
            } else {
                left = selectedTitle.getRight() - (selectedTitle.getWidth() + INDICATOR_WIDTH) / 2;
                right = selectedTitle.getRight() - (selectedTitle.getWidth() - INDICATOR_WIDTH) / 2;
            }



            // int right = selectedTitle.getRight() - (selectedTitle.getWidth() - INDICATOR_WIDTH) / 2;



            int color = tabColorizer.getIndicatorColor(mSelectedPosition);

            if (mSelectionOffset > 0f && mSelectedPosition < (getChildCount() - 1)) {
                int nextColor = tabColorizer.getIndicatorColor(mSelectedPosition + 1);
                if (color != nextColor) {
                    color = blendColors(nextColor, color, mSelectionOffset);
                }

                //positionOffset是当前页面滑动比例，如果页面向右翻动，这个值不断变大，最后在趋近1的情况后突变为0。
                // 如果页面向左翻动，这个值不断变小，最后变为0。
                // Draw the selection partway between the tabs
                TextView nextTitle = (TextView) getChildAt(mSelectedPosition + 1);


                /*int nextOffset = getIndicatorOffset(nextTitle);
                left = (int) (interpolator.getInterpolation(mSelectionOffset) * (nextTitle.getLeft() + nextOffset) + (1.0f - interpolator.getInterpolation(mSelectionOffset)) * left);
                right = (int) (mSelectionOffset * (nextTitle.getRight() - nextOffset) + (1.0f - mSelectionOffset)
                        * right);*/


                int l = nextTitle.getRight() - (nextTitle.getWidth() + INDICATOR_WIDTH) / 2;
                int r = nextTitle.getRight() - (nextTitle.getWidth() - INDICATOR_WIDTH) / 2;
                left = (int) (interpolator.getInterpolation(mSelectionOffset) * l + (1.0f - interpolator.getInterpolation(mSelectionOffset)) * left);
                right = (int) (decelerateInterpolator.getInterpolation(mSelectionOffset) * r + (1.0f - decelerateInterpolator.getInterpolation(mSelectionOffset))
                        * right);
            }

            if (mLastIndex == -1 || mLastIndex != mCurrentIndex) {
                if (mLastIndex != -1) {
                    setTextColor(mLastIndex, mDefaultTextColor);
                }
                mLastIndex = mCurrentIndex;
                setTextColor(mCurrentIndex, color);
            }

            if (mHasBottomIndicator) {
                mSelectedIndicatorPaint.setColor(mBottomIndicatorColor == 0 ? color : mBottomIndicatorColor);

                mRectF.set(left, height - mSelectedIndicatorThickness, right, height);
                canvas.drawRoundRect(mRectF, radius, radius, mSelectedIndicatorPaint);
            }
        }

        // Vertical separators between the titles
        if (mHasTitleDivider) {
            int separatorTop = (height - dividerHeightPx) / 2;
            for (int i = 0; i < childCount - 1; i++) {
                View child = getChildAt(i);
                mDividerPaint.setColor(tabColorizer.getDividerColor(i));
                canvas.drawLine(child.getRight(), separatorTop, child.getRight(), separatorTop + dividerHeightPx,
                        mDividerPaint);
            }
        }
    }

    /**
     * 获取文字底部滑动标志条的偏移量
     *
     * @param textView
     * @return int
     */
    private int getIndicatorOffset(TextView textView) {
        // 设置滑动条的偏移量为左边Padding的0.99倍
        // 保证滑动条边界靠近文字的左侧或者右侧
        int offset = 0;
        if (mTitleTabHorizontalMatch) {
            String text = textView.getText().toString();
            int width = textView.getWidth();
            float textLength = textView.getPaint().measureText(text);
            offset = (int) ((width - textLength) / 2.0f - 1 * density);
        } else {
            offset = (int) (0.99f * textView.getPaddingLeft());
        }
        return offset;
    }

    private void setTextColor(int position, int color) {
        TextView textView = (TextView) getChildAt(position);
        textView.setTextColor(color);
    }

    /**
     * Set the alpha value of the {@code color} to be the given {@code alpha}
     * value.
     */
    private static int setColorAlpha(int color, byte alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Blend {@code color1} and {@code color2} using the given ratio.
     *
     * @param ratio
     *            of which to blend. 1.0 will return {@code color1}, 0.5 will
     *            give an even blend, 0.0 will return {@code color2}.
     */
    private static int blendColors(int color1, int color2, float ratio) {
        final float inverseRation = 1f - ratio;
        float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
        float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
        float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    private static class SimpleTabColorizer implements SlidingTabLayout.TabColorizer {
        private int[] mIndicatorColors;
        private int[] mDividerColors;

        @Override
        public final int getIndicatorColor(int position) {
            return mIndicatorColors[position % mIndicatorColors.length];
        }

        @Override
        public final int getDividerColor(int position) {
            return mDividerColors[position % mDividerColors.length];
        }

        void setIndicatorColors(int... colors) {
            mIndicatorColors = colors;
        }

        void setDividerColors(int... colors) {
            mDividerColors = colors;
        }
    }
}