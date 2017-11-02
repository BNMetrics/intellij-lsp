package org.jetbrains.plugins.scala.lang.psi.api.expr

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.lang.psi.api.ScControlFlowOwner
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns.ScCaseClauses
import org.jetbrains.plugins.scala.lang.psi.controlFlow.{Instruction, ScControlFlowPolicy}

/**
* @author Alexander Podkhalyuzin
* Date: 06.03.2008
*/

trait ScBlockExpr extends ScExpression with ScBlock with ScControlFlowOwner {
  def asSimpleExpression: Option[ScExpression] = Some(exprs) collect {
    case Seq(it) if !it.isInstanceOf[ScBlockExpr] => it
  }

  def caseClauses: Option[ScCaseClauses] = findChild(classOf[ScCaseClauses])

  override def getControlFlow(policy: ScControlFlowPolicy): Seq[Instruction] = {
    if (isAnonymousFunction) super.getControlFlow(policy)
    else {
      val parent = PsiTreeUtil.getParentOfType(this, classOf[ScControlFlowOwner])
      parent.getControlFlow(policy)
    }
  }

  override def controlFlowScope: Option[ScCaseClauses] = if (isAnonymousFunction) caseClauses else None
}

object ScBlockExpr {
  object Expressions {
    def unapplySeq(e: ScBlockExpr): Some[Seq[ScExpression]] = Some(e.exprs)
  }
}