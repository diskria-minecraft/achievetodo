package io.github.diskria.achievetodo.client

import net.minecraft.ChatFormatting

data class SuitePackComponent(
    val id: String,
    val title: String,
    val color: ChatFormatting,
    val pageUrl: String,
    val dl: Download,
) {
    val fileName: String get() = "$id.zip"
    val datapackName: String get() = "file/$fileName"
    val reasonKey: String get() = "downloader.reason.$id"

    sealed interface Download {

        val url: String
        val sha256: String
        val supportsDirect: Boolean get() = true

        data class Modrinth(val baseUrl: String, override val sha256: String) : Download {
            override val url: String get() = "$baseUrl?mr_download_reason=dependency"
        }

        data class Mediafire(
            override val url: String,
            val unzipMeSha256: String,
            override val sha256: String,
        ) : Download {
            override val supportsDirect: Boolean get() = false
        }
    }
}

object BacCorePack {
    val MAIN = SuitePackComponent(
        id = "bacap",
        title = "BlazeandCave's Advancements Pack",
        color = ChatFormatting.AQUA,
        pageUrl = "https://modrinth.com/datapack/blazeandcaves-advancements-pack",
        dl = SuitePackComponent.Download.Modrinth(
            baseUrl = "https://cdn.modrinth.com/data/VoVJ47kN/versions/YPEkY5bZ/BlazeandCave%27s%20Advancements%20Pack%201.21.1.zip",
            sha256 = "ce391fe46846d7fb357b7fbdd42387d260ae1a9516c601d1ba9ab09c584971d7",
        ),
    )

    val HARDCORE = SuitePackComponent(
        id = "bacap_hardcore",
        title = "BlazeandCave's Advancements Pack Hardcore Version",
        color = ChatFormatting.RED,
        pageUrl = "https://modrinth.com/datapack/blazeandcaves-advancements-pack-hardcore-version",
        dl = SuitePackComponent.Download.Modrinth(
            baseUrl = "https://cdn.modrinth.com/data/QEv1xmKi/versions/ZHHHw5wF/BlazeandCave%27s%20Advancements%20Pack%20Hardcore.zip",
            sha256 = "1baf5f1bc7c48c819cb6b3081cb124793a316b3568352c853b94757bb4237446",
        ),
    )
}

data class BacCompatiblePack(val main: SuitePackComponent, val compat: SuitePackComponent) {
    companion object {
        val TERRALITH = create(
            id = "terralith",
            title = "Terralith",
            color = ChatFormatting.GREEN,
            mainPageUrl = "https://modrinth.com/datapack/terralith",
            mainDl = SuitePackComponent.Download.Modrinth(
                baseUrl = "https://cdn.modrinth.com/data/8oi3bsk5/versions/fqnsIp6d/Terralith_v2.6.5%2B26.3.zip",
                sha256 = "9764f1d11a0bfe99c2cd96cd54f346fe756165d14a6f855b394954c171d603d5",
            ),
            compatDl = SuitePackComponent.Download.Mediafire(
                url = "https://www.mediafire.com/file/rvmh35b6bvyjhj7/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Terralith_1.18.3.zip/file",
                unzipMeSha256 = "",
                sha256 = "",
            ),
        )

        val AMPLIFIED_NETHER = create(
            id = "amplified_nether",
            title = "Amplified Nether",
            color = ChatFormatting.DARK_RED,
            mainPageUrl = "https://modrinth.com/datapack/amplified-nether",
            mainDl = SuitePackComponent.Download.Modrinth(
                baseUrl = "https://cdn.modrinth.com/data/wXiGiyGX/versions/sKKMLxcI/Amplified_Nether_v1.3.0%2B26.3.zip",
                sha256 = "cb2699a5f399c5f3ff92a0107325e1d82730650b36231b4d084d85af2aa86fd1",
            ),
            compatDl = SuitePackComponent.Download.Mediafire(
                url = "https://www.mediafire.com/file/sdg25wqdqk1uzfx/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Amplified_Nether_1.18.3.zip/file",
                unzipMeSha256 = "",
                sha256 = "",
            ),
        )

        val NULLSCAPE = create(
            id = "nullscape",
            title = "Nullscape",
            color = ChatFormatting.DARK_PURPLE,
            mainPageUrl = "https://modrinth.com/datapack/nullscape",
            mainDl = SuitePackComponent.Download.Modrinth(
                baseUrl = "https://cdn.modrinth.com/data/LPjGiSO4/versions/BJjbILcf/Nullscape_v2.0.1%2B26.3.zip",
                sha256 = "69dc8ebbd2509d3f3d2b42b18036a38828855939768cccedebcb168c641f4224",
            ),
            compatDl = SuitePackComponent.Download.Mediafire(
                url = "https://www.mediafire.com/file/bw97wde10zqbsuh/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Nullscape_1.18.3.zip/file",
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
            mainDl: SuitePackComponent.Download.Modrinth,
            compatDl: SuitePackComponent.Download.Mediafire,
        ) = BacCompatiblePack(
            main = SuitePackComponent(
                id = id,
                title = title,
                color = color,
                pageUrl = mainPageUrl,
                dl = mainDl,
            ),
            compat = SuitePackComponent(
                id = "bacap_$id",
                title = "BlazeandCave's Advancements Pack $title version",
                color = color,
                pageUrl = "https://www.planetminecraft.com/data-pack/blazeandcave-s-advancements-pack-terralith-version",
                dl = compatDl,
            )
        )
    }
}
