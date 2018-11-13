package io.monix.website

import scala.collection.JavaConverters._
import scala.util.Try
import shapeless._
import shapeless.labelled.FieldType

trait YAML[T] {
  def rawDecode(any: Any): T

  final def decode(any: Any): Try[T] =
    Try(rawDecode(any))
}

trait LowPriorityYAML {
  implicit def deriveInstance[F, G](implicit gen: LabelledGeneric.Aux[F, G], yg: Lazy[YAML[G]]): YAML[F] =
    new YAML[F] {
      def rawDecode(any: Any): F = gen.from(yg.value.rawDecode(any))
    }
}

object YAML extends LowPriorityYAML {

  def apply[T](implicit T: YAML[T]): YAML[T] = T

  def decodeTo[T : YAML](any: Any): Try[T] =
    YAML[T].decode(any)

  implicit def listYAML[T : YAML]: YAML[List[T]] =
    (any: Any) => {
      any.asInstanceOf[java.util.List[_]].asScala.toList.map(YAML[T].rawDecode)
    }

  implicit def listOptionYAML[T : YAML]: YAML[Option[List[T]]] = {
    case null => None
    case any: Any =>
      Some(any.asInstanceOf[java.util.List[_]].asScala.toList.map(YAML[T].rawDecode))
  }

  implicit def stringYAML: YAML[String] =
    (any: Any) => any.asInstanceOf[String]

  implicit def deriveHNil: YAML[HNil] =
    (any: Any) => HNil

  implicit def deriveHCons[K <: Symbol, V, T <: HList]
    (implicit
      key: Witness.Aux[K],
      yv: Lazy[YAML[V]],
      yt: Lazy[YAML[T]]
    ): YAML[FieldType[K, V] :: T] = new YAML[FieldType[K, V] :: T] {

    def rawDecode(any: Any) = {
      val k = key.value.name
      val map = any.asInstanceOf[java.util.Map[String, _]]
      val value = if (map.containsKey(k)) map.get(k) else null
      val head: FieldType[K, V] = labelled.field(yv.value.rawDecode(value))
      val tail = yt.value.rawDecode(map)
      head :: tail
    }
  }

}
