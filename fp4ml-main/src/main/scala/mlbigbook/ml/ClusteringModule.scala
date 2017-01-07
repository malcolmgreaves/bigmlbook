package mlbigbook.ml

import fif.Data

import scala.language.{postfixOps, higherKinds, reflectiveCalls}

trait ClusteringModule extends ItemNumVecModule {

  type Vectorizer = {
    val vectorize: Item => V[N]
    val nDimensions: Int
  }

  type Distance = (V[N], V[N]) => N

  case class Center(id: String, mean: V[N])

  final def cluster[D[_]: Data](
      conf: ClusteringConf,
      dist: Distance,
      mkVectorizer: D[Item] => Vectorizer
  )(data: D[Item]): Seq[Center] =
    cluster(conf, dist, mkVectorizer(data))(data)

  def cluster[D[_]: Data](
      conf: ClusteringConf,
      dist: Distance,
      toVec: Vectorizer
  )(data: D[Item]): Seq[Center]

  import Data.ops._

  final def assign[D[_]: Data](
      centers: Seq[Center],
      distance: Distance,
      vectorizer: Vectorizer
  )(
      data: D[Item]
  ): D[String] =
    assign(centers, distance)(
      data map { vectorizer.vectorize }
    )

  final def assign[D[_]: Data](
      centers: Seq[Center],
      distance: Distance
  )(
      data: D[V[N]]
  ): D[String] =
    if (centers isEmpty)
      data map { _ =>
        ""
      } else if (centers.size == 1) {
      val label = centers.head.id
      data map { _ =>
        label
      }

    } else {

      val lessThan = implicitly[Numeric[N]].lt _
      val restCents = centers.slice(1, centers.size)

      data map { v =>
        val (nearestLabel, _) =
          restCents.foldLeft(centers.head.id, distance(centers.head.mean, v)) {

            case (currChampion @ (minLabel, minDistance), center) =>
              val distToCenter = distance(center.mean, v)
              if (lessThan(distToCenter, minDistance))
                (center.id, distToCenter)
              else
                currChampion
          }

        nearestLabel
      }
    }

}
