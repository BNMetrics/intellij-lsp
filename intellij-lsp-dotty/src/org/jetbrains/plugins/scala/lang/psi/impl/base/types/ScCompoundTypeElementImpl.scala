package org.jetbrains.plugins.scala.lang.psi.impl.base.types

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElementImpl
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.base.types._
import org.jetbrains.plugins.scala.lang.psi.types.ScCompoundType
import org.jetbrains.plugins.scala.lang.psi.types.result._

/**
 * @author Alexander Podkhalyuzin
 */

class ScCompoundTypeElementImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScCompoundTypeElement {
  protected def innerType: TypeResult = {
    val componentsTypes = components.map(_.`type`().getOrAny)
    val compoundType = refinement.map { r =>
      ScCompoundType.fromPsi(componentsTypes, r.holders, r.types)
    }.getOrElse(new ScCompoundType(componentsTypes))

    Right(compoundType)
  }

  override def accept(visitor: ScalaElementVisitor) {
    visitor.visitCompoundTypeElement(this)
  }

  override def accept(visitor: PsiElementVisitor) {
    visitor match {
      case s: ScalaElementVisitor => accept(s)
      case _ => super.accept(visitor)
    }
  }
}