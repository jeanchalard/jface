package com.j.jface.org

import android.content.Intent
import android.os.Handler
import android.widget.TextView
import com.google.android.gms.auth.api.Auth
import com.google.firebase.iid.FirebaseInstanceId
import com.j.jface.R
import com.j.jface.firebase.Firebase
import com.j.jface.lifecycle.JOrgBoot
import com.j.jface.lifecycle.WrappedActivity
import java.util.concurrent.Executors

class AuthTrampoline(args : WrappedActivity.Args) : WrappedActivity(args)
{
  val context = mA
  private val executor = Executors.newSingleThreadExecutor()
  private val statusText : TextView by lazy { mA.findViewById<TextView>(R.id.auth_trampoline_status_text) }
  private val handler : Handler = Handler()

  init
  {
    if (Firebase.isLoggedIn()) finish()
    else
    {
      mA.setContentView(R.layout.auth_trampoline)
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
      FirebaseInstanceId.getInstance().getToken() // Force token generation
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
    FirebaseInstanceId.getInstance().getToken() // Force token generation
    handler.postDelayed({ finish() }, 5000)
  }

  fun onSignInFailure(msg : String) = log(msg)

  private fun finish()
  {
    mA.startActivity(Intent().setClass(mA, JOrgBoot::class.java))
    mA.finish()
  }
  override fun onDestroy() = executor.shutdown()
}
