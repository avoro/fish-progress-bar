package com.github.avoro.fishprogressbar

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.wm.IdeFrame
import javax.swing.UIManager

class FishProgressBarListener : LafManagerListener, ApplicationActivationListener {

    init {
        updateProgressBarUI()
    }

    override fun lookAndFeelChanged(source: LafManager) {
        updateProgressBarUI()
    }

    override fun applicationActivated(ideFrame: IdeFrame) {
        updateProgressBarUI()
    }

    private fun updateProgressBarUI() {
        UIManager.put("ProgressBarUI", FishProgressBarUI::class.java.name)
        UIManager.getDefaults().put(FishProgressBarUI::class.java.name, FishProgressBarUI::class.java)
    }
}
