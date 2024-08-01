package com.scalafx.fractal.model.AST

import scala.util.Random

object StandartASTComponents {

	lazy val plus: UDO = UDO("+", 1, Option(0d), (left, right) => left + right)
	lazy val minus: UDO = UDO("-", 1, Option(0d), (left, right) => left - right)
	lazy val multiply: UDO = UDO("*", 2, Option(1d), (left, right) => left * right)
	lazy val division: UDO = UDO("/", 2, None, (left, right) => left / right)
	lazy val pow: UDO = UDO("^", 3, None, (left, right) => Math.pow(left, right))

	lazy val pi: UDC = UDC("pi", Math.PI)
	lazy val e: UDC = UDC("e", Math.E)

	val avg: UDF = UDF("avg", params => params.sum / params.size)
	val abs: UDF = UDF("abs", params => params.head.abs)
	val cos: UDF = UDF("cos", params => Math.cos(params.head))
	val sin: UDF = UDF("sin", params => Math.sin(params.head))
	val ln: UDF = UDF("ln", params => Math.log(params.head))
	val max: UDF = UDF("max", params => params.max)
	val min: UDF = UDF("min", params => params.min)
	val ran: UDF = UDF("ran", params => Random.nextDouble())

	lazy val standartUDOs: Set[UDO] = Set(plus, minus, multiply, division, pow)
	lazy val standartUDCs: Set[UDC] = Set(pi, e)
	lazy val standartUDFs: Set[UDF] = Set(avg, abs, cos, sin, ln, max, min, ran)

}
