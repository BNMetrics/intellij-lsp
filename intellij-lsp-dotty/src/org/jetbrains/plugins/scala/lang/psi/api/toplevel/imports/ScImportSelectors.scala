package org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement

/** 
* @author Alexander Podkhalyuzin
* Date: 20.02.2008
*/

trait ScImportSelectors extends ScalaPsiElement {
  def selectors: Seq[ScImportSelector]

  def hasWildcard : Boolean

  def wildcardElement: Option[PsiElement]
}