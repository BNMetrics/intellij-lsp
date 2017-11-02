package org.jetbrains.plugins.scala.lang.refactoring.util

import com.intellij.openapi.util.text.StringUtil.isEmpty
import com.intellij.psi._
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.refactoring.ScalaNamesValidator._

import scala.reflect.NameTransformer

/**
  * User: Alexander Podkhalyuzin
  * Date: 24.06.2008
  */
object ScalaNamesUtil {
  val EXCLUDE_PREFIX = "exclude:"

  def isQualifiedName(text: String): Boolean =
    !isEmpty(text) && text.split('.').forall(isIdentifier(_))

  def isOperatorName(text: String): Boolean = isIdentifier(text) && isOpCharacter(text(0))

  def isOpCharacter(c: Char): Boolean = {
    c match {
      case '~' | '!' | '@' | '#' | '%' | '^' | '*' | '+' | '-' | '<' | '>' | '?' | ':' | '=' | '&' | '|' | '/' | '\\' =>
        true
      case ch =>
        Character.getType(ch) == Character.MATH_SYMBOL.toInt || Character.getType(ch) == Character.OTHER_SYMBOL.toInt
    }
  }

  def scalaName(element: PsiElement): String = element match {
    case scNamed: ScNamedElement => scNamed.name
    case psiNamed: PsiNamedElement => psiNamed.getName
  }

  def qualifiedName(named: PsiNamedElement): Option[String] = {
    ScalaPsiUtil.nameContext(named) match {
      case pack: PsiPackage => Some(pack.getQualifiedName)
      case clazz: PsiClass => Some(clazz.qualifiedName)
      case memb: PsiMember =>
        val containingClass = memb.containingClass
        if (containingClass != null && containingClass.qualifiedName != null && memb.hasModifierProperty(PsiModifier.STATIC)) {
          Some(Seq(containingClass.qualifiedName, named.name).filter(_ != "").mkString("."))
        } else None
      case _ => None
    }
  }

  def toJavaName(name: String): String = {
    val toEncode = name match {
      case ScalaNamesUtil.isBacktickedName(s) => s
      case _ => name
    }
    Option(toEncode).map(NameTransformer.encode).orNull
  }

  def equivalentFqn(l: String, r: String): Boolean =
    l == r || cleanFqn(l) == cleanFqn(r)

  def cleanFqn(fqn: String): String =
    splitName(fqn).map(clean).mkString(".")

  def equivalent(l: String, r: String): Boolean =
    l == r || clean(l) == clean(r)

  def clean(name: String): String = {
    val toDecode = name match {
      case ScalaNamesUtil.isBacktickedName(s) => s
      case _ => name
    }
    Option(toDecode).map(NameTransformer.decode).orNull
  }

  def escapeKeywordsFqn(fqn: String): String =
    splitName(fqn).map(escapeKeyword).mkString(".")

  def splitName(name: String): Seq[String] = {
    if (name == null || name.isEmpty) Seq.empty
    else if (name.contains(".")) name.split("\\.")
    else Seq(name)
  }

  def escapeKeyword(s: String): String =
    if (isKeyword(s)) s"`$s`" else s

  def nameFitToPatterns(qualName: String, patterns: Array[String], strict: Boolean): Boolean = {
    val (exclude, include) = patterns.toSeq.partition(_.startsWith(EXCLUDE_PREFIX))

    !exclude.exists(excl => fitToPattern(excl.stripPrefix(EXCLUDE_PREFIX), qualName, strict)) &&
      include.exists(fitToPattern(_, qualName, strict))
  }

  private def fitToPattern(pattern: String, qualName: String, strict: Boolean): Boolean = {

    @scala.annotation.tailrec
    def fitToUnderscorePattern(pattern: String, qualName: String, strict: Boolean): Boolean = {
      val subPattern = pattern.stripSuffix("._")
      val dotIdx = qualName.lastIndexOf('.')

      if (dotIdx <= 0) false
      else {
        val subName = qualName.substring(0, dotIdx)

        if (subPattern.endsWith("._"))
          fitToUnderscorePattern(subPattern, subName, strict = true)
        else if (strict)
          subName == subPattern
        else
          subName.startsWith(subPattern)
      }
    }

    if (pattern.endsWith("._"))
      fitToUnderscorePattern(pattern, qualName, strict)
    else qualName == pattern
  }

  object isBackticked {
    def unapply(named: ScNamedElement): Option[String] = {
      val name = named.name
      isBacktickedName.unapply(name)
    }
  }

  object isBacktickedName {
    def unapply(name: String): Option[String] = {
      if (name == null || name.isEmpty) None
      else if (name != "`" && name.startsWith("`") && name.endsWith("`")) Some(name.substring(1, name.length - 1))
      else None
    }
  }
}
