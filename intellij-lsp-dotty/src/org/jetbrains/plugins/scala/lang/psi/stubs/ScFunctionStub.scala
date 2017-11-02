package org.jetbrains.plugins.scala.lang.psi.stubs

import com.intellij.psi.stubs.NamedStub
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.stubs.impl.{ScExpressionOwnerStub, ScTypeElementOwnerStub}

/**
  * User: Alexander Podkhalyuzin
  * Date: 14.10.2008
  */
trait ScFunctionStub extends NamedStub[ScFunction]
  with ScMemberOrLocal
  with ScTypeElementOwnerStub[ScFunction]
  with ScExpressionOwnerStub[ScFunction] {

  def isImplicit: Boolean

  def isDeclaration: Boolean

  def annotations: Array[String]

  def hasAssign: Boolean
}