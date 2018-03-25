package com.j.jface.action.firebase

import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.j.jface.Const
import com.j.jface.R
import com.j.jface.action.ActionChain
import com.j.jface.action.InformUserAction
import com.j.jface.action.then

fun auth(context : Context) : ActionChain<Unit, Task<GoogleSignInAccount>, AuthResult?>
{
  val client : GoogleSignInClient = GoogleSignIn.getClient(context, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
   .requestIdToken(context.getString(R.string.firebase_client_id))
   .build())
  return { client.silentSignIn() }.then { it : Task<GoogleSignInAccount> ->
    if (it.isSuccessful)
    {
      // ...wait wat ? Not supposed to have to call await() here, the continuation is supposed to be called after it's
      // complete, but in the case of Firebase for some reason it's called immediately and I have no idea why
      Tasks.await(FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(it.result.idToken, null)))
    }
    else
    {
      // TODO : test both of these codepaths
      if (context is Activity)
      {
        context.runOnUiThread { context.startActivityForResult(client.signInIntent, Const.GOOGLE_SIGN_IN_RESULT_CODE) }
        null
      }
      else
      {
        val intent = PendingIntent.getActivity(context, Const.GOOGLE_SIGN_IN_RESULT_CODE, client.signInIntent, FLAG_ONE_SHOT or FLAG_UPDATE_CURRENT)
        InformUserAction(context, "Log in required", pendingIntent = intent).invoke()
        null
      }
    }
  }
}
