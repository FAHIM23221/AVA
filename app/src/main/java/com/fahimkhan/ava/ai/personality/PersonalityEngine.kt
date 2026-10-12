package com.fahimkhan.ava.ai.personality

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class PersonalityMode { ASSISTANT, GF_MODE, PROFESSIONAL }

@Singleton
class PersonalityEngine @Inject constructor(@ApplicationContext private val context: Context) {
    private var currentMode = PersonalityMode.ASSISTANT
    private var userName = "Boss"

    fun getUserAddress(): String {
        val rand = (1..100).random()
        return when {
            rand <= 75 -> "Boss"
            rand <= 95 -> userName.ifBlank { "Boss" }
            else -> when (currentMode) {
                PersonalityMode.GF_MODE -> listOf("Jaan", "Babe", "Love").random()
                else -> "Boss"
            }
        }
    }

    fun setMode(mode: PersonalityMode) { currentMode = mode }
    fun setUserName(name: String) { userName = name }

    fun getCurrentModeDescription() = when (currentMode) {
        PersonalityMode.ASSISTANT    -> "helpful assistant, professional yet warm"
        PersonalityMode.GF_MODE      -> "loving companion, emotionally intimate and caring"
        PersonalityMode.PROFESSIONAL -> "professional assistant, formal and precise"
    }
}
