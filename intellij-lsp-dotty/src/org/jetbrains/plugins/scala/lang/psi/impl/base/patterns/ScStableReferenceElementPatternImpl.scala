package org.jetbrains.plugins.scala.lang.psi.impl.base.patterns

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScStableReferenceElementPattern
import org.jetbrains.plugins.scala.lang.psi.types.result.TypeResult

/**
 * @author ilyas
 */

class ScStableReferenceElementPatternImpl(node : ASTNode) extends ScalaPsiElementImpl(node) with ScStableReferenceElementPattern {
  override def accept(visitor: PsiElementVisitor): Unit = {
    visitor match {
      case visitor: ScalaElementVisitor => super.accept(visitor)
      case _ => super.accept(visitor)
    }
  }

  override def toString: String = "StableElementPattern"

  override def `type`(): TypeResult = this.flatMapType(getReferenceExpression)
}