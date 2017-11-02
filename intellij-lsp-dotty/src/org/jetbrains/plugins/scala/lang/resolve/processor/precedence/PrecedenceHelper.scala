package org.jetbrains.plugins.scala.lang.resolve.processor.precedence

import java.util

import com.intellij.psi.util.PsiTreeUtil.getContextOfType
import com.intellij.psi.{PsiElement, PsiPackage}
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReferenceElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScPackaging
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportExpr
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.usages._
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.lang.resolve.{ResolveUtils, ScalaResolveResult}

import scala.annotation.tailrec

/**
  * User: Alexander Podkhalyuzin
  * Date: 01.12.11
  */
//todo: logic is too complicated, too many connections between classes. Rewrite?
trait PrecedenceHelper[Repr] {

  import PrecedenceHelper._

  def getPlace: PsiElement

  protected lazy val placePackageName: String = ResolveUtils.getPlacePackage(getPlace)
  protected val levelSet: util.HashSet[ScalaResolveResult] = new util.HashSet
  protected val qualifiedNamesSet: util.HashSet[Repr] = new util.HashSet[Repr]
  protected val levelQualifiedNamesSet: util.HashSet[Repr] = new util.HashSet[Repr]

  protected val holder: TopPrecedenceHolder[Repr]

  protected def clear(): Unit = {
    levelQualifiedNamesSet.clear()
    qualifiedNamesSet.clear()
    levelSet.clear()
  }

  private lazy val suspiciousPackages: Set[String] = collectPackages(getPlace)

  protected def ignored(results: Seq[ScalaResolveResult]): Boolean =
    results.headOption.flatMap(findQualifiedName)
      .exists((IgnoredPackages ++ suspiciousPackages).contains)

  protected def isCheckForEqualPrecedence = true

  protected def clearLevelQualifiedSet(result: ScalaResolveResult) {
    levelQualifiedNamesSet.clear()
  }

  protected def getLevelSet(result: ScalaResolveResult): util.HashSet[ScalaResolveResult] = levelSet

  /**
    * Do not add ResolveResults through candidatesSet. It may break precedence. Use this method instead.
    */
  protected def addResult(result: ScalaResolveResult): Boolean = addResults(Seq(result))

  protected def addResults(results: Seq[ScalaResolveResult]): Boolean = {
    if (results.isEmpty) return true
    val result: ScalaResolveResult = results.head

    import holder.toRepresentation
    lazy val qualifiedName: Repr = result
    lazy val levelSet = getLevelSet(result)

    def addResults() {
      if (qualifiedName != null) levelQualifiedNamesSet.add(qualifiedName)
      val iterator = results.iterator
      while (iterator.hasNext) {
        levelSet.add(iterator.next())
      }
    }

    val currentPrecedence = precedence(result)
    val topPrecedence = holder(result)
    if (currentPrecedence < topPrecedence) return false
    else if (currentPrecedence == topPrecedence && levelSet.isEmpty) return false
    else if (currentPrecedence == topPrecedence) {
      if (isCheckForEqualPrecedence && qualifiedName != null &&
        (levelQualifiedNamesSet.contains(qualifiedName) ||
          qualifiedNamesSet.contains(qualifiedName))) {
        return false
      } else if (qualifiedName != null && qualifiedNamesSet.contains(qualifiedName)) return false
      if (!ignored(results)) addResults()
    } else {
      if (qualifiedName != null && qualifiedNamesSet.contains(qualifiedName)) {
        return false
      } else {
        if (!ignored(results)) {
          holder(result) = currentPrecedence
          val levelSetIterator = levelSet.iterator()
          while (levelSetIterator.hasNext) {
            val next = levelSetIterator.next()
            if (holder.filterNot(next, result)(precedence)) {
              levelSetIterator.remove()
            }
          }
          clearLevelQualifiedSet(result)
          addResults()
        }
      }
    }
    true
  }

  protected def precedence(result: ScalaResolveResult): Int =
    if (result.prefixCompletion) PrecedenceTypes.PREFIX_COMPLETION
    else result.getPrecedence(getPlace, placePackageName)
}

object PrecedenceHelper {

  private val IgnoredPackages: Set[String] =
    Set("java.lang", "scala", "scala.Predef")

  private def collectPackages(element: PsiElement): Set[String] = {
    @tailrec
    def collectPackages(element: PsiElement, result: Set[String] = Set.empty): Set[String] =
      getContextOfType(element, true, classOf[ScPackaging]) match {
        case packaging: ScPackaging => collectPackages(packaging, result + packaging.fullPackageName)
        case null => result
      }

    collectPackages(element)
  }

  private def findQualifiedName(result: ScalaResolveResult): Option[String] =
    findImportReference(result)
      .flatMap(_.bind())
      .map(_.element)
      .collect {
        case p: PsiPackage => p.getQualifiedName
        case o: ScObject => o.qualifiedName
      }

  private def findImportReference(result: ScalaResolveResult): Option[ScStableCodeReferenceElement] =
    result.importsUsed.toSeq match {
      case Seq(head) =>
        val importExpression = head match {
          case ImportExprUsed(expr) => expr
          case ImportSelectorUsed(selector) => getContextOfType(selector, true, classOf[ScImportExpr])
          case ImportWildcardSelectorUsed(expr) => expr
        }
        Some(importExpression.qualifier)
      case _ => None
    }
}