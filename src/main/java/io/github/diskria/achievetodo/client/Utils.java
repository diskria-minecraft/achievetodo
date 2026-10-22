package io.github.diskria.achievetodo.client;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {

    public static String calculateSHA1(Path path) {
        if (path == null || Files.notExists(path)) {
            return null;
        }
        MessageDigest sha1Digest;
        try {
            sha1Digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try (InputStream is = Files.newInputStream(path)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                sha1Digest.update(buffer, 0, bytesRead);
            }
            byte[] bytes = sha1Digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().toLowerCase();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull BlockBox toBlockBox(@NotNull Box box) {
        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.ceil(box.maxX) - 1;
        int maxY = (int) Math.ceil(box.maxY) - 1;
        int maxZ = (int) Math.ceil(box.maxZ) - 1;
        return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static @NotNull String buildModrinthModUrl(String slug) {
        return "https://modrinth.com/mod/" + slug;
    }
}
