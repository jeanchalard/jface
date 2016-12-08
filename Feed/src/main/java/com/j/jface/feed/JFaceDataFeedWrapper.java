/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.j.jface.feed;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MenuItem;

/**
 * The activity wrapper for the host app for Jface.
 */
public class JFaceDataFeedWrapper extends Activity
{
  @Nullable private JFaceDataFeed mW;

  // Activity callbacks
  @Override protected void onCreate(@Nullable final Bundle savedInstanceState)
  {
    super.onCreate(null);
    mW = new JFaceDataFeed(this, savedInstanceState);
  }

  @Override public void onSaveInstanceState(@NonNull final Bundle savedInstanceState)
  {
    mW.onSaveInstanceState(savedInstanceState);
  }

  @Override public void onConfigurationChanged(final Configuration c)
  {
    super.onConfigurationChanged(c);
    mW.onConfigurationChanged(c);
  }

  @Override public boolean onOptionsItemSelected(final MenuItem i)
  {
    if (mW.onOptionsItemSelected(i)) return true;
    return super.onOptionsItemSelected(i);
  }

  @Override public void onPostCreate(final Bundle b)
  {
    super.onPostCreate(b);
    mW.onPostCreate(b);
  }

  @Override public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] results)
  {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    mW.onRequestPermissionsResult(requestCode, permissions, results);
  }
}
