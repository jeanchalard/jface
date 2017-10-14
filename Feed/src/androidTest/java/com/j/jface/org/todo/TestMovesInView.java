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
import com.j.jface.lifecycle.TodoProviderBoot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

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

  public void testMoveXAfterY(final TodoListView listView, final int x, final int y)
  {
    final Todo tx = listView.get(x);
    final Todo ty = y < 0 ? null : listView.get(y);
    final int z = y + 1; final Todo tz = z >= listView.size() ? null : listView.get(z);
    if (null != ty && ty.ord.startsWith(tx.ord)) return; // Don't try to order a todo after one if its children, it makes no sense
// Ça suffit pas : quand tu startDragging ça change les indices, c'est la mouise
    listView.startDragging(tx);
    final int dir = x < y ? 1 : -1;
    for (int cur = x; cur != z; cur += dir)
      listView.moveTemporarily(cur, cur + dir);
    listView.stopDragging(tx);
    try
    {
      listView.assertListIsConsistentWithDB();
    }
    catch (final AssertionError e)
    {
      final String txS = "\"" + tx.ord + "\" (" + tx.text + ")";
      final String tyS = null == ty ? "null" : "\"" + ty.ord + "\" (" + ty.text + ")";
      final String tzS = null == tz ? "null" : "\"" + tz.ord + "\" (" + tz.text + ")";
      Log.e("WRONG DRAG", "Trying to put " + txS + " between " + tyS + " and " + tzS);
      throw e;
    }
  }

  public int testMoveAllHelper(final TodoListView listView, final int openablesState[], final int iteration)
  {
    Log.e("TESTING", "" + openablesState.length + " " + iteration);
    resetDB();
    final TodoList l = TodoList.getInstance(mContext);
    final TodoListView view = new TodoListView(l);
    // Apply openables. The openablesState array is already in large to small order.
    for (int index : openablesState)
      if (index < 0)
        listView.toggleOpen(listView.get(-index - 1));
    final int destinationsCount = listView.size() + 1;
    final int movedIndex = iteration / destinationsCount;
    final int destination = (iteration % destinationsCount) - 1; // -1 to size()
    if (movedIndex != destination)
    {
      try { testMoveXAfterY(listView, movedIndex, destination); }
      catch (Throwable e)
      {
        Log.e("CRASH", "    final int openablesState[] = { " + TextUtils.join(", ", Util.arrayListFromIntArray(openablesState)) + " }; final int iteration = " + iteration + ";");
        throw e;
      }
    }
    l.unload();
    final int next = iteration + (destination + 1 == movedIndex ? 2 : 1);
    return destinationsCount * listView.size() <= next ? -1 : next;
  }

  @Test public void testMove()
  {
    final TodoListView listView = new TodoListView(TodoList.getInstance(mContext));
    final ArrayList<Integer> openablesIndices = new ArrayList<>();
    for (int i = listView.size() - 1 ; i >= 0; --i)
      if (!listView.get(i).ui.leaf) openablesIndices.add(i);
    final int openablesState[] = new int[openablesIndices.size()];
    for (int i = openablesState.length - 1; i >= 0; --i) openablesState[i] = openablesIndices.get(i);

    for (int iteration = 2; iteration >= 0; )
      iteration = testMoveAllHelper(listView, openablesState, iteration);
  }

  @Test public void testOne()
  {
    final TodoListView listView = new TodoListView(TodoList.getInstance(mContext));
    final int openablesState[] = { 32, 26, 12, 11, 5, 0 }; final int iteration = 4;
    testMoveAllHelper(listView, openablesState, iteration);
  }
}
