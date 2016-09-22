enablePlugins(GatlingPlugin)

scalaVersion := "2.11.8"

scalacOptions := Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps")

libraryDependencies ++= Seq(
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.2",
  "io.gatling"            % "gatling-test-framework"    % "2.2.2" % "test",
  "io.gatling"            % "gatling-app"    % "2.2.2",
  "com.github.scopt"     %% "scopt"          % "3.5.0")
