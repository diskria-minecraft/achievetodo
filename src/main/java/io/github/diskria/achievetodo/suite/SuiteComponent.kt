package io.github.diskria.achievetodo.suite

import io.github.diskria.achievetodo.extensions.EmptyPackResourcesSupplier
import io.github.diskria.achievetodo.extensions.createPackMetadata
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.FilePackResources
import net.minecraft.server.packs.PackLocationInfo
import net.minecraft.server.packs.PackSelectionConfig
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.repository.Pack
import net.minecraft.server.packs.repository.Pack.Position
import net.minecraft.server.packs.repository.PackSource
import java.util.*
import kotlin.io.path.div

data class SuiteComponent(
    val id: String,
    val name: String,
    val color: ChatFormatting,
    val pageUrl: String,
    val download: Download,
) {
    val fileName = "$id.zip"
    val packId = "file/$fileName"

    val path by lazy { SuitePack.dir / fileName }

    val missingPack by lazy {
        Pack(
            PackLocationInfo(packId, Component.literal(name), PackSource.DEFAULT, Optional.empty()),
            EmptyPackResourcesSupplier,
            createPackMetadata(Component.translatable("achievetodo.suite.$id.description")),
            MISSING_PACK_SELECTION_CONFIG,
        ).apply {
            suiteComponent = this@SuiteComponent
            isMissingSuiteComponent = true
        }
    }

    fun installedPack() = Pack.readMetaAndCreate(
        PackLocationInfo(packId, Component.literal(name), PackSource.BUILT_IN, Optional.empty()),
        FilePackResources.FileResourcesSupplier(path),
        PackType.SERVER_DATA,
        INSTALLED_PACK_SELECTION_CONFIG,
    )?.apply {
        suiteComponent = this@SuiteComponent
        isInstalledSuiteComponent = true
    }

    sealed interface Download {

        val pageUrl: String
        val sha256: String

        data class Modrinth(
            override val pageUrl: String,
            override val sha256: String,
            private val fileUrlBase: String,
        ) : Download {
            val fileUrl = "$fileUrlBase?mr_download_reason=dependency"
        }

        data class Mediafire(
            override val pageUrl: String,
            override val sha256: String,
            val unzipMeSha256: String,
        ) : Download
    }

    companion object {
        val MISSING_PACK_SELECTION_CONFIG = PackSelectionConfig(false, Position.TOP, true)
        val INSTALLED_PACK_SELECTION_CONFIG = PackSelectionConfig(true, Position.TOP, true)
    }
}

object BacCorePack {
    val MAIN = SuiteComponent(
        id = "bacap",
        name = "BlazeandCave's Advancements Pack (BACAP)",
        color = ChatFormatting.AQUA,
        pageUrl = "https://modrinth.com/datapack/blazeandcaves-advancements-pack",
        download = SuiteComponent.Download.Modrinth(
            pageUrl = "https://modrinth.com/datapack/blazeandcaves-advancements-pack/version/1.21.1",
            fileUrlBase = "https://cdn.modrinth.com/data/VoVJ47kN/versions/YPEkY5bZ/BlazeandCave%27s%20Advancements%20Pack%201.21.1.zip",
            sha256 = "ce391fe46846d7fb357b7fbdd42387d260ae1a9516c601d1ba9ab09c584971d7",
        ),
    )

    val HARDCORE = SuiteComponent(
        id = "bacap_hardcore",
        name = "BACAP - Hardcore Version",
        color = ChatFormatting.RED,
        pageUrl = "https://modrinth.com/datapack/blazeandcaves-advancements-pack-hardcore-version",
        download = SuiteComponent.Download.Modrinth(
            pageUrl = "https://modrinth.com/datapack/blazeandcaves-advancements-pack-hardcore-version/version/1.21",
            fileUrlBase = "https://cdn.modrinth.com/data/QEv1xmKi/versions/ZHHHw5wF/BlazeandCave%27s%20Advancements%20Pack%20Hardcore.zip",
            sha256 = "1baf5f1bc7c48c819cb6b3081cb124793a316b3568352c853b94757bb4237446",
        ),
    )
}

