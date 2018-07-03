package com.j.jface.lifecycle

import com.j.jface.feed.JFaceDataFeed
import com.j.jface.org.DebugActivity
import com.j.jface.org.JOrg
import com.j.jface.org.editor.TodoEditor
import com.j.jface.org.todo.TodoProvider

class AuthTrampolineJOrgBoot : ActivityWrapper<AuthTrampolineJOrg>()
class AuthTrampolineJFaceDataFeedBoot : ActivityWrapper<AuthTrampolineJFaceDataFeed>()
class AuthTrampolineJOrg(args : WrappedActivity.Args) : AuthTrampoline(args) { override val trampolineDestination get() = JOrgBoot::class.java }
class AuthTrampolineJFaceDataFeed(args : WrappedActivity.Args) : AuthTrampoline(args) { override val trampolineDestination get() = JFaceDataFeedBoot::class.java }
class DebugActivityBoot : ActivityWrapper<DebugActivity>()
class JFaceDataFeedBoot : ActivityWrapper<JFaceDataFeed>()
class JOrgBoot : AppCompatActivityWrapper<JOrg>()
class TodoEditorBoot : ActivityWrapper<TodoEditor>()
class TodoProviderBoot : ContentProviderWrapper<TodoProvider>()
