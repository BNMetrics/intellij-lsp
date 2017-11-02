package org.jetbrains.plugins.scala.lang.psi.stubs

import com.intellij.psi.stubs.NamedStub
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScParameter
import org.jetbrains.plugins.scala.lang.psi.stubs.impl.{ScExpressionOwnerStub, ScTypeElementOwnerStub}

/**
 * User: Alexander Podkhalyuzin
 * Date: 19.10.2008
 */

trait ScParameterStub extends NamedStub[ScParameter]
  with ScTypeElementOwnerStub[ScParameter]
  with ScExpressionOwnerStub[ScParameter] {

  def isStable: Boolean

  def isDefaultParameter: Boolean

  def isRepeated: Boolean

  def isVal: Boolean

  def isVar: Boolean

  def isCallByNameParameter: Boolean

  def deprecatedName: Option[String]
}