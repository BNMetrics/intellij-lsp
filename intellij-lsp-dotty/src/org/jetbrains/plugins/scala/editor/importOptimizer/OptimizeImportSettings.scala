package org.jetbrains.plugins.scala.editor.importOptimizer

import java.util.regex.Pattern

import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.lang.formatting.settings.ScalaCodeStyleSettings

/**
  * @author Nikolay.Tropin
  */
case class OptimizeImportSettings(addFullQualifiedImports: Boolean,
                                  isLocalImportsCanBeRelative: Boolean,
                                  sortImports: Boolean,
                                  collectImports: Boolean,
                                  isUnicodeArrow: Boolean,
                                  spacesInImports: Boolean,
                                  classCountToUseImportOnDemand: Int,
                                  importLayout: Array[String],
                                  isAlwaysUsedImport: String => Boolean,
                                  scalastyleSettings: ScalastyleSettings) {

  def scalastyleGroups: Option[Seq[Pattern]] = scalastyleSettings.groups
  def scalastyleOrder: Boolean = scalastyleSettings.scalastyleOrder

  private def this(s: ScalaCodeStyleSettings, scalastyleSettings: ScalastyleSettings) {

    this(
      s.isAddFullQualifiedImports,
      s.isDoNotChangeLocalImportsOnOptimize,
      s.isSortImports,
      s.isCollectImports,
      s.REPLACE_CASE_ARROW_WITH_UNICODE_CHAR,
      s.SPACES_IN_IMPORTS,
      s.getClassCountToUseImportOnDemand,
      s.getImportLayout,
      s.isAlwaysUsedImport,
      scalastyleSettings
    )
  }
}

object OptimizeImportSettings {
  def apply(project: Project): OptimizeImportSettings = {
    val codeStyleSettings = ScalaCodeStyleSettings.getInstance(project)
    val scalastyleSettings =
/*      if (codeStyleSettings.isSortAsScalastyle) {
        val scalastyleConfig = ScalastyleCodeInspection.configuration(project)
        val scalastyleChecker = scalastyleConfig.flatMap(_.checks.find(_.className == ScalastyleSettings.importOrderChecker))
        val groups = scalastyleChecker.filter(_.enabled).flatMap(ScalastyleSettings.groups)
        ScalastyleSettings(scalastyleOrder = true, groups)
      }*/ //CHANGED
      ScalastyleSettings(scalastyleOrder = false, None)

    new OptimizeImportSettings(codeStyleSettings, scalastyleSettings)
  }
}