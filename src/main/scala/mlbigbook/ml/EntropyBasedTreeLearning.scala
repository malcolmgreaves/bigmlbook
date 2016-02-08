package mlbigbook.ml

import fif.{ Data, TravData }

import scala.language.{ higherKinds, postfixOps }

object EntropyBasedTreeLearning {

  import FeatureVectorSupport._
  import fif.Data.ops._

  def apply[D[_]: Data](
    dtModule:       DecisionTree.Type[Boolean, Seq[String]],
    data:           D[(Seq[String], Boolean)],
    importantFeats: FeatureImportance
  )(
    implicit
    fs: FeatureSpace
  ): Option[dtModule.Node] =
    if (fs.size > 0 && fs.isCategorical.forall(identity))
      learn(data, fs.features.indices.toSet)(
        implicitly[Data[D]],
        fs,
        dtModule,
        importantFeats
      )
    else
      None

  private[this] def learn[D[_]: Data](
    data:         D[(Seq[String], Boolean)],
    featuresLeft: Set[Int]
  )(
    implicit
    fs:             FeatureSpace,
    dtModule:       DecisionTree.Type[Boolean, Seq[String]],
    importantFeats: FeatureImportance
  ): Option[dtModule.Node] =

    if (data isEmpty)
      None

    else {

      val (nPos, nNeg) =
        data.aggregate((0l, 0l))(
          {
            case ((nP, nN), (_, label)) =>
              if (label)
                (nP + 1l, nN)
              else
                (nP, nN + 1l)
          },
          {
            case ((nP1, nN1), (nP2, nN2)) =>
              (nP1 + nP2, nN1 + nN2)
          }
        )

      val majorityDec = nPos > nNeg

      if (featuresLeft isEmpty)
        Some(dtModule.Leaf(majorityDec))

      else {

        (nPos, nNeg) match {

          case (0l, nonZero) =>
            Some(dtModule.Leaf(false))

          case (nonZero, 0l) =>
            Some(dtModule.Leaf(true))

          case (_, _) =>

            val gainRatioPerFeature = importantFeats(data)

            {
              implicit val v = TupleVal2[String]
              implicit val td = TravData
              Argmax(gainRatioPerFeature.toTraversable)
            }
              .map {
                case (nameOfMinEntropyFeature, _) =>
                  // partition data according to the discrete values of each
                  val distinctValues = fs.categorical2values(nameOfMinEntropyFeature)
                  val indexOfMinEntropyFeat = fs.feat2index(nameOfMinEntropyFeature)
                  val newFeaturesLeft = featuresLeft - indexOfMinEntropyFeat

                  val partitionedByDistinctValues: Seq[(String, D[(Seq[String], Boolean)])] =
                    distinctValues
                      .map { distinct =>

                        val partitionedForDistinct =
                          data.filter {
                            case (catFeats, _) =>
                              catFeats(indexOfMinEntropyFeat) == distinct
                          }

                        (distinct, partitionedForDistinct)
                      }

                  // Final steps to make the parent node:
                  // (1) recursively apply learn() to each partition
                  // (2) when not none, add as a child
                  // (3) if all none, then turn into a leaf w/ decision = majority vote of data
                  // (4) in parent's test, if given a bad feature vector or one whose feature
                  //     value maps to a learn() call that resulted in None, then default to
                  //     a leaf node with decision = majority vote

                  val childrenResults =
                    partitionedByDistinctValues
                      .map {
                        case (_, partitionedForDistinct) =>
                          learn(partitionedForDistinct, newFeaturesLeft)
                      }
                      .zipWithIndex

                  val defaultToMajDecision = dtModule.Leaf(majorityDec)

                  if (childrenResults.forall { case (m, _) => m.isEmpty })
                    defaultToMajDecision

                  else {

                    val distinct2child =
                      childrenResults
                        .collect {
                          case (Some(child), index) =>
                            (distinctValues(index), child)
                        }
                        .toMap

                    val children = distinct2child.values.toSeq

                    dtModule.Parent(
                      (fv: Seq[String]) => {
                        val valueOfMinEntropyFeat = fv(indexOfMinEntropyFeat)
                        if (fv.size > indexOfMinEntropyFeat &&
                          distinct2child.contains(valueOfMinEntropyFeat))
                          distinct2child(valueOfMinEntropyFeat)
                        else
                          defaultToMajDecision
                      },
                      children
                    )
                  }
              }
        }
      }
    }

}

