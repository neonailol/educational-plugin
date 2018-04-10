package com.jetbrains.edu.coursecreator.stepik

import com.intellij.openapi.application.invokeAndWaitIfNeed
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector.*
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class StepikCourseUploader(private val project: Project) {
  private var isCourseInfoChanged = false
  private var newLessons: List<Lesson> = ArrayList()
  private var lessonsInfoToUpdate: List<Lesson> = ArrayList()
  private var lessonsToUpdate: List<Lesson> = ArrayList()
  private var tasksToUpdateByLessonIndex: Map<Int, List<Task>> = HashMap()
  private var tasksToPostByLessonIndex: Map<Int, List<Task>> = HashMap()
  private val course: RemoteCourse = StudyTaskManager.getInstance(project).course as RemoteCourse

  private fun doInit() {
    var courseFromServer = StudyTaskManager.getInstance(project).latestCourseFromServer
    if (courseFromServer != null) {
      setTaskFileTextFromDocuments()
    }
    else {
      courseFromServer = course
    }

    isCourseInfoChanged = courseInfoChanged(courseFromServer)

    val serverLessonIds = lessonIds(courseFromServer)
    newLessons = course.lessons.filter { lesson -> !serverLessonIds.contains(lesson.id) }

    lessonsInfoToUpdate = lessonsInfoToUpdate(course, serverLessonIds, courseFromServer)

    val updateCandidates = course.lessons.filter { lesson -> serverLessonIds.contains(lesson.id) }
    val lessonsById = courseFromServer.lessons.associateBy({ it.id }, { it })
    tasksToPostByLessonIndex = updateCandidates.associateBy({ it.index },
                                                            { newTasks(lessonsById[it.id]!!, it) }).filterValues { !it.isEmpty() }
    tasksToUpdateByLessonIndex = updateCandidates.associateBy({ it.index },
                                                              { tasksToUpdate(lessonsById[it.id]!!, it) }).filterValues { !it.isEmpty() }
    lessonsToUpdate = updateCandidates.filter {
      tasksToPostByLessonIndex.containsKey(it.index) || tasksToUpdateByLessonIndex.containsKey(it.index)
    }
  }

  private fun setTaskFileTextFromDocuments() {
    runInEdtAndWait {
      runReadAction {
        course.lessons
          .flatMap { it.taskList }
          .flatMap { it.taskFiles.values }
          .forEach { it.text = EduUtils.createStudentFile(project, it.getVirtualFile(project)!!, it.task, 0)!!.text }
      }
    }
  }

  private fun taskIds(lessonFormServer: Lesson) = lessonFormServer.taskList.map { task -> task.stepId }

  private fun newTasks(lessonFormServer: Lesson, updateCandidate: Lesson): List<Task> {
    val onServerTaskIds = taskIds(lessonFormServer)
    return updateCandidate.taskList.filter { task -> !onServerTaskIds.contains(task.stepId) }
  }


  private fun lessonIds(latestCourseFromServer: RemoteCourse) = latestCourseFromServer.lessons.map { lesson -> lesson.id }

  private fun courseInfoChanged(latestCourseFromServer: RemoteCourse): Boolean {
    return course.name != latestCourseFromServer.name ||
           course.description != latestCourseFromServer.description ||
           course.humanLanguage != latestCourseFromServer.humanLanguage ||
           course.languageID != latestCourseFromServer.languageID
  }

  fun uploadWithProgress(showNotification: Boolean) {

    val task: Backgroundable = object : Backgroundable(project, "Updating Course", true) {
      override fun run(progressIndicator: ProgressIndicator) {
        EduUtils.execCancelable {
          progressIndicator.isIndeterminate = true
          progressIndicator.text = "Getting items to update"
          doInit()
          progressIndicator.checkCanceled()

          var postedCourse: RemoteCourse? = null
          if (isCourseInfoChanged) {
            progressIndicator.text = "Updating course info"
            postedCourse = CCStepikConnector.updateCourseInfo(project, course)
          }
          progressIndicator.checkCanceled()

          if (!newLessons.isEmpty()) {
            if (!isCourseInfoChanged) {
              // it's updating course update date that is used in student project to check if there are new lessons
              postedCourse = CCStepikConnector.updateCourseInfo(project, course)
            }
            uploadLessons(progressIndicator)
          }
          progressIndicator.checkCanceled()

          if (!lessonsInfoToUpdate.isEmpty()) {
            updateLessonsInfo(progressIndicator)
          }
          progressIndicator.checkCanceled()

          if (!lessonsToUpdate.isEmpty()) {
            updateTasks(progressIndicator)
          }
          progressIndicator.checkCanceled()

          updateAdditionalMaterials(postedCourse ?: course)

          if (isCourseInfoChanged || !newLessons.isEmpty() || !lessonsInfoToUpdate.isEmpty() || !lessonsToUpdate.isEmpty()) {
            StudyTaskManager.getInstance(project).latestCourseFromServer = StudyTaskManager.getInstance(
              project).course!!.copy() as? RemoteCourse
          }
          if (showNotification) {
            val message = buildString {
              if (!newLessons.isEmpty()) {
                append(if (newLessons.size == 1) "One lesson pushed." else "Pushed: ${newLessons.size} lessons.")
                append("\n")
              }

              if (!lessonsInfoToUpdate.isEmpty() || !lessonsToUpdate.isEmpty()) {
                val size = lessonsInfoToUpdate.size + lessonsToUpdate.size
                append(if (size == 1) "One lesson updated" else "Updated: $size lessons")
              }

              if (isCourseInfoChanged && this.isEmpty()) {
                append("Course info updated")
              }
            }
            val title = if (message.isEmpty()) "Course is up to date" else "Course updated"
            CCStepikConnector.showNotification(project, title, message, seeOnStepikAction("/course/" + course.id))
          }
        }
      }
    }
    val indicator = BackgroundableProcessIndicator(task)
    indicator.isIndeterminate = false
    ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, indicator)
  }

  private fun updateAdditionalMaterials(postedCourse: RemoteCourse?) {
    invokeAndWaitIfNeed { FileDocumentManager.getInstance().saveAllDocuments() }
    val sectionIds = postedCourse!!.sectionIds
    if (!updateAdditionalMaterials(project, course, sectionIds)) {
      CCStepikConnector.postAdditionalFiles(course, project, course.id, sectionIds.size)
    }
  }

  private fun updateTasks(progressIndicator: ProgressIndicator) {
    val totalSize = tasksToPostByLessonIndex.values.sumBy { lessonsList -> lessonsList.size } + tasksToUpdateByLessonIndex.values.sumBy { lessonsList -> lessonsList.size }
    val showVerboseProgress = showVerboseProgress(totalSize)
    progressIndicator.isIndeterminate = showVerboseProgress
    var taskNumber = 0
    lessonsToUpdate.forEach { lesson ->
      progressIndicator.text = "Updating lesson: ${lesson.name}"
      if (tasksToPostByLessonIndex.contains(lesson.index)) {
        taskNumber++
        uploadTask(lesson, taskNumber, showVerboseProgress, progressIndicator, totalSize)
      }

      if (tasksToUpdateByLessonIndex.contains(lesson.index)) {
        taskNumber++
        updateTask(lesson, taskNumber, showVerboseProgress, progressIndicator, totalSize)
      }
    }
  }

  private fun updateTask(lesson: Lesson,
                         taskNumber: Int,
                         showVerboseProgress: Boolean,
                         progressIndicator: ProgressIndicator,
                         totalSize: Int) {
    tasksToUpdateByLessonIndex[lesson.index]!!.forEach { task ->
      if (showVerboseProgress) {
        progressIndicator.fraction = taskNumber.toDouble() / totalSize
      }
      progressIndicator.text2 = "Updating task: ${task.name}"
      CCStepikConnector.updateTask(project, task, false)
    }
  }

  private fun uploadTask(lesson: Lesson,
                         taskNumber: Int,
                         showVerboseProgress: Boolean,
                         progressIndicator: ProgressIndicator,
                         totalSize: Int): Int {
    tasksToPostByLessonIndex[lesson.index]!!.forEach { task ->
      if (showVerboseProgress) {
        progressIndicator.fraction = taskNumber.toDouble() / totalSize
      }
      progressIndicator.text2 = "Posting task: ${task.name}"
      CCStepikConnector.postTask(project, task, lesson.id)
    }
    return taskNumber
  }

  private fun updateLessonsInfo(progressIndicator: ProgressIndicator) {
    val showVerboseProgress = showVerboseProgress(lessonsInfoToUpdate.size)
    progressIndicator.isIndeterminate = !showVerboseProgress
    lessonsInfoToUpdate.withIndex().forEach { (index, lesson) ->
      if (showVerboseProgress) {
        progressIndicator.fraction = (index / newLessons.size).toDouble()
      }
      progressIndicator.text = "Updating lesson info: ${lesson.name}"
      CCStepikConnector.updateLessonInfo(project, lesson, false)
    }
  }

  private fun uploadLessons(progressIndicator: ProgressIndicator) {
    val showVerboseProgress = showVerboseProgress(newLessons.size)
    progressIndicator.isIndeterminate = !showVerboseProgress

    newLessons.withIndex().forEach { (index, lesson) ->
      if (showVerboseProgress) {
        progressIndicator.fraction = (index / newLessons.size).toDouble()
      }
      progressIndicator.text = "Posting lesson: ${lesson.name}"
      CCStepikConnector.postLesson(project, lesson)
      val sections = course.sectionIds
      val sectionId = sections[sections.size - 1]
      val unitId = postUnit(lesson.id, lesson.index, sectionId, project)
      if (unitId != -1) {
        lesson.unitId = unitId
      }
    }
  }

  private fun showVerboseProgress(totalSize: Int) = totalSize > 1

  private fun tasksToUpdate(lessonFormServer: Lesson, updateCandidate: Lesson): List<Task> {
    val onServerTaskIds = taskIds(lessonFormServer)
    val tasksUpdateCandidate = updateCandidate.taskList.filter { task -> task.stepId in onServerTaskIds }

    val taskById = lessonFormServer.taskList.associateBy({ it.stepId }, { it })
    return tasksUpdateCandidate.filter { it != taskById[it.stepId] }
  }

  private fun lessonsInfoToUpdate(course: Course,
                                  serverLessonIds: List<Int>,
                                  latestCourseFromServer: RemoteCourse): List<Lesson> {
    return course.lessons
      .filter { lesson -> serverLessonIds.contains(lesson.id) }
      .filter { updateCandidate ->
        val lessonFormServer = latestCourseFromServer.getLesson(updateCandidate.id)
        lessonFormServer.index != updateCandidate.index ||
        lessonFormServer.name != updateCandidate.name ||
        lessonFormServer.isPublic != updateCandidate.isPublic
      }
  }
}

