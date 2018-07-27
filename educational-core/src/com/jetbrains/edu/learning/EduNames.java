/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning;

import org.jetbrains.annotations.NonNls;

@NonNls
public class EduNames {

  public static final String PLUGIN_ID = "com.jetbrains.edu";

  public static final String TASK_HTML = "task.html";
  public static final String TASK_MD = "task.md";
  public static final String HINTS = "hints";
  public static final String LESSON = "lesson";
  public static final String FRAMEWORK_LESSON = "framework lesson";
  public static final String SECTION = "section";
  public static final String TASK = "task";
  public static final String COURSE = "course";
  public static final String TEST_TAB_NAME = "test";
  public static final String USER_TEST_INPUT = "input";
  public static final String USER_TEST_OUTPUT = "output";
  public static final String WINDOW_POSTFIX = "_window.";
  public static final String WINDOWS_POSTFIX = "_windows";
  public static final String ANSWERS_POSTFIX = "_answers";
  public static final String USER_TESTS = "userTests";
  public static final String TEST_HELPER = "test_helper.py";

  public static final String COURSE_META_FILE = "course.json";
  public static final String ADDITIONAL_MATERIALS = "Edu additional materials";

  // Used as course type only
  public static final String PYCHARM = "PyCharm";

  public static final String STUDY = "Study";
  public static final String ADAPTIVE = "Adaptive";

  public static final String SRC = "src";
  public static final String TEST = "test";
  public static final String UTIL = "util";
  public static final String BUILD = "build";
  public static final String OUT = "out";

  public static final String STUDY_PROJECT_XML_PATH = "/.idea/study_project.xml";

  // IDs of supported languages. They are the same that `Language#getID` returns
  // but in some cases we don't have corresponding Language in classpath to get its id via `getID` method
  public static final String JAVA = "JAVA";
  public static final String KOTLIN = "kotlin";
  public static final String PYTHON = "Python";
  public static final String STEPIK_IDS_JSON = "stepik_ids.json";
  public static final String CHECKIO_PYTHON = "CheckiO-Python";
  public static final String CHECKIO_PYTHON_INTERPRETER = "python-3";


  public static final String OAUTH_OK_PAGE = "/oauthResponsePages/okPage.html";
  public static final String OAUTH_ERROR_PAGE = "/oauthResponsePages/errorPage.html";

  private EduNames() {
  }

}
