package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionPlaces.ACTION_SEARCH
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionToolWindowFactory
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JLabel


class SwitchTaskPanelAction : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent?) {
    val project = e?.project
    val result = createDialog().showAndGet()
    if (result && project != null) {
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW)
      toolWindow.contentManager.removeAllContents(false)
      TaskDescriptionToolWindowFactory().createToolWindowContent(project, toolWindow)
      TaskDescriptionView.getInstance(project).updateTaskDescription()
    }
  }

  private fun createDialog(): DialogWrapper = MyDialog(false)

  class MyDialog(canBeParent: Boolean) : DialogWrapper(null, canBeParent) {
    private val JAVAFX_ITEM = "JavaFX"
    private val SWING_ITEM = "Swing"
    private val myComboBox: ComboBox<String> = ComboBox()

    override fun createCenterPanel(): JComponent? = myComboBox

    override fun createNorthPanel(): JComponent? = JLabel("Choose panel: ")

    override fun getPreferredFocusedComponent(): JComponent? = myComboBox

    override fun doOKAction() {
      super.doOKAction()
      EduSettings.getInstance().setShouldUseJavaFx(myComboBox.selectedItem == JAVAFX_ITEM)
    }

    init {
      val comboBoxModel = DefaultComboBoxModel<String>()
      if (EduUtils.hasJavaFx()) {
        comboBoxModel.addElement(JAVAFX_ITEM)
      }
      comboBoxModel.addElement(SWING_ITEM)
      comboBoxModel.selectedItem =
          if (EduUtils.hasJavaFx() && EduSettings.getInstance().shouldUseJavaFx()) JAVAFX_ITEM else SWING_ITEM
      myComboBox.model = comboBoxModel
      title = "Switch Task Description Panel"
      myComboBox.setMinimumAndPreferredWidth(250)
      init()
    }
  }

  override fun update(e: AnActionEvent?) {
    val place = e?.place
    val project = e?.project
    e?.presentation?.isEnabled = project != null && EduUtils.isStudyProject(project) || ACTION_SEARCH == place
  }
}