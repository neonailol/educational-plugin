package com.jetbrains.edu.learning.gradle.generation

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modifyModules
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Consumer
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.gradle.JdkProjectSettings
import com.jetbrains.edu.learning.gradle.generation.EduGradleUtils.sanitizeName
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

open class GradleCourseProjectGenerator(
  builder: GradleCourseBuilderBase,
  course: Course
) : CourseProjectGenerator<JdkProjectSettings>(builder, course) {

  override fun createCourseStructure(project: Project, baseDir: VirtualFile, settings: JdkProjectSettings) {
    // Hack!
    // We rename all modules (really only root module because new project has only one root module)
    // to have names which are expected by gradle importer.
    //
    // We do these hacky things to avoid the following situation:
    // If project dir contains some symbols (' ', '/', '\', ':', '<', '>', '"', '?', '*', '|')  in its name (for example, `Awesome Course`)
    // then after project creation we will get `Awesome Course` root module.
    // But gradle importer won't find it because it expects `Awesome_Course` module
    // and it'll create new root module.
    // After project reopening we will get an exception because of two modules with same content.
    //
    // Note we don't create gradle project from the beginning
    // because it is much slower and prevents showing project content at the beginning
    project.modifyModules {
      for (module in modules) {
        val sanitizedName = sanitizeName(module.name)
        if (sanitizedName != module.name) {
          renameModule(module, sanitizedName)
        }
      }
    }

    PropertiesComponent.getInstance(project).setValue(SHOW_UNLINKED_GRADLE_POPUP, false, true)
    super.createCourseStructure(project, baseDir, settings)
  }

  override fun afterProjectGenerated(project: Project, projectSettings: JdkProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
    setJdk(project, projectSettings)
    setupGradleSettings(project)
  }

  protected open fun setupGradleSettings(project: Project) {
    EduGradleUtils.setGradleSettings(project, project.basePath!!)
  }

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    val gradleCourseBuilder = myCourseBuilder as GradleCourseBuilderBase
    EduGradleUtils.createProjectGradleFiles(baseDir, gradleCourseBuilder.templates, gradleCourseBuilder.templateVariables(project))
  }

  private fun setJdk(project: Project, settings: JdkProjectSettings) {
    val jdk = getJdk(settings)

    // Try to apply model, i.e. commit changes from sdk model into ProjectJdkTable
    try {
      settings.model.apply()
    } catch (e: ConfigurationException) {
      LOG.error(e)
    }

    runWriteAction { ProjectRootManager.getInstance(project).projectSdk = jdk }
  }

  private fun getJdk(settings: JdkProjectSettings): Sdk? {
    val selectedItem = settings.jdkItem ?: return null
    if (selectedItem is JdkComboBox.SuggestedJdkItem) {
      val type = selectedItem.sdkType
      val path = selectedItem.path
      val jdkRef = Ref<Sdk>()
      settings.model.addSdk(type, path, Consumer<Sdk> { jdkRef.set(it) })
      return jdkRef.get()
    }
    return selectedItem.jdk
  }

  companion object {

    private val LOG = Logger.getInstance(GradleCourseProjectGenerator::class.java)
    // Unfortunately, org.jetbrains.plugins.gradle.service.project.GradleStartupActivity#SHOW_UNLINKED_GRADLE_POPUP is private
    // so create own const
    private const val SHOW_UNLINKED_GRADLE_POPUP = "show.inlinked.gradle.project.popup"
  }
}
