package org.jetbrains.plugins.scala.lang.psi.impl.expr

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.types.result._

/**
* @author Alexander Podkhalyuzin
* Date: 07.03.2008
* Time: 9:24:19
*/

class ScParenthesisedExprImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScParenthesisedExpr {
  override def toString: String = "ExpressionInParenthesis"

  protected override def innerType: TypeResult = {
    expr match {
      case Some(x: ScExpression) =>
        val res = x.getNonValueType()
        res
      case _ => Failure("No expression in parentheseses")
    }
  }

  override def accept(visitor: ScalaElementVisitor) {
    visitor.visitExprInParent(this)
  }

  override def accept(visitor: PsiElementVisitor) {
    visitor match {
      case visitor: ScalaElementVisitor => visitor.visitExprInParent(this)
      case _ => super.accept(visitor)
    }
  }

}