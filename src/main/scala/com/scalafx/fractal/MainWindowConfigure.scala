package com.scalafx.fractal

import com.scalafx.fractal.Main.stage
import javafx.scene.Parent
import scalafx.Includes._
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafxml.core.{FXMLView, NoDependencyResolver}

import java.io.IOException
import java.net.URL

trait MainWindowConfigure {

	lazy val mainWindow: JFXApp3.PrimaryStage = new JFXApp3.PrimaryStage {

		val resource: URL = getClass.getResource("/FXMLs/main_window.fxml")
		if (resource == null) {
			throw new IOException(f"Cannot load resource: ${resource.toURI}")
		}

		val root: Parent = FXMLView(resource, NoDependencyResolver)

		root.stylesheets += getClass.getResource("/styles.css").toExternalForm

		title = "ScalaFX"
		scene = new Scene(root, 600, 450)

	}

}
