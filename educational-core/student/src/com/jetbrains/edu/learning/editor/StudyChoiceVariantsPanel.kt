package com.jetbrains.edu.learning.editor

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.jetbrains.edu.learning.courseFormat.Task
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.geometry.Insets
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.control.CheckBox
import javafx.scene.control.RadioButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants


class StudyChoiceVariantsPanel(task: Task) : JScrollPane() {

  init {
    val jfxPanel = JFXPanel()
    Platform.runLater {
      val group = Group()
      val scene = Scene(group)
      jfxPanel.scene = scene
      val vBox = VBox()
      vBox.spacing = 10.0
      vBox.padding = Insets(15.0, 10.0, 10.0, 15.0)
      if (task.isMultipleChoice) {
        for ((index, variant) in task.choiceVariants.withIndex()) {
          val isSelected = task.selectedVariants.contains(index)
          val checkBox = CheckBox(variant)
          checkBox.isSelected = isSelected
          checkBox.font = Font.font((EditorColorsManager.getInstance().globalScheme.editorFontSize + 2).toDouble())
          checkBox.stylesheets.add(String::class.java.getResource("/style/buttons.css").toExternalForm())
          checkBox.selectedProperty().addListener { observableValue, wasSelected, isSelected ->
            if (isSelected) {
              task.selectedVariants.add(index)
            }
            else {
              task.selectedVariants.remove(index)
            }
          }
          vBox.children.add(checkBox)
        }
      }
      else {
        val toggleGroup = ToggleGroup()
        for ((index, variant) in task.choiceVariants.withIndex()) {
          val isSelected = task.selectedVariants.contains(index)
          val radioButton = RadioButton(variant)
          radioButton.isSelected = isSelected
          radioButton.font = Font.font((EditorColorsManager.getInstance().globalScheme.editorFontSize + 2).toDouble())
          radioButton.stylesheets.add(String::class.java.getResource("/style/buttons.css").toExternalForm())
          radioButton.toggleGroup = toggleGroup
          radioButton.selectedProperty().addListener { observableValue, wasSelected, isSelected ->
            if (isSelected) {
              task.selectedVariants.add(index)
            }
            else {
              task.selectedVariants.remove(index)
            }
          }
          vBox.children.add(radioButton)
        }
      }

      group.children.add(vBox)
    }
    setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    setViewportView(jfxPanel)
  }
}
