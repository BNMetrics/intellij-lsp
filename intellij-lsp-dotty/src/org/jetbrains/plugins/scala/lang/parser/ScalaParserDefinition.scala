package org.jetbrains.plugins.scala.lang.parser

import com.intellij.lang.{ASTNode, ParserDefinition}
import com.intellij.openapi.project.Project
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.tree.{IStubFileElementType, TokenSet}
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.{FileViewProvider, PsiElement, PsiFile}
import org.jetbrains.plugins.dotty.DottyFileImpl
import org.jetbrains.plugins.scala.lang.lexer.{ScalaLexer, ScalaTokenTypes}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportStmt
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaFileImpl
import org.jetbrains.plugins.scala.settings._

/**
  * @author ilyas
  */
class ScalaParserDefinition extends ScalaParserDefinitionWrapper {

  protected val psiCreator: PsiCreator = ScalaPsiCreator

  def createLexer(project: Project): ScalaLexer = {
    //val settings = ScalaProjectSettings.getInstance(project)
    new ScalaLexer(false)
  }

  def createParser(project: Project): ScalaParser =
    new ScalaParser

  def getFileNodeType: IStubFileElementType[_ <: PsiFileStub[_ <: PsiFile]] =
    ScalaElementTypes.FILE

  def getCommentTokens: TokenSet = ScalaTokenTypes.COMMENTS_TOKEN_SET

  def getStringLiteralElements: TokenSet = ScalaTokenTypes.STRING_LITERAL_TOKEN_SET

  override def getWhitespaceTokens: TokenSet = ScalaTokenTypes.WHITES_SPACES_TOKEN_SET
  def createElement(astNode: ASTNode): PsiElement =
    psiCreator.createElement(astNode)

  def createFile(fileViewProvider: FileViewProvider): PsiFile = {
    new DottyFileImpl(fileViewProvider)
    /*val ext = ScalaFileFactory.EP_NAME.getExtensions
      ext.view
      .flatMap(_.createFile(fileViewProvider))
      .headOption
      .getOrElse(new ScalaFileImpl(fileViewProvider))*/
  }

  override def spaceExistanceTypeBetweenTokens(leftNode: ASTNode, rightNode: ASTNode): ParserDefinition.SpaceRequirements = {
    val importStatement = PsiTreeUtil.getParentOfType(leftNode.getPsi, classOf[ScImportStmt])
    val isNeighbour = Option(importStatement).exists {
      _.getTextRange.getEndOffset == rightNode.getTextRange.getStartOffset
    }

    import com.intellij.lang.ParserDefinition.SpaceRequirements._
    rightNode.getElementType match {
      case ScalaTokenTypes.tWHITE_SPACE_IN_LINE if rightNode.getText.contains('\n') => MAY
      case _ if isNeighbour => MUST_LINE_BREAK
      case ScalaTokenTypes.kIMPORT => MUST_LINE_BREAK
      case _ => super.spaceExistanceTypeBetweenTokens(leftNode, rightNode)
    }
  }
}
