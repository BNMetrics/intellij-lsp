package org.jetbrains.plugins.scala.lang.psi.api.base.types

import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReferenceElement

/**
* @author Alexander Podkhalyuzin
* Date: 22.02.2008
*/

trait ScInfixTypeElement extends ScTypeElement {
  def leftTypeElement: ScTypeElement = findChildByClassScala(classOf[ScTypeElement])

  def rightTypeElement: Option[ScTypeElement] = findChildrenByClassScala(classOf[ScTypeElement]) match {
    case Array(_, right) => Some(right)
    case _ => None
  }
}

trait ScReferenceableInfixTypeElement extends ScInfixTypeElement with ScDesugarizableToParametrizedTypeElement {
  override protected val typeName = "InfixType"

  def reference: ScStableCodeReferenceElement = findChildByClassScala(classOf[ScStableCodeReferenceElement])

  override def desugarizedText = s"${reference.getText}[${leftTypeElement.getText}, ${rightTypeElement.map(_.getText).getOrElse("Nothing")}]"
}