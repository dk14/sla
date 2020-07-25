val dottyVersion = "0.25.0-RC2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "dotty-simple",
    version := "0.1.0",

    scalaVersion := dottyVersion,

    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test", 
    libraryDependencies += ("com.github.blemale" %% "scaffeine" % "3.1.0" % "compile").withDottyCompat(scalaVersion.value)
  )
