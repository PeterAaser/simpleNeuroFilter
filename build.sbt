// organization := "com.github.fractal"
name := "neuroFilters"
version := "0.0.1"

scalaVersion := "2.12.4"
// Compiler settings. Use scalac -X for other options and their description.
// See Here for more info http://www.scala-lang.org/files/archive/nightly/docs/manual/html/scalac.html
scalacOptions ++= List("-language:higherKinds","-feature","-deprecation", "-unchecked", "-Xlint")
scalacOptions -= "-Ywarn-unused-import"


addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")
libraryDependencies ++= Dependencies.backendDeps.value

// For Settings/Task reference, see http://www.scala-sbt.org/release/sxr/sbt/Keys.scala.html
javaOptions += "-Xmx8G"
connectInput in run := true

resolvers ++= Seq(
  "neuroflow-libs" at "https://github.com/zenecture/neuroflow-libs/raw/master/"
)
