package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public final class BiomeRepository {
    private static final Random generator = new Random();
    private static final HashMap<Integer, Integer> IDtoColor = new HashMap<>(256);
    private static final TreeMap<String, Integer> nameToColor = new TreeMap<>();
    private static boolean dirty;

    private BiomeRepository() {}

    public static void loadBiomeColors() {
        File saveDir = new File(VoxelConstants.getMinecraft().runDirectory, "/voxelmap/");
        File settingsFile = new File(saveDir, "biomecolors.txt");
        if (settingsFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(settingsFile));

                String sCurrentLine;
                while ((sCurrentLine = br.readLine()) != null) {
                    String[] curLine = sCurrentLine.split("=");
                    if (curLine.length == 2) {
                        String name = curLine[0];
                        int color = 0;

                        try {
                            color = Integer.decode(curLine[1]);
                        } catch (NumberFormatException var10) {
                            VoxelConstants.getLogger().warn("Error decoding integer string for biome colors; " + curLine[1]);
                        }

                        if (nameToColor.put(name, color) != null) {
                            dirty = true;
                        }
                    }
                }

                br.close();
            } catch (IOException var12) {
                VoxelConstants.getLogger().error("biome load error: " + var12.getLocalizedMessage(), var12);
            }
        }

        try {
            InputStream is = VoxelConstants.getMinecraft().getResourceManager().getResource(new Identifier("voxelmap", "conf/biomecolors.txt")).get().getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                String[] curLine = sCurrentLine.split("=");
                if (curLine.length == 2) {
                    String name = curLine[0];
                    int color;

                    try {
                        color = Integer.decode(curLine[1]);
                    } catch (NumberFormatException var9) {
                        VoxelConstants.getLogger().warn("Error decoding integer string for biome colors; " + curLine[1]);
                        color = 0;
                    }

                    if (nameToColor.get(name) == null) {
                        nameToColor.put(name, color);
                        dirty = true;
                    }
                }
            }

            br.close();
            is.close();
        } catch (IOException var11) {
            VoxelConstants.getLogger().error("Error loading biome color config file from litemod!", var11);
        }

    }

    public static void saveBiomeColors() {
        if (dirty) {
            File saveDir = new File(VoxelConstants.getMinecraft().runDirectory, "/voxelmap/");
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            File settingsFile = new File(saveDir, "biomecolors.txt");

            try {
                PrintWriter out = new PrintWriter(new FileWriter(settingsFile));

                for (Map.Entry<String, Integer> entry : nameToColor.entrySet()) {
                    String name = entry.getKey();
                    Integer color = entry.getValue();
                    StringBuilder hexColor = new StringBuilder(Integer.toHexString(color));

                    while (hexColor.length() < 6) {
                        hexColor.insert(0, "0");
                    }

                    hexColor.insert(0, "0x");
                    out.println(name + "=" + hexColor);
                }

                out.close();
            } catch (IOException var8) {
                VoxelConstants.getLogger().error("biome save error: " + var8.getLocalizedMessage(), var8);
            }
        }

        dirty = false;
    }

    public static int getBiomeColor(int biomeID) {
        Integer color = IDtoColor.get(biomeID);

        if (color != null) return color;

        Biome biome = VoxelConstants.getPlayer().world.getRegistryManager().get(RegistryKeys.BIOME).get(biomeID);

        if (biome == null) {
            VoxelConstants.getLogger().warn("non biome");
            IDtoColor.put(biomeID, 0);

            return 0;
        }

        String identifier = VoxelConstants.getPlayer().world.getRegistryManager().get(RegistryKeys.BIOME).getId(biome).toString();
        color = nameToColor.get(identifier);

        if (color == null) {
            String friendlyName = getName(biome);

            color = nameToColor.get(friendlyName);

            if (color != null) {
                nameToColor.remove(friendlyName);
                nameToColor.put(identifier, color);
                dirty = true;
            }
        }

        if (color == null) {
            int r = generator.nextInt(255);
            int g = generator.nextInt(255);
            int b = generator.nextInt(255);

            color = r << 16 | g << 8 | b;
            nameToColor.put(identifier, color);
            dirty = true;
        }

        IDtoColor.put(biomeID, color);
        return color;
    }

    @NotNull
    private static String getName(Biome biome) {
        Identifier resourceLocation = VoxelConstants.getPlayer().world.getRegistryManager().get(RegistryKeys.BIOME).getId(biome);
        String translationKey = Util.createTranslationKey("biome", resourceLocation);

        String name = I18n.translate(translationKey);

        if (name.equals(translationKey)) return TextUtils.prettify(resourceLocation.getPath());
        return name;
    }

    @NotNull
    public static String getName(int biomeID) {
        Biome biome = VoxelConstants.getPlayer().world.getRegistryManager().get(RegistryKeys.BIOME).get(biomeID);

        if (biome != null) return getName(biome);
        return "Unknown";
    }
}
