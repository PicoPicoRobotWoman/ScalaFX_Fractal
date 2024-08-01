package com.scalafx.fractal.controller

import com.scalafx.fractal.model.AST.AST._
import com.scalafx.fractal.model.AST._
import com.scalafx.fractal.model.DTO.{AttractorSettingsPanelDTO, PointSettingsDTO}
import scalafx.scene.control.{ScrollPane, TextField}
import scalafx.scene.layout.{Pane, VBox}
import scalafx.scene.paint.Color
import scalafx.scene.shape.Circle
import scalafxml.core.{FXMLView, NoDependencyResolver}
import scalafxml.core.macros.sfxml

import java.io.IOException
import java.net.URL
import scala.util.{Random, Try}

@sfxml
class MainWindowController(private val settingsVbox: VBox,
													 private val drawCNS: Pane,
													 private val drawSP: ScrollPane,
													 private val divisionCoefficientTF: TextField,
													 private val radiusTF: TextField,
													 private val hexTF: TextField,
													 private val iterTF: TextField) {

	lazy val resource: URL = getClass.getResource("/FXMLs/attractor_settings_panel.fxml")
	if (resource == null) {
		throw new IOException(f"Cannot load resource: ${resource.toURI}")
	}

	def handleAddAction(): Unit = {

		val settingPanel = FXMLView(resource, NoDependencyResolver)

		settingsVbox.children.add(settingsVbox.children.size - 1, settingPanel)

	}

	def handleremoveAction(): Unit = {
		if (settingsVbox.children.size > 1)
			settingsVbox.children.remove(settingsVbox.children.size - 2)
	}

	def rendering(): Unit = {

		drawCNS.children.clear()

		val centerX: Double = drawSP.getWidth / 2
		val centerY: Double = drawSP.getHeight / 2

		val pointSettingsDTO = PointSettingsDTO(
			divisionCoefficient = Try( divisionCoefficientTF.getText.toInt ).getOrElse(1),
			radius = Try( radiusTF.getText.toInt ).getOrElse(1),
			hex = validHexColor(hexTF.getText),
			iterCount = Try( iterTF.getText.toInt ).getOrElse(10000)
		)

		val attractorPoints = settingsVbox
			.children
			.init
			.map(validAttractorDTO)
			.filter(_.isSuccess)
			.map(_.get)
			.filter(_.formulaY.nonEmpty)
			.filter(_.formulaX.nonEmpty)
			.flatMap {
				attractor =>
					val coordinates = (0 until attractor.iterCount)
						.map {
							i =>
								Try {

									implicit val udcs: Set[UDC] = StandartASTComponents.standartUDCs + UDC("i", i) + UDC("ic", attractor.iterCount)
									implicit val udfs: Set[UDF] = StandartASTComponents.standartUDFs
									implicit val udos: Set[UDO] = StandartASTComponents.standartUDOs

									val x = attractor.formulaX.parseAST().calc()
									val y = attractor.formulaY.parseAST().calc()

									((x, y), attractor)
								}
						}

					if (!coordinates.exists(_.isFailure)) {
						coordinates
							.map(_.get)
					}
					else {
						List.empty
					}

			}

			attractorPoints
				.foreach {
					case ((x, y), attractor) =>

						val circeCenterX = centerX + x
						val circeCenterY = centerY + y
						val circeColor = Color.web(attractor.hex)

						val circle = new Circle {
							centerX = circeCenterX
							centerY = circeCenterY
							radius = attractor.radius
							fill = circeColor
						}

						drawCNS.children.add(circle)
				}

		val totalAttractorWeight = attractorPoints.map(_._2.probabilityWeight).sum

		val fractalPoints: List[(Double, Double)] = Try {
			(0 until pointSettingsDTO.iterCount)
				.foldLeft(List.empty[(Double, Double)]) {
					case (Nil, _) =>
						List((Random.nextDouble() * centerX * 2, Random.nextDouble() * centerY * 2))
					case (prePoints, _) =>
						val lastPoint = prePoints.last

						val randomValue = Random.nextDouble() * totalAttractorWeight
						val randomAttractPoint: ((Double, Double), AttractorSettingsPanelDTO) = attractorPoints
							.foldLeft((0.0, Option.empty[((Double, Double), AttractorSettingsPanelDTO)])) {

								case ((cumulativeWeight, selectedPoint), point) =>
									val newCumulativeWeight = cumulativeWeight + point._2.probabilityWeight
									if (randomValue <= newCumulativeWeight && selectedPoint.isEmpty) {
										(newCumulativeWeight, Some(point))
									} else {
										(newCumulativeWeight, selectedPoint)
									}
							}
							._2
							.get

						val totalCoefficient = pointSettingsDTO.divisionCoefficient + randomAttractPoint._2.divisionCoefficient
						val newPoint = (
							(lastPoint._1 * pointSettingsDTO.divisionCoefficient + randomAttractPoint._1._1 * randomAttractPoint._2.divisionCoefficient) / totalCoefficient,
							(lastPoint._2 * pointSettingsDTO.divisionCoefficient + randomAttractPoint._1._2 * randomAttractPoint._2.divisionCoefficient) / totalCoefficient
						)

						prePoints :+ newPoint
				}
		}
			.getOrElse(List.empty)

		fractalPoints
			.foreach {
				case (x, y) =>

					val circeCenterX = centerX + x
					val circeCenterY = centerY + y
					val circeColor = Color.web(pointSettingsDTO.hex)

					val circle = new Circle {
						centerX = circeCenterX
						centerY = circeCenterY
						radius = pointSettingsDTO.radius
						fill = circeColor
					}

					drawCNS.children.add(circle)
			}

	}

	private def validHexColor(color: String): String= {
		Try {

			val hexColorPattern = "^#([A-Fa-f0-9]{6})$".r
			if (hexColorPattern.matches(color))
				color
			else
				"#000000"

		}.getOrElse("#000000")
	}

	private def validAttractorDTO(node: javafx.scene.Node): Try[AttractorSettingsPanelDTO] = {

		Try {

			AttractorSettingsPanelDTO(
				formulaX = node.lookup("#formulaTFX").asInstanceOf[javafx.scene.control.TextField].getText,
				formulaY = node.lookup("#formulaTFY").asInstanceOf[javafx.scene.control.TextField].getText,
				probabilityWeight = Try(node.lookup("#probabilityWeightTF").asInstanceOf[javafx.scene.control.TextField].getText.toInt).getOrElse(1),
				divisionCoefficient = Try(node.lookup("#divisionCoefficientTF").asInstanceOf[javafx.scene.control.TextField].getText.toInt).getOrElse(1),
				radius = Try(node.lookup("#radiusTF").asInstanceOf[javafx.scene.control.TextField].getText.toInt).getOrElse(1),
				hex = validHexColor(node.lookup("#hexTF").asInstanceOf[javafx.scene.control.TextField].getText),
				iterCount = Try(node.lookup("#iterTF").asInstanceOf[javafx.scene.control.TextField].getText.toInt).getOrElse(1)
			)

		}

	}

}
