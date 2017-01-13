package com.azavea.ingest.common

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory, Point}
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

import scala.util.parsing.combinator.RegexParsers

sealed trait Expr {
  def genSFT(builder: SimpleFeatureTypeBuilder): Unit = ()
  def eval(csvrow: Array[String]): Any
  def makeSimpleFeature(sft: SimpleFeatureType, csvrow: Array[String]): SimpleFeature = {
    val builder = new SimpleFeatureBuilder(sft)
    val values = eval(csvrow).asInstanceOf[Seq[Any]].map(_.asInstanceOf[(String, Object)])
    values foreach { case (name, value) => builder.add(value) }
    builder.buildFeature(sft.getName.toString)
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

object ShapefileFromCSV {
  case class Params (mode: String = "",
                     source: String = "",
                     instanceId: String = "",
                     zookeepers: String = "zookeeper",
                     user: String = "root",
                     password: String = "GisPwd",
                     namespace: String = "",
                     dropLines: Int = 0,
                     separator: String = "\t",
                     codec: Expr = Spec(Nil),
                     tyBuilder: SimpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder,
                     urlList: Seq[String] = Nil)

  val parser = new scopt.OptionParser[Params]("geowave-csv-ingest") {
    override def terminate(exitState: Either[String, Unit]): Unit = ()

    head("geowave-csv-ingest", "0.1")

    help("help").hidden
    note("")

    cmd("geowave")
      .action( (_, conf) => conf.copy(mode = "geowave", instanceId = "geowave") )
      .text("Perform a GeoWave-backed ingest")
      .children(
        opt[String]('i',"instance")
          .action( (s, conf) => conf.copy(instanceId = s) )
          .text("Accumulo instance ID [default=geowave]"),
        opt[String]('z',"zookeepers")
          .action( (s, conf) => conf.copy(zookeepers = s) )
          .text("Comma-separated list of zookeepers [default=zookeeper]"),
        opt[String]('u',"user")
          .action( (s, conf) => conf.copy(user = s) )
          .text("User namer [default=root]"),
        opt[String]('p',"password")
          .action( (s, conf) => conf.copy(password = s) )
          .text("Password [default=GisPwd]"),
        opt[String]('n',"namespace")
          .action( (s, conf) => conf.copy(namespace = s) )
          .required
          .text("Accumulo namespace (should start with 'geowave.')"),
        note(""),
        cmd("csv")
          .action( (_, conf) => conf.copy(source = "csv") )
          .children(
            opt[Int]('d',"drop")
              .action( (i, conf) => conf.copy(dropLines = i) )
              .text("Number of header lines to drop [default=0]"),
            opt[String]('s',"separator")
              .action( (s, conf) => conf.copy(separator = s) )
              .text("Field separator for input text file [default=<Tab>]"),
            opt[String]("codec")
              .action( (s, conf) => conf.copy(codec = SpecParser(s)) )
              .required
              .text("Codec description for SimpleFeature (see below)"),
            opt[String]("typename")
              .action( (s, conf) => { conf.tyBuilder.setName(s) ; conf })
              .required
              .text("Name for the SimpleFeatureType")
          ),
        cmd("shapefile")
          .action( (_, conf) => conf.copy(source = "shp") )
      )

    cmd("geomesa")
      .action( (_, conf) => conf.copy(mode = "geomesa", instanceId = "geomesa") )
      .text("Perform a GeoMesa-backed ingest")
      .children(
        opt[String]('i',"instance")
          .action( (s, conf) => conf.copy(instanceId = s) )
          .text("Accumulo instance ID [default=geomesa]"),
        opt[String]('z',"zookeepers")
          .action( (s, conf) => conf.copy(zookeepers = s) )
          .text("Comma-separated list of zookeepers [default=zookeeper]"),
        opt[String]('u',"user")
          .action( (s, conf) => conf.copy(user = s) )
          .text("User namer [default=root]"),
        opt[String]('p',"password")
          .action( (s, conf) => conf.copy(password = s) )
          .text("Password [default=GisPwd]"),
        opt[String]('n',"namespace")
          .action( (s, conf) => conf.copy(namespace = s) )
          .required
          .text("Accumulo namespace (should start with 'geomesa.')"),
        note(""),
        cmd("csv")
          .action( (_, conf) => conf.copy(source = "csv") )
          .children(
            opt[Int]('d',"drop")
              .action( (i, conf) => conf.copy(dropLines = i) )
              .text("Number of header lines to drop [default=0]"),
            opt[String]('s',"separator")
              .action( (s, conf) => conf.copy(separator = s) )
              .text("Field separator for input text file [default=<Tab>]"),
            opt[String]("codec")
              .action( (s, conf) => conf.copy(codec = SpecParser(s)) )
              .required
              .text("Codec description for SimpleFeature (see below)"),
            opt[String]("typename")
              .action( (s, conf) => { conf.tyBuilder.setName(s) ; conf })
              .required
              .text("Name for the SimpleFeatureType")
          ),
        cmd("shapefile")
          .action( (_, conf) => conf.copy(source = "shp") )
      )

    arg[String]("<url> ...")
      .unbounded
      .action( (f, conf) => conf.copy(urlList = conf.urlList :+ f) )
      .text("List of .csv file URLs to ingest")
    note("""
Codecs are defined as comma-separated list of `key=value' pairs.  A value may
take one of several forms:
    (1) `$n', where n gives the column number of the desired field (n >= 1),
        resulting in a string value.
    (2) `f(args)', where f is a function name and args are a comma-separated
        list of values, and f is chosen from the following set:
                int     string -> integer
                double  string -> double
                point   (double, double) -> point
                concat  (string, ..., string) -> string
                date    (string, string) -> date
    (3) `{string}', which describes a string literal, delimited by braces.
For example, given a file with the day and time in columns 6 and 7,
respectively, this can be converted to a timestamp in the codec using the
expression
              timestamp=date({yyyy-MM-ddHH:mm:ss}, concat($6, $7))

Note: `date' takes a format string compatible with java.text.SimpleDateFormat.
""")
  }


  def checkParser(args: Array[String]) = {
    val params = parser.parse(args, Params()) match {
      case Some(prms) => prms
      case None => {
        java.lang.System.exit(0)
        Params()
      }
    }

    // val builder = new SimpleFeatureTypeBuilder
    // params.codec.genSFT(builder)
    // builder.buildFeatureType

    println(params.codec)

  }

}


// --fields the_geom=point($6,$7),country=$20,code=int($18)
