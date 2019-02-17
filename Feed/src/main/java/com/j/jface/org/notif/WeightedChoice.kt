package com.j.jface.org.notif

import java.util.Random

interface WeightedChoice
{
  val weight : Int
}

fun <T : WeightedChoice> chooseWeighted(choices : ArrayList<T>) : T
{
  var lot = Random().nextInt(choices.sumBy { it.weight })
  choices.forEach {
    lot -= it.weight
    if (lot <= 0) return it
  }
  return choices[0]
}

fun <T : WeightedChoice> chooseWeighted(choices : Array<T>) : T
{
  var lot = Random().nextInt(choices.sumBy { it.weight })
  choices.forEach {
    lot -= it.weight
    if (lot <= 0) return it
  }
  return choices[0]
}
