package org.jetbrains.plugins.scala.lang.psi.impl.base.patterns

import com.intellij.lang.ASTNode
import com.intellij.psi._
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.plugins.scala.extensions.ifReadAllowed
import org.jetbrains.plugins.scala.lang.TokenSets
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.psi.{ScalaPsiElementImpl, ScalaPsiUtil}
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.base.patterns._
import org.jetbrains.plugins.scala.lang.psi.types.ScTypeExt
import org.jetbrains.plugins.scala.lang.psi.types.result._

/**
 * @author Alexander Podkhalyuzin
 */

class ScNamingPatternImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScNamingPattern {
  override def accept(visitor: PsiElementVisitor) {
    visitor match {
      case visitor: ScalaElementVisitor => super.accept(visitor)
      case _ => super.accept(visitor)
    }
  }

  override def toString: String = "NamingPattern: " + ifReadAllowed(name)("")

  def nameId: PsiElement = findChildByType[PsiElement](TokenSets.ID_SET)

  def isWildcard: Boolean = findChildByType[PsiElement](ScalaTokenTypes.tUNDER) != null

  override def `type`(): TypeResult = {
    if (getLastChild.isInstanceOf[ScSeqWildcard]) {
      return this.expectedType match {
        case Some(x) => Right(x)
        case _ =>  Failure("No expected type for wildcard naming")
      }
    }
    if (named == null) Failure("Cannot infer type")
    else {
      this.expectedType match {
        case Some(expectedType) => named.`type`().map(expectedType.glb(_))
        case _ => named.`type`()
      }
    }
  }

  override def processDeclarations(processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement,
                                   place: PsiElement): Boolean = {
    if (isStable) {
      ScalaPsiUtil.processImportLastParent(processor, state, place, lastParent, `type`())
    } else true
  }

  override def getOriginalElement: PsiElement = super[ScNamingPattern].getOriginalElement
}