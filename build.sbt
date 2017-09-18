lazy val projectName                    = "coroutines"

//lazy val projectVersion                 = "0.8-SNAPSHOT"
//lazy val projectOrg                     = "com.storm-enroute"

// we change these to publish other artifact
lazy val projectVersion                 = "0.1.0"
lazy val projectOrg                     = "de.sciss"

def repoName                            = projectName

// ---- main dependencies ----

lazy val scalaParserCombinatorsVersion  = "1.0.6"

// ---- test dependencies ----

lazy val scalaTestVersion               = "3.0.4"
lazy val scalaAsyncVersion              = "0.9.7"
lazy val scalaMeterVersion              = "0.9-SNAPSHOT"

// ---- settings ----

lazy val coroutinesCommonSettings = Seq(
  organization          := projectOrg,
  version               := projectVersion,
  scalaVersion          := "2.12.3",
  crossScalaVersions    := Seq("2.12.3", "2.11.11"),
  resolvers             += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  libraryDependencies  ++= Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators"  % scalaParserCombinatorsVersion % "provided",
    "org.scala-lang"         %  "scala-reflect"             % scalaVersion.value            % "provided",
    "org.scalatest"          %% "scalatest"                 % scalaTestVersion              % "test",
    "com.storm-enroute"      %% "scalameter"                % scalaMeterVersion             % "test"
  ),
  testFrameworks        += new TestFramework("org.scalameter.ScalaMeterFramework"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-feature",
    "-encoding", "utf8",
    "-Xlint",
    "-Xfuture"
  ),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11))  => Seq("-Yinline-warnings", "-optimise")
      case _              => Seq("-opt:l:inline", "-opt-inline-from:**")
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

// ---- modules ----

lazy val coroutines: Project = Project(id = projectName, base = file("."))
  .aggregate(coroutinesCommon)
  .dependsOn(coroutinesCommon % "compile->compile;test->test")
  .settings(coroutinesCommonSettings)
  .configs(Benchmarks)
  .settings(inConfig(Benchmarks)(Defaults.testSettings): _*)
  .settings(
    name := projectName,
    libraryDependencies ++= Seq(
      "com.storm-enroute"       %% "scalameter"   % scalaMeterVersion % "test;bench",
      "org.scala-lang.modules"  %% "scala-async"  % scalaAsyncVersion % "test;bench"
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
