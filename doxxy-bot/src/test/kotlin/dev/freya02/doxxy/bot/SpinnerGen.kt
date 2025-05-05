package dev.freya02.doxxy.bot

import io.github.freya022.botcommands.api.core.utils.withResource
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

fun main() {
    val img = withResource("/emojis/sync.png") { ImageIO.read(it) }

    val path = Path("spinner").createDirectories()

    val padding = 25
    val steps = 60
    repeat(steps) { i ->
        val image = BufferedImage(img.width + padding * 2, img.height + padding * 2, img.type)
        val graphics = image.createGraphics()

        val rotate = 360.0 / steps * i
        graphics.rotate(Math.toRadians(-rotate), img.width / 2.0 + padding, img.height / 2.0 + padding)
        graphics.drawImage(img, padding, padding, img.width, img.height, null)

        ImageIO.write(image, "png", path.resolve("$i.png").toFile())
    }
}