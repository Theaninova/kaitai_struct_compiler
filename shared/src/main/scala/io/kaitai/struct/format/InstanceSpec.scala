package io.kaitai.struct.format

import io.kaitai.struct.datatype.DataType
import io.kaitai.struct.exprlang.{Ast, Expressions}

sealed abstract class InstanceSpec(val doc: Option[String])
case class ValueInstanceSpec(_doc: Option[String], value: Ast.expr, ifExpr: Option[Ast.expr], var dataType: Option[DataType]) extends InstanceSpec(_doc)
case class ParseInstanceSpec(_doc: Option[String], dataType: DataType, cond: ConditionalSpec, pos: Option[Ast.expr], io: Option[Ast.expr]) extends InstanceSpec(_doc) with AttrLikeSpec

object InstanceSpec {
  def fromYaml(src: Any, path: List[String], metaDef: MetaDefaults, id: InstanceIdentifier): InstanceSpec = {
    val srcMap = ParseUtils.asMapStr(src, path)

    val pos = ParseUtils.getOptValueStr(srcMap, "pos", path).map(Expressions.parse)
    val io = ParseUtils.getOptValueStr(srcMap, "io", path).map(Expressions.parse)

    ParseUtils.getOptValueStr(srcMap, "value", path).map(Expressions.parse) match {
      case Some(value) =>
        // value instance
        // TODO: check conflicts with all other keys

        // Wrap everything in EnumById if "enum" is used
        val value2 = ParseUtils.getOptValueStr(srcMap, "enum", path) match {
          case None =>
            value
          case Some(enumName) =>
            Ast.expr.EnumById(Ast.identifier(enumName), value)
        }

        val ifExpr = ParseUtils.getOptValueStr(srcMap, "if", path).map(Expressions.parse)

        ValueInstanceSpec(
          ParseUtils.getOptValueStr(srcMap, "doc", path),
          value2,
          ifExpr,
          None
        )
      case None =>
        // normal positional instance
        val fakeAttrMap = srcMap.filterKeys((key) => key != "pos" && key != "io")
        val a = AttrSpec.fromYaml(fakeAttrMap, path, metaDef, id)
        ParseInstanceSpec(a.doc, a.dataType, a.cond, pos, io)
    }
  }
}
