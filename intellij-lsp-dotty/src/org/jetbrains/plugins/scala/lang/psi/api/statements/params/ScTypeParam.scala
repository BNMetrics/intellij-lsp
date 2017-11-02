package org.jetbrains.plugins.scala.lang.psi.api.statements.params

import com.intellij.psi._
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScAnnotationsHolder
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.{ScPolymorphicElement, ScTypeParametersOwner}
import org.jetbrains.plugins.scala.lang.psi.types.api._

/**
 * @author Alexander Podkhalyuzin
 */

trait ScTypeParam extends ScalaPsiElement with ScPolymorphicElement with PsiTypeParameter with ScAnnotationsHolder {
  val typeParamId: Long

  def isCovariant: Boolean

  def isContravariant: Boolean

  def variance: Variance = if (isCovariant) Covariant else if (isContravariant) Contravariant else Invariant

  def owner: ScTypeParametersOwner

  def getOffsetInFile: Int

  def getContainingFileName: String

  def typeParameterText: String

  def isHigherKindedTypeParameter: Boolean
}