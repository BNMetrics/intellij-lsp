package org.jetbrains.plugins.scala.lang.psi.api.expr

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition

/** 
* @author Alexander Podkhalyuzin
* Date: 06.03.2008
*/

trait ScReturnStmt extends ScExpression {
  def expr: Option[ScExpression] = findChild(classOf[ScExpression])

  def returnKeyword: PsiElement

  def returnFunction: Option[ScFunctionDefinition]

  override def accept(visitor: ScalaElementVisitor): Unit = visitor.visitReturnStatement(this)
}