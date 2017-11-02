package org.jetbrains.plugins.scala.lang.psi.api.toplevel

import com.intellij.psi.tree.IElementType
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes._
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.result.TypeResult

trait ScTypeBoundsOwner extends ScalaPsiElement {
  def lowerBound: TypeResult

  def upperBound: TypeResult
  def viewBound: Seq[ScType] = Nil
  def contextBound: Seq[ScType] = Nil

  def hasBounds: Boolean = lowerTypeElement.nonEmpty || upperTypeElement.nonEmpty
  def hasImplicitBound: Boolean = viewTypeElement.nonEmpty || contextBoundTypeElement.nonEmpty

  def lowerTypeElement: Option[ScTypeElement] = None

  def upperTypeElement: Option[ScTypeElement] = None
  def viewTypeElement: Seq[ScTypeElement] = Nil
  def contextBoundTypeElement: Seq[ScTypeElement] = Nil

  def removeImplicitBounds() {}

  def boundsText: String = {
    def toString(bounds: Traversable[ScTypeElement], elementType: IElementType) = bounds.map {
      case e => s"${elementType.toString} ${e.getText}"
    }

    (toString(lowerTypeElement, tLOWER_BOUND) ++
      toString(upperTypeElement, tUPPER_BOUND) ++
      toString(viewTypeElement, tVIEW) ++
      toString(contextBoundTypeElement, tCOLON))
      .mkString(" ")
  }
}