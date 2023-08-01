package com.fourinachamber.fortyfive.map.statusbar

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.fourinachamber.fortyfive.game.SaveState
import com.fourinachamber.fortyfive.game.card.Card
import com.fourinachamber.fortyfive.screen.general.*
import com.fourinachamber.fortyfive.screen.general.customActor.CustomInputField
import com.fourinachamber.fortyfive.utils.Timeline
import onj.parser.OnjParser
import onj.parser.OnjSchemaParser
import onj.schema.OnjSchema
import onj.value.OnjArray
import onj.value.OnjObject


class Backpack(
    private val screen: OnjScreen,
    cardsFile: String,
    backpackFile: String,
    private val deckNameWidgetName: String,
    private val deckSelectionParentWidgetName: String,
    private val deckCardsWidgetName: String,
    private val backPackCardsWidgetName: String,
) :
    CustomFlexBox(screen),
    InOutAnimationActor {

    private val _allCards: MutableList<Card>
    private val cardImgs: MutableList<CustomImageActor> = mutableListOf()
    private val minDeckSize: Int
    private val numberOfSlots: Int
    private val minNameSize: Int
    private val maxNameSize: Int

    private lateinit var deckNameWidget: CustomInputField
    private lateinit var deckCardsWidget: CustomScrollableFlexBox
    private lateinit var backPackCardsWidget: CustomScrollableFlexBox
    private lateinit var deckSelectionParentWidget: CustomFlexBox

    init {
        val cardsOnj = OnjParser.parseFile(cardsFile)
        cardsFileSchema.assertMatches(cardsOnj)
        cardsOnj as OnjObject
        val cardPrototypes =
            (Card.getFrom(cardsOnj.get<OnjArray>("cards"), screen) {}).filter { it.name in SaveState.cards }
        _allCards = cardPrototypes.map { it.create() }.toMutableList()
        val onj = OnjParser.parseFile(backpackFile)
        backpackFileSchema.assertMatches(onj)
        onj as OnjObject
        minDeckSize = onj.get<Long>("minCardsPerDeck").toInt()
        numberOfSlots = onj.get<Long>("slotsPerDeck").toInt()
        val nameOnj = onj.get<OnjObject>("deckNameDef")
        minNameSize = nameOnj.get<Long>("minLength").toInt()
        maxNameSize = nameOnj.get<Long>("maxLength").toInt()
    }

    override fun initAfterChildrenExist() {
        deckNameWidget = screen.namedActorOrError(deckNameWidgetName) as CustomInputField
        deckCardsWidget = screen.namedActorOrError(deckCardsWidgetName) as CustomScrollableFlexBox
        backPackCardsWidget = screen.namedActorOrError(backPackCardsWidgetName) as CustomScrollableFlexBox
        deckSelectionParentWidget = screen.namedActorOrError(deckSelectionParentWidgetName) as CustomFlexBox
        deckNameWidget.maxLength = maxNameSize
        initDeckName()
        initDeckSelection()
    }

    private fun initDeckSelection() {
        for (i in 0 until deckSelectionParentWidget.children.size) {
            val child = deckSelectionParentWidget.children.get(i)
            child.onButtonClick { changeDeckTo(i + 1) }
        }
    }

    private fun changeDeckTo(newDeckId: Int, firstInit: Boolean = false) {
        if (SaveState.curDeck.id != newDeckId || firstInit) {
            val oldActor = deckSelectionParentWidget.children[SaveState.curDeck.id - 1] as CustomImageActor
            oldActor.backgroundHandle = oldActor.backgroundHandle?.replace("_hover", "")
            SaveState.curDeckNbr = newDeckId
            resetDeckNameField()
            (deckSelectionParentWidget.children[newDeckId - 1] as CustomImageActor).backgroundHandle += "_hover"
        }
    }

    private fun initDeckName() {
        resetDeckNameField()
        deckNameWidget.limitListener = object : CustomInputField.CustomMaxReachedListener {
            override fun maxReached(field: CustomInputField, wrong: String) {
                println("max reached, name '$wrong' too long") //TODO this with error-block
            }
        }

        deckNameWidget.keyslistener = object : CustomInputField.CustomInputFieldListener {
            override fun keyTyped(e: InputEvent, ch: Char) {
                if (ch == '\n' || ch == '\r') saveCurrentDeckName()
            }
        }

        deckNameWidget.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (deckNameWidget.isDisabled && tapCount == 2) editDeckName()
            }
        })
    }

    private fun resetDeckNameField() {
        deckNameWidget.setText(SaveState.curDeck.name)
        deckNameWidget.isDisabled = true
        deckNameWidget.clearSelection()
    }

    private fun saveCurrentDeckName() {
        deckNameWidget.clearSelection()
        deckNameWidget.isDisabled = true
        SaveState.curDeck.name = deckNameWidget.text.toString()
    }

    private fun editDeckName() {
        deckNameWidget.isDisabled = false
        stage.keyboardFocus = (deckNameWidget)
    }

    override fun display(): Timeline {
        changeDeckTo(SaveState.curDeck.id, true)
        return Timeline.timeline {
            parallelActions(
                getInOutTimeLine(true, false, this@Backpack.children[0] as CustomFlexBox).asAction(),
                getInOutTimeLine(true, true, this@Backpack.children[1] as CustomFlexBox).asAction()
            )
        }
    }

    private fun getInOutTimeLine(isGoingIn: Boolean, goingRight: Boolean, target: CustomFlexBox) = Timeline.timeline {
        val amount = stage.viewport.worldWidth / 2
        action {
            isVisible = true
            if (isGoingIn) {
                target.offsetX = amount * (if (goingRight) 1 else -1)
            }
        }
        repeat(amount.toInt() / 4) {
            action {
                println(target.x)
                target.offsetX += 4F * (if (goingRight) -1 else 1) * (if (isGoingIn) 1 else -1)
            }
            delay(1)
        }
    }

    override fun hide(): Timeline {
        return Timeline.timeline {
            parallelActions(
                getInOutTimeLine(false, false, this@Backpack.children[0] as CustomFlexBox).asAction(),
                getInOutTimeLine(false, true, this@Backpack.children[1] as CustomFlexBox).asAction()
            )
        }
    }

    companion object {
        private val backpackFileSchema: OnjSchema by lazy {
            OnjSchemaParser.parseFile(Gdx.files.internal("onjschemas/backpack.onjschema").file())
        }
        private val cardsFileSchema: OnjSchema by lazy {
            OnjSchemaParser.parseFile("onjschemas/cards.onjschema")
        }
        const val logTag: String = "Backpack"
    }
}