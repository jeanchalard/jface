package com.j.jface.org.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.j.jface.Const;
import com.j.jface.R;

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
  @NonNull private GregorianCalendar mCalendar;
  @NonNull private final TextView mMonthView;
  @NonNull private final TextView mDayTimeView;
  @NonNull private final TextView mResetDayButton;
  @NonNull private final TextView mResetMinutesButton;
  @NonNull private final NumericPicker mDateView;
  @NonNull private final NumericPicker mHourView;
  @NonNull private final NumericPicker mMinuteView;
  @NonNull private final CalendarGridView mCalendarView;
  public CalendarView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    setOrientation(GridLayout.HORIZONTAL);
    LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.calendar_view, this, true);
    mMonthView = (TextView)findViewById(R.id.calendarView_monthName);
    mDayTimeView = (TextView)findViewById(R.id.calendarView_dayTime);
    mCalendarView = (CalendarGridView)findViewById(R.id.calendarView_gridView);
    mDateView = (NumericPicker)findViewById(R.id.calendarView_date);
    mHourView = (NumericPicker)findViewById(R.id.calendarView_hour);
    mMinuteView = (NumericPicker)findViewById(R.id.calendarView_minute);
    mResetDayButton = (TextView)findViewById(R.id.calendarView_resetDay);
    mResetMinutesButton = (TextView)findViewById(R.id.calendarView_resetMinutes);
    mCalendar = mCalendarView.mCalendar;
    mDateView.setMinValue(16000); mDateView.setMaxValue(Integer.MAX_VALUE);
    mHourView.setMinValue(0); mHourView.setMaxValue(23);
    mMinuteView.setMinValue(0); mMinuteView.setMaxValue(59);
    setDate(System.currentTimeMillis());
    setupListeners();
  }

  public void setDate(final long when)
  {
    mCalendar.setTimeInMillis(when);
    updateTops();
    mDateView.setValue((int)(mCalendar.getTimeInMillis() / 86400000));
    mHourView.setValue(mCalendar.get(HOUR_OF_DAY));
    mMinuteView.setValue(mCalendar.get(MINUTE));
    mCalendarView.invalidate();
  }

  private void updateTops()
  {
    mMonthView.setText(String.format(Locale.JAPANESE, "%04d年 %02d月", mCalendar.get(YEAR), mCalendar.get(MONTH) + 1));
    mDayTimeView.setText(String.format(Locale.JAPANESE, "%02d (%s)  –  %02d : %02d",
     mCalendar.get(DAY_OF_MONTH), Const.WEEKDAYS[mCalendar.get(DAY_OF_WEEK) - 1], mCalendar.get(HOUR_OF_DAY), mCalendar.get(MINUTE)));
  }

  private void setupListeners()
  {
    mDateView.setOnValueChangedListener(this);
    mResetDayButton.setOnClickListener(this);
    mResetMinutesButton.setOnClickListener(this);
  }

  @Override public void onValueChange(@NonNull final NumericPicker picker, final int oldVal, final int newVal)
  {
    final int field;
    if (picker == mDateView) field = DAY_OF_MONTH;
    else if (picker == mHourView) field = HOUR_OF_DAY;
    else /* if (picker == mMinuteView) */ field = MINUTE;
    mCalendar.add(field, newVal - oldVal);
    updateTops();
    mCalendarView.invalidate();
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
    setDate(mCalendar.getTimeInMillis());
  }

  public static class CalendarGridView extends View
  {
    @NonNull public final GregorianCalendar mCalendar = new GregorianCalendar();
    @NonNull private final Paint mPaint = new Paint();
    @NonNull private final Path mPath = new Path();
    private int mTileWidth = 20;
    private int mTileHeight = 20;

    public CalendarGridView(final Context context, final AttributeSet attrs)
    {
      super(context, attrs);
      mPaint.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));
    }

    private void paintDay(final int d, @NonNull final Canvas canvas)
    {
      final int x = (d % 7) * mTileWidth;
      final int y = (d / 7) * mTileHeight;
      canvas.drawRect(x + 1, y + 1, x + mTileWidth, y + mTileHeight, mPaint);
    }

    @Override public void onDraw(@NonNull final Canvas canvas)
    {
      mPaint.setStyle(Paint.Style.FILL);

      final int firstDayOfMonth = mCalendar.getActualMinimum(DAY_OF_MONTH);
      final int lastDayOfMonth = mCalendar.getActualMaximum(DAY_OF_MONTH);
      final int day = mCalendar.get(DAY_OF_MONTH);
      final int dayOfWeek = (mCalendar.get(DAY_OF_WEEK) - MONDAY) % 7;
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
    }

    @Override public void onSizeChanged(final int w, final int h, final int oldw, final int oldh)
    {
      mTileWidth = (w - 1) / 7;
      mTileHeight = (h - 1) / 6;
    }

    @Override protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec)
    {
      final int w = MeasureSpec.getSize(widthMeasureSpec);
      final int h = MeasureSpec.getSize(heightMeasureSpec);
      setMeasuredDimension(7 * ((w - 1) / 7) + 1, 6 * ((h - 1) / 6) + 1);
    }
  }
}
