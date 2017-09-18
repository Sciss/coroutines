import java.io.{File, FileInputStream}

val scalaTestVersion              = "3.0.4"
val scalaAsyncVersion             = "0.9.7"
val scalaParserCombinatorsVersion = "1.0.6"
val scalaMeterVersion             = "0.8.2"

val repoName                      = "coroutines"

def versionFromFile(file: File, labels: List[String]): String = {
  val fis = new FileInputStream(file)
  val props = new java.util.Properties()
  try props.load(fis)
  finally fis.close()
  labels.map(label => Option(props.getProperty(label)).get).mkString(".")
}

val frameworkVersion = Def.setting {
  versionFromFile(
    (baseDirectory in coroutines).value / "version.conf",
    List("coroutines_major", "coroutines_minor"))
}

val coroutinesCrossScalaVersions = Def.setting {
  val dir  = (baseDirectory in coroutines).value
  val path = dir + File.separator + "cross.conf"
  scala.io.Source.fromFile(path).getLines.filter(_.trim != "").toSeq
}

val coroutinesScalaVersion = Def.setting {
  coroutinesCrossScalaVersions.value.head
}

val coroutinesSettings = Seq(
  name                  := "coroutines",
  organization          := "com.storm-enroute",
  version               := frameworkVersion.value,
  scalaVersion          := coroutinesScalaVersion.value,
  crossScalaVersions    := coroutinesCrossScalaVersions.value,
  libraryDependencies  ++= dependencies(scalaVersion.value),
  // libraryDependencies  ++= superRepoDependencies("coroutines"),
  testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-optimise",
    "-Yinline-warnings"
  ),
  resolvers ++= Seq(
    "Sonatype OSS Snapshots" at
      "https://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype OSS Releases" at
      "https://oss.sonatype.org/content/repositories/releases"
  ),
  ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
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
      <url>git@github.com:storm-enroute/coroutines.git</url>
      <connection>scm:git:git@github.com:storm-enroute/coroutines.git</connection>
    </scm>
    <developers>
      <developer>
        <id>axel22</id>
        <name>Aleksandar Prokopec</name>
        <url>http://axel22.github.com/</url>
      </developer>
    </developers>
)

def dependencies(scalaVersion: String) =
  CrossVersion.partialVersion(scalaVersion) match {
  case Some((2, major)) if major >= 11 => Seq(
    "org.scalatest"          %% "scalatest"                % scalaTestVersion  % "test",
    "com.storm-enroute"      %% "scalameter-core"          % scalaMeterVersion % "test;bench",
    "org.scala-lang.modules" %% "scala-parser-combinators" % scalaParserCombinatorsVersion,
    "org.scala-lang"         %  "scala-reflect"            % scalaVersion,
    "org.scala-lang.modules" %% "scala-async"              % scalaAsyncVersion % "test;bench"
  )
  case _ => Nil
}

val coroutinesCommonSettings = Seq(
  name                  := "coroutines-common",
  organization          := "com.storm-enroute",
  version               := frameworkVersion.value,
  scalaVersion          := coroutinesScalaVersion.value,
  crossScalaVersions    := coroutinesCrossScalaVersions.value,
  libraryDependencies  ++= commonDependencies(scalaVersion.value),
  // libraryDependencies  ++= superRepoDependencies("coroutines-common"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-optimise",
    "-Yinline-warnings"
  ),
  resolvers ++= Seq(
    "Sonatype OSS Snapshots" at
      "https://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype OSS Releases" at
      "https://oss.sonatype.org/content/repositories/releases"
  ),
  ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
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
      <url>git@github.com:storm-enroute/coroutines.git</url>
      <connection>scm:git:git@github.com:storm-enroute/coroutines.git</connection>
    </scm>
    <developers>
      <developer>
        <id>axel22</id>
        <name>Aleksandar Prokopec</name>
        <url>http://axel22.github.com/</url>
      </developer>
    </developers>
)

val coroutinesExtraSettings = Seq(
  name                  := "coroutines-extra",
  organization          := "com.storm-enroute",
  version               := frameworkVersion.value,
  scalaVersion          := coroutinesScalaVersion.value,
  crossScalaVersions    := coroutinesCrossScalaVersions.value,
  libraryDependencies  ++= extraDependencies(scalaVersion.value),
  testFrameworks        += new TestFramework("org.scalameter.ScalaMeterFramework"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-optimise",
    "-Yinline-warnings"
  ),
  resolvers ++= Seq(
    "Sonatype OSS Snapshots" at
      "https://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype OSS Releases" at
      "https://oss.sonatype.org/content/repositories/releases"
  ),
  ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
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
      <url>git@github.com:storm-enroute/coroutines.git</url>
      <connection>scm:git:git@github.com:storm-enroute/coroutines.git</connection>
    </scm>
    <developers>
      <developer>
        <id>axel22</id>
        <name>Aleksandar Prokopec</name>
        <url>http://axel22.github.com/</url>
      </developer>
    </developers>
)

def commonDependencies(scalaVersion: String) =
  CrossVersion.partialVersion(scalaVersion) match {
  case Some((2, major)) if major >= 11 => Seq(
    "org.scalatest"          %% "scalatest"                % scalaTestVersion % "test",
    "org.scala-lang.modules" %% "scala-parser-combinators" % scalaParserCombinatorsVersion,
    "org.scala-lang"         %  "scala-reflect"            % scalaVersion
  )
  case _ => Nil
}

def extraDependencies(scalaVersion: String) =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, major)) if major >= 11 => Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
    )
    case _ => Nil
  }

lazy val Benchmarks = config("bench") .extend (Test)

lazy val coroutines: Project = Project(
  "coroutines",
  file("."),
  settings = coroutinesSettings
) .configs(
  Benchmarks
) .settings(
  inConfig(Benchmarks)(Defaults.testSettings): _*
) .aggregate(
  coroutinesCommon
) .dependsOn(
  coroutinesCommon % "compile->compile;test->test"
)

lazy val coroutinesCommon: Project = Project(
  "coroutines-common",
  file("coroutines-common"),
  settings = coroutinesCommonSettings
)

lazy val coroutinesExtra: Project = Project(
  "coroutines-extra",
  file("coroutines-extra"),
  settings = coroutinesExtraSettings
) .dependsOn(
  coroutines % "compile->compile;test->test"
)