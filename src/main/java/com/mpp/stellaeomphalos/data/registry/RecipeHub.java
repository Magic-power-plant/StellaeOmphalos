package com.mpp.stellaeomphalos.data.registry;

import com.google.gson.*;
import com.mpp.stellaeomphalos.core.platform.*;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Server-session state; reload replaces the datapack view and replays each registered script once.
 */
public final class RecipeHub implements RecipeMutationApi {
    private final BiFunction<ResourceLocation, JsonObject, Recipe<?>> parser;
    private final RecipeBaseline baseline;
    private RecipeManager manager;
    private Map<ResourceLocation, Recipe<?>> effective = Map.of();
    private Map<ResourceLocation, RecipeIndex> indices = Map.of();
    private final Map<ResourceLocation, Recipe<?>> added = new LinkedHashMap<>();
    private final Set<ResourceLocation> removed = new HashSet<>(), disabled = new HashSet<>();
    private final List<java.util.function.Consumer<RecipeMutationApi>> scripts = new ArrayList<>();
    private boolean replaying;
    private Set<ResourceLocation> effectiveDisabled = Set.of();

    public RecipeHub(
            RecipeManager manager,
            RecipeBaseline baseline,
            BiFunction<ResourceLocation, JsonObject, Recipe<?>> parser) {
        this.manager = manager;
        this.baseline = baseline;
        this.parser = parser;
        compile();
    }

    public int epoch() {
        return baseline.epoch();
    }

    public Set<ResourceLocation> disabledFamilies() {
        return effectiveDisabled;
    }

    public void registerScript(java.util.function.Consumer<RecipeMutationApi> script) {
        scripts.add(script);
    }

    public void reload(RecipeManager manager) {
        this.manager = manager;
        added.clear();
        removed.clear();
        disabled.clear();
        compile();
        replaying = true;
        try {
            for (var script : scripts) {
                var previousAdded = new LinkedHashMap<>(added);
                var previousRemoved = new HashSet<>(removed);
                var previousDisabled = new HashSet<>(disabled);
                try {
                    script.accept(this);
                } catch (RuntimeException error) {
                    added.clear();
                    added.putAll(previousAdded);
                    removed.clear();
                    removed.addAll(previousRemoved);
                    disabled.clear();
                    disabled.addAll(previousDisabled);
                    com.mojang.logging.LogUtils.getLogger()
                            .error("Recipe script failed; mutations rolled back", error);
                }
            }
        } finally {
            replaying = false;
            compile();
        }
    }

    private void compile() {
        var recipes = new TreeMap<ResourceLocation, Recipe<?>>();
        var ids = new HashSet<ResourceLocation>();
        for (var recipe : manager.getRecipes())
            if (recipe instanceof RecipeDefinition) {
                ids.add(recipe.getId());
                recipes.put(recipe.getId(), recipe);
            }
        removed.forEach(recipes::remove);
        recipes.putAll(added);
        var configured =
                com.mpp.stellaeomphalos.OmphalosConfig.COMMON
                        .snapshot()
                        .get("crafting.disabledFamilies");
        var allDisabled = new HashSet<>(disabled);
        if (configured instanceof List<?> names)
            for (var name : names) allDisabled.add(ResourceLocation.tryParse(name.toString()));
        effectiveDisabled = Set.copyOf(allDisabled);
        recipes.values().removeIf(r -> allDisabled.contains(((RecipeDefinition) r).family()));
        effective = Map.copyOf(recipes);
        var groups = new HashMap<ResourceLocation, List<Recipe<?>>>();
        for (var recipe : recipes.values())
            groups.computeIfAbsent(((RecipeDefinition) recipe).family(), k -> new ArrayList<>())
                    .add(recipe);
        var next = new HashMap<ResourceLocation, RecipeIndex>();
        groups.forEach((id, list) -> next.put(id, new RecipeIndex(list)));
        indices = Map.copyOf(next);
        baseline.record(ids, removed, added.keySet(), allDisabled);
    }

