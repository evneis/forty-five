package com.fourinachamber.fortyfive.map.shop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.fourinachamber.fortyfive.map.detailMap.ShopMapEvent
import com.fourinachamber.fortyfive.screen.ResourceManager
import com.fourinachamber.fortyfive.screen.general.CustomImageActor
//import com.fourinachamber.fortyfive.map.shop.Shop
import com.fourinachamber.fortyfive.screen.general.OnjScreen
import com.fourinachamber.fortyfive.screen.general.ScreenController
import com.fourinachamber.fortyfive.screen.general.styles.StyledActor
import com.fourinachamber.fortyfive.screen.general.styles.WidthStyleProperty
import onj.parser.OnjParser
import onj.parser.OnjSchemaParser
import onj.schema.OnjSchema
import onj.value.OnjArray
import onj.value.OnjFloat
import onj.value.OnjObject
import onj.value.OnjValue
import kotlin.random.Random

class ShopScreenController(onj: OnjObject) : ScreenController() {

    private lateinit var screen: OnjScreen
    private lateinit var context: ShopMapEvent

    //    private val personImageActorName = onj.get<String>("shopPersonImageActor")
    private lateinit var person: OnjObject

    //    private val cardConfigFile = onj.get<String>("cardsFile")
//    private val cardDragAndDropBehaviour = onj.get<OnjNamedObject>("cardDragBehaviour")
//    private val cardDrawActorName = onj.get<String>("cardDrawActor")
//    lateinit var closeButton: Actor

    private val shopFilePath = onj.get<String>("shopsFile")
    private val npcsFilePath = onj.get<String>("npcsFile")
    private val personWidgetName = onj.get<String>("personWidgetName")

    //    lateinit var personImageActor: CustomImageActor
    private lateinit var personWidget: PersonWidget

    override fun init(onjScreen: OnjScreen, context: Any?) {
        screen = onjScreen
        if (context !is ShopMapEvent) throw RuntimeException("context for shopScreenController must be a shopMapEvent")
        this.context = context
        val personWidget = onjScreen.namedActorOrError(personWidgetName)
//        personImageActor = screen.namedActorOrError(personImageActorName) as CustomImageActor
        if (personWidget !is PersonWidget) {
            throw RuntimeException("widget with name $personWidgetName must be of type shopWidget")
        }
        this.personWidget = personWidget
        val shopFile = OnjParser.parseFile(Gdx.files.internal(shopFilePath).file())
//        shopsSchema.assertMatches(shopFile)
        shopFile as OnjObject
        val npcsFile = OnjParser.parseFile(Gdx.files.internal(npcsFilePath).file())
//        shopsSchema.assertMatches(npcsFile)
        npcsFile as OnjObject

        person = shopFile
            .get<OnjArray>("people")
            .value
            .map { it as OnjObject }
            .find { it.get<String>("name") == context.person }
            ?: throw RuntimeException("unknown shop: ${context.person}")
//        println(person)
        val imgData = (npcsFile
            .get<OnjArray>("npcs")
            .value
            .map { it as OnjObject }
            .find { it.get<String>("name") == person.get<String>("npcImageName") }
            ?: throw RuntimeException("unknown shop: ${context.person}")).get<OnjObject>("image")

        println(imgData)
        personWidget.setDrawable(imgData)
//        personImageActor.setSize(200.0F, 700.0F)
//
//        println("" + personImageActor.x + "  " + personImageActor.y)
//        println("" + personImageActor.width + "  " + personImageActor.height)
//        println(personImageActor.parent.children)

//        personDrawable = ResourceManager.get(screen, imgData.get<String>("textureName"))
        //TODO Template Strings, technical design
    }

    override fun update() {
        super.update()
//        val imgData = person.get<OnjObject>("image")
//        if (personImageActor.width != imgData.get<OnjFloat>("width").value.toFloat()) {
//            personImageActor.width = imgData.get<OnjFloat>("width").value.toFloat()
//            personImageActor.height = imgData.get<OnjFloat>("height").value.toFloat()
//            personImageActor.x = imgData.get<OnjFloat>("positionLeft").value.toFloat()
//            personImageActor.y = imgData.get<OnjFloat>("positionTop").value.toFloat()
//            personImageActor.drawable = personDrawable
//        }
    }

//    override fun update() {
//        super.update()
//        println(personImageActor.width)
////        System.exit(0)
//        personImageActor.setSize(200F,800F)
//        personImageActor.isVisible=true
//    }

    companion object {

        const val schemaPath: String = "onjschemas/shops.onjschema"

        val shopsSchema: OnjSchema by lazy {
            OnjSchemaParser.parseFile(Gdx.files.internal(schemaPath).file())
        }

    }

}