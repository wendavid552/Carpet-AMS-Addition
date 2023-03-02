
package club.mcams.carpet;

import carpet.CarpetExtension;
import carpet.CarpetServer;
//#if MC>=11900
//$$ import carpet.api.settings.CarpetRule;
//$$ import carpet.api.settings.RuleHelper;
//$$ import carpet.script.Module;
//#else
import carpet.settings.ParsedRule;
import carpet.script.bundled.BundledModule;
//#endif

import club.mcams.carpet.commands.chunkloadingCommandRegistry;
import club.mcams.carpet.function.ChunkLoading;
import club.mcams.carpet.logging.AmsCarpetLoggerRegistry;
import club.mcams.carpet.util.AmsCarpetTranslations;
import club.mcams.carpet.util.Logging;
import club.mcams.carpet.util.recipes.CraftingRule;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ReloadCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
//#if MC>=11900
//$$ import net.minecraft.command.CommandRegistryAccess;
//#endif

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class AmsServer implements CarpetExtension {

    public static MinecraftServer minecraftServer;
    private static final AmsServer INSTANCE = new AmsServer();
    public static final String name = AmsServerMod.getModId();
    public static final String MOD_NAME = "Carpet AMS Addition";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @Override
    public String version() {
        return name;
    }
    public void onPlayerLoggedOut(ServerPlayerEntity player) {
        ChunkLoading.onPlayerDisconnect(player);
    }
    public static void init() {
        CarpetServer.manageExtension(INSTANCE);
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return AmsCarpetTranslations.getTranslationFromResourcePath(lang);
    }

    @Override
    public void onGameStarted() {
        // let's /carpet handle our few simple settings
        CarpetServer.settingsManager.parseSettingsClass(AmsServerSettings.class);
        LOGGER.info(MOD_NAME + " " + "v" + AmsServerMod.getVersion() + " 载入成功");
        LOGGER.info("开源链接：https://github.com/Minecraft-AMS/Carpet-AMS-Addition");
        LOGGER.info("BUG反馈：https://github.com/Minecraft-AMS/Carpet-AMS-Addition/issues");
    }

    @Override
    public void onServerLoaded(MinecraftServer server) {
        minecraftServer = server;
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        File datapackPath = new File(server.getSavePath(WorldSavePath.DATAPACKS).toString() + "/AmsData/data/");
        if (Files.isDirectory(datapackPath.toPath())) {
            try {
                FileUtils.deleteDirectory(datapackPath);
            } catch (IOException e) {
                Logging.logStackTrace(e);
            }
        }
    }

    @Override
    public void onTick(MinecraftServer server) {

    }

    //#if MC>=11900
    //$$    @Override
    //$$    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, final CommandRegistryAccess commandBuildContext) {
    //$$        chunkloadingCommandRegistry.register(dispatcher);
    //$$    }
    //#else
    @Override
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        chunkloadingCommandRegistry.register(dispatcher);
    }
    //#endif

    @Override
    public void onPlayerLoggedIn(ServerPlayerEntity player) {
        ChunkLoading.onPlayerConnect(player);
    }

    @Override
    public void registerLoggers() {
        AmsCarpetLoggerRegistry.registerLoggers();
    }

    @Override
    public void onServerLoadedWorlds(MinecraftServer server) {
        String datapackPath = server.getSavePath(WorldSavePath.DATAPACKS).toString();
        if (Files.isDirectory(new File(datapackPath + "/Ams_flexibleData/").toPath())) {
            try {
                FileUtils.deleteDirectory(new File(datapackPath + "/Ams_flexibleData/"));
            } catch (IOException e) {
                Logging.logStackTrace(e);
            }
        }
        datapackPath += "/AmsData/";
        boolean isFirstLoad = !Files.isDirectory(new File(datapackPath).toPath());

        try {
            Files.createDirectories(new File(datapackPath + "data/ams/recipes").toPath());
            Files.createDirectories(new File(datapackPath + "data/ams/advancements").toPath());
            Files.createDirectories(new File(datapackPath + "data/minecraft/recipes").toPath());
            copyFile("assets/carpet-ams-addition/AmsRecipeTweakPack/pack.mcmeta", datapackPath + "pack.mcmeta");
        } catch (IOException e) {
            Logging.logStackTrace(e);
        }

        copyFile(
                "assets/carpet-ams-addition/AmsRecipeTweakPack/ams/advancements/root.json",
                datapackPath + "data/ams/advancements/root.json"
        );

        for (Field f : AmsServerSettings.class.getDeclaredFields()) {
            CraftingRule craftingRule = f.getAnnotation(CraftingRule.class);
            if (craftingRule == null) continue;
            registerCraftingRule(
                    craftingRule.name().isEmpty() ? f.getName() : craftingRule.name(),
                    craftingRule.recipes(),
                    craftingRule.recipeNamespace(),
                    datapackPath + "data/"
            );
        }
        reload();
        if (isFirstLoad) {
            //#if MC>=11900
            //$$server.getCommandManager().executeWithPrefix(server.getCommandSource(), "/datapack enable \"file/AmsData\"");
            //#else
            server.getCommandManager().execute(server.getCommandSource(), "/datapack enable \"file/AmsData\"");
            //#endif
        }
    }

    private void registerCraftingRule(String ruleName, String[] recipes, String recipeNamespace, String dataPath) {
        updateCraftingRule(CarpetServer.settingsManager.getRule(ruleName),recipes,recipeNamespace,dataPath,ruleName);
        CarpetServer.settingsManager.addRuleObserver
        ((source, rule, s) -> {
            //#if MC>=11900
            //$$if (rule.name().equals(ruleName)) {
            //$$    updateCraftingRule(rule, recipes, recipeNamespace, dataPath, ruleName);
            //$$    reload();
            //$$}
            //#else
            if (rule.name.equals(ruleName)) {
                updateCraftingRule(rule, recipes, recipeNamespace, dataPath, ruleName);
                reload();
            }
            //#endif
        });
    }

    private void updateCraftingRule(
            ParsedRule<?> rule,
            String[] recipes,
            String recipeNamespace,
            String datapackPath,
            String ruleName
    ) {
        ruleName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, ruleName);
        //#if MC>=11900
        //$$if (rule.type() == String.class) {
        //$$String value = RuleHelper.toRuleString(rule.value());
        //#else
        if (rule.type == String.class) {
            String value = rule.getAsString();
            //#endif
            List<String> installedRecipes = Lists.newArrayList();
            try {
                Stream<Path> fileStream = Files.list(new File(datapackPath + recipeNamespace, "recipes").toPath());
                fileStream.forEach(( path -> {
                    for (String recipeName : recipes) {
                        String fileName = path.getFileName().toString();
                        if (fileName.startsWith(recipeName)) {
                            installedRecipes.add(fileName);
                        }
                    }
                } ));
                fileStream.close();
            } catch (IOException e) {
                Logging.logStackTrace(e);
            }

            deleteRecipes(installedRecipes.toArray(new String[0]), recipeNamespace, datapackPath, ruleName, false);

            if (recipeNamespace.equals("ams")) {
                List<String> installedAdvancements = Lists.newArrayList();
                try {
                    Stream<Path> fileStream = Files.list(new File(datapackPath, "ams/advancements").toPath());
                    String finalRuleName = ruleName;
                    fileStream.forEach(( path -> {
                        String fileName = path.getFileName().toString().replace(".json", "");
                        if (fileName.startsWith(finalRuleName)) {
                            installedAdvancements.add(fileName);
                        }
                    } ));
                    fileStream.close();
                } catch (IOException e) {
                    Logging.logStackTrace(e);
                }
                for (String advancement : installedAdvancements.toArray(new String[0])) {
                    removeAdvancement(datapackPath, advancement);
                }
            }

            if (!value.equals("off")) {
                List<String> tempRecipes = Lists.newArrayList();
                for (String recipeName : recipes) {
                    tempRecipes.add(recipeName + "_" + value + ".json");
                }

                copyRecipes(tempRecipes.toArray(new String[0]), recipeNamespace, datapackPath, ruleName + "_" + value);
            }
        }
        //#if MC>=11900
        //$$else if (rule.type() == Integer.class && (Integer) rule.value() > 0) {
        //#else
        else if (rule.type == int.class && (Integer) rule.get() > 0) {
            //#endif
            copyRecipes(recipes, recipeNamespace, datapackPath, ruleName);
            int value = (Integer) rule.get();
            for (String recipeName : recipes) {
                String filePath = datapackPath + recipeNamespace + "/recipes/" + recipeName;
                JsonObject jsonObject = readJson(filePath);
                assert jsonObject != null;
                jsonObject.getAsJsonObject("result").addProperty("count", value);
                writeJson(jsonObject, filePath);
            }
        }
        //#if MC>=11900
        //$$else if (rule.type() == Boolean.class && RuleHelper.getBooleanValue(rule)) {
        //#else
        else if (rule.type == boolean.class && rule.getBoolValue()) {
            //#endif
            copyRecipes(recipes, recipeNamespace, datapackPath, ruleName);
        } else {
            deleteRecipes(recipes, recipeNamespace, datapackPath, ruleName, true);
        }
    }

    private void copyRecipes(String[] recipes, String recipeNamespace, String datapackPath, String ruleName) {
        for (String recipeName : recipes) {
            copyFile(
                    "assets/carpet-ams-addition/AmsRecipeTweakPack/" + recipeNamespace + "/recipes/" + recipeName,
                    datapackPath + recipeNamespace + "/recipes/" + recipeName
            );
        }
        if (recipeNamespace.equals("ams")) {
            writeAdvancement(datapackPath, ruleName, recipes);
        }
    }

    private void deleteRecipes(
            String[] recipes,
            String recipeNamespace,
            String datapackPath,
            String ruleName,
            boolean removeAdvancement
    ) {
        for (String recipeName : recipes) {
            try {
                Files.deleteIfExists(new File(datapackPath + recipeNamespace + "/recipes", recipeName).toPath());
            } catch (IOException e) {
                Logging.logStackTrace(e);
            }
        }
        if (removeAdvancement && recipeNamespace.equals("ams")) {
            removeAdvancement(datapackPath, ruleName);
        }
    }

    private void writeAdvancement(String datapackPath, String ruleName, String[] recipes) {
        copyFile(
                "assets/carpet-ams-addition/AmsRecipeTweakPack/ams/advancements/recipe_rule.json",
                datapackPath + "ams/advancements/" + ruleName + ".json"
        );

        JsonObject advancementJson = readJson(datapackPath + "ams/advancements/" + ruleName + ".json");
        assert advancementJson != null;
        JsonArray recipeRewards = advancementJson.getAsJsonObject("rewards").getAsJsonArray("recipes");

        for (String recipeName : recipes) {
            recipeRewards.add("ams:" + recipeName.replace(".json", ""));
        }
        writeJson(advancementJson, datapackPath + "ams/advancements/" + ruleName + ".json");
    }

    private void removeAdvancement(String datapackPath, String ruleName) {
        try {
            Files.deleteIfExists(new File(datapackPath + "ams/advancements/" + ruleName + ".json").toPath());
        } catch (IOException e) {
            Logging.logStackTrace(e);
        }
    }

    private void reload() {
        ResourcePackManager resourcePackManager = minecraftServer.getDataPackManager();
        resourcePackManager.scanPacks();
        Collection<String> collection = Lists.newArrayList(resourcePackManager.getEnabledNames());
        collection.add("AmsData");

        ReloadCommand.tryReloadDataPacks(collection, minecraftServer.getCommandSource());
    }

    private void copyFile(String resourcePath, String targetPath) {
        InputStream source = BundledModule.class.getClassLoader().getResourceAsStream(resourcePath);
        Path target = new File(targetPath).toPath();

        try {
            assert source != null;
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Logging.logStackTrace(e);
        } catch (NullPointerException e) {
            LOGGER.error("Resource '" + resourcePath + "' is null:");
            Logging.logStackTrace(e);
        }
    }
        //#if MC>=11900
        private static JsonObject readJson(String filePath) {
            try {
                FileReader reader = new FileReader(filePath);
                return JsonParser.parseReader(reader).getAsJsonObject();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
        //#else
        //$$ private static JsonObject readJson(String filePath) {
        //$$    JsonParser jsonParser = new JsonParser();
        //$$    try {
        //$$        FileReader reader = new FileReader(filePath);
        //$$        return jsonParser.parse(reader).getAsJsonObject();
        //$$    } catch (FileNotFoundException e) {
        //$$        e.printStackTrace();
        //$$    }
        //$$    return null;
        //$$}
        //#endif

        private static void writeJson(JsonObject jsonObject, String filePath) {
            try {
                FileWriter writer = new FileWriter(filePath);
                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject));
                writer.close();
            } catch (IOException e) {
                Logging.logStackTrace(e);
            }
        }
    }



