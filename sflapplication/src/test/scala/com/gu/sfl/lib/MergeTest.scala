package com.gu.sfl.lib

import org.specs2.mutable.Specification
import sfl.lib.MergeLogic._

class MergeTest extends Specification {

  "mergeList with defaul key" should {
    "perform a trivial merge" in {
       mergeList(List(1,2,3), List(1,2,3)).toSet shouldEqual(Set(1,2,3))
    }

    "merge from an empty list to a new target" in {
      mergeList(Nil, List(1,2,3)).toSet shouldEqual(Set(1,2,3))
    }

    "merge from a full delete" in {
      mergeList(List(1,2,3), Nil) shouldEqual(Nil)
    }

    "merge a later addition" in {
      mergeList(List(1,2,3), List(1,2,3,4)).toSet shouldEqual(Set(1,2,3,4))
    }

    "merges a conflicted addition" in {
      mergeList(List(1,2,4), List(1,3,4)).toSet shouldEqual(Set(1,2,3,4))
    }

    "handles simple re-ordering correctly" in {
      mergeList(List(1,2,3), List(1,3,2)).toSet shouldEqual(Set(1,3,2))
    }

    "handles less simple re-ordering correctly" in {
      mergeList(List(1,2,3), List(3,1,2)).toSet shouldEqual(Set(3,1,2))
    }

    "handles a simple delete (removes missing element)" in {
      mergeList(List(1,2,4), List(1,4)).toSet shouldEqual(Set(1,4))
    }

    "handles a reorder and deletion (takes order and elements from latter)" in {
      mergeList(List(1,2,4), List(4,1)).toSet shouldEqual(Set(4,1))
    }

    "merges conflicted delete with the default key" in {
      /*
        Note that this submission could have involved the deletion of lots of
        items, the only ones that are merged back into existence are those
        that are explicitly present elsewhere
      */

      mergeList(List(1,3), List(2,3)).toSet shouldEqual(Set(1,2,3))
    }

    "merges conflicted deletes of differing lengths" in {
      mergeList(List(1,3), List(2,3,4)).toSet shouldEqual(Set(1,2,3,4))
    }
  }

  "mergeList and a provided key" should {
    case class TestObject(id: Int, name: String)
    implicit val ordering = Ordering.by[TestObject, Int](_.id)

    val (one, two, three, four, five) = (TestObject(1,"1"), TestObject(2,"2"), TestObject(3,"3"), TestObject(4,"4"), TestObject(5,"5"))
    val anotherOne = TestObject(1, "another")

    "performs a trivial merge" in {
       mergeListBy(List(one, two), List(one, two))(_.id).toSet shouldEqual(Set(one, two))
    }

    "used key to perform a trivial merge with differing items" in {
      mergeListBy(List(one, two), List(anotherOne, two))(_.id).toSet shouldEqual(Set(anotherOne, two))
    }

    "correctly performs a simple additikon merge (includes addition from the latter" in {
      mergeListBy(List(one, two), List(one, two, three))(_.id).toSet shouldEqual(Set(one, two, three))
    }

    "handles a conflicted addition (new items in both" in {
      mergeListBy(List(one, three, four), List(one, two, four))(_.id).toSet shouldEqual(Set(one, two, three, four))
    }

    "handles a conflicted addition of different lengths (new items in both)" in {
      mergeListBy(List(one, two, four), List(one, three, four, five))(_.id).toSet shouldEqual(Set(one, two, three, four, five))
    }

    "handles a confliced addition considdering key(additions with botyh but keeps latter item by key" in {

      mergeListBy(List(one, two, four), List(anotherOne, three, four))(_.id).toSet shouldEqual(Set(anotherOne, two, three, four))
    }
  }

  "mergeList obeys the provided ordering" in {
    val (one, two, three, four, anotherOne) = ("a" -> 1, "b" -> 2, "c" -> 3, "d" -> 4, "a" -> 2)
    implicit val ordering = Ordering.by[(String, Int), Int](_._2)

    "will preserve ordering correctly in trivial merge" in {
      mergeList(List(two,one), List(two, one)) shouldEqual(List(one, two))
    }

    "sorts correctly after a simple addition merge" in {
      mergeList(List(one, two, three), List(two, one, four, three)) shouldEqual(List(one, two, three, four))
    }

    "sorts correctly after a conflicted addition merge" in {
      mergeList(List(one, four, two), List(one, four, three)) shouldEqual(List(one, two, three, four))
    }

    "when deduping with a key takes the most recent one according to the supplied ordering" in {
      mergeListBy(List(one), List(anotherOne))(_._1) shouldEqual(List(anotherOne))
    }

    "in a complex merge, take the most recent according to the provided ordering" in {
      mergeListBy(List(two, one, four), List(anotherOne, three))(_._1) shouldEqual(List(anotherOne, two, three, four))

    }
  }
}
