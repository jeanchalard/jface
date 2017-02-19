package com.j.jface.org.editor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Scroller;

import com.j.jface.Const;
import com.j.jface.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * A widget that enables the user to select a date from a predefined range.
 * Shamelessly stolen and adapted from the standard NumberPicker, which can't
 * accommodate us because it insists the middle element has to be an EditText
 * with a filter that only accepts digits, in spite of giving the option of
 * supplying a formatter.
 */
public class NumericPicker extends LinearLayout
{
  /**
   * The number of items show in the selector wheel.
   */
  private static final int SELECTOR_WHEEL_ITEM_COUNT = 3;

  /**
   * The index of the middle selector item.
   */
  private static final int SELECTOR_MIDDLE_ITEM_INDEX = SELECTOR_WHEEL_ITEM_COUNT / 2;

  /**
   * The coefficient by which to adjust (divide) the max fling velocity.
   */
  private static final int SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT = 8;

  /**
   * The the duration for adjusting the selector wheel.
   */
  private static final int SELECTOR_ADJUSTMENT_DURATION_MILLIS = 800;

  /**
   * The strength of fading in the top and bottom while drawing the selector.
   */
  private static final float TOP_AND_BOTTOM_FADING_EDGE_STRENGTH = 1.0f;

  /**
   * Supported formats.
   */
  private static final int FORMAT_INTEGER = 0;
  private static final int FORMAT_DATE = 1;

  /**
   * ...and which format we're in.
   */
  private final int mFormatStyle;

  /**
   * To avoid creating one for each formatting.
   */
  @NonNull private GregorianCalendar mTmpCal;

  /**
   * The distance between the two selection dividers.
   */
  private final int mSelectionDividersDistance;

  /**
   * The height of the text.
   */
  private final int mTextSize;

  /**
   * The height of the gap between text elements if the selector wheel.
   */
  private int mSelectorTextGapHeight;

  /**
   * Lower value of the range of numbers allowed for the DatePicker
   */
  private int mMinValue;

  /**
   * Upper value of the range of numbers allowed for the DatePicker
   */
  private int mMaxValue;

  /**
   * Current value of this DatePicker
   */
  private int mValue;

  /**
   * Listener to be notified upon current value change.
   */
  private OnValueChangeListener mOnValueChangeListener;

  /**
   * Listener to be notified upon scroll state change.
   */
  private OnScrollListener mOnScrollListener;

  /**
   * Cache for the string representation of selector indices.
   */
  private final SparseArray<String> mSelectorIndexToStringCache = new SparseArray<>();

  /**
   * The selector indices whose value are show by the selector.
   */
  private final int[] mSelectorIndices = new int[SELECTOR_WHEEL_ITEM_COUNT];

  /**
   * The {@link Paint} for drawing the selector.
   */
  private final TextPaint mSelectorWheelPaint;

  /**
   * The height of a selector element (text + gap).
   */
  private int mSelectorElementHeight;

  /**
   * The initial offset of the scroll selector.
   */
  private int mInitialScrollOffset = Integer.MIN_VALUE;

  /**
   * The current offset of the scroll selector.
   */
  private int mCurrentScrollOffset;

  /**
   * The {@link Scroller} responsible for flinging the selector.
   */
  private final Scroller mFlingScroller;

  /**
   * The {@link Scroller} responsible for adjusting the selector.
   */
  private final Scroller mAdjustScroller;

  /**
   * The previous Y coordinate while scrolling the selector.
   */
  private int mPreviousScrollerY;

  /**
   * The Y position of the last down event.
   */
  private float mLastDownEventY;

  /**
   * The Y position of the last down or move event.
   */
  private float mLastDownOrMoveEventY;

  /**
   * Determines speed during touch scrolling.
   */
  private VelocityTracker mVelocityTracker;

  /**
   * @see ViewConfiguration#getScaledTouchSlop()
   */
  private int mTouchSlop;

  /**
   * @see ViewConfiguration#getScaledMinimumFlingVelocity()
   */
  private int mMinimumFlingVelocity;

  /**
   * @see ViewConfiguration#getScaledMaximumFlingVelocity()
   */
  private int mMaximumFlingVelocity;

  /**
   * Flag whether the selector should wrap around.
   */
  private boolean mWrapSelectorWheel;

  /**
   * The back ground color used to optimize scroller fading.
   */
  private final int mSolidColor;

