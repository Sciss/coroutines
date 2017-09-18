import java.io.{File, FileInputStream}

lazy val scalaTestVersion               = "3.0.4"
lazy val scalaAsyncVersion              = "0.9.7"
lazy val scalaParserCombinatorsVersion  = "1.0.6"
lazy val scalaMeterVersion              = "0.9-SNAPSHOT"

lazy val projectName                    = "coroutines"
lazy val projectVersion                 = "0.8-SNAPSHOT"
def repoName                            = projectName

def versionFromFile(file: File, labels: List[String]): String = {
  val fis   = new FileInputStream(file)
  val props = new java.util.Properties()
  try props.load(fis)
  finally fis.close()
  labels.map(label => Option(props.getProperty(label)).get).mkString(".")
}

lazy val coroutinesCrossScalaVersions = Def.setting {
  val dir  = (baseDirectory in coroutines).value
  val path = dir / "cross.conf"
  scala.io.Source.fromFile(path).getLines.filter(_.trim != "").toSeq
}

lazy val coroutinesScalaVersion = Def.setting {
  coroutinesCrossScalaVersions.value.head
}

lazy val coroutinesCommonSettings = Seq(
  organization          := "com.storm-enroute",
  version               := projectVersion,
  scalaVersion          := "2.12.3",
  crossScalaVersions    := Seq("2.12.3", "2.11.11"),
  resolvers             += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  libraryDependencies  ++= Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators"  % scalaParserCombinatorsVersion,
    "org.scala-lang"         %  "scala-reflect"             % scalaVersion.value,
    "org.scalatest"          %% "scalatest"                 % scalaTestVersion  % "test",
    "com.storm-enroute"      %% "scalameter"                % scalaMeterVersion % "test"
  ),
  testFrameworks        += new TestFramework("org.scalameter.ScalaMeterFramework"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-optimise"
  ),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11))  => Seq("-Yinline-warnings")
      case _              => Nil
    }
  },
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org"
    if (isSnapshot.value)
      Some("snapshots" at s"$nexus/content/repositories/snapshots")
    else
      Some("releases"  at s"$nexus/service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra :=
    <url>http://storm-enroute.com/</url>
    <licenses>
      <license>
        <name>BSD-style</name>
        <url>http://opensource.org/licenses/BSD-3-Clause</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:storm-enroute/{repoName}.git</url>
      <connection>scm:git:git@github.com:storm-enroute/{repoName}.git</connection>
    </scm>
    <developers>
      <developer>
        <id>axel22</id>
        <name>Aleksandar Prokopec</name>
        <url>http://axel22.github.com/</url>
      </developer>
    </developers>
)

lazy val Benchmarks = config("bench").extend(Test)

lazy val coroutines: Project = Project(id = projectName, base = file("."))
  .aggregate(coroutinesCommon)
  .dependsOn(coroutinesCommon % "compile->compile;test->test")
  .settings(coroutinesCommonSettings)
  .configs(Benchmarks)
  .settings(inConfig(Benchmarks)(Defaults.testSettings): _*)
  .settings(
    name := projectName,
    libraryDependencies ++= Seq(
      "com.storm-enroute"       %% "scalameter"   % scalaMeterVersion % "bench",
      "org.scala-lang.modules"  %% "scala-async"  % scalaAsyncVersion % "bench"
    )
  )

lazy val coroutinesCommon: Project = Project(id = s"$projectName-common", base = file(s"$projectName-common"))
  .settings(coroutinesCommonSettings)
  .settings(
    name := s"$projectName-common",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-async" % scalaAsyncVersion % "test"
    )
  )

lazy val coroutinesExtra: Project = Project(id = s"$projectName-extra", base = file(s"$projectName-extra"))
  .dependsOn(coroutines % "compile->compile;test->test")
  .settings(coroutinesCommonSettings)
  .settings(
    name := s"$projectName-extra"
  )