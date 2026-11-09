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
        val sha1: String
        val supportsDirect: Boolean get() = true

        data class Modrinth(val baseUrl: String, override val sha1: String) : Download {
            override val url: String get() = "$baseUrl?mr_download_reason=dependency"
        }

        data class Mediafire(override val url: String, val unzipMeSha1: String, override val sha1: String) : Download {
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
            sha1 = "18bb8fc57e606a1ad41b3f326c6f211e83d4fd2b",
        ),
    )

    val HARDCORE = SuitePackComponent(
        id = "bacap_hardcore",
        title = "BlazeandCave's Advancements Pack Hardcore Version",
        color = ChatFormatting.RED,
        pageUrl = "https://modrinth.com/datapack/blazeandcaves-advancements-pack-hardcore-version",
        dl = SuitePackComponent.Download.Modrinth(
            baseUrl = "https://cdn.modrinth.com/data/QEv1xmKi/versions/ZHHHw5wF/BlazeandCave%27s%20Advancements%20Pack%20Hardcore.zip",
            sha1 = "9c20e14bbef224d2cc4ce63c8d24de1abc7a2475",
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
                sha1 = "f177f61facf8c43705debe1e1508792dcf4d5323",
            ),
            compatDl = SuitePackComponent.Download.Mediafire(
                url = "https://www.mediafire.com/file/rvmh35b6bvyjhj7/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Terralith_1.18.3.zip/file",
                unzipMeSha1 = "23725f7b5f730f178c23c6b26daa001fa579b07d",
                sha1 = "1a666bd0d6c9eff05ef90d4c58e4b9836399f9b2",
            ),
        )

        val AMPLIFIED_NETHER = create(
            id = "amplified_nether",
            title = "Amplified Nether",
            color = ChatFormatting.DARK_RED,
            mainPageUrl = "https://modrinth.com/datapack/amplified-nether",
            mainDl = SuitePackComponent.Download.Modrinth(
                baseUrl = "https://cdn.modrinth.com/data/wXiGiyGX/versions/sKKMLxcI/Amplified_Nether_v1.3.0%2B26.3.zip",
                sha1 = "c1252e0b65462f80f33baa8d2bc4e11eade19699",
            ),
            compatDl = SuitePackComponent.Download.Mediafire(
                url = "https://www.mediafire.com/file/sdg25wqdqk1uzfx/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Amplified_Nether_1.18.3.zip/file",
                unzipMeSha1 = "03c941f721bb94623738acaaffcb3501ebd32ee2",
                sha1 = "4070cca88d1709286969cd5dacb8aba4b5addfb7",
            ),
        )

        val NULLSCAPE = create(
            id = "nullscape",
            title = "Nullscape",
            color = ChatFormatting.DARK_PURPLE,
            mainPageUrl = "https://modrinth.com/datapack/nullscape",
            mainDl = SuitePackComponent.Download.Modrinth(
                baseUrl = "https://cdn.modrinth.com/data/LPjGiSO4/versions/BJjbILcf/Nullscape_v2.0.1%2B26.3.zip",
                sha1 = "d759ee004b6d6e633bc849eee0ea2a929de8eabc",
            ),
            compatDl = SuitePackComponent.Download.Mediafire(
                url = "https://www.mediafire.com/file/bw97wde10zqbsuh/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Nullscape_1.18.3.zip/file",
                unzipMeSha1 = "3b6c4fe8016eb77d6776e86d7d9166b3a0c59561",
                sha1 = "4e53953e042f14cbc8aaef86474a572fca8ebf3f",
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
