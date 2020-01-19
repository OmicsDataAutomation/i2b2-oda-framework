package com.oda.i2b2.variantview

import scala.util.parsing.combinator._
import edu.harvard.i2b2.common.exception.I2B2Exception

object Kind extends Enumeration {
  type Kind = Value
  val HOMREF, HET, HOMVAR, NOCALL, UNKNOWN = Value

  def toKind(s: String) = s.toUpperCase match {
    case "HOMREF" => HOMREF
    case "HET" => HET
    case "HOMVAR" => HOMVAR
    case "NOCALL"|"NA" => NOCALL
    case _ => throw new I2B2Exception("Invalid genotype selection")
  }
}

/** 
  * Condition represents conditional expressions of the form 
  *    "rs12345 is HOMOREF"
  */
private class Condition(val name: String, val op:String, val value: String) {
  var id: Int = 0
}

object ConditionParser extends RegexParsers {
  private val random = new scala.util.Random(System.currentTimeMillis)

  private def or_conditions: Parser[List[(String, String, Kind.Kind, Int)]] = repsep(and_conditions, or) ^^ {
    case cond_list_list =>
      (for(cond_list <- cond_list_list; cond <- cond_list) yield (cond.name, cond.op, Kind.toKind(cond.value), cond.id)).toList
      
  }

  private def and_conditions: Parser[List[Condition]] = repsep(condition, and) ^^ {
    case cond_list =>
      val id: Int = random.nextInt
      cond_list.foreach { _.id = id }
      cond_list
  }

  private def condition: Parser[Condition] = id ~ (is_not|is) ~ kind ^^ {
    case id ~ op ~ kind =>
      new Condition(id, op.toLowerCase, kind)
  }

  private def is_not: Parser[String] = "IS NOT"|"is not"|"Is Not" 

  private def is: Parser[String] = "IS"|"is"|"Is" 

  private def or: Parser[String] = "OR"|"or"|"Or"

  private def and: Parser[String] = "AND"|"and"|"And"

  private def kind: Parser[String] = """[a-zA-Z]+""".r ^^ { _.toString  }

  private def id: Parser[String] = """rs[0-9]+""".r ^^ { _.toString }

  /**
    * Parse strings of the form
    * "rs12345 is HOMOREF and rs23456 is not NOCALL or rs34567 is het and rs34567 is not homoref"
    * Associativity of the operators is from left to right with the following precedence is descending order - 
    *    is, is not
    *    and
    *    or
    * Brackets are not supported.
    */
  def parse(input: String): List[(String, String, Kind.Kind, Int)] = parseAll(or_conditions, input) match {
    case Success(result, _) => result
    case failure : NoSuccess => throw new I2B2Exception(failure.msg)
   }
}

/** Test
def main(args: Array[String]) {
  val result = ConditionParser.parse("rs123 is homref and rs234 is not het or rs234 IS nocall")
  println(result)
}
*/

