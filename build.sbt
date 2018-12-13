import com.sun.jna.Platform

val compileNative = taskKey[Unit]("Compile cpp into shared library.")

lazy val root = (project in file(".")).settings(
  name := "annoy4s",
  version := "0.8.0",
  scalaVersion := "2.11.12",
  crossScalaVersions := Seq("2.11.12", "2.12.6"),
  libraryDependencies ++= Seq(
    "com.github.pathikrit" %% "better-files" % "3.6.0",
    "net.java.dev.jna" % "jna" % "4.5.2",
    "org.slf4s" %% "slf4s-api" % "1.7.25",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test",
    "org.slf4j" % "slf4j-simple" % "1.7.25" % "test"
  ),
  fork := true,
  organization := "net.pishen",
  licenses += "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html"),
  homepage := Some(url("https://github.com/annoy4s/annoy4s")),
  pomExtra := (
    <scm>
      <url>https://github.com/annoy4s/annoy4s.git</url>
      <connection>scm:git:git@github.com:annoy4s/annoy4s.git</connection>
    </scm>
    <developers>
      <developer>
        <id>pishen</id>
        <name>Pishen Tsai</name>
      </developer>
      <developer>
        <id>nemo83</id>
        <name>Giovanni Gargiulo</name>
      </developer>
    </developers>
  ),
  compileNative := {
    val libDir = file(s"src/main/resources/${Platform.RESOURCE_PREFIX}")
    if (!libDir.exists) {
      libDir.mkdirs()
    }
    val lib = libDir / (if (Platform.isMac) "libannoy.dylib" else "libannoy.so")
    val source = file("src/main/cpp/annoyjava.cpp")
    val cmd = s"g++ -o ${lib.getAbsolutePath} -shared ${if (Platform.isMac) "-dynamiclib" else "-fPIC"} ${source.getAbsolutePath}"
    println(cmd)
    import scala.sys.process._
    cmd.!
  }
)
