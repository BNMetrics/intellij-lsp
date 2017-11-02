package org.jetbrains.plugins.scala.lang.psi.api.base.types

/**
* @author Alexander Podkhalyuzin
* Date: 13.03.2008
*/

trait ScParenthesisedTypeElement extends ScTypeElement {
  override protected val typeName = "TypeInParenthesis"

  def typeElement: Option[ScTypeElement] = findChild(classOf[ScTypeElement])
}

object ScParenthesisedTypeElement {
  def unapply(e: ScParenthesisedTypeElement): Option[ScTypeElement] = e.typeElement
}