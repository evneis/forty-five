package com.fourinachamber.fortyfive.game

import com.badlogic.gdx.Gdx
import com.fourinachamber.fortyfive.game.enemy.Enemy
import com.fourinachamber.fortyfive.game.enemy.EnemyAction
import com.fourinachamber.fortyfive.game.enemy.EnemyPrototype
import com.fourinachamber.fortyfive.utils.FortyFiveLogger
import com.fourinachamber.fortyfive.utils.between
import com.fourinachamber.fortyfive.utils.templateParam
import onj.parser.OnjParser
import onj.parser.OnjSchemaParser
import onj.schema.OnjSchema
import onj.value.OnjArray
import onj.value.OnjObject
import java.lang.Double.max
import kotlin.properties.Delegates

class GameDirector(private val controller: GameController) {


    private var turns by Delegates.notNull<Int>()

    private var turnRevealTime: Int = -1
    private var enemyActionTime: Int = -1

    fun init() {
        val enemiesOnj = OnjParser.parseFile(Gdx.files.internal("config/enemies.onj").file())
        enemiesFileSchema.assertMatches(enemiesOnj)
        enemiesOnj as OnjObject

        val difficulty = difficultyScale()
        FortyFiveLogger.debug(logTag, "difficulty = $difficulty")
        val enemyPrototypes = Enemy.readEnemies(enemiesOnj.get<OnjArray>("enemies"))
        val chosen = chooseEnemy(enemyPrototypes)
        FortyFiveLogger.debug(logTag, "chose enemy ${chosen.name}")
        turns = chosen.turnCount.random()
        FortyFiveLogger.debug(logTag, "chose $turns turns")
        if (turns >= 10) {
            turnRevealTime = (turns * (2.0 / 3.0)).toInt()
            enemyActionTime = (turns * (1.0 / 3.0)).toInt()
        } else {
            controller.remainingTurns = turns
        }
        FortyFiveLogger.debug(logTag, "turnRevealTime = $turnRevealTime; enemyActionTime = $enemyActionTime")
        val enemy = scaleAndCreateEnemy(chosen, difficulty)
        FortyFiveLogger.debug(logTag, "enemy: health = ${enemy.health}; damage = ${enemy.damage}")
        controller.initEnemyArea(enemy)
    }

    fun onNewTurn() {
        if (controller.turnCounter == turnRevealTime) {
            controller.remainingTurns = turns - controller.turnCounter
        }
        if (controller.turnCounter == enemyActionTime) {
            doEnemyAction()
        }
    }

    private fun doEnemyAction() {
        // TODO: logic
        val timeline = EnemyAction.RevolverRotation.getTimeline(controller, 0.5)
        controller.executeTimelineLater(timeline)
    }

    private fun chooseEnemy(prototypes: List<EnemyPrototype>): EnemyPrototype {
        return prototypes.random() // TODO: some more sophisticated logic
    }

    private fun scaleAndCreateEnemy(prototype: EnemyPrototype, difficultyScale: Double): Enemy {
        // TODO: some more sophisticated logic
        val health = (prototype.baseHealthPerTurn * turns * difficultyScale).toInt()
        val damage = (prototype.baseDamage * difficultyScale).toInt()
        return prototype.create(health, damage)
    }

    private fun difficultyScale(): Double {
        return 1.0 + SaveState.enemiesDefeated / 10.0
    }

    // TODO: remove, just for testing
    private var curEvaluation: Double by templateParam("game.curEvaluation", 0.0)

    fun currentEval(): Double {
        val isValue = evaluateState()
        val shouldValue = (1.0 / turns) * controller.turnCounter
        val eval = (isValue - shouldValue).between(0.0, 1.0)
        curEvaluation = eval
        return eval
    }

    fun evaluateState(): Double { // TODO: make private
        val enemy = controller.enemyArea.enemies[0]

        val turns = controller.turnCounter - 1

        // the ratio of how many reserves were spent to how the amount of turns, minus the baseReserves
        // a value higher than 0 indicates that the player gains reserves from different sources, a negative value
        // indicates suboptimal reserve usage
        val avgReserveGainOrLoss = if (turns == 0) {
            0.0
        } else {
            controller.reservesSpent.toDouble() / turns.toDouble()  - controller.baseReserves
        }
        // how much damage the bullets in the revolver would do when fired ignoring effects
        val damageInRevolver = controller.revolver.slots.sumOf { it.card?.baseDamage ?: 0 }
        val damagedPercent = 1.0 -
                (max(enemy.currentHealth.toDouble() - damageInRevolver, 0.0) / enemy.health.toDouble())

        val cardsDrawn = if (turns == 0) {
            0.0
        } else {
            (controller.cardsDrawn - controller.cardsToDrawInFirstRound) / turns.toDouble() - controller.cardsToDraw
        }

        val eval = if (damagedPercent > 0.8) {
            // when player is close to beating the enemy, return only enemy health
            damagedPercent
        } else {
            var x = ((avgReserveGainOrLoss.between(-3.0, 3.0) + 3.0) / 6.0) - 0.5 // normalize reserve value
            x = x * 0.4 + damagedPercent * 0.6 + (cardsDrawn / 10.0) // mix reserve value with cards drawn value
            if (controller.remainingCards < (turns - controller.turnCounter) * controller.cardsToDraw) {
                x -= 0.3 // punish player for running out of cards
            }
            x
        }.between(0.0, 1.0)
        return eval
    }

    companion object {

        const val logTag = "director"

        private val enemiesFileSchema: OnjSchema by lazy {
            OnjSchemaParser.parseFile(Gdx.files.internal("onjschemas/enemies.onjschema").file())
        }

    }
}
