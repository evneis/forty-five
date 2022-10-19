package com.fourinachamber.fourtyfive.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import onj.*

object OnjReaderUtils {

    fun readTextures(onj: OnjArray): Map<String, TextureRegion> = onj
        .value
        .map { it as OnjObject }
        .map { it.get<String>("name") to Texture(it.get<String>("file")) }
        .associate { it.first to TextureRegion(it.second) }


    fun readCursors(onj: OnjArray): Map<String, Cursor> {
        val cursors = mutableMapOf<String, Cursor>()
        onj.value.forEach {
            it as OnjObject
            val pixmap = Pixmap(Gdx.files.internal(it.get<String>("file")))
            val hotspotX = it.get<Long>("hotspotX").toInt()
            val hotspotY = it.get<Long>("hotspotY").toInt()
            val cursor = Gdx.graphics.newCursor(pixmap, hotspotX, hotspotY)
            pixmap.dispose()
            cursors[it.get<String>("name")] = cursor
        }
        return cursors
    }

    fun readAtlases(onj: OnjArray): Pair<Map<String, TextureRegion>, List<TextureAtlas>> {
        val atlases = mutableListOf<TextureAtlas>()
        val textures = mutableMapOf<String, TextureRegion>()
        onj
            .value
            .forEach { atlasOnj ->
                atlasOnj as OnjObject
                val atlas = TextureAtlas(atlasOnj.get<String>("file"))
                atlases.add(atlas)
                atlasOnj
                    .get<OnjArray>("defines")
                    .value
                    .map { (it as OnjString).value }
                    .forEach {
                        textures[it] = atlas.findRegion(it) ?: run {
                            throw RuntimeException("unknown texture name in atlas: $it")
                        }
                    }
            }
        return textures to atlases
    }

    fun readFonts(onj: OnjArray): Map<String, BitmapFont> = onj
        .value
        .map { it as OnjNamedObject }
        .associate {
            when (it.name) {
                "BitmapFont" -> it.get<String>("name") to readBitmapFont(it)
                "FreeTypeFont" -> it.get<String>("name") to readFreeTypeFont(it)
                "DistanceFieldFont" -> it.get<String>("name") to readDistanceFieldFont(it)
                else -> throw RuntimeException("Unknown font type ${it.name}")
            }
        }

    fun readPostProcessors(onj: OnjArray): Map<String, PostProcessor> = onj
        .value
        .map { it as OnjObject }
        .associate {
            it.get<String>("name") to readPostProcessor(it)
        }

    fun readBitmapFont(it: OnjNamedObject): BitmapFont {
        val font = BitmapFont(Gdx.files.internal(it.get<String>("file")))
        font.setUseIntegerPositions(false)
        return font
    }

    fun readFreeTypeFont(fontOnj: OnjNamedObject): BitmapFont {
        val generator = FreeTypeFontGenerator(Gdx.files.internal(fontOnj.get<String>("file")))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = fontOnj.get<Long>("size").toInt()
        val font = generator.generateFont(parameter)
        generator.dispose()
        font.setUseIntegerPositions(false)
        return font
    }

    fun readDistanceFieldFont(fontOnj: OnjNamedObject): BitmapFont {
        val texture = Texture(Gdx.files.internal(fontOnj.get<String>("imageFile")), true)
        val useMipMapLinearLinear = fontOnj.getOr("useMipMapLinearLinear", false)
        texture.setFilter(
            if (useMipMapLinearLinear) Texture.TextureFilter.MipMapLinearLinear else Texture.TextureFilter.MipMapLinearNearest,
            Texture.TextureFilter.Linear
        )
        val font = BitmapFont(Gdx.files.internal(fontOnj.get<String>("fontFile")), TextureRegion(texture), false)
        font.setUseIntegerPositions(false)
        return font
    }

    fun readPixmaps(onj: OnjArray): Map<String, Pixmap> = onj
        .value
        .map { it as OnjObject }
        .associate { it.get<String>("name") to Pixmap(Gdx.files.internal(it.get<String>("file"))) }

    fun readPostProcessor(onj: OnjObject): PostProcessor {
        val shader = ShaderProgram(
            Gdx.files.internal(onj.get<String>("vertexShader")),
            Gdx.files.internal(onj.get<String>("fragmentShader"))
        )
        if (!shader.isCompiled) throw RuntimeException(shader.log)
        val uniformsToBind = onj.get<OnjArray>("uniforms").value.map { it.value as String }

        val timeOffset = onj.getOr<Long>("timeOffset", 0).toInt()

        val args: MutableMap<String, Any?> = if (!onj["args"]!!.isNull()) {
            val map = mutableMapOf<String, Any?>()

            val argsOnj = onj.get<OnjObject>("args")

            for ((key, value)  in argsOnj.value) when (value) {

                is OnjFloat -> map[key] = value.value.toFloat()
                is OnjInt -> map[key] = value.value.toFloat()

                else -> throw RuntimeException("binding type ${value::class.simpleName} as a uniform" +
                        " is currently not supported")
            }

            map

        } else mutableMapOf()

        return PostProcessor(shader, uniformsToBind, args, timeOffset)
    }

    fun readAnimations(anims: OnjArray): Map<String, Animation> = anims
        .value
        .associate {
            it as OnjObject
            val name = it.get<String>("name")
            name to readAnimation(it)
        }

    fun readAnimation(onj: OnjObject): Animation {
        val atlas = TextureAtlas(Gdx.files.internal(onj.get<String>("atlasFile")))

        val frames: Array<TextureRegion> = if (onj.hasKey<OnjArray>("frames")) {
            val framesOnj = onj.get<OnjArray>("frames").value
            Array(framesOnj.size) { atlas.findRegion(framesOnj[it].value as String) }
        } else {
            // there has to be an easier way
            val framesMap = mutableMapOf<Int, TextureRegion>()
            for (region in atlas.regions) {
                val index = try {
                    Integer.parseInt(region.name)
                } catch (e: java.lang.NumberFormatException) {
                    continue
                }
                if (framesMap.containsKey(index)) throw RuntimeException("duplicate frame number: $index")
                framesMap[index] = region
            }
            framesMap
                .toList()
                .sortedBy { it.first }
                .map { it.second }
                .toTypedArray()
        }
        val initialFrame = onj.get<Long>("initialFrame").toInt()
        val frameTime = onj.get<Long>("frameTime").toInt()
        if (frameTime == 0) throw RuntimeException("frameTime can not be zero")
        return Animation(frames, atlas.textures, initialFrame, frameTime)
    }

    data class Animation(
        val frames: Array<TextureRegion>,
        val textures: Iterable<Texture>,
        val initialFrame: Int,
        val frameTime: Int
    ) : Disposable {

        init {
            if (initialFrame !in frames.indices) throw RuntimeException("frameOffset must be a valid index into frames")
        }

        override fun dispose() {
            textures.forEach(Disposable::dispose)
        }

    }

}