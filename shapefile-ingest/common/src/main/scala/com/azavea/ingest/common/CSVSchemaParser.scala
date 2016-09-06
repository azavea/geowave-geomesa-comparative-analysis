package com.azavea.ingest.common

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory, Point}
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

import scala.collection.JavaConversions._
import scala.util.parsing.combinator.RegexParsers

object CSVSchemaParser {

  sealed trait Expr {
    def genSFT(builder: SimpleFeatureTypeBuilder): Unit = ()
    def eval(csvrow: Array[String]): Any

    def makeSimpleFeature(sftName: String, csvrow: Array[String]): SimpleFeature = {
      val values = eval(csvrow).asInstanceOf[Seq[(String, Object)]]
      val sftBuilder = new SimpleFeatureTypeBuilder
      sftBuilder.setName(sftName)

      val builder = new SimpleFeatureBuilder(sftBuilder.buildFeatureType)
      builder.addAll(values.map(_._2))
      builder.buildFeature(builder.getFeatureType.getName.toString)
    }

    def makeSimpleFeature(sftName: String, csvrow: Array[String], id: String): SimpleFeature = {
      val values = eval(csvrow).asInstanceOf[Seq[(String, Object)]]
      val sftBuilder = new SimpleFeatureTypeBuilder
      sftBuilder.setName(sftName)

      val builder = new SimpleFeatureBuilder(sftBuilder.buildFeatureType)
      builder.addAll(values.map(_._2))
      builder.buildFeature(id)
    }
  }

  case class Literal(s: String) extends Expr {
    def eval(csvrow: Array[String]) = s
  }

  case class Field(id: Int) extends Expr {
    def eval(csvrow: Array[String]) = csvrow(id - 1)
  }

  case class Func(f: String, args: Seq[Expr]) extends Expr {
    def returnType(): Class[_] = f match {
      case "int" => classOf[Int]
      case "double" => classOf[Double]
      case "point" => classOf[Point]
      case "date" => classOf[java.util.Date]
      case "concat" => classOf[String]
    }

    def assertArity(n: Int) = {
      if (args.length != n) {
        throw new java.lang.IllegalArgumentException(s"Function '$f' requires $n arguments, received ${args.length}")
      }
    }

    def eval(csvrow: Array[String]) = f match {
      case "int" => { assertArity(1) ; args(0).eval(csvrow).asInstanceOf[String].toInt }
      case "double" => { assertArity(1) ; args(0).eval(csvrow).asInstanceOf[String].toDouble }
      case "concat" => { ("" /: args) { case (str, ex) => str + ex.eval(csvrow).asInstanceOf[String] } }
      case "point" => { 
        assertArity(2)
        val gf = new GeometryFactory
        gf.createPoint(new Coordinate(args(0).eval(csvrow).asInstanceOf[String].toDouble, 
                                      args(1).eval(csvrow).asInstanceOf[String].toDouble))
      }
      case "date" => {
        assertArity(2)
        val df = new java.text.SimpleDateFormat(args(0).eval(csvrow).asInstanceOf[String])
        df.parse(args(1).eval(csvrow).asInstanceOf[String])
      }
    }
  }

  case class Assgn(tok: String, body: Expr) extends Expr {
    override def genSFT(builder: SimpleFeatureTypeBuilder) = { 
      body match {
        case Field(_) => builder.add(tok, classOf[String])
        case func: Func => {
          builder.add(tok, func.returnType)
          if (func.returnType == classOf[java.util.Date])
            builder.userData("geomesa.index.dtg", tok)
        }
        case _ => ()
      }
    }

    def eval(csvrow: Array[String]) = (tok, body.eval(csvrow))
  }

  case class Spec(exprs: Seq[Expr]) extends Expr {
    override def genSFT(builder: SimpleFeatureTypeBuilder) = exprs.foreach { ex => ex.genSFT(builder) }

    def eval(csvrow: Array[String]) = exprs.map(_.eval(csvrow))
  }

  object SpecParser extends RegexParsers {
    def expr: Parser[Expr] = rep1sep(assign, ",") ^^ Spec
    def assign: Parser[Expr] = token ~ "=" ~ atom ^^ { case name ~ "=" ~ value => Assgn(name, value) }
    def atom: Parser[Expr] = field | func | literal
    def field: Parser[Expr] = "$" ~ """[0-9]*""".r ^^ { case "$" ~ n => Field(n.toInt) }
    def func: Parser[Expr] = token ~ ("(" ~> repsep(atom, ",") <~ ")") ^^ { case fn ~ args => Func(fn, args) }
    def literal: Parser[Expr] = "{" ~> """[^}]*""".r <~ "}" ^^ Literal
    def token: Parser[String] = """[_a-zA-Z]+[_a-zA-Z0-9]*""".r

    def apply(input: String): Expr = parseAll(expr, input) match {
      case Success(result, _) => result
      case failure : NoSuccess => scala.sys.error(failure.msg)
    }
  }

  def schemaToSimpleFeatureType(spec: Expr, builder: SimpleFeatureTypeBuilder): SimpleFeatureType = {
    spec.genSFT(builder)
    builder.buildFeatureType
  }

}

