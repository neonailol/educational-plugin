package com.jetbrains.edu.coursecreator.configuration.mixins

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem


class PlaceholderStudyItem(val title: String): StudyItem() {
  override fun init(course: Course?, parentItem: StudyItem?, isRestarted: Boolean) {
    throw NotImplementedError()
  }

  override fun getName() = title

  override fun setName(name: String?) {
    throw NotImplementedError()
  }

  override fun getId(): Int {
    throw NotImplementedError()
  }

  override fun getDir(project: Project): VirtualFile {
    throw NotImplementedError()
  }

  override fun getCourse(): Course {
    throw NotImplementedError()
  }
}