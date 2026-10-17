package io.github.valoliin.mythslegendstranslator.diagnostic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.resource.Resource;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Diagnostic informatif : lit les spawn JSON effectivement charges via le ResourceManager.
 * Ne modifie jamais la recherche Cobblemon, les objets, les chances, ni le spawn.
 * Les evaluations incompletes sont marquees inconnues : pas de fausse garantie.
 */
public final class SpawnDiagnostics {
    private SpawnDiagnostics() {}
    private static final String PREFIX = "§6[Myths Translator] §r";

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)
                || !(world instanceof ServerWorld serverWorld)) return TypedActionResult.pass(stack);
            Identifier item = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
            if (!"mythsandlegends".equals(item.getNamespace())) return TypedActionResult.pass(stack);
            // Filtrer les objets qui sont réellement des clés dans les règles de spawn.
            List<Rule> rules = readRules(serverWorld, item.toString());
            if (rules.isEmpty()) return TypedActionResult.pass(stack);
            if (player.isCreative()) fullReport(serverPlayer, serverWorld, item, rules);
            else survivalHint(serverPlayer, serverWorld, rules);
            return TypedActionResult.pass(stack); // ne jamais empêcher la logique Myths.
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(literal("mythstranslator")
                .requires(source -> source.hasPermissionLevel(2))
                .then(literal("day").executes(ctx -> run(ctx.getSource().getPlayerOrThrow(), "time set day")))
                .then(literal("night").executes(ctx -> run(ctx.getSource().getPlayerOrThrow(), "time set night")))
                .then(literal("clear").executes(ctx -> run(ctx.getSource().getPlayerOrThrow(), "weather clear")))
                .then(literal("thunder").executes(ctx -> run(ctx.getSource().getPlayerOrThrow(), "weather thunder")))
                .then(literal("biome").then(argument("id", StringArgumentType.word())
                    .executes(ctx -> locateBiome(ctx.getSource().getPlayerOrThrow(), StringArgumentType.getString(ctx, "id")))))));
    }

    private static int run(ServerPlayerEntity player, String command) {
        if (!player.isCreative()) { player.sendMessage(Text.literal(PREFIX + "§cRéservé au mode Créatif."), false); return 0; }
        player.getServer().getCommandManager().executeWithPrefix(player.getCommandSource().withLevel(4), command);
        return 1;
    }

    /** Recherche et TP sur un point de surface sûr (2 blocs libres), sans créer de nouveaux chunks. */
    private static int locateBiome(ServerPlayerEntity player, String id) {
        if (!player.isCreative()) { player.sendMessage(Text.literal(PREFIX + "§cRéservé au mode Créatif."), false); return 0; }
        if (!id.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+") && !id.matches("#[a-z0-9_.-]+:[a-z0-9_/.-]+")) return 0;
        ServerWorld world = player.getServerWorld();
        boolean tag = id.startsWith("#");
        Identifier name = Identifier.tryParse(tag ? id.substring(1) : id);
        if (name == null) return 0;
        TagKey<Biome> tagKey = TagKey.of(RegistryKeys.BIOME, name);
        Pair<BlockPos, RegistryEntry<Biome>> found = world.locateBiome(entry ->
            tag ? entry.isIn(tagKey) : entry.matchesId(name), player.getBlockPos(), 6400, 32, 64);
        if (found == null) { player.sendMessage(Text.literal(PREFIX + "§cBiome introuvable dans un rayon de 6400 blocs."), false); return 0; }
        BlockPos target = found.getFirst();
        // Déterminer la surface via MOTION_BLOCKING et éviter la téléportation en hauteur/void.
        int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, target.getX(), target.getZ());
        if (y <= world.getBottomY() || y + 2 >= world.getTopY()) {
            player.sendMessage(Text.literal(PREFIX + "§cAucun point de surface sûr trouvé ; TP annulé."), false);
            return 0;
        }
        player.teleport(world, target.getX() + 0.5, y + 0.1, target.getZ() + 0.5, player.getYaw(), player.getPitch());
        player.sendMessage(Text.literal(PREFIX + "§aTéléporté vers " + id + ". Vérifie les conditions puis réutilise la clé."), false);
        return 1;
    }

    private static void survivalHint(ServerPlayerEntity player, ServerWorld world, List<Rule> rules) {
        boolean plausible = false;
        for (Rule rule : rules) {
            if (biomeMatches(world, player.getBlockPos(), rule.biomes) && timeMatches(world, rule.time)
                && thunderMatches(world, rule.thunder)) { plausible = true; break; }
        }
        // Uniquement un indice prudent : une condition vérifiée ici n'est pas une validation du moteur Cobblemon.
        if (!plausible) player.sendMessage(Text.literal(PREFIX + "§eLes conditions d'apparition ne semblent pas réunies ici."), false);
    }

    private static void fullReport(ServerPlayerEntity player, ServerWorld world, Identifier item, List<Rule> rules) {
        BlockPos pos = player.getBlockPos();
        player.sendMessage(Text.literal(PREFIX + "§bDiagnostic de " + item + " (" + rules.size() + " variantes)"), false);
        int count = 0;
        Set<String> biomeOptions = new LinkedHashSet<>();
        Set<String> periods = new LinkedHashSet<>();
        boolean thunder = false;
        for (Rule rule : rules) {
            if (++count > 8) break;
            boolean b = biomeMatches(world, pos, rule.biomes);
            boolean t = timeMatches(world, rule.time);
            boolean w = thunderMatches(world, rule.thunder);
            player.sendMessage(Text.literal("§7• " + rule.pokemon + " [" + rule.id + "]  "
                + (b ? "§aBiome ✓ " : "§cBiome ✗ ")
                + (t ? "§aHeure ✓ " : "§cHeure ✗ ")
                + (w ? "§aMétéo ✓" : "§cMétéo ✗")), false);
            player.sendMessage(Text.literal("§8  Biomes : " + String.join(", ", rule.biomes)
                + (rule.time == null ? "" : " | période : " + rule.time)
                + (rule.thunder == null ? "" : " | orage : " + rule.thunder)), false);
            if (!rule.extra.isEmpty()) player.sendMessage(Text.literal("§6  Autres critères (non évalués) : " + rule.extra), false);
            biomeOptions.addAll(rule.biomes);
            if (rule.time != null) periods.add(rule.time);
            if (Boolean.TRUE.equals(rule.thunder)) thunder = true;
        }
        if (rules.size() > 8) player.sendMessage(Text.literal("§7... " + (rules.size()-8) + " variantes supplémentaires."), false);
        // Les boutons restent des actions autonomes : ils ne modifient pas la logique de spawn.
        for (String biome : biomeOptions) {
            if (biomeOptions.size() > 10) break;
            player.sendMessage(button("[TP " + biome + "]", "/mythstranslator biome " + biome), false);
        }
        if (periods.contains("night")) player.sendMessage(button("[Mettre la nuit]", "/mythstranslator night"), false);
        if (periods.contains("day")) player.sendMessage(button("[Mettre le jour]", "/mythstranslator day"), false);
        if (thunder) player.sendMessage(button("[Déclencher un orage]", "/mythstranslator thunder"), false);
        player.sendMessage(button("[Ciel dégagé]", "/mythstranslator clear"), false);
        player.sendMessage(Text.literal("§7Informations extraites des JSON chargés ; des restrictions Cobblemon peuvent s'ajouter."), false);
    }

    private static MutableText button(String title, String command) {
        return Text.literal("§b§n" + title).styled(style -> style
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Cliquer pour exécuter : " + command))));
    }

    private static boolean biomeMatches(ServerWorld world, BlockPos pos, List<String> biomes) {
        if (biomes.isEmpty()) return true;
        RegistryEntry<Biome> entry = world.getBiome(pos);
        for (String biome : biomes) {
            if (biome.startsWith("#")) {
                Identifier id = Identifier.tryParse(biome.substring(1));
                if (id != null && entry.isIn(TagKey.of(RegistryKeys.BIOME, id))) return true;
            } else {
                Identifier id = Identifier.tryParse(biome);
                if (id != null && entry.matchesId(id)) return true;
            }
        }
        return false;
    }
    private static boolean timeMatches(ServerWorld world, String time) {
        if (time == null) return true;
        long ticks = world.getTimeOfDay() % 24000L;
        return switch (time.toLowerCase()) {
            case "day" -> ticks < 12000;
            case "night" -> ticks >= 13000 && ticks < 23000;
            default -> true; // pas de conclusion si format inconnu
        };
    }
    private static boolean thunderMatches(ServerWorld world, Boolean value) {
        return value == null || world.isThundering() == value;
    }

    private static List<Rule> readRules(ServerWorld world, String key) {
        List<Rule> matches = new ArrayList<>();
        // ResourceManager : JSON réellement actifs, y compris overrides datapacks.
        Map<Identifier, Resource> resources = world.getServer().getResourceManager()
            .findResources("spawn_pool_world", id -> id.getPath().endsWith(".json") && id.getNamespace().equals("cobblemon"));
        for (var entry : resources.entrySet()) {
            try (Reader reader = entry.getValue().getReader()) {
                JsonObject document = JsonParser.parseReader(reader).getAsJsonObject();
                if (!document.has("spawns") || !document.get("spawns").isJsonArray()) continue;
                for (JsonElement element : document.getAsJsonArray("spawns")) {
                    JsonObject spawn = element.getAsJsonObject();
                    if (!spawn.has("condition")) continue;
                    JsonObject condition = spawn.getAsJsonObject("condition");
                    if (!condition.has("key_item") || !key.equals(condition.get("key_item").getAsString())) continue;
                    List<String> biomes = new ArrayList<>();
                    if (condition.has("biomes") && condition.get("biomes").isJsonArray())
                        for (JsonElement b : condition.getAsJsonArray("biomes")) biomes.add(b.getAsString());
                    List<String> extras = new ArrayList<>();
                    for (var c : condition.entrySet()) {
                        if (!Set.of("biomes", "key_item", "timeRange", "isThundering").contains(c.getKey()))
                            extras.add(c.getKey() + "=" + c.getValue());
                    }
                    matches.add(new Rule(spawn.has("id") ? spawn.get("id").getAsString() : "?",
                        spawn.has("pokemon") ? spawn.get("pokemon").getAsString() : "?",
                        biomes, condition.has("timeRange") ? condition.get("timeRange").getAsString() : null,
                        condition.has("isThundering") ? condition.get("isThundering").getAsBoolean() : null,
                        String.join("; ", extras)));
                }
            } catch (Exception ignored) { /* Un autre datapack peut contenir un JSON non standard. */ }
        }
        return matches;
    }
    private record Rule(String id, String pokemon, List<String> biomes, String time,
                        Boolean thunder, String extra) { }
}
