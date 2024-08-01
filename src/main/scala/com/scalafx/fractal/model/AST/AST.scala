package com.scalafx.fractal.model.AST

import scala.util.Try

object AST {

	implicit class Parser(expression: String)(implicit udfs: Set[UDF] = Set.empty, udos: Set[UDO] = Set.empty, udcs: Set[UDC] = Set.empty) {

		def parseAST(): AST = {

			val sep: Char = '|'
			val spewSymbols: Set[String] = Set("(", ")", ",")
			val expr = expression.trim.replaceAll(" ", "")

			val udoSymbols = udos.map(_.symbol)
			val udfSymbols = udfs.map(_.symbol)
			val udcSymbols = udcs.map(_.symbol)

			val updExpr = udos
				.filter(_.zeroElem.nonEmpty)
				.map(op => (op.symbol, op.zeroElem.get))
				.foldLeft(expr) {
					case (acc, (sym, zer)) =>
						val updatedExpr = acc.replaceAllLiterally(s"($sym", s"($zer$sym")
						if (updatedExpr.startsWith(sym)) s"$zer$updatedExpr" else updatedExpr
				}

			val parsedTokens: List[Token] = udoSymbols
				.union(spewSymbols)
				.foldLeft(updExpr) {
					case (acc, sym) =>
						acc.replaceAllLiterally(sym, f"$sep$sym$sep")
				}
				.replaceAllLiterally(f"$sep$sep", f"$sep")
				.stripPrefix(sep.toString)
				.stripSuffix(sep.toString)
				.split(sep)
				.filter(_.nonEmpty)
				.toList
				.map {
					case str if Try(str.toDouble).isSuccess => NumberToken(str.toDouble)
					case str if udoSymbols.contains(str) => UDOToken(udos.find(_.symbol == str).get)
					case str if udfSymbols.contains(str) => UDFToken(udfs.find(_.symbol == str).get)
					case str if udcSymbols.contains(str) => UDCToken(udcs.find(_.symbol == str).get)
					case str if str == "(" => LeftParenthesisToken()
					case str if str == ")" => RightParenthesisToken()
					case str if str == "," => CommaToken()
					case str => throw new Exception(f"$str: нераспознаный токен")
				}
				.filter(_ != null)

			val resolvedTokens: List[Token] = parsedTokens
				.map {
					case ct: UDCToken => NumberToken(ct.udc.value)
					case token: Token => token
				}

			flattenAST(resolvedTokens)
		}

	}

	private def flattenAST(sourceTokens: List[Token])(implicit udos: Set[UDO]): AST = {

		def stripSurroundingTokens(tokens: List[Token]): List[Token] = tokens match {
			case Nil => Nil
			case head :: tail if head.isInstanceOf[LeftParenthesisToken] && tail.nonEmpty && tail.last.isInstanceOf[RightParenthesisToken] && findNonEnclosedOperators(tokens, udos).isEmpty =>
				tail.init
			case _ => tokens
		}

		def findNonEnclosedOperators(tokens: List[Token], operators: Set[UDO]): Option[Int] = {
			val sortedOperators = operators.toList.sortBy(_.precedence) // Сортируем операторы по приоритету

			// Вспомогательная функция для проверки вложенности оператора
			def isEnclosed(index: Int, tokens: List[Token]): Boolean = {
				var level = 0
				for (i <- 0 until index) {
					tokens(i) match {
						case LeftParenthesisToken() => level += 1
						case RightParenthesisToken() => level -= 1
						case _ =>
					}
				}
				level != 0
			}

			// Находим первый невложенный оператор с наименьшим приоритетом
			val nonEnclosedOperators = tokens.zipWithIndex.collect {
				case (UDOToken(op), index) if !isEnclosed(index, tokens) =>
					val opPriority = sortedOperators.indexWhere(_.symbol == op.symbol)
					(opPriority, index)
			}

			// Возвращаем индекс оператора с наименьшим приоритетом
			nonEnclosedOperators.sortBy(_._1).headOption.map(_._2)
		}

		def splitByNonEnclosedCommas(tokens: List[Token]): List[List[Token]] = {
			def splitHelper(tokens: List[Token], level: Int = 0, acc: List[Token] = Nil, result: List[List[Token]] = Nil): List[List[Token]] = {
				tokens match {
					case Nil => (acc.reverse :: result).reverse
					case CommaToken() :: tail if level == 0 =>
						splitHelper(tail, level, Nil, acc.reverse :: result)
					case LeftParenthesisToken() :: tail =>
						splitHelper(tail, level + 1, LeftParenthesisToken() :: acc, result)
					case RightParenthesisToken() :: tail =>
						splitHelper(tail, level - 1, RightParenthesisToken() :: acc, result)
					case token :: tail =>
						splitHelper(tail, level, token :: acc, result)
				}
			}

			splitHelper(tokens)
		}

		def toAST(tokens: List[Token]): AST = {

			val curTokens: List[Token] = stripSurroundingTokens(tokens)

			curTokens match {
				case List(single: NumberToken) =>
					AST(
						single,
						Nil
					)
				case _ =>
					findNonEnclosedOperators(curTokens, udos) match {
						case Some(index) =>
							val (left, right) = curTokens.splitAt(index)
							AST(
								curTokens(index),
								List(
									toAST(left),
									toAST(right.tail)
								)
							)
						case None =>
							curTokens.headOption.collect {
									case udf: UDFToken =>
										val params: List[List[Token]] = splitByNonEnclosedCommas(stripSurroundingTokens(curTokens.tail))
										AST(udf, params.map(toAST))
								}.orNull
					}
			}

		}

		toAST(sourceTokens)
	}

	implicit class Calculator(sourceAst: AST) {
		def calc(): Double = {

			def ASTToDouble(ast: AST): Double = {
				ast match {
					case ast if ast.token.isInstanceOf[NumberToken] => ast.token.asInstanceOf[NumberToken].value
					case ast if ast.token.isInstanceOf[UDOToken] =>
						val fun = ast.token.asInstanceOf[UDOToken].udo.fun
						val params = ast.subASTs.map(ASTToDouble)
						fun(params.head, params.last)
					case ast if ast.token.isInstanceOf[UDFToken] =>
						val fun = ast.token.asInstanceOf[UDFToken].udf.fun
						val params = Try( ast.subASTs.map(ASTToDouble)).getOrElse(List.empty)
						fun(params)
				}
			}

			ASTToDouble(sourceAst)
		}
	}

}

case class AST(token: Token, subASTs: List[AST]) {
	/*
	override def toString: String = {
		def toStringHelper(node: AST, depth: Int): String = {
			val indent = "  " * depth
			val childrenStr = node.children.map(toStringHelper(_, depth + 1)).mkString("\n")
			s"$indent${node.token.toString}\n$childrenStr"
		}
		toStringHelper(this, 0)
	}
	*/

}
