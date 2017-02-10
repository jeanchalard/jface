package com.j.jface.org.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.j.jface.R;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public class CalendarView extends LinearLayout
{
  @NonNull private GregorianCalendar mCalendar;
  @NonNull private final TextView mMonthView;
  @NonNull private final DatePicker mDateView;
  @NonNull private final TextView mHourView;
  @NonNull private final TextView mMinuteView;
  @NonNull private final CalendarGridView mCalendarView;
  public CalendarView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    setOrientation(LinearLayout.HORIZONTAL);
    setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.calendar_view, this, true);
    mMonthView = (TextView)findViewById(R.id.calendarView_monthName);
    mCalendarView = (CalendarGridView)findViewById(R.id.calendarView_gridView);
    mHourView = (TextView)findViewById(R.id.calendarView_hour);
    mDateView = (DatePicker)findViewById(R.id.calendarView_date);
    mMinuteView = (TextView)findViewById(R.id.calendarView_minute);
    mCalendar = mCalendarView.mCalendar;
    mDateView.setMinValue(16000); mDateView.setMaxValue(Integer.MAX_VALUE);
    setDate(System.currentTimeMillis());
  }

  public void setDate(final long when)
  {
    mCalendar.setTimeInMillis(when);
    mMonthView.setText("" + (mCalendar.get(Calendar.MONTH) + 1) + "月");
    mDateView.setValue((int)(mCalendar.getTimeInMillis() / 86400000));
    mHourView.setText(String.format(Locale.JAPANESE, "%02d", mCalendar.get(Calendar.HOUR_OF_DAY)));
    mMinuteView.setText(String.format(Locale.JAPANESE, "%02d", mCalendar.get(Calendar.MINUTE)));
    mCalendarView.invalidate();
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

      final int firstDayOfMonth = mCalendar.getActualMinimum(Calendar.DAY_OF_MONTH);
      final int lastDayOfMonth = mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
      final int day = mCalendar.get(Calendar.DAY_OF_MONTH);
      final int dayOfWeek = (mCalendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY) % 7;
      final int dayOfWeekOfFirstDay = (dayOfWeek + firstDayOfMonth - day + 35) % 7;
      mPaint.setColor(0x1FFFFFFF);
      for (int d = dayOfWeekOfFirstDay - 1; d >= 0; --d) paintDay(d, canvas);
      for (int d = lastDayOfMonth + dayOfWeekOfFirstDay; d < 6 * 7; ++d) paintDay(d, canvas);

      mPaint.setColor(0x7F3F7FFF);
      paintDay(day + dayOfWeekOfFirstDay, canvas);

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
