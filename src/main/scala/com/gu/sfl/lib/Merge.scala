package com.gu.sfl.lib

import scala.annotation.tailrec
import scala.reflect.internal.util.Collections.distinctBy

//This is a virtual straight rob from identity
object Merge {

  implicit class RichList[A](val l: List[A]) {
    def tailOrNil = if(l.isEmpty) Nil else l.tail
  }

  def mergeListBy[A, K](before: List[A], after: List[A])(key: A => K)(implicit ordering: Ordering[A]): List[A] = {
    val (beforeKeys, afterKeys) = (before.map(key).toSet, after.map(key).toSet)
    val removedKeys = beforeKeys -- afterKeys

    if(afterKeys.intersect(beforeKeys) == afterKeys) after.sorted
    else if (removedKeys.isEmpty) after.sorted
    else {
      @tailrec
      def combine(b: List[A], a: List[A], acc: List[Option[A]] = Nil) : List[A] = {
        if(b.isEmpty && a.isEmpty) acc.flatten
        else combine(b.tailOrNil, a.tailOrNil, a.headOption :: b.headOption :: acc )
      }
      val combined = combine(before, after)
      val sorted = combined.sorted(ordering.reverse)
      val distinct = distinctBy(sorted)(key)
      distinct.sorted(ordering)
    }
  }

  def mergeList[A](before: List[A], after: List[A])(implicit ordering: Ordering[A]): List[A] =
    mergeListBy[A, A](before, after)(identity)
}