data class BacCompatiblePack(val main: SuiteComponent, val compat: SuiteComponent) {
    companion object {
        val TERRALITH = create(
            id = "terralith",
            title = "Terralith",
            color = ChatFormatting.GREEN,
            mainPageUrl = "https://modrinth.com/datapack/terralith",
            mainDownload = SuiteComponent.Download.Modrinth(
                pageUrl = "https://modrinth.com/datapack/terralith/version/fqnsIp6d",
                fileUrlBase = "https://cdn.modrinth.com/data/8oi3bsk5/versions/fqnsIp6d/Terralith_v2.6.5%2B26.3.zip",
                sha256 = "9764f1d11a0bfe99c2cd96cd54f346fe756165d14a6f855b394954c171d603d5",
            ),
            compatDownload = SuiteComponent.Download.Mediafire(
                pageUrl = "https://www.mediafire.com/file/rvmh35b6bvyjhj7/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Terralith_1.18.3.zip/file",
                unzipMeSha256 = "",
                sha256 = "",
            ),
        )

        val AMPLIFIED_NETHER = create(
            id = "amplified_nether",
            title = "Amplified Nether",
            color = ChatFormatting.DARK_RED,
            mainPageUrl = "https://modrinth.com/datapack/amplified-nether",
            mainDownload = SuiteComponent.Download.Modrinth(
                pageUrl = "https://modrinth.com/datapack/amplified-nether/version/sKKMLxcI",
                fileUrlBase = "https://cdn.modrinth.com/data/wXiGiyGX/versions/sKKMLxcI/Amplified_Nether_v1.3.0%2B26.3.zip",
                sha256 = "cb2699a5f399c5f3ff92a0107325e1d82730650b36231b4d084d85af2aa86fd1",
            ),
            compatDownload = SuiteComponent.Download.Mediafire(
                pageUrl = "https://www.mediafire.com/file/sdg25wqdqk1uzfx/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Amplified_Nether_1.18.3.zip/file",
                unzipMeSha256 = "",
                sha256 = "",
            ),
        )

        val NULLSCAPE = create(
            id = "nullscape",
            title = "Nullscape",
            color = ChatFormatting.DARK_PURPLE,
            mainPageUrl = "https://modrinth.com/datapack/nullscape",
            mainDownload = SuiteComponent.Download.Modrinth(
                pageUrl = "https://modrinth.com/datapack/nullscape/version/BJjbILcf",
                fileUrlBase = "https://cdn.modrinth.com/data/LPjGiSO4/versions/BJjbILcf/Nullscape_v2.0.1%2B26.3.zip",
                sha256 = "69dc8ebbd2509d3f3d2b42b18036a38828855939768cccedebcb168c641f4224",
            ),
            compatDownload = SuiteComponent.Download.Mediafire(
                pageUrl = "https://www.mediafire.com/file/bw97wde10zqbsuh/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Nullscape_1.18.3.zip/file",
                unzipMeSha256 = "",
                sha256 = "",
            ),
        )

        val ALL = listOf(TERRALITH, AMPLIFIED_NETHER, NULLSCAPE)

        private fun create(
            id: String,
            title: String,
            color: ChatFormatting,
            mainPageUrl: String,
            mainDownload: SuiteComponent.Download.Modrinth,
            compatDownload: SuiteComponent.Download.Mediafire,
        ) = BacCompatiblePack(
            main = SuiteComponent(
                id = id,
                name = title,
                color = color,
                pageUrl = mainPageUrl,
                download = mainDownload,
            ),
            compat = SuiteComponent(
                id = "bacap_$id",
                name = "BACAP - $title Version",
                color = color,
                pageUrl = "https://www.planetminecraft.com/data-pack/blazeandcave-s-advancements-pack-terralith-version",
                download = compatDownload,
            )
        )
    }
}
