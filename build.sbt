lazy val root = (project in file(".")).settings(
  name := "annoy4s",
  scalaVersion := "2.11.8",
  libraryDependencies ++= Seq(
    "net.java.dev.jna" % "jna" % "4.2.2"
  ),
  fork := true
)
