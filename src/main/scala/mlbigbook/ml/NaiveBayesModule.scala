package mlbigbook.ml

import mlbigbook.data._
import mlbigbook.wordcount.NumericMap

import scala.reflect.ClassTag


abstract class ProbabilitySpace_GoofingAround {

  sealed trait Event

  case class SingleEvent(name: String) extends Event

  case class ConditionedEvent(e: Event, c: Event) extends Event


  type Probability = Double

  type Density = Event => Probability


  object Implicits {

    implicit class Conditional(e: Event) {
      def |(conditionedOn: Event): ConditionedEvent =
        ConditionedEvent(e, conditionedOn)
    }
  }


  object Op {

    import Implicits._

    val e1 = SingleEvent("hello")
    val e2 = SingleEvent("world")
    val e3 = SingleEvent("universe")

    val c1 = e1 | e2
    val c2 = e1 | e3

  }
}

abstract class NaiveBayesModule[N : Numeric] {




  // count features
  // incorporate smoothing
  // for each vector, v:
  //    c = v.class()                                             assert(shouldBeNeg.)
  //    for each feature, f:
  //      if v(f) is nonzero:
  //        featurevalu
  // by class
  //    classcount[c] += 1
  //
  // likelihood[c][f] = LOG ( featurevaluecount[c][f] / sum c' { featurevalucecount[c'][f] } )
  // posterior[c] = LOG ( classcount[c] / sum c' { classcount[c'] } )
  //
  // for new one:
  //    for each feature *f* in new one:
  //      s += likelihood[c'][*f*]
  //    posterior[c'] + s


  type LabelMap[L] = NumericMap[N]#M[L]

  type FeatureMap[Label, Feature] = Map[Label, NumericMap[N]#M[Feature]]

  object FeatureMap {
    def empty[Label,Feature]: FeatureMap[Label, Feature] =
      Map.empty[Label, NumericMap[N]#M[Feature]]
  }

  type Counts[Label, Feature] = (LabelMap[Label], FeatureMap[Label, Feature])

  def count[Label : Equiv, Feature : Equiv](data: DistData[(Label,Iterable[Feature])])(implicit nm: NumericMap[N]): Counts[Label, Feature] =
    data
      .aggregate((nm.empty[Label], FeatureMap.empty[Label, Feature]))(
        {
          case ((labelMap, featureMap), (label, features)) =>

            val updatedLabelMap = nm.increment(labelMap, label)

            val existing = featureMap.getOrElse(label, nm.empty[Feature])

            val updatedFeatureMapForLabel =
              features
                .foldLeft(existing) {
                  case (fm, feature) => nm.increment(fm, feature)
                }

            (updatedLabelMap, featureMap + (label -> updatedFeatureMapForLabel))
        },
        {
          case ((lm1, fm1), (lm2, fm2)) =>

            val combinedLabelMap = nm.combine(lm1, lm2)

            val combinedFeatureMap =
              combinedLabelMap.keys
                .map { label =>
                  (label, nm.combine(fm1(label), fm2(label)))
                }
                .toMap

            (combinedLabelMap, combinedFeatureMap)
        }
      )



  type Probability = Double
  type Prior[Label] = Label => Probability
  type Likelihood[Feature, Label] = Label => Feature => Probability

  def counts2priorandlikeihood[F,L](c: Counts[L,F]): (Prior[L], Likelihood[F,L]) = {

    val num = implicitly[Numeric[N]]

    val prior = {
      val totalClassCount = num.toDouble(c._1.map(_._2).sum)
      c._1.map {
        case (label, count) => (label, math.log(num.toDouble(count) / totalClassCount))
      }
    }

    val likelihood = {
      val totalFeatureClassCount = num.toDouble(c._2.map(_._2.map(_._2).sum).sum)
      c._2.map {
        case (label, featureMap) =>
          (
            label,
            featureMap.map {
              case (feature, count) => (feature, math.log(num.toDouble(count) / totalFeatureClassCount))
            }
          )
      }
    }

    (prior, likelihood)
  }

  trait Dist[T] {
    def probabilityOf(x: T): Option[Double]
    def unsafeProbabilityOf(x: T): Double
    def range: Iterable[T]
  }

  case class MapDist[T](m: Map[T, Double]) extends Dist[T] {
    def probabilityOf(x:T) = m.get(x)
    def unsafeProbabilityOf(x: T) = m(x)
    def range = m.keys
  }

  type NaiveBayes[Feature,Label] = Iterable[Feature] => Dist[Label]


  def counts2nb[Feature,Label](c: Counts[Label, Feature]): NaiveBayes[Feature,Label] =  {


    val labels = c._1.keys.toSeq

    val (prior, likelihood) = counts2priorandlikeihood(c)

    (features: Iterable[Feature]) =>
      MapDist(
        labels.map { label =>
          (
            label,
            prior(label) + features.map(f => likelihood(label)(f)).sum
            )
        }
          .toMap
      )
  }

}