package com.j.jface.lifecycle

import android.content.Intent
import android.os.Handler
import android.widget.TextView
import com.google.android.gms.auth.api.Auth
import com.google.firebase.iid.FirebaseInstanceId
import com.j.jface.R
import com.j.jface.feed.FCMHandler
import com.j.jface.firebase.Firebase
import java.util.concurrent.Executors

abstract class AuthTrampoline(args : WrappedActivity.Args) : WrappedActivity(args)
{
  abstract val trampolineDestination : Class<*>

  val context = args.activity
  private val executor = Executors.newSingleThreadExecutor()
  private val statusText : TextView by lazy { mA.findViewById<TextView>(R.id.auth_trampoline_status_text) }
  private val handler : Handler = Handler()

  init
  {
    if (Firebase.isLoggedIn()) finish()
    else
    {
      context.setContentView(R.layout.auth_trampoline)
      statusText.text = "Signing in with Google..."
      trySignIn()
    }
  }

  private fun log(msg : String)
  {
    handler.post { statusText.text = statusText.text.toString() + "\n" + msg }
  }

  private fun trySignIn()
  {
    if (Firebase.isLoggedIn())
    {
      FirebaseInstanceId.getInstance().token // Force token generation
      log("Login successful.")
      handler.postDelayed({ finish() }, 5000)
    }
    else
      // Will cause a callback, either onActivityResult (after GoogleSignIn went through) or onSignInSuccess or onSignInFailure (for Firebase auth).
      Firebase.signIn(this)
  }

  override fun onActivityResult(requestCode : Int, resultCode : Int, data : Intent?)
  {
    val signInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
    if (signInResult.isSuccess)
    {
      log("Google sign in successful.\nSigning in to Firebase...")
      trySignIn()
    }
    else log("Google sign in failed : " + signInResult.status + " : " + signInResult.status.statusMessage)
  }

  private var signIns : Int = 0
  fun onSignInSuccess()
  {
    if (!Firebase.isLoggedIn())
    {
      if (++signIns < 3)
      {
        log("Unknown error, trying again...")
        trySignIn()
      } else log("Bailing, fix your code")
    }
    log("Login successful.")
    FirebaseInstanceId.getInstance().token // Force token generation
    FCMHandler.registerTokenForWearData(context)
    handler.postDelayed({ finish() }, 5000)
  }

  fun onSignInFailure(msg : String) = log(msg)

  private fun finish()
  {
    val intent = if (mA.intent.hasCategory(Intent.CATEGORY_LAUNCHER)) Intent() else mA.intent
    intent.setClass(mA, trampolineDestination)
    context.startActivity(intent)
    context.finish()
  }
  override fun onDestroy() = executor.shutdown()
}
