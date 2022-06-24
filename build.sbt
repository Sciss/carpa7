lazy val baseName       = "carpa7"
lazy val baseNameL      = baseName.toLowerCase
lazy val projectVersion = "0.1.0-SNAPSHOT"

lazy val gitHost        = "codeberg.org"
lazy val gitUser        = "sciss"
lazy val gitRepo        = baseName

lazy val root = project.in(file("."))
  .settings(
    name         := baseName,
    description  := "Materials for a colloquium",
    version      := projectVersion,
    homepage     := Some(url(s"https://$gitHost/$gitUser/$gitRepo")),
    licenses     := Seq("AGPL v3+" -> url("http://www.gnu.org/licenses/agpl-3.0.txt")),
    scalaVersion := "2.13.8",
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8"),
    // resolvers    += "imagej.releases" at "https://maven.scijava.org/content/repositories/releases/",
    libraryDependencies ++= Seq(
      "de.sciss"            %% "fileutil"             % deps.main.fileUtil,     // utility functions
      "de.sciss"            %% "numbers"              % deps.main.numbers,      // numeric utilities
      "org.rogach"          %% "scallop"              % deps.main.scallop,      // command line option parsing
    ),
  )

lazy val deps = new {
  lazy val main = new {
    val fileUtil    = "1.1.5"
//    val imageJ      = "1.53j" // "1.47h"
    val numbers     = "0.2.1"
    val scallop     = "4.1.0"
  }
}

