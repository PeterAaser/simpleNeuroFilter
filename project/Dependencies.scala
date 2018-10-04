import sbt._

// udash depends on org.spire-math:jawn-parser_2.12:0.10.4
object Dependencies {
  val versionOfScala = "2.12.4"

  val fs2Version = "0.10.4"
  val http4sVersion = "0.18.0"
  val circeVersion = "0.9.1"
  val catsVersion = "1.1.0"
  val catsEffectVersion = "0.10"

  val ScalaTagsVersion = "0.6.2"
  val ScalaRxVersion = "0.3.2"
  val jqueryVersion = "3.2.1"

  // Dependencies for JVM part of code
  val backendDeps = Def.setting(
    Seq(
      "com.lihaoyi" %% "sourcecode" % "0.1.4",                      // expert println debugging
      "com.lihaoyi" %% "pprint" % "0.5.3",                          // pretty print for types and case classes
      "org.typelevel" %% "cats-core" % catsVersion,                 // abstract category dork stuff

      "com.chuusai" %% "shapeless" % "2.3.2",                   // Abstract level category dork stuff

      "joda-time" % "joda-time" % "2.9.9",
      "org.joda" % "joda-convert" % "2.0.1",

      "org.typelevel" %% "cats-effect" % catsEffectVersion,     // IO monad category wank

      "co.fs2" %% "fs2-core" % fs2Version,                      // The best library
      "co.fs2" %% "fs2-io"   % fs2Version,                      // The best library

      ))
}
