package com.j.jface.action

import com.google.android.gms.wearable.DataClient

fun <T, R> (() -> T).then(next : (T) -> R) = ActionChain<Unit, Unit, T>(this).then(next)
fun <T, R> ((DataClient) -> T).then(next : (T) -> R) = ActionChain<DataClient, DataClient, T>(this).then(next)

class ActionChain<in Arg, Transition, Result>
{
  val car : ActionChain<Arg, *, Transition>?
  val cdr : (Transition) -> Result
  constructor(f : () -> Result)
  {
    car = null
    cdr = { f() }
  }
  constructor(f : (Arg) -> Result)
  {
    car = null
    cdr = f as (Transition) -> Result
  }
  private constructor(car : ActionChain<Arg, *, Transition>, cdr : (Transition) -> Result)
  {
    this.car = car
    this.cdr = cdr
  }
  private constructor(car : ActionChain<Arg, *, Transition>, cdr : () -> Result)
  {
    this.car = car
    this.cdr = { cdr() }
  }

  fun <U> then(f : () -> U) : ActionChain<Arg, Result, U> = ActionChain(this, f)
  fun <U> then(f : (Result) -> U) : ActionChain<Arg, Result, U> = ActionChain(this, f)
}
