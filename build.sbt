
name := "ScalaFX_Fractal"
version := "1.0.0"


scalaVersion := "2.13.12"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-encoding", "utf8", "-Ymacro-annotations")

lazy val scalaFxVersion = "16.0.0-R24"
lazy val scalaFxCoreVersion = "0.5"

lazy val scalaFxDependencies = Seq(
	"org.scalafx" %% "scalafx" % scalaFxVersion,
	"org.scalafx" %% "scalafxml-core-sfx8" % scalaFxCoreVersion
)

lazy val javaFxDependencies =	{
	lazy val osName = System.getProperty("os.name") match {
		case n if n.startsWith("Linux") => "linux"
		case n if n.startsWith("Mac") => "mac"
		case n if n.startsWith("Windows") => "win"
		case _ => throw new Exception("Unknown platform!")
	}
	Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
		.map(m => "org.openjfx" % s"javafx-$m" % "16" classifier osName)
}

libraryDependencies ++= scalaFxDependencies ++ javaFxDependencies

mainClass in assembly := Some("com.scalafx.fractal.Main")

assemblyMergeStrategy in assembly := {
	case x if x.endsWith("module-info.class") => MergeStrategy.discard
	case x if x.startsWith("META-INF/services/") => MergeStrategy.filterDistinctLines
	case x if x.startsWith("META-INF/") => MergeStrategy.discard
	case x if x.endsWith(".html") => MergeStrategy.discard
	case x => MergeStrategy.first
}

assemblyJarName in assembly := f"${name.value}.jar"
