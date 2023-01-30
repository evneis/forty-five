package com.fourinachamber.fourtyfive.screen.gameComponents

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.fourinachamber.fourtyfive.game.enemy.Enemy
import com.fourinachamber.fourtyfive.screen.general.ZIndexActor
import com.fourinachamber.fourtyfive.screen.general.ZIndexGroup

/**
 * actor representing the area in which enemies can appear on the screen
 */
class EnemyArea : WidgetGroup(), ZIndexActor, ZIndexGroup {

    override var fixedZIndex: Int = 0

    private var _enemies: MutableList<Enemy> = mutableListOf()

    /**
     * all enemies in this area
     */
    val enemies: List<Enemy>
        get() = _enemies

    /**
     * adds a new enemy to this area
     */
    fun addEnemy(enemy: Enemy) {
        _enemies.add(enemy)
        addActor(enemy.actor)
        invalidate()
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)
    }

    override fun layout() {
        for (enemy in _enemies) {
            enemy.actor.setPosition(enemy.offsetX, enemy.offsetY)
        }
    }

    override fun resortZIndices() {
        children.sort { el1, el2 ->
            (if (el1 is ZIndexActor) el1.fixedZIndex else -1) -
                    (if (el2 is ZIndexActor) el2.fixedZIndex else -1)
        }
    }

}
