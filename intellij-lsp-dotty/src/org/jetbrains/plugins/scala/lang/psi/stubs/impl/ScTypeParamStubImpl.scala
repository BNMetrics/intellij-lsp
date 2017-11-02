package org.jetbrains.plugins.scala.lang.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.{IStubElementType, StubElement}
import com.intellij.util.SofterReference
import com.intellij.util.io.StringRef
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParam
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory._
import org.jetbrains.plugins.scala.lang.psi.stubs.ScTypeParamStub
import org.jetbrains.plugins.scala.lang.psi.stubs.elements.StringRefArrayExt

/**
  * User: Alexander Podkhalyuzin
  * Date: 17.06.2009
  */
class ScTypeParamStubImpl(parent: StubElement[_ <: PsiElement],
                          elementType: IStubElementType[_ <: StubElement[_ <: PsiElement], _ <: PsiElement],
                          nameRef: StringRef,
                          private val textRef: StringRef,
                          protected[impl] val lowerBoundTextRef: Option[StringRef],
                          protected[impl] val upperBoundTextRef: Option[StringRef],
                          private val viewBoundsTextRefs: Array[StringRef],
                          private val contextBoundsTextRefs: Array[StringRef],
                          val isCovariant: Boolean,
                          val isContravariant: Boolean,
                          private val containingFileNameRef: StringRef,
                          val positionInFile: Int)
  extends ScNamedStubBase[ScTypeParam](parent, elementType, nameRef)
    with ScTypeParamStub with ScBoundsOwnerStub[ScTypeParam] {

  private var viewElementsReferences: SofterReference[Seq[ScTypeElement]] = null
  private var contextElementsReferences: SofterReference[Seq[ScTypeElement]] = null

  override def text: String = StringRef.toString(textRef)

  override def containingFileName: String = StringRef.toString(containingFileNameRef)

  override def viewBoundsTexts: Array[String] = viewBoundsTextRefs.asStrings

  def viewBoundsTypeElements: Seq[ScTypeElement] = {
    getFromReference(viewElementsReferences) {
      case (context, child) =>
        viewBoundsTexts.map {
          createTypeElementFromText(_, context, child)
        }
    } (viewElementsReferences = _)
  }

  override def contextBoundsTexts: Array[String] = contextBoundsTextRefs.asStrings

  def contextBoundsTypeElements: Seq[ScTypeElement] = {
    getFromReference(contextElementsReferences) {
      case (context, child) =>
        contextBoundsTexts.map {
          createTypeElementFromText(_, context, child)
        }
    } (contextElementsReferences = _)
  }
}