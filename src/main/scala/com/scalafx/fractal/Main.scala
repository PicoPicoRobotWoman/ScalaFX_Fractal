package com.scalafx.fractal

import scalafx.application.JFXApp3

object Main extends JFXApp3 with MainWindowConfigure {

  override def start(): Unit = {
    stage = mainWindow
  }

}