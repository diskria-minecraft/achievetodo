package io.github.diskria.achievetodo.client;

import io.github.diskria.achievetodo.server.Constants;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;

public enum ExternalPack {

    BACAP(
        "BlazeandCave's Advancements Pack (BACAP)",
        Formatting.AQUA,
        "https://modrinth.com/datapack/blazeandcaves-advancements-pack",
        "https://cdn.modrinth.com/data/VoVJ47kN/versions/i8N5hYLH/BlazeandCave%27s%20Advancements%20Pack%201.18.1.zip",
        null,
        "45b8bb0076bbf5b92fde7dc9590c6686937abbc0",
        true
    ),
    BACAP_HARDCORE(
        "BACAP (Hardcore version)",
        Formatting.RED,
        "https://modrinth.com/datapack/blazeandcaves-advancements-pack-hardcore-version",
        "https://cdn.modrinth.com/data/QEv1xmKi/versions/uRKM9Bou/BlazeandCave%27s%20Advancements%20Pack%20Hardcore.zip",
        null,
        "ec5203496a822e6145562cd81e781ca0eea2c968",
        true
    ),
    BACAP_TERRALITH(
        "BACAP (Terralith version)",
        Formatting.GREEN,
        "https://www.planetminecraft.com/data-pack/blazeandcave-s-advancements-pack-terralith-version/",
        "https://www.mediafire.com/file/ljb8qwofxk4dq9i/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Terralith_1.18.zip/file",
        "2699070cf5040ab519c223178ee64ee9eafe3691",
        "3d8cc170c1bf2a00460a8d7e779acbe9d5034dea",
        false
    ),
    BACAP_AMPLIFIED_NETHER(
        "BACAP (Amplified Nether version)",
        Formatting.DARK_RED,
        "https://www.planetminecraft.com/data-pack/blazeandcave-s-advancements-pack-terralith-version/",
        "https://www.mediafire.com/file/ak5sjemiz60mzrc/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Amplified_Nether_1.18.zip/file",
        "981ff801e3cf7eace1ddc2fff8b6165c12ea52b0",
        "9956d0a7d26e0b7d166711fa4b6bf856d2993a44",
        false
    ),
    BACAP_NULLSCAPE(
        "BACAP (Nullscape version)",
        Formatting.DARK_PURPLE,
        "https://www.planetminecraft.com/data-pack/blazeandcave-s-advancements-pack-terralith-version/",
        "https://www.mediafire.com/file/hsj4koctw778e43/%255BUNZIP_ME%255D_BlazeandCave%2527s_Advancements_Pack_Nullscape_1.18.zip/file",
        "029c29644a9e94dd8c4111dc3ab2165e79fe4d66",
        "6a50de576558b6b9079a60ffdff73cd9e622eac1",
        false
    ),
    TERRALITH(
        "Terralith",
        Formatting.GREEN,
        "https://www.planetminecraft.com/data-pack/terralith-overworld-evolved-100-biomes-caves-and-more/",
        "https://cdn.modrinth.com/data/8oi3bsk5/versions/PcYlKx8w/Terralith_1.21_v2.5.7.zip",
        null,
        "9645d4c557e8419154cc5775c5fe3ba9e82bfb8f",
        true
    ),
    AMPLIFIED_NETHER(
        "Amplified Nether",
        Formatting.DARK_RED,
        "https://www.planetminecraft.com/data-pack/amplified-nether-1-18/",
        "https://cdn.modrinth.com/data/wXiGiyGX/versions/jfHNaJaE/Amplified_Nether_1.21_v1.2.7.zip",
        null,
        "473ddf1042ae7d4c19eff01944d0b9da0f11c831",
        true
    ),
    NULLSCAPE(
        "Nullscape",
        Formatting.DARK_PURPLE,
        "https://www.planetminecraft.com/data-pack/nullscape/",
        "https://cdn.modrinth.com/data/LPjGiSO4/versions/J4B2BaWk/Nullscape_1.21_v1.2.10.zip",
        null,
        "0a55bff36ab26b13963213ed1482d1b8b84ab568",
        true
    );

    private final String title;
    private final Formatting color;
    private final String pageUrl;
    private final String downloadUrl;
    private final String wrapperSha1;
    private final String sha1;
    private final boolean inGameDownloadSupported;

    ExternalPack(
        String title,
        Formatting color,
        String pageUrl,
        String downloadUrl,
        String wrapperSha1,
        String sha1,
        boolean inGameDownloadSupported
    ) {
        this.title = title;
        this.color = color;
        this.pageUrl = pageUrl;
        this.downloadUrl = downloadUrl;
        this.wrapperSha1 = wrapperSha1;
        this.sha1 = sha1;
        this.inGameDownloadSupported = inGameDownloadSupported;
    }

    public static ExternalPack mapFromFileName(String fileName) {
        return Arrays.stream(ExternalPack.values())
            .filter(pack -> pack.getFileName().equals(fileName))
            .findFirst()
            .orElse(null);
    }

    public String getTitle() {
        return title;
    }

    public Formatting getColor() {
        return color;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getWrapperSha1() {
        return wrapperSha1;
    }

    public String getSha1() {
        return sha1;
    }

    public boolean isInGameDownloadSupported() {
        return inGameDownloadSupported;
    }

    public @NotNull String getFileName() {
        return getName() + Constants.FileExtension.ZIP;
    }

    public @NotNull String getDatapackName() {
        return "file/" + getFileName();
    }

    public @NotNull String getReasonKey() {
        return "downloader.reason." + getName();
    }

    private @NotNull String getName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