    private void changed() {
        if (!replaying) compile();
    }

    public Optional<Recipe<?>> byId(ResourceLocation id) {
        return Optional.ofNullable(effective.get(id));
    }

    public List<Recipe<?>> all(ResourceLocation family) {
        var index = indices.get(family);
        return index == null ? List.of() : index.all();
    }

    public List<Recipe<?>> byInput(ResourceLocation family, Collection<ItemStack> input) {
        var index = indices.get(family);
        return index == null ? List.of() : index.byInput(input);
    }

    @Override
    public List<ResourceLocation> findRecipes(ResourceLocation family, ItemStack output) {
        var index = indices.get(family);
        return index == null
                ? List.of()
                : index.byOutput(output).stream().map(Recipe::getId).toList();
    }

    private Recipe<?> validate(ResourceLocation id, JsonObject json) {
        var recipe = parser.apply(id, json);
        if (!(recipe instanceof RecipeDefinition))
            throw new IllegalArgumentException("Unsupported recipe");
        return recipe;
    }

    @Override
    public void add(ResourceLocation id, JsonObject json) {
        if (added.containsKey(id) || manager.byKey(id).isPresent() && !removed.contains(id))
            throw new IllegalArgumentException("Recipe already exists: " + id);
        added.put(id, validate(id, json));
        removed.remove(id);
        changed();
    }

    @Override
    public void remove(ResourceLocation id) {
        added.remove(id);
        removed.add(id);
        changed();
    }

    @Override
    public void replace(ResourceLocation id, JsonObject json) {
        if (!added.containsKey(id) && manager.byKey(id).isEmpty())
            throw new IllegalArgumentException("Recipe missing: " + id);
        added.put(id, validate(id, json));
        removed.remove(id);
        changed();
    }

    @Override
    public void scale(ResourceLocation id, String field, double factor) {
        if (!Set.of(
                                "duration",
                                "lumen",
                                "chance",
                                "production_multiplier",
                                "shatter_multiplier",
                                "weight",
                                "cost")
                        .contains(field)
                || !Double.isFinite(factor)
                || factor < 0) throw new IllegalArgumentException("Invalid scale");
        var recipe = added.getOrDefault(id, effective.get(id));
        if (!(recipe instanceof RecipeDefinition definition))
            throw new IllegalArgumentException("Recipe missing");
        var json = definition.definition();
        if (!json.has(field)) throw new IllegalArgumentException("Field missing");
        double value = json.get(field).getAsDouble() * factor;
        if (Set.of("duration", "lumen", "chance", "weight", "cost").contains(field))
            json.addProperty(field, (long) Math.floor(value));
        else json.addProperty(field, value);
        replace(id, json);
    }

    @Override
    public void disable(ResourceLocation family) {
        disabled.add(family);
        changed();
    }

    @Override
    public void resetToBaseline() {
        added.clear();
        removed.clear();
        disabled.clear();
        changed();
    }

    @Override
    public void resetToBaseline(ResourceLocation family) {
        added.values().removeIf(r -> ((RecipeDefinition) r).family().equals(family));
        removed.removeIf(
                id ->
                        manager.byKey(id)
                                .filter(
                                        r ->
                                                r instanceof RecipeDefinition d
                                                        && d.family().equals(family))
                                .isPresent());
        disabled.remove(family);
        changed();
    }

    public void removeByOutput(ResourceLocation family, ItemStack output) {
        for (var id : findRecipes(family, output)) remove(id);
    }

    public void removeByInput(ResourceLocation family, ItemStack input) {
        for (var r : byInput(family, List.of(input)))
            if (((RecipeDefinition) r)
                    .displaySlots().values().stream().anyMatch(s -> s.test(input)))
                remove(r.getId());
    }

    public void scaleAll(ResourceLocation family, String field, double factor) {
        for (var recipe : List.copyOf(all(family))) scale(recipe.getId(), field, factor);
    }
}
