package org.jetbrains.plugins.scala.lang.psi.api.expr.xml

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression

/**
* @author Alexander Podkhalyuzin
* Date: 21.04.2008
*/

trait ScXmlExpr extends ScExpression {
  def getElements: Seq[PsiElement] = getChildren.filter(_ match {
    case _: ScXmlElement | _: ScXmlPI | _: ScXmlCDSect | _: ScXmlComment  => true
    case _ => false
  })
}