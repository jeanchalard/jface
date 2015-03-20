package com.j.jface.face;

public class Triplet<T>
{
  final T first;
  final T second;
  final T third;
  public Triplet(final T f, final T s, final T t) {
    first = f;
    second = s;
    third = t;
  }
}
