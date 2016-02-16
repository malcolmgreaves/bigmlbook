/*
 * Collection of classes, traits, and objects for manipulating Map types.
 *
 * @author Malcolm Greaves
 */
package mlbigbook.data

import mlbigbook.ml.{EqualityMap, Equality}

import scala.collection.Map

object MapOps {

  /**
   * Given two input maps of the same type, re-order them such that the first element of the
   * resulting tuple has more keys than the second element.
   */
  def reoderSmallLarge[A, B](a: Map[A, B], b: Map[A, B]): (Map[A, B], Map[A, B]) =
    if (a.size < b.size)
      (a, b)
    else
      (b, a)

  object Implicits {

    /**
     * Implicit AddMap and MultiplyMap instances for Double.
     */
    object DoubleM {
      implicit val Add = new OLD_AddMap[Double]
      implicit val Multiply = new MultiplyMap[Double]
    }

    /**
     * Implicit AddMap and MultiplyMap instances for Long.
     */
    object LongM {
      implicit val Add = new OLD_AddMap[Long]
      implicit val Multiply = new MultiplyMap[Long]
    }

    /**
     * Implicit AddMap and MultiplyMap instances for Int.
     */
    object IntM {
      implicit val Add = new OLD_AddMap[Int]
      implicit val Multiply = new MultiplyMap[Int]
    }
  }

}

/** Object for adding double and long maps together */
object OLD_AddMap {
  val Real = new OLD_AddMap[Double]
  val Whole = new OLD_AddMap[Long]
}

/** Class that supports operations for adding elements and combining maps */
class OLD_AddMap[@specialized(Byte, Int, Long, Float, Double) N: Numeric] {

  import scala.Numeric.Implicits._

  val empty: Map[String, N] = Map()

  def add(m: Map[String, N], k: String, v: N): Map[String, N] = {
    m.get(k) match {
      case Some(existing) => (m - k) + (k -> (existing + v))
      case None           => m + (k -> v)
    }
  }

  /**
   * Combines two maps. If maps m1 and m2 both have key k, then the resulting
   * map will have m1(k) + m2(k) for the value of k.
   */
  def combine(m1: Map[String, N], m2: Map[String, N]): Map[String, N] = {
    val (a, b) = if (m1.size < m2.size) (m1, m2) else (m2, m1)
    a.foldLeft(b)({
      case (aggmap, (k, v)) => aggmap.get(k) match {
        case Some(existing) => (aggmap - k) + (k -> (existing + v))
        case None           => aggmap + (k -> v)
      }
    })
  }
}

class AddMap[K :Equality, @specialized(Byte, Int, Long, Float, Double) N: Numeric] {

  import scala.Numeric.Implicits._

  val empty: Map[K, N] = EqualityMap.empty[K, N]

  def add(m: Map[K, N], k: K, v: N): Map[K, N] = {
    m.get(k) match {
      case Some(existing) => (m - k) + (k -> (existing + v))
      case None           => m + (k -> v)
    }
  }

  /**
    * Combines two maps. If maps m1 and m2 both have key k, then the resulting
    * map will have m1(k) + m2(k) for the value of k.
    */
  def combine(m1: Map[K, N], m2: Map[K, N]): Map[K, N] = {
    val (a, b) = MapOps.reoderSmallLarge(m1, m2)
    a.foldLeft(b)({
      case (aggmap, (k, v)) => aggmap.get(k) match {
        case Some(existing) => (aggmap - k) + (k -> (existing + v))
        case None           => aggmap + (k -> v)
      }
    })
  }
}

/** Class that supports operations on maps that indicate the presense of keys. */
object IndicatorMap extends OLD_AddMap[Long] {

  override def add(m: Map[String, Long], word: String, ignore: Long): Map[String, Long] =
    mark(m, word)

  /** Ensures that word is in the resulting map with a value of 1 */
  def mark(m: Map[String, Long], word: String): Map[String, Long] =
    m.get(word) match {

      case Some(existing) =>

        if (existing == 1)
          m
        else
          (m - word) + (word -> 1)

      case None =>
        m + (word -> 1)
    }

  /** Constructs a map that has all of the words from the input maps */
  override def combine(m1: Map[String, Long], m2: Map[String, Long]): Map[String, Long] = {
    val (a, b) =
      if (m1.size < m2.size)
        (m1, m2)
      else
        (m2, m1)

    a.foldLeft(b) {
      case (aggmap, (k, _)) => mark(aggmap, k)
    }
  }
}

/** Object for multiplying double maps together. */
object MultiplyMap {
  val Real: MultiplyMap[Double] = new MultiplyMap[Double]()
}

/** Class for multiplying maps together */
class MultiplyMap[@specialized(Long, Double) N: Numeric] {

  import scala.Numeric.Implicits._

  private val addmap = new OLD_AddMap[N]()
  private lazy val empty: Map[String, N] = Map()

  /**
   * Constructs a mapping where all elements' values are multiplied together.
   * Note that only keys that appear in both maps will be present in the resulting mapping.
   * The keys that are not in the resulting mapping would have value 0.
   */
  def multiplyWith(larger: Map[String, N])(smaller: Map[String, N]): Map[String, N] = {
    smaller.aggregate(empty)(
      {
        case (aggmap, (k, v)) => larger.get(k) match {
          case Some(existing) => (aggmap - k) + (k -> (existing * v))
          case None           => aggmap // (anything) * 0 = 0
        }
      },
      addmap.combine
    )
  }
}
