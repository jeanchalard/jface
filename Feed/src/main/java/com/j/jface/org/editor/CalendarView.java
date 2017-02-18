package com.j.jface.org.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.j.jface.Const;
import com.j.jface.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONDAY;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;

public class CalendarView extends LinearLayout implements NumericPicker.OnValueChangeListener, View.OnClickListener
{
  public interface DateChangeListener
  {
    public void onDateChanged(final long newDate);
  }
  @NonNull private final GregorianCalendar mCalendar;
  @NonNull private final TextView mMonthView;
  @NonNull private final TextView mDayTimeView;
  @NonNull private final TextView mResetDayButton;
  @NonNull private final TextView mResetMinutesButton;
  @NonNull private final NumericPicker mDateView;
  @NonNull private final NumericPicker mHourView;
  @NonNull private final NumericPicker mMinuteView;
  @NonNull private final CalendarGridView mCalendarView;
  @NonNull private final ArrayList<DateChangeListener> mListeners;
  public CalendarView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    setOrientation(GridLayout.HORIZONTAL);
    LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.calendar_view, this, true);
    mMonthView = (TextView)findViewById(R.id.calendarView_monthName);
    mDayTimeView = (TextView)findViewById(R.id.calendarView_dayTime);
    mCalendarView = (CalendarGridView)findViewById(R.id.calendarView_gridView);
    mCalendarView.mParent = this;
    mDateView = (NumericPicker)findViewById(R.id.calendarView_date);
    mHourView = (NumericPicker)findViewById(R.id.calendarView_hour);
    mMinuteView = (NumericPicker)findViewById(R.id.calendarView_minute);
    mResetDayButton = (TextView)findViewById(R.id.calendarView_resetDay);
    mResetMinutesButton = (TextView)findViewById(R.id.calendarView_resetMinutes);
    mCalendar = new GregorianCalendar();
    mDateView.setMinValue(16000); mDateView.setMaxValue(Integer.MAX_VALUE);
    mHourView.setMinValue(0); mHourView.setMaxValue(23);
    mMinuteView.setMinValue(0); mMinuteView.setMaxValue(59);
    mCalendar.setTimeInMillis(System.currentTimeMillis());
    mListeners = new ArrayList<>(); // Listeners to this Calendar view
    refreshDateFromCalendar();
    setupInternalListeners();
  }

  public void addDateChangeListener(@NonNull final DateChangeListener listener)
  {
    mListeners.add(listener);
  }

  public void setDate(final long date)
  {
    mCalendar.setTimeInMillis(date);
    refreshDateFromCalendar();
  }

  private void refreshDateFromCalendar()
  {
    final long newDate = mCalendar.getTimeInMillis();
    updateTops();
    mDateView.setValue((int)((newDate + mCalendar.get(Calendar.ZONE_OFFSET)) / 86400000));
    mHourView.setValue(mCalendar.get(HOUR_OF_DAY));
    mMinuteView.setValue(mCalendar.get(MINUTE));
    mCalendarView.invalidate();
    for (final DateChangeListener listener : mListeners)
      listener.onDateChanged(newDate);
  }

  private void updateTops()
  {
    mMonthView.setText(String.format(Locale.JAPANESE, "%04d年 %02d月", mCalendar.get(YEAR), mCalendar.get(MONTH) + 1));
    mDayTimeView.setText(String.format(Locale.JAPANESE, "%02d (%s)  –  %02d : %02d",
     mCalendar.get(DAY_OF_MONTH), Const.WEEKDAYS[mCalendar.get(DAY_OF_WEEK) - 1], mCalendar.get(HOUR_OF_DAY), mCalendar.get(MINUTE)));
  }

  private void setupInternalListeners()
  {
    mDateView.setOnValueChangedListener(this);
    mHourView.setOnValueChangedListener(this);
    mMinuteView.setOnValueChangedListener(this);
    mResetDayButton.setOnClickListener(this);
    mResetMinutesButton.setOnClickListener(this);
  }

  @Override public void onValueChange(@NonNull final NumericPicker picker, final int oldVal, final int newVal)
  {
    final int field;
    if (picker == mDateView) field = DAY_OF_MONTH;
    else if (picker == mHourView) field = HOUR_OF_DAY;
    else /* if (picker == mMinuteView) */ field = MINUTE;
    final int adjustment;
    if (Math.abs(newVal - oldVal) != 1)
      adjustment = (field == HOUR_OF_DAY ? 24 : 60) * (newVal > oldVal ? -1 : 1);
    else
      adjustment = 0;
    mCalendar.add(field, newVal - oldVal + adjustment);
    refreshDateFromCalendar();
  }

  @Override public void onClick(@NonNull final View v)
  {
    if (v == mResetDayButton)
    {
      final GregorianCalendar c = new GregorianCalendar();
      mCalendar.set(c.get(YEAR), c.get(MONTH), c.get(DAY_OF_MONTH));
    }
    else if (v == mResetMinutesButton)
    {
      final int m = mCalendar.get(MINUTE);
      final int add;
      if (m > 50) add = 60 - m;
      else add = -m;
      mCalendar.add(MINUTE, add);
    }
    refreshDateFromCalendar();
  }

  public static class CalendarGridView extends View
  {
    @Nullable private CalendarView mParent;
    @NonNull private final Paint mPaint = new Paint();
    @NonNull private final Path mPath = new Path();
    private int mTileWidth = 20;
    private int mTileHeight = 20;

    public CalendarGridView(final Context context, final AttributeSet attrs)
    {
      super(context, attrs);
      mPaint.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));
    }

    private void paintDay(final int index, @NonNull final Canvas canvas)
    {
      final int x = (index % 7) * mTileWidth;
      final int y = (index / 7) * mTileHeight;
      canvas.drawRect(x + 1, y + 1, x + mTileWidth, y + mTileHeight, mPaint);
    }

    private void writeDay(final int index, final int day, @NonNull final Canvas canvas, @NonNull final Paint paint)
    {
      final int x = (index % 7) * mTileWidth;
      final int y = (index / 7) * mTileHeight;
      final String s = String.valueOf(day);
      final int w = (int)(paint.measureText(s) + 0.5);
      canvas.drawText(String.valueOf(day), x + mTileWidth - w - 4, y + mTileHeight - 4, paint);
    }

    @NonNull final GregorianCalendar mTmpCal = new GregorianCalendar();
    @Override public void onDraw(@NonNull final Canvas canvas)
    {
      final CalendarView parent = mParent;
      if (null == parent) return;
      final GregorianCalendar calendar = parent.mCalendar;
      mPaint.setStyle(Paint.Style.FILL);

      final int firstDayOfMonth = calendar.getActualMinimum(DAY_OF_MONTH);
      final int lastDayOfMonth = calendar.getActualMaximum(DAY_OF_MONTH);
      final int day = calendar.get(DAY_OF_MONTH);
      final int dayOfWeek = (calendar.get(DAY_OF_WEEK) - MONDAY) % 7;
      final int dayOfWeekOfFirstDay = (dayOfWeek + firstDayOfMonth - day + 35) % 7;
      mPaint.setColor(0x1FFFFFFF);
      for (int d = dayOfWeekOfFirstDay - 1; d >= 0; --d) paintDay(d, canvas);
      for (int d = lastDayOfMonth + dayOfWeekOfFirstDay; d < 6 * 7; ++d) paintDay(d, canvas);

      mPaint.setColor(0x3F3F7FFF);
      for (int d = dayOfWeekOfFirstDay >= 6 ? 12 : 5; d < lastDayOfMonth + dayOfWeekOfFirstDay; d += 7) paintDay(d, canvas);
      mPaint.setColor(0x3FFF7F3F);
      for (int d = 6; d < lastDayOfMonth + dayOfWeekOfFirstDay; d += 7) paintDay(d, canvas);

      mPaint.setColor(0x5FFFFF00);
      paintDay(day + dayOfWeekOfFirstDay - 1, canvas);

      mPaint.setColor(0xFFFFFFFF);
      mPaint.setStyle(Paint.Style.STROKE);
      for (int i = 0; i <= 6; ++i)
      {
        final int x = i * mTileWidth;
        final int y = i * mTileHeight;
        mPath.moveTo(x, 0);
        mPath.lineTo(x, 6 * mTileHeight);
        mPath.moveTo(0, y);
        mPath.lineTo(7 * mTileWidth, y);
      }
      mPath.moveTo(7 * mTileWidth, 0);
      mPath.lineTo(7 * mTileWidth, 6 * mTileHeight);
      canvas.drawPath(mPath, mPaint);
      mPath.reset();

      writeDay(day + dayOfWeekOfFirstDay - 1, day, canvas, mPaint);
      writeDay(dayOfWeekOfFirstDay, firstDayOfMonth, canvas, mPaint);
      writeDay(dayOfWeekOfFirstDay + lastDayOfMonth - 1, lastDayOfMonth, canvas, mPaint);
      for (int d = dayOfWeekOfFirstDay == 0 ? 0 : 7; d < dayOfWeekOfFirstDay + lastDayOfMonth; d += 7) writeDay(d, d - dayOfWeekOfFirstDay + 1, canvas, mPaint);

      mPaint.setAlpha(64);
      mTmpCal.setTimeInMillis(calendar.getTimeInMillis());
      mTmpCal.add(DAY_OF_MONTH, -day + firstDayOfMonth - 1); // Just case some month does not start on day 1. What planet do I live on.
      int dayOfLastMonth = mTmpCal.get(DAY_OF_MONTH);
      for (int d = dayOfWeekOfFirstDay - 1; d >= 0; --d, --dayOfLastMonth) writeDay(d, dayOfLastMonth, canvas, mPaint);
      mTmpCal.setTimeInMillis(calendar.getTimeInMillis());
      mTmpCal.add(DAY_OF_MONTH, lastDayOfMonth - day + 1);
      int dayOfNextMonth = mTmpCal.get(DAY_OF_MONTH);
      for (int d = dayOfWeekOfFirstDay + lastDayOfMonth; d < 6 * 7; ++d, ++dayOfNextMonth) writeDay(d, dayOfNextMonth, canvas, mPaint);
      mPaint.setAlpha(255);
    }

    @Override public void onSizeChanged(final int w, final int h, final int oldw, final int oldh)
    {
      mTileWidth = (w - 1) / 7;
      mTileHeight = (h - 1) / 6;
      mPaint.setTextSize(mTileHeight / 2);
    }

    @Override protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec)
    {
      final int w = MeasureSpec.getSize(widthMeasureSpec);
      final int h = MeasureSpec.getSize(heightMeasureSpec);
      setMeasuredDimension(7 * ((w - 1) / 7) + 1, 6 * ((h - 1) / 6) + 1);
    }

    public boolean onTouchEvent(@NonNull final MotionEvent event)
    {
      if (MotionEvent.ACTION_UP != event.getAction()) return MotionEvent.ACTION_DOWN == event.getAction();
      final CalendarView parent = mParent;
      if (null == parent) return false;
      final GregorianCalendar calendar = parent.mCalendar;
      final float x = event.getX();
      final float y = event.getY();
      final int row = (int)(y / mTileHeight);
      final int column = (int)(x / mTileWidth);
      final int tile = row * 7 + column;
      final int firstDayOfMonth = calendar.getActualMinimum(DAY_OF_MONTH);
      final int day = calendar.get(DAY_OF_MONTH);
      final int dayOfWeek = (calendar.get(DAY_OF_WEEK) - MONDAY) % 7;
      final int dayOfWeekOfFirstDay = (dayOfWeek + firstDayOfMonth - day + 35) % 7;
      final int tappedDate = tile - dayOfWeekOfFirstDay + 1;
      calendar.add(DAY_OF_MONTH, tappedDate - day);
      parent.refreshDateFromCalendar();
      return true;
    }
  }
}
