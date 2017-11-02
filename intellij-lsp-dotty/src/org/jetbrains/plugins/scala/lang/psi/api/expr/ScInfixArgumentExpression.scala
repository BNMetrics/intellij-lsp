package org.jetbrains.plugins.scala.lang.psi.api.expr

/**
 * @author Alefas
 * @since 25/03/14.
 */
trait ScInfixArgumentExpression extends ScExpression {
  /**
   * Return true if this expression is arguments for method invocation
   */
  def isCall: Boolean = {
    getContext match {
      case infix: ScInfixExpr => infix.getArgExpr == this
      case _ => false
    }
  }
}
