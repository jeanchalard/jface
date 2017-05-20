package com.j.jface;

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
import android.util.Log;

import com.j.jface.lifecycle.TodoProviderBoot;
import com.j.jface.org.todo.Todo;
import com.j.jface.org.todo.TodoSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TestMovesInView
{
  TodoSource mSource;

  @Before public void setupFixture()
  {
    Looper.prepare();
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
    final Context context = getContextWithFixtures();
    mSource = new TodoSource(context);
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

  private void assertListIsConsistentWithDB(final ArrayList<Todo> todoList)
  {
    assertEquals(todoList, mSource.fetchTodoList());
  }

  @Test public void testBasicConsistency() throws Exception
  {
    final ArrayList<Todo> todoList = mSource.fetchTodoList();
    assertListIsConsistentWithDB(todoList);
//    assertEquals("com.example.j.myapplication", appContext.getPackageName());
  }
}
