package com.scalafx.fractal.model.AST

case class UDF(symbol: String,
							fun: List[Double] => Double)
