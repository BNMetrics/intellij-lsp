package org.jetbrains.plugins.scala.lang.psi.api.expr

/**
* @author Alexander Podkhalyuzin
* Date: 06.03.2008
*/

trait ScTuple extends ScInfixArgumentExpression {
  def exprs : Seq[ScExpression] = findChildrenByClassScala(classOf[ScExpression]).toSeq
}

object ScTuple {
  def unapply(e: ScTuple): Some[Seq[ScExpression]] = Some(e.exprs)
}