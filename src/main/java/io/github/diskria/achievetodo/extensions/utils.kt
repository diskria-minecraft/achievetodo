package io.github.diskria.achievetodo.extensions

import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackMetadataResources
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.metadata.MetadataSectionType
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.PackCompatibility
import net.minecraft.world.flag.FeatureFlagSet
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.stream.Stream
import java.util.zip.ZipFile
import kotlin.io.path.outputStream

fun Int.isEven() = (this and 1) == 0
fun Int.toEven(): Int = this and 1.inv()

fun String.capitalized(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

fun createPackMetadata(description: Component): Pack.Metadata =
    Pack.Metadata(description, PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), emptyList())

fun extractNestedZip(from: Path, to: Path): Boolean = runCatching {
    ZipFile(from.toFile()).use { outerZip ->
        val nestedZipEntry = outerZip.entries().asSequence().singleOrNull {
            !it.isDirectory && it.name.endsWith(".zip", ignoreCase = true)
        } ?: return false
        outerZip.getInputStream(nestedZipEntry).use { nested ->
            to.outputStream(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { extracted ->
                nested.copyTo(extracted)
            }
        }
        true
    }
}.isSuccess

object EmptyPackResourcesSupplier : Pack.ResourcesSupplier {

    override fun openMetadata(location: PackLocationInfo): PackMetadataResources = object : PackMetadataResources {
        override fun location() = location
        override fun getRootResource(vararg pathSegments: String) = null
        override fun <T : Any> getMetadataSection(metadataSerializer: MetadataSectionType<T>) = null
        override fun close() {}
    }

    override fun openResources(location: PackLocationInfo, metadata: Pack.Metadata): Stream<PackResources> =
        Stream.empty()
}

typealias Op<R> = Operation<R>

operator fun <R> Op<R>.invoke(vararg args: Any?): R = call(*args)

typealias Cancel = CallbackInfo
typealias Return<R> = CallbackInfoReturnable<R>

operator fun Cancel.invoke() {
    cancel()
}

operator fun <R> Return<R>.invoke(value: R) {
    returnValue = value
}
