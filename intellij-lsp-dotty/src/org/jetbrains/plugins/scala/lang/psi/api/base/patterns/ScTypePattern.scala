package org.jetbrains.plugins.scala.lang.psi.api.base.patterns

import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement

/** 
* @author Alexander Podkhalyuzin
* Date: 28.02.2008
*/

trait ScTypePattern extends ScalaPsiElement {
  def typeElement: ScTypeElement = findChildByClassScala(classOf[ScTypeElement])
}