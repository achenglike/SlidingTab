package com.github.achenglike.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SlidingTabLayout extends HorizontalScrollView {

    /**
     * Allows complete control over the colors drawn in the tab layout. Set with
     * {@link #setCustomTabColorizer(TabColorizer)}.
     */
    public interface TabColorizer {

        /**
         * @return return the color of the indicator used when {@code position}
         *         is selected.
         */
        int getIndicatorColor(int position);

        /**
         * @return return the color of the divider drawn to the right of
         *         {@code position}.
         */
        int getDividerColor(int position);

    }

    private static final int TITLE_OFFSET_DIPS = 24;
    // private static final int TAB_VIEW_VERTICAL_PADDING_DIPS = 8;
    private static final int TAB_VIEW_HORIZONTAL_PADDING_DIPS = 10;
    private static final int TAB_VIEW_TEXT_SIZE_SP = 14;
    private static final float DEAULT_TAB_HEIGHT = 57;

    private static final int DEFAULT_TEXT_COLOR = 0xFF000000;

    private float mTabLayoutHeight;

    private int mTitleOffset;
    private float mTitleTextSize;
    private boolean mTitleTabHorizontalMatch;
    private float mHorizontalPadding;
    private float mLeftPadding; //整个slidingTabLayout左边padding值
    private float mRightPadding; //整个slidingTabLayout右边padding值
    private int mTabViewLayoutId;
    private int mTabViewTextViewId;

    private ViewPager mViewPager;
    private ViewPager.OnPageChangeListener mViewPagerPageChangeListener;

    private final SlidingTabStrip mTabStrip;

    /**
     * 标题文字
     */
    private int mDefaultTextColor;

    /**
     * 底部滑动条颜色
     */
    private int mBottomIndicatorColor;

    /**
     * 选中文字色彩
     */
    private int selectedIndicatorColor;

    public SlidingTabLayout(Context context) {
        this(context, null);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingTabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        float density = getResources().getDisplayMetrics().density;

        TypedArray styled = context.obtainStyledAttributes(attrs, R.styleable.SlidingTab, 0, 0);
        mTitleTextSize = styled.getDimensionPixelSize(R.styleable.SlidingTab_title_text_size,
                sp2px(context, TAB_VIEW_TEXT_SIZE_SP));
        mTitleTabHorizontalMatch = styled.getBoolean(R.styleable.SlidingTab_title_tab_horizontal_match, true);
        mHorizontalPadding = styled.getDimension(R.styleable.SlidingTab_title_tab_horzontal_padding,
                TAB_VIEW_HORIZONTAL_PADDING_DIPS * density);
        mDefaultTextColor = styled.getColor(R.styleable.SlidingTab_title_text_default_color, DEFAULT_TEXT_COLOR);
        mTabLayoutHeight = styled.getDimension(R.styleable.SlidingTab_tab_height, DEAULT_TAB_HEIGHT * density);
        mBottomIndicatorColor = styled.getColor(R.styleable.SlidingTab_bottom_indicator_color, 0);
        selectedIndicatorColor = styled.getColor(R.styleable.SlidingTab_selected_text_color, DEFAULT_TEXT_COLOR);
        mLeftPadding = styled.getDimension(R.styleable.SlidingTab_left_padding, 0);
        mRightPadding = styled.getDimension(R.styleable.SlidingTab_right_padding, 0);

        styled.recycle();

        // Disable the Scroll Bar
        setHorizontalScrollBarEnabled(false);
        // Make sure that the Tab Strips fills this View
        // 设置此项的目的是为了能够充满并滚动，否则HorizontalScrollview无法滚动
        setFillViewport(true);

        mTitleOffset = (int) (TITLE_OFFSET_DIPS * density);

        // 在构造中主要是添加一个LinearLayout到HorizontalScrollView当中
        mTabStrip = new SlidingTabStrip(context, attrs);
        mTabStrip.setDefaultTextColor(mDefaultTextColor);
        mTabStrip.setBottomIndicatorColor(mBottomIndicatorColor);
        mTabStrip.setSelectedIndicatorColors(selectedIndicatorColor);
        addView(mTabStrip, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    /**
     * Set the custom {@link TabColorizer} to be used.
     *
     * If you only require simple custmisation then you can use
     * {@link #setSelectedIndicatorColors(int...)} and
     * {@link #setDividerColors(int...)} to achieve similar effects.
     */
    public void setCustomTabColorizer(TabColorizer tabColorizer) {
        mTabStrip.setCustomTabColorizer(tabColorizer);
    }

    /**
     * Sets the colors to be used for indicating the selected tab. These colors
     * are treated as a circular array. Providing one color will mean that all
     * tabs are indicated with the same color.
     */
    public void setSelectedIndicatorColors(int... colors) {
        mTabStrip.setSelectedIndicatorColors(colors);
    }

    public void setBottomIndicatorColor(int color) {
        mBottomIndicatorColor = color;
        if (mTabStrip != null) {
            mTabStrip.setBottomIndicatorColor(color);
            mTabStrip.invalidate();
        }
    }

    /**
     * Sets the colors to be used for tab dividers. These colors are treated as
     * a circular array. Providing one color will mean that all tabs are
     * indicated with the same color.
     */
    public void setDividerColors(int... colors) {
        mTabStrip.setDividerColors(colors);
    }

    /**
     * Set the {@link ViewPager.OnPageChangeListener}. When using
     * {@link SlidingTabLayout} you are required to set any
     * {@link ViewPager.OnPageChangeListener} through this method. This is so
     * that the layout can update it's scroll position correctly.
     *
     * @see ViewPager#setOnPageChangeListener(ViewPager.OnPageChangeListener)
     */
    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        mViewPagerPageChangeListener = listener;
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the
     * pager content (number of tabs and tab titles) does not change after this
     * call has been made.
     */
    public void setViewPager(ViewPager viewPager) {
        mTabStrip.removeAllViews();

        mViewPager = viewPager;
        if (viewPager != null) {
            viewPager.setOnPageChangeListener(new InternalViewPagerListener());
            populateTabStrip();
        }
    }

    public void setDefaultTextColor(int color) {
        mDefaultTextColor = color;
        if (mTabStrip != null) {
            mTabStrip.setDefaultTextColor(color);
            mTabStrip.invalidate();
        }
    }

    /**
     * Set the custom layout to be inflated for the tab views.
     *
     * @param layoutResId Layout id to be inflated
     * @param textViewId id of the {@link TextView} in the inflated view
     */
    public void setCustomTabView(int layoutResId, int textViewId) {
        mTabViewLayoutId = layoutResId;
        mTabViewTextViewId = textViewId;
    }

    /**
     * Create a default view to be used for tabs. This is called if a custom tab
     * view is not set via {@link #setCustomTabView(int, int)}.
     */
    protected TextView createDefaultTabView(Context context) {
        TextView textView = new TextView(context);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTitleTextSize);
        textView.setSingleLine(true);
        textView.setTextColor(mDefaultTextColor);
        // textView.setTypeface(Typeface.DEFAULT_BOLD);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // If we're running on Honeycomb or newer, then we can use the
            // Theme's
            // selectableItemBackground to ensure that the View has a pressed
            // state
            TypedValue outValue = new TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            textView.setBackgroundResource(outValue.resourceId);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            // If we're running on ICS or newer, enable all-caps to match the
            // Action Bar tab style
            // textView.setAllCaps(true);
        }

        int height = (int) (mTabLayoutHeight);
        int horizontalPadding = (int) mHorizontalPadding;
        textView.setBackgroundResource(android.R.color.transparent);
        textView.setPadding(horizontalPadding, 0, horizontalPadding, 0);
        if (mTitleTabHorizontalMatch) {
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, height, 1.0f));
        } else {
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, height));
        }

        return textView;
    }

    private void populateTabStrip() {
        final PagerAdapter adapter = mViewPager.getAdapter();
        final OnClickListener tabClickListener = new TabClickListener();

        for (int i = 0; i < adapter.getCount(); i++) {
            View tabView = null;
            TextView tabTitleView = null;

            if (mTabViewLayoutId != 0) {
                // If there is a custom tab view layout id set, try and inflate
                // it
                tabView = LayoutInflater.from(getContext()).inflate(mTabViewLayoutId, mTabStrip, false);
                tabTitleView = (TextView) tabView.findViewById(mTabViewTextViewId);
            }

            if (tabView == null) {
                tabView = createDefaultTabView(getContext());
            }

            if (tabTitleView == null && TextView.class.isInstance(tabView)) {
                tabTitleView = (TextView) tabView;
            }

            if (tabTitleView != null) {
                tabTitleView.setText(adapter.getPageTitle(i));
            }
            tabView.setOnClickListener(tabClickListener);

            int horizontalPadding = (int) mHorizontalPadding; //每个tab左右padding

            //by tiancuicui 2017.08.24 目前免费课程分类页面设置了左右的padding
            //添加整个slidingTabLayout左边padding
            if(i == 0 && mLeftPadding > 0) {
                int leftPadding = (int) mLeftPadding; //slidingTabLayout左边padding
                tabView.setPadding(horizontalPadding + leftPadding, 0, horizontalPadding, 0);
            }

            //添加整个slidingTabLayout右边padding
            if(i == adapter.getCount() - 1 && mRightPadding > 0) {
                int rightPadding = (int) mRightPadding; //slidingTabLayout右边padding
                tabView.setPadding(horizontalPadding, 0, horizontalPadding + rightPadding, 0);
            }

            mTabStrip.addView(tabView);
        }
    }

    /**
     * 为了兼容夜间模式而特地添加的
     * @param background
     */
    public void setSlidingTabBackground(Drawable background) {
        if (mTabStrip != null)
            mTabStrip.setBackgroundDrawable(background);
        super.setBackgroundDrawable(background);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mViewPager != null) {
            scrollToTab(mViewPager.getCurrentItem(), 0);
        }
    }

    private void scrollToTab(int tabIndex, int positionOffset) {
        final int tabStripChildCount = mTabStrip.getChildCount();
        if (tabStripChildCount == 0 || tabIndex < 0 || tabIndex >= tabStripChildCount) {
            return;
        }

        View selectedChild = mTabStrip.getChildAt(tabIndex);
        if (selectedChild != null) {
            // 距离当前TAB选项标题的左边距和当前标题偏移量之和
            int targetScrollX = selectedChild.getLeft() + positionOffset;

            if (tabIndex > 0 || positionOffset > 0) {
                // If we're not at the first child and are mid-scroll, make sure
                // we obey the offset
                targetScrollX -= mTitleOffset;
            }

            scrollTo(targetScrollX, 0);
        }
    }

    private class InternalViewPagerListener implements ViewPager.OnPageChangeListener {
        // private int mScrollState;

        /**
         * @param position
         *            索引值
         * @param positionOffset
         *            偏移率(0-0.999) 约为(0-1)
         * @param positionOffsetPixels
         *            像素偏移量(0-1078) 约为(0-1080)
         */
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            int tabStripChildCount = mTabStrip.getChildCount();
            if ((tabStripChildCount == 0) || (position < 0) || (position >= tabStripChildCount)) {
                return;
            }

            mTabStrip.onViewPagerPageChanged(position, positionOffset);

            View selectedTitle = mTabStrip.getChildAt(position);
            // 表示当前标题长度的偏移量
            int extraOffset = (selectedTitle != null) ? (int) (positionOffset * selectedTitle.getWidth()) : 0;
            scrollToTab(position, extraOffset);

            if (mViewPagerPageChangeListener != null) {
                mViewPagerPageChangeListener.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // mScrollState = state;

            if (mViewPagerPageChangeListener != null) {
                mViewPagerPageChangeListener.onPageScrollStateChanged(state);
            }
        }

        @Override
        public void onPageSelected(int position) {
            mTabStrip.onViewPagerSelected(position);
            // if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
            // mTabStrip.onViewPagerPageChanged(position, 0f);
            // scrollToTab(position, 0);
            // }

            if (mViewPagerPageChangeListener != null) {
                mViewPagerPageChangeListener.onPageSelected(position);
            }
        }

    }

    private class TabClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            for (int i = 0; i < mTabStrip.getChildCount(); i++) {
                if (v == mTabStrip.getChildAt(i)) {
                    mViewPager.setCurrentItem(i);
                    return;
                }
            }
        }
    }

    private static int sp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (dpValue * scale + 0.5f);
    }
}
