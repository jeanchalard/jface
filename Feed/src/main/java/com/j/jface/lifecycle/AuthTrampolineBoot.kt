package com.j.jface.lifecycle

class AuthTrampolineJOrgBoot : ActivityWrapper<AuthTrampolineJOrg>()
class AuthTrampolineJFaceDataFeedBoot : ActivityWrapper<AuthTrampolineJFaceDataFeed>()
class AuthTrampolineJOrg(args : WrappedActivity.Args) : AuthTrampoline(args) { override val trampolineDestination get() = JOrgBoot::class.java }
class AuthTrampolineJFaceDataFeed(args : WrappedActivity.Args) : AuthTrampoline(args) { override val trampolineDestination get() = JFaceDataFeedBoot::class.java }
