package com.j.jface.org.todo;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ProviderInfo;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.RenamingDelegatingContext;
import android.test.mock.MockContentResolver;
import android.text.TextUtils;
import android.util.Log;

import com.j.jface.Util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class TestMovesInView
{
  Context mContext;

  @Before public void setupFixture()
  {
    Looper.prepare();
    mContext = getContextWithFixtures();
  }

  private void resetDB()
  {
    try
    {
      final InputStream f = InstrumentationRegistry.getContext().getAssets().open("fixtureDB");
      final File df = InstrumentationRegistry.getTargetContext().getDatabasePath("fixtureTodoDb");
      Util.copy(f, new FileOutputStream(df));
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }

  @NonNull private Context getContextWithFixtures()
  {
    final MockContentResolver mockResolver = new MockContentResolver();
    final Context c = new ContextWrapper(InstrumentationRegistry.getTargetContext())
    {
      @Override public ContentResolver getContentResolver() { return mockResolver; }
    };
    final ProviderInfo info = new ProviderInfo();
    info.authority = "com.j.jface.provider";
    final TodoProviderBoot provider = new TodoProviderBoot();
    final RenamingDelegatingContext renamingContext = new RenamingDelegatingContext(InstrumentationRegistry.getTargetContext(), "fixture");
    renamingContext.makeExistingFilesAndDbsAccessible();
    provider.attachInfo(renamingContext, info);
    provider.onCreate();
    mockResolver.addProvider(info.authority, provider);
    return c;
  }

  @Test public void testBasicConsistency() throws Exception
  {
    final TodoSource source = new TodoSource(mContext);
    final ArrayList<TodoCore> todoList = source.fetchTodoList();
    assertEquals(todoList, source.fetchTodoList());
  }

  public void helperMoveXBeforeY(final TodoListFoldableView listView, final Todo tx, final Todo ty)
  {
    if (null != ty && ty.ord.startsWith(tx.ord) && tx != ty) return; // Don't try to order a todo after one if its children, it makes no sense
    final int x = listView.findIndexByOrd(tx.ord);
    final int fy = null == ty ? listView.size() - 1 : listView.findIndexByOrd(ty.ord);
    if (x < 0 || fy < 0) return; // At least one of these Todos are not visible because a parent is closed
//    Log.e("TESTING", "   " + x + " (" + tx.text + ") → " + fy + " (" + (null == ty ? "null" : ty.text) + ")");
    listView.startDragging(tx);
    // StartDragging may have changed indices. It can't have changed x (as it only collapses stuff under the dragged Todo and x is
    // that one) but if y is below x and x was not a leaf then the index for y will have changed. Find the new y.
    final int y = null == ty ? listView.size() - 1 : listView.findIndexByOrd(ty.ord);
    final int dir = x < y ? 1 : -1;
    for (int cur = x; cur != y; cur += dir)
      listView.moveTemporarily(cur, cur + dir);
    listView.stopDragging(tx);
    try
    {
      listView.assertListIsConsistentWithDB();
    }
    catch (final Throwable e)
    {
      final String txS = "\"" + tx.ord + "\" (" + tx.text + ")";
      final String tyS = null == ty ? "null" : "\"" + ty.ord + "\" (" + ty.text + ")";
      Log.e("WRONG DRAG", "Trying to put " + txS + " between " + tyS + " and " + tyS + " " + e);
      throw e;
    }
  }

  public void helperApplyOpenablesStateAndMoveXBeforeY(final int openablesState[], final int movedIndex, final int destination)
  {
    // Don't eschew testing for moved == dest, because it must work to grab a Todo and drop it without moving it.
    Log.e("TESTING", "moving " + movedIndex + " → " + destination);
    resetDB();
    final TodoList l = TodoList.getInstance(mContext);
    final TodoListFoldableView listView = new TodoListFoldableView(l);
    // Apply openables. The openablesState array is already in large to small order.
    for (int index : openablesState)
      if (index < 0)
        listView.toggleOpen(listView.get(-index - 1));
    try
    {
      final Todo movedTodo = l.get(movedIndex);
      final Todo destTodo = l.get(destination);
      helperMoveXBeforeY(listView, movedTodo, destTodo);
    }
    catch (final Throwable e)
    {
      Log.e("CRASH", "    final int openablesState[] = { " + TextUtils.join(", ", Util.arrayListFromIntArray(openablesState)) + " };");
      Log.e("CRASH", "    helperApplyOpenablesStateAndMoveXBeforeY(openablesState, " + movedIndex + ", " + destination + ");");
      throw e;
    }
    l.unload();
  }

  private static boolean nextOpenables(final int[] openables)
  {
    for (int i = openables.length - 1; i >= 0; --i)
    {
      openables[i] = -openables[i] - 1;
      if (openables[i] < 0) return true;
    }
    return false;
  }

  private void printOpenablesStep(final int[] openables)
  {
    int l = 0;
    for (int i = 0; i < openables.length; ++i)
    {
      l = l << 1;
      if (openables[i] < 0) l += 1;
    }
    l += 1;
    Log.e("STEP", "" + l + " / " + (1 << openables.length));
  }

  @Test public void testMove()
  {
    final long startTime = System.currentTimeMillis();
    final TodoList list = TodoList.getInstance(mContext);
    final TodoListView listView = new TodoListFoldableView(list);
    final ArrayList<Integer> openablesIndices = new ArrayList<>();
    final int size = listView.size();
    for (int i = size - 1 ; i >= 0; --i)
      if (!listView.get(i).ui.leaf) openablesIndices.add(i);
    final int openablesState[] = new int[openablesIndices.size()];
    for (int i = openablesState.length - 1; i >= 0; --i) openablesState[i] = openablesIndices.get(i);
    list.unload();

    do
    {
      printOpenablesStep(openablesState);
      // Source is the index of the Todo to move. It goes from 0 to size() - 1, obviously. Destination is the index
      // to put it at, so it will replace the Todo that is currently at this position. So if source is before destination,
      // it will go *after* destination ; if it's after, it will go *before*. That means there are only size() possible
      // destinations. The apparent discrepancy comes from the fact that, while there are indeed size() + 1 possible
      // "insertion points" for a new Todo, two of them surround the source Todo and are coded for by the same index.
      for (int source = 0; source < size; ++source)
        for (int destination = 0; destination < size; ++destination)
          helperApplyOpenablesStateAndMoveXBeforeY(openablesState, source, destination);
    }
    while (nextOpenables(openablesState));
    final long endTime = System.currentTimeMillis();
    final double duration = endTime - startTime;
    Log.e("TEST FINISHED", "Tested in " + (duration / 1_000) + "(" + startTime + " ~ " + endTime + ")");
  }

  @Test public void testOne()
  {
    final int openablesState[] = { 32, 26, 12, 11, 5, 0 }; final int iteration = 4;
    helperApplyOpenablesStateAndMoveXBeforeY(openablesState, 0, 31);
  }
}
