lazy val baseName       = "carpa7"
lazy val baseNameL      = baseName.toLowerCase
lazy val projectVersion = "0.1.0-SNAPSHOT"

lazy val root = project.in(file("."))
  .settings(
    name         := baseName,
    description  := "Materials for a colloquium",
    version      := projectVersion,
    homepage     := Some(url(s"https://github.com/Sciss/$baseName")),
    licenses     := Seq("AGPL v3+" -> url("http://www.gnu.org/licenses/agpl-3.0.txt")),
    scalaVersion := "2.13.6",
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8"),
    // resolvers    += "imagej.releases" at "https://maven.scijava.org/content/repositories/releases/",
    libraryDependencies ++= Seq(
      "de.sciss"            %% "fileutil"             % deps.main.fileUtil,     // utility functions
      "de.sciss"            %% "numbers"              % deps.main.numbers,      // numeric utilities
      // "mpicbg"              %  "mpicbg"               % deps.main.mpicbg,       // 2D transforms
      // "net.harawata"        %  "appdirs"              % deps.main.appDirs,      // finding standard directories
      // "net.imagej"          %  "ij"                   % deps.main.imageJ,       // analyzing image data
      // "org.unbescape"       % "unbescape"             % deps.main.unbescape,    // decode HTML entities
      "org.rogach"          %% "scallop"              % deps.main.scallop,      // command line option parsing
      // "org.scala-lang.modules" %% "scala-swing"       % deps.main.scalaSwing,   // UI
    ),
  )

lazy val deps = new {
  lazy val main = new {
    val appDirs     = "1.2.1"
    val fileUtil    = "1.1.5"
    val imageJ      = "1.53j" // "1.47h"
    val mpicbg      = "1.4.1"
    val numbers     = "0.2.1"
    val pi4j        = "1.4"
    val scaladon    = "0.5.0"
    val scalaSwing  = "3.0.0"
    val scallop     = "4.0.3"
    val serial      = "2.0.1"
    val unbescape   = "1.1.6.RELEASE"
  }
}

