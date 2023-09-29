package com.fourinachamber.fortyfive.screen.gameComponents

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.fourinachamber.fortyfive.game.GameController
import com.fourinachamber.fortyfive.game.StatusEffect
import com.fourinachamber.fortyfive.screen.general.CustomHorizontalGroup
import com.fourinachamber.fortyfive.screen.general.CustomLabel
import com.fourinachamber.fortyfive.screen.general.OnjScreen

/**
 * used for displaying status effects
 */
class StatusEffectDisplay(
    private val screen: OnjScreen,
    private val font: BitmapFont,
    private val fontColor: Color,
    private val fontScale: Float
) : CustomHorizontalGroup(screen) {

    private val effects: MutableList<Pair<StatusEffect, CustomLabel>> = mutableListOf()

    override fun draw(batch: Batch?, parentAlpha: Float) {
        effects.forEach { (effect, label) -> label.setText(effect.getDisplayText()) }
        super.draw(batch, parentAlpha)
    }

    /**
     * adds a new status effect that will be displayed
     */
    fun displayEffect(effect: StatusEffect) {
        if (effect in effects.map { it.first }) return
        val remainingLabel = CustomLabel(screen, effect.getDisplayText(), Label.LabelStyle(font, fontColor))
        remainingLabel.setFontScale(fontScale)
        addActor(effect.icon)
        addActor(remainingLabel)
        effects.add(effect to remainingLabel)
    }

    /**
     * removes a status effect
     */
    fun removeEffect(effect: StatusEffect) {
        val iterator = effects.iterator()
        while (iterator.hasNext()) {
            val (effectToTest, label) = iterator.next()
            if (effect !== effectToTest) continue
            removeActor(effect.icon)
            removeActor(label)
            break
        }
    }

}