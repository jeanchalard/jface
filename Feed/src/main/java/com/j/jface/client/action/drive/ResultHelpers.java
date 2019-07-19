package com.j.jface.client.action.drive;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;

/**
 * Utility functions for those heavily-encapsulated things that are async results and futures
 */
public class ResultHelpers
{
  @NonNull public static String errorMessage(@Nullable final Result result, @NonNull final String defaultMessage)
  {
    if (null == result) return defaultMessage;
    final Status status = result.getStatus();
    if (null == status) return defaultMessage;
    if (status.isSuccess()) throw new RuntimeException("Error message for a successful result ?");
    final String error = status.getStatusMessage();
    if (null == error) return defaultMessage;
    return error;
  }
}
