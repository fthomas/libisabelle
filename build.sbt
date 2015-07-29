lazy val standardSettings = Seq(
  organization := "info.hupel",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.5", "2.11.7", "2.12.0-M2"),
  javacOptions += "-Xlint:unchecked"
)

lazy val warningSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-unchecked",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfatal-warnings"
  )
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)


lazy val root = project.in(file("."))
  .settings(standardSettings)
  .settings(noPublishSettings)
  .aggregate(pideInterface, libisabelle, setup, pide2014, pide2015, bootstrap, tests)

lazy val pideInterface = project.in(file("pide-interface"))
  .settings(moduleName := "pide-interface")
  .settings(standardSettings)
  .settings(warningSettings)
  .settings(Seq(
    libraryDependencies ++= {
      if (scalaVersion.value startsWith "2.10")
        Seq()
      else
        Seq("org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4")
    }
  ))

lazy val libisabelle = project
  .dependsOn(pideInterface)
  .settings(standardSettings)
  .settings(warningSettings)

lazy val setup = project.in(file("setup"))
  .dependsOn(libisabelle, pideInterface)
  .settings(standardSettings)
  .settings(warningSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.apache.commons" % "commons-compress" % "1.9",
      "org.apache.commons" % "commons-lang3" % "3.3.2",
      "com.github.fge" % "java7-fs-more" % "0.2.0",
      "com.google.code.findbugs" % "jsr305" % "1.3.+" % "compile"
    )
  )

lazy val pide2014 = project.in(file("pide/2014"))
  .dependsOn(pideInterface)
  .settings(moduleName := s"pide-2014")
  .settings(standardSettings)

lazy val pide2015 = project.in(file("pide/2015"))
  .dependsOn(pideInterface)
  .settings(moduleName := s"pide-2015")
  .settings(standardSettings)

lazy val versions = Map(
  "2014" -> pide2014,
  "2015" -> pide2015
)

lazy val bootstrap = project.in(file("bootstrap"))
  .dependsOn(libisabelle, setup, pideInterface)
  .settings(noPublishSettings)
  .settings(standardSettings)
  .settings(warningSettings)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoPackage := "edu.tum.cs.isabelle.bootstrap",
    buildInfoKeys ++= {
      versions.toList.map { case (v, p) =>
        BuildInfoKey.map(exportedProducts in (p, Runtime)) {
          case (_, classFiles) =>
            (s"Isa$v", (classFiles.map(_.data.toURI.toURL)))
        }
      }
    }
  )

lazy val tests = project.in(file("tests"))
  .dependsOn(bootstrap)
  .settings(noPublishSettings)
  .settings(standardSettings)
  .settings(warningSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % "3.6.3" % "test",
      "org.specs2" %% "specs2-scalacheck" % "3.6.3" % "test",
      "org.scalacheck" %% "scalacheck" % "1.12.2" % "test"
    )
  )
