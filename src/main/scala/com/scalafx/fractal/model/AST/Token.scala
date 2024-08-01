package com.scalafx.fractal.model.AST

sealed trait Token
case class NumberToken(value: Double) extends Token
case class UDOToken(udo: UDO) extends Token
case class UDFToken(udf: UDF) extends Token
case class UDCToken(udc: UDC) extends Token
case class LeftParenthesisToken() extends Token
case class RightParenthesisToken() extends Token
case class CommaToken() extends Token