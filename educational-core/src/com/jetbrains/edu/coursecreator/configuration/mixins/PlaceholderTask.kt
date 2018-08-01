package com.jetbrains.edu.coursecreator.configuration.mixins

import com.jetbrains.edu.learning.courseFormat.tasks.Task


class PlaceholderTask(title: String): Task(title) {
  override fun getTaskType(): String {
    throw NotImplementedError()
  }
}