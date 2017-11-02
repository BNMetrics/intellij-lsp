package org.jetbrains.plugins.scala.lang.psi.api.statements.params

import com.intellij.psi._
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement

/**
* @author Alexander Podkhalyuzin
* Date: 21.03.2008
*/

trait ScParameters extends ScalaPsiElement with PsiParameterList {

  def params: Seq[ScParameter] = clauses.flatMap((clause: ScParameterClause) => clause.parameters)

  def clauses: Seq[ScParameterClause]

  def addClause(clause: ScParameterClause): ScParameters = {
    getNode.addChild(clause.getNode)
    this
  }

  def getParameterIndex(p: PsiParameter): Int = params.indexOf(List(p))

  def getParametersCount: Int = params.length

  override def getParameters: Array[PsiParameter] = params.toArray
}