  /**
   * Divider for showing item to be selected while scrolling
   */
  private final Drawable mSelectionDivider;

  /**
   * The height of the selection divider.
   */
  private final int mSelectionDividerHeight;

  /**
   * The current scroll state of the number picker.
   */
  private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

  /**
   * The top of the top selection divider.
   */
  private int mTopSelectionDividerTop;

  /**
   * The bottom of the bottom selection divider.
   */
  private int mBottomSelectionDividerBottom;

  /**
   * Interface to listen for changes of the current value.
   */
  public interface OnValueChangeListener {

    /**
     * Called upon a change of the current value.
     *
     * @param picker The DatePicker associated with this listener.
     * @param oldVal The previous value.
     * @param newVal The new value.
     */
    void onValueChange(NumericPicker picker, int oldVal, int newVal);
  }

  /**
   * Interface to listen for the picker scroll state.
   */
  public interface OnScrollListener {
    @IntDef({SCROLL_STATE_IDLE, SCROLL_STATE_TOUCH_SCROLL, SCROLL_STATE_FLING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScrollState {}

    /**
     * The view is not scrolling.
     */
    public static int SCROLL_STATE_IDLE = 0;

    /**
     * The user is scrolling using touch, and his finger is still on the screen.
     */
    public static int SCROLL_STATE_TOUCH_SCROLL = 1;

    /**
     * The user had previously been scrolling using touch and performed a fling.
     */
    public static int SCROLL_STATE_FLING = 2;

    /**
     * Callback invoked while the number picker scroll state has changed.
     *
     * @param view The view whose scroll state is being reported.
     * @param scrollState The current scroll state. One of
     *            {@link #SCROLL_STATE_IDLE},
     *            {@link #SCROLL_STATE_TOUCH_SCROLL} or
     *            {@link #SCROLL_STATE_IDLE}.
     */
    public void onScrollStateChange(NumericPicker view, @ScrollState int scrollState);
  }

  /**
   * Create a new number picker.
   *
   * @param context The application environment.
   */
  public NumericPicker(@NonNull final Context context)
  {
    this(context, null);
  }

  /**
   * Create a new number picker.
   *
   * @param context The application environment.
   * @param attrs A collection of attributes.
   */
  public NumericPicker(@NonNull final Context context, @NonNull final AttributeSet attrs)
  {
    super(context, attrs);
    mSolidColor = context.getColor(android.R.color.transparent);

    final Drawable selectionDivider = context.getDrawable(R.drawable.numeric_picker_divider);
    if (selectionDivider != null)
    {
      selectionDivider.setCallback(this);
      selectionDivider.setLayoutDirection(getLayoutDirection());
      if (selectionDivider.isStateful()) selectionDivider.setState(getDrawableState());
    }
    mSelectionDivider = selectionDivider;

    mSelectionDividerHeight = (int)context.getResources().getDimension(R.dimen.selection_divider_height);
    mSelectionDividersDistance = (int)context.getResources().getDimension(R.dimen.selection_dividers_distance);

    // By default Linearlayout that we extend is not drawn. This is
    // its draw() method is not called but dispatchDraw() is called
    // directly (see ViewGroup.drawChild()). However, this class uses
    // the fading edge effect implemented by View and we need our
    // draw() method to be called. Therefore, we declare we will draw.
    setWillNotDraw(false);

    // initialize constants
    ViewConfiguration configuration = ViewConfiguration.get(context);
    mTouchSlop = configuration.getScaledTouchSlop();
    mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
    mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity() / SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT;
    mTextSize = (int)context.getResources().getDimension(R.dimen.calendar_view_text_size);

    // create the selector wheel paint
    TextPaint paint = new TextPaint();
    paint.setAntiAlias(true);
    paint.setTextAlign(Align.CENTER);
    paint.setTextSize(mTextSize);
    paint.setTypeface(Typeface.DEFAULT);
    paint.setColor(0xFFFFFFFF);//context.getColor(android.R.color.primary_text_dark));
    paint.density = context.getResources().getDisplayMetrics().density;
    mSelectorWheelPaint = paint;

    // create the fling and adjust scrollers
    mFlingScroller = new Scroller(getContext(), null, true);
    mAdjustScroller = new Scroller(getContext(), new DecelerateInterpolator(2.5f));

    int formatStyle = FORMAT_INTEGER;
    boolean wrap = false;
    final TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.NumericPicker, 0, 0);
    try
    {
      formatStyle = a.getInteger(R.styleable.NumericPicker_format, FORMAT_INTEGER);
      wrap = a.getBoolean(R.styleable.NumericPicker_wrap, false);
    }
    finally
    {
      a.recycle();
    }
    mFormatStyle = formatStyle;
    mWrapSelectorWheel = wrap;
    if (FORMAT_DATE == formatStyle)
      mTmpCal = new GregorianCalendar();
  }

  @Override protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom)
  {
    if (changed)
    {
      // need to do all this when we know our size
      initializeSelectorWheel();
      initializeFadingEdges();
      mTopSelectionDividerTop = (getHeight() - mSelectionDividersDistance) / 2 - mSelectionDividerHeight;
      mBottomSelectionDividerBottom = mTopSelectionDividerTop + 2 * mSelectionDividerHeight + mSelectionDividersDistance;
    }
  }

  @Override protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec)
  {
    final int hPadding = 30; final int vPaddingPerElement = 5;
    final int maxWidth = (int)(mSelectorWheelPaint.measureText(formatNumber(mValue)) + 0.5) + hPadding;
    final int height = (mTextSize + vPaddingPerElement) * (mSelectorIndices.length - 1);
    // Try greedily to fit the max width and height.
    final int newWidthMeasureSpec = makeMeasureSpec(widthMeasureSpec, maxWidth);
    final int newHeightMeasureSpec = makeMeasureSpec(heightMeasureSpec, -1);
    super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec);
    // Flag if we are measured with width or height less than the respective min.
    final int widthSize = resolveSizeAndState(getMeasuredWidth(), widthMeasureSpec, 0);
    final int desiredHeight = Math.max(height, getMeasuredHeight());
    final int heightSize = resolveSizeAndState(desiredHeight, heightMeasureSpec, 0);
    setMeasuredDimension(widthSize, heightSize);
  }

  /**
   * Makes a measure spec that tries greedily to use the max value.
   *
   * @param measureSpec The measure spec.
   * @param maxSize The max value for the size.
   * @return A measure spec greedily imposing the max size.
   */
  private int makeMeasureSpec(final int measureSpec, final int maxSize) {
    if (maxSize < 0) return measureSpec;
    final int size = MeasureSpec.getSize(measureSpec);
    final int mode = MeasureSpec.getMode(measureSpec);
    switch (mode) {
      case MeasureSpec.EXACTLY:
        return measureSpec;
      case MeasureSpec.AT_MOST:
        return MeasureSpec.makeMeasureSpec(Math.min(size, maxSize), MeasureSpec.EXACTLY);
      case MeasureSpec.UNSPECIFIED:
        return MeasureSpec.makeMeasureSpec(maxSize, MeasureSpec.EXACTLY);
      default:
        throw new IllegalArgumentException("Unknown measure mode: " + mode);
    }
  }

  /**
   * Move to the final position of a scroller. Ensures to force finish the scroller
   * and if it is not at its final position a scroll of the selector wheel is
   * performed to fast forward to the final position.
   *
   * @param scroller The scroller to whose final position to get.
   * @return True of the a move was performed, i.e. the scroller was not in final position.
   */
  private boolean moveToFinalScrollerPosition(@NonNull final Scroller scroller) {
    scroller.forceFinished(true);
    int amountToScroll = scroller.getFinalY() - scroller.getCurrY();
    int futureScrollOffset = (mCurrentScrollOffset + amountToScroll) % mSelectorElementHeight;
    int overshootAdjustment = mInitialScrollOffset - futureScrollOffset;
    if (overshootAdjustment == 0) return false;
    if (Math.abs(overshootAdjustment) > mSelectorElementHeight / 2) {
      if (overshootAdjustment > 0)
        overshootAdjustment -= mSelectorElementHeight;
      else
        overshootAdjustment += mSelectorElementHeight;
    }
    amountToScroll += overshootAdjustment;
    scrollBy(0, amountToScroll);
    return true;
  }

  @Override public boolean onInterceptTouchEvent(@NonNull final MotionEvent event)
  {
    if (!isEnabled()) return false;
    final int action = event.getActionMasked();
    switch (action) {
      case MotionEvent.ACTION_DOWN: {
        mLastDownOrMoveEventY = mLastDownEventY = event.getY();
        // Make sure we support flinging inside scrollables.
        getParent().requestDisallowInterceptTouchEvent(true);
        if (!mFlingScroller.isFinished()) {
          mFlingScroller.forceFinished(true);
          mAdjustScroller.forceFinished(true);
          onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
        }
        else if (!mAdjustScroller.isFinished())
        {
          mFlingScroller.forceFinished(true);
          mAdjustScroller.forceFinished(true);
        }
        return true;
      }
    }
    return false;
  }

  @Override public boolean onTouchEvent(@NonNull final MotionEvent event)
  {
    if (!isEnabled()) return false;
    if (mVelocityTracker == null) mVelocityTracker = VelocityTracker.obtain();
    mVelocityTracker.addMovement(event);
    int action = event.getActionMasked();
    switch (action) {
      case MotionEvent.ACTION_MOVE:
      {
        float currentMoveY = event.getY();
        if (mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL)
        {
          int deltaDownY = (int)Math.abs(currentMoveY - mLastDownEventY);
          if (deltaDownY > mTouchSlop)
            onScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
        }
        else
        {
          int deltaMoveY = (int)(currentMoveY - mLastDownOrMoveEventY);
          scrollBy(0, deltaMoveY);
          invalidate();
        }
        mLastDownOrMoveEventY = currentMoveY;
      }
      break;
      case MotionEvent.ACTION_UP:
      {
        VelocityTracker velocityTracker = mVelocityTracker;
        velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
        int initialVelocity = (int) velocityTracker.getYVelocity();
        if (Math.abs(initialVelocity) > mMinimumFlingVelocity)
        {
          fling(initialVelocity);
          onScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
        }
        else
        {
          ensureScrollWheelAdjusted();
          onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
        }
        mVelocityTracker.recycle();
        mVelocityTracker = null;
      }
      break;
    }
    return true;
  }

  @Override public void computeScroll()
  {
    Scroller scroller = mFlingScroller;
    if (scroller.isFinished())
    {
      scroller = mAdjustScroller;
      if (scroller.isFinished()) return;
    }
    scroller.computeScrollOffset();
    int currentScrollerY = scroller.getCurrY();
    if (mPreviousScrollerY == 0)
      mPreviousScrollerY = scroller.getStartY();
    scrollBy(0, currentScrollerY - mPreviousScrollerY);
    mPreviousScrollerY = currentScrollerY;
    if (scroller.isFinished())
      onScrollerFinished(scroller);
    else
      invalidate();
  }

  @Override public void scrollBy(final int x, final int y)
  {
    int[] selectorIndices = mSelectorIndices;
    if (!mWrapSelectorWheel && y > 0 && selectorIndices[SELECTOR_MIDDLE_ITEM_INDEX] <= mMinValue)
    {
      mCurrentScrollOffset = mInitialScrollOffset;
      return;
    }
    if (!mWrapSelectorWheel && y < 0 && selectorIndices[SELECTOR_MIDDLE_ITEM_INDEX] >= mMaxValue)
    {
      mCurrentScrollOffset = mInitialScrollOffset;
      return;
    }
    mCurrentScrollOffset += y;
    while (mCurrentScrollOffset - mInitialScrollOffset > mSelectorTextGapHeight)
    {
      mCurrentScrollOffset -= mSelectorElementHeight;
      decrementSelectorIndices(selectorIndices);
      setValueInternal(selectorIndices[SELECTOR_MIDDLE_ITEM_INDEX], true);
      if (!mWrapSelectorWheel && selectorIndices[SELECTOR_MIDDLE_ITEM_INDEX] <= mMinValue)
        mCurrentScrollOffset = mInitialScrollOffset;
    }
    while (mCurrentScrollOffset - mInitialScrollOffset < -mSelectorTextGapHeight)
    {
      mCurrentScrollOffset += mSelectorElementHeight;
      incrementSelectorIndices(selectorIndices);
      setValueInternal(selectorIndices[SELECTOR_MIDDLE_ITEM_INDEX], true);
      if (!mWrapSelectorWheel && selectorIndices[SELECTOR_MIDDLE_ITEM_INDEX] >= mMaxValue)
        mCurrentScrollOffset = mInitialScrollOffset;
    }
  }

  @Override protected int computeVerticalScrollOffset() { return mCurrentScrollOffset; }
  @Override protected int computeVerticalScrollRange() { return (mMaxValue - mMinValue + 1) * mSelectorElementHeight; }
  @Override protected int computeVerticalScrollExtent() { return getHeight(); }
  @Override public int getSolidColor() { return mSolidColor; }

  /**
   * Sets the listener to be notified on change of the current value.
   *
   * @param onValueChangedListener The listener.
   */
  public void setOnValueChangedListener(@NonNull final OnValueChangeListener onValueChangedListener)
  {
    mOnValueChangeListener = onValueChangedListener;
  }

  /**
   * Set listener to be notified for scroll state changes.
   *
   * @param onScrollListener The listener.
   */
  public void setOnScrollListener(@NonNull final OnScrollListener onScrollListener)
  {
    mOnScrollListener = onScrollListener;
  }

  /**
   * Set the current value for the number picker.
   * <p>
   * If the argument is less than the {@link NumericPicker#getMinValue()} the
   * current value is set to the {@link NumericPicker#getMinValue()} value.
   * </p>
   * <p>
   * If the argument is less than the {@link NumericPicker#getMinValue()} the
   * current value is set to the {@link NumericPicker#getMaxValue()} value.
   * </p>
   * <p>
   * If the argument is less than the {@link NumericPicker#getMaxValue()} the
   * current value is set to the {@link NumericPicker#getMaxValue()} value.
   * </p>
   * <p>
   * If the argument is less than the {@link NumericPicker#getMaxValue()} the
   * current value is set to the {@link NumericPicker#getMinValue()} value.
   * </p>
   *
   * @param value The current value.
   * @see #setMinValue(int)
   * @see #setMaxValue(int)
   */
  public void setValue(int value)
  {
    setValueInternal(value, false);
  }

  /**
   * Returns the value of the picker.
   *
   * @return The value.
   */
  public int getValue()
  {
    return mValue;
  }

  /**
   * Returns the min value of the picker.
   *
   * @return The min value
   */
  public int getMinValue()
  {
    return mMinValue;
  }

  /**
   * Sets the min value of the picker.
   *
   * @param minValue The min value inclusive.
   */
  public void setMinValue(final int minValue)
  {
    if (mMinValue == minValue) return;
    if (minValue < 0) throw new IllegalArgumentException("minValue must be >= 0");
    mMinValue = minValue;
    if (mMinValue > mValue) mValue = mMinValue;
    initializeSelectorWheelIndices();
    invalidate();
  }

  /**
   * Returns the max value of the picker.
   *
   * @return The max value.
   */
  public int getMaxValue()
  {
    return mMaxValue;
  }

  /**
   * Sets the max value of the picker.
   *
   * @param maxValue The max value inclusive.
   */
  public void setMaxValue(final int maxValue)
  {
    if (mMaxValue == maxValue) return;
    if (maxValue < 0) throw new IllegalArgumentException("maxValue must be >= 0");
    mMaxValue = maxValue;
    if (mMaxValue < mValue) mValue = mMaxValue;
    initializeSelectorWheelIndices();
    invalidate();
  }

  @Override protected float getTopFadingEdgeStrength() { return TOP_AND_BOTTOM_FADING_EDGE_STRENGTH; }
  @Override protected float getBottomFadingEdgeStrength() { return TOP_AND_BOTTOM_FADING_EDGE_STRENGTH; }

  @Override protected void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
  }

  @Override protected void drawableStateChanged()
  {
    super.drawableStateChanged();

    final Drawable selectionDivider = mSelectionDivider;
    if (selectionDivider != null && selectionDivider.isStateful() && selectionDivider.setState(getDrawableState()))
      invalidateDrawable(selectionDivider);
  }

  @Override public void jumpDrawablesToCurrentState()
  {
    super.jumpDrawablesToCurrentState();

    if (mSelectionDivider != null)
      mSelectionDivider.jumpToCurrentState();
  }

  @Override protected void onDraw(@NonNull final Canvas canvas)
  {
    final int left = getLeft();
    final int right = getRight();
    float x = (right - left) / 2;
    float y = mCurrentScrollOffset;

    // draw the selector wheel
    int[] selectorIndices = mSelectorIndices;
    for (int i = 0; i < selectorIndices.length; i++)
    {
      int selectorIndex = selectorIndices[i];
      String scrollSelectorValue = mSelectorIndexToStringCache.get(selectorIndex);
      if (i == SELECTOR_MIDDLE_ITEM_INDEX)
        mSelectorWheelPaint.setAlpha(255);
      else
        mSelectorWheelPaint.setAlpha(128);
      canvas.drawText(scrollSelectorValue, x, y, mSelectorWheelPaint);
      y += mSelectorElementHeight;
    }

    // draw the selection dividers
    if (mSelectionDivider != null)
    {
      // draw the top divider
      int topOfTopDivider = mTopSelectionDividerTop;
      int bottomOfTopDivider = topOfTopDivider + mSelectionDividerHeight;
      mSelectionDivider.setBounds(0, topOfTopDivider, right, bottomOfTopDivider);
      mSelectionDivider.draw(canvas);

      // draw the bottom divider
      int bottomOfBottomDivider = mBottomSelectionDividerBottom;
      int topOfBottomDivider = bottomOfBottomDivider - mSelectionDividerHeight;
      mSelectionDivider.setBounds(0, topOfBottomDivider, right, bottomOfBottomDivider);
      mSelectionDivider.draw(canvas);
    }
  }

  /**
   * Resets the selector indices and clear the cached string representation of
   * these indices.
   */
  private void initializeSelectorWheelIndices()
  {
    mSelectorIndexToStringCache.clear();
    int[] selectorIndices = mSelectorIndices;
    int current = getValue();
    for (int i = 0; i < mSelectorIndices.length; i++)
    {
      int selectorIndex = current + (i - SELECTOR_MIDDLE_ITEM_INDEX);
      if (mWrapSelectorWheel)
        selectorIndex = getWrappedSelectorIndex(selectorIndex);
      selectorIndices[i] = selectorIndex;
      ensureCachedScrollSelectorValue(selectorIndices[i]);
    }
  }

  /**
   * Sets the current value of this DatePicker.
   *
   * @param current The new value of the DatePicker.
   * @param notifyChange Whether to notify if the current value changed.
   */
  private void setValueInternal(int current, final boolean notifyChange)
  {
    if (mValue == current) return;
    // Wrap around the values if we go past the start or end
    if (mWrapSelectorWheel)
      current = getWrappedSelectorIndex(current);
    else
    {
      current = Math.max(current, mMinValue);
      current = Math.min(current, mMaxValue);
    }
    int previous = mValue;
    mValue = current;
    if (notifyChange) notifyChange(previous, current);
    initializeSelectorWheelIndices();
    invalidate();
  }

  private void initializeSelectorWheel()
  {
    initializeSelectorWheelIndices();
    int[] selectorIndices = mSelectorIndices;
    mSelectorElementHeight = getHeight() / (selectorIndices.length - 1);
    mSelectorTextGapHeight = mSelectorElementHeight / 2;
    mInitialScrollOffset = -(mSelectorWheelPaint.getFontMetricsInt().top + mSelectorWheelPaint.getFontMetricsInt().bottom) / 2;
    mCurrentScrollOffset = mInitialScrollOffset;
  }

  private void initializeFadingEdges()
  {
    setVerticalFadingEdgeEnabled(true);
    setFadingEdgeLength(mTextSize);
  }

  /**
   * Callback invoked upon completion of a given <code>scroller</code>.
   */
  private void onScrollerFinished(@NonNull final Scroller scroller)
  {
    ensureScrollWheelAdjusted();
    if (scroller == mFlingScroller)
      onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
  }

  /**
   * Handles transition to a given <code>scrollState</code>
   */
  private void onScrollStateChange(final int scrollState)
  {
    if (mScrollState == scrollState) return;
    mScrollState = scrollState;
    if (mOnScrollListener != null)
      mOnScrollListener.onScrollStateChange(this, scrollState);
  }

  /**
   * Flings the selector with the given <code>velocityY</code>.
   */
  private void fling(final int velocityY)
  {
    mPreviousScrollerY = 0;
    if (velocityY > 0)
      mFlingScroller.fling(0, 0, 0, velocityY, 0, 0, 0, Integer.MAX_VALUE);
    else
      mFlingScroller.fling(0, Integer.MAX_VALUE, 0, velocityY, 0, 0, 0, Integer.MAX_VALUE);
    invalidate();
  }

  /**
   * @return The wrapped index <code>selectorIndex</code> value.
   */
  private int getWrappedSelectorIndex(final int selectorIndex)
  {
    if (mMaxValue == mMinValue) return selectorIndex;
    if (selectorIndex > mMaxValue)
      return mMinValue + (selectorIndex - mMaxValue) % (mMaxValue - mMinValue) - 1;
    else if (selectorIndex < mMinValue)
      return mMaxValue - (mMinValue - selectorIndex) % (mMaxValue - mMinValue) + 1;
    return selectorIndex;
  }

  /**
   * Increments the <code>selectorIndices</code> whose string representations
   * will be displayed in the selector.
   */
  private void incrementSelectorIndices(@NonNull final int[] selectorIndices)
  {
    for (int i = 0; i < selectorIndices.length - 1; i++)
      selectorIndices[i] = selectorIndices[i + 1];
    int nextScrollSelectorIndex = selectorIndices[selectorIndices.length - 2] + 1;
    if (mWrapSelectorWheel && nextScrollSelectorIndex > mMaxValue)
      nextScrollSelectorIndex = mMaxValue;
    selectorIndices[selectorIndices.length - 1] = nextScrollSelectorIndex;
    ensureCachedScrollSelectorValue(nextScrollSelectorIndex);
  }

  /**
   * Decrements the <code>selectorIndices</code> whose string representations
   * will be displayed in the selector.
   */
  private void decrementSelectorIndices(@NonNull final int[] selectorIndices)
  {
    for (int i = selectorIndices.length - 1; i > 0; i--)
      selectorIndices[i] = selectorIndices[i - 1];
    int nextScrollSelectorIndex = selectorIndices[1] - 1;
    if (mWrapSelectorWheel && nextScrollSelectorIndex < mMinValue)
      nextScrollSelectorIndex = mMaxValue;
    selectorIndices[0] = nextScrollSelectorIndex;
    ensureCachedScrollSelectorValue(nextScrollSelectorIndex);
  }

  /**
   * Ensures we have a cached string representation of the given <code>
   * selectorIndex</code> to avoid multiple instantiations of the same string.
   */
  private void ensureCachedScrollSelectorValue(final int selectorIndex)
  {
    SparseArray<String> cache = mSelectorIndexToStringCache;
    String scrollSelectorValue = cache.get(selectorIndex);
    if (scrollSelectorValue != null) return;
    if (selectorIndex < mMinValue || selectorIndex > mMaxValue)
      scrollSelectorValue = "";
    else
      scrollSelectorValue = formatNumber(selectorIndex);
    cache.put(selectorIndex, scrollSelectorValue);
  }

  /**
   * Formats the value according to the format attribute.
   * @param value the value.
   * @return the string rep of the value.
   */
  private String formatNumber(final int value) {
    if (FORMAT_DATE == mFormatStyle)
    {
      mTmpCal.setTimeInMillis(((long)value) * 86400000 - mTmpCal.get(Calendar.ZONE_OFFSET));
      return String.format(Locale.JAPANESE, "%02d - %02d %s", mTmpCal.get(Calendar.MONTH) + 1, mTmpCal.get(Calendar.DAY_OF_MONTH), Const.WEEKDAYS[mTmpCal.get(Calendar.DAY_OF_WEEK) - 1]);
    }
    else // FORMAT_INTEGER
      return String.format(Locale.JAPANESE, "%02d", value);
  }

  /**
   * Notifies the listener, if registered, of a change of the value of this
   * DatePicker.
   */
  private void notifyChange(final int previous, final int current)
  {
    if (mOnValueChangeListener != null)
      mOnValueChangeListener.onValueChange(this, previous, mValue);
  }

  /**
   * Ensures that the scroll wheel is adjusted i.e. there is no offset and the
   * middle element is in the middle of the widget.
   *
   * @return Whether an adjustment has been made.
   */
  private boolean ensureScrollWheelAdjusted()
  {
    // adjust to the closest value
    int deltaY = mInitialScrollOffset - mCurrentScrollOffset;
    if (deltaY != 0)
    {
      mPreviousScrollerY = 0;
      if (Math.abs(deltaY) > mSelectorElementHeight / 2)
        deltaY += (deltaY > 0) ? -mSelectorElementHeight : mSelectorElementHeight;
      mAdjustScroller.startScroll(0, 0, 0, deltaY, SELECTOR_ADJUSTMENT_DURATION_MILLIS);
      invalidate();
      return true;
    }
    return false;
  }
}
