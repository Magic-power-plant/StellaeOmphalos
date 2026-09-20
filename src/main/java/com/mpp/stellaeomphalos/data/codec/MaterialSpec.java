package com.mpp.stellaeomphalos.data.codec;

import com.google.gson.*;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.*;

/** Capability contract; consumption is evaluated on copies before any inventory is changed. */
public sealed interface MaterialSpec permits MaterialSpec.ItemMaterial, MaterialSpec.FluidMaterial {
    boolean test(ItemStack stack);

    List<ItemStack> displayStacks();

    Optional<FluidIngredient> fluidRequirement();

    ItemStack consumeOne(ItemStack stack);

    static MaterialSpec parse(JsonElement element) {
        if (element == null || element.isJsonNull()) throw new JsonParseException("Empty material");
        if (element.isJsonObject()) {
            var json = element.getAsJsonObject();
            if (json.has("fluid") || json.has("fluid_tag"))
                return new FluidMaterial(FluidIngredient.parse(json));
            var source = json.has("items") ? json.get("items") : json;
            return new ItemMaterial(
                    Ingredient.fromJson(source),
                    RecipeJson.nbt(json.get("include_nbt")),
                    RecipeJson.nbt(json.get("exclude_nbt")),
                    json.has("absent_nbt")
                            ? java.util.stream.StreamSupport.stream(
                                            json.getAsJsonArray("absent_nbt").spliterator(), false)
                                    .map(JsonElement::getAsString)
                                    .toList()
                            : List.of());
        }
        return new ItemMaterial(Ingredient.fromJson(element), new CompoundTag(), new CompoundTag());
    }

    record ItemMaterial(
            Ingredient ingredient, CompoundTag include, CompoundTag exclude, List<String> absent)
            implements MaterialSpec {
        public ItemMaterial(Ingredient ingredient, CompoundTag include, CompoundTag exclude) {
            this(ingredient, include, exclude, List.of());
        }

        public ItemMaterial {
            include = include.copy();
            exclude = exclude.copy();
            absent = List.copyOf(absent);
        }

        @Override
        public CompoundTag include() {
            return include.copy();
        }

        @Override
        public CompoundTag exclude() {
            return exclude.copy();
        }

        public boolean test(ItemStack stack) {
            return ingredient.test(stack)
                    && (include.isEmpty() || NbtSubset.contains(stack.getTag(), include))
                    && (exclude.isEmpty() || !NbtSubset.contains(stack.getTag(), exclude))
                    && (!stack.hasTag() || absent.stream().noneMatch(stack.getTag()::contains));
        }

        public List<ItemStack> displayStacks() {
            return Arrays.stream(ingredient.getItems()).map(ItemStack::copy).toList();
        }

        public Optional<FluidIngredient> fluidRequirement() {
            return Optional.empty();
        }

        public ItemStack consumeOne(ItemStack stack) {
            if (!test(stack)) throw new IllegalStateException("Material changed");
            var remaining = stack.getCraftingRemainingItem();
            if (!remaining.isEmpty()) {
                if (stack.getCount() != 1)
                    throw new IllegalStateException("Container remainder needs a free slot");
                return remaining;
            }
            var copy = stack.copy();
            copy.shrink(1);
            return copy;
        }
    }

    record FluidMaterial(FluidIngredient fluid) implements MaterialSpec {
        public boolean test(ItemStack stack) {
            if (stack.getCount() != 1) return false;
            return FluidUtil.getFluidHandler(stack.copy())
                    .map(
                            handler -> {
                                for (int tank = 0; tank < handler.getTanks(); tank++) {
                                    var present = handler.getFluidInTank(tank);
                                    if (fluid.test(present)
                                            && fluid.test(
                                                    handler.drain(
                                                            new net.minecraftforge.fluids
                                                                    .FluidStack(
                                                                    present, fluid.amount()),
                                                            IFluidHandler.FluidAction.SIMULATE)))
                                        return true;
                                }
                                return false;
                            })
                    .orElse(false);
        }

        public List<ItemStack> displayStacks() {
            return fluid.fluids().stream()
                    .map(
                            f ->
                                    FluidUtil.getFilledBucket(
                                            new net.minecraftforge.fluids.FluidStack(f, 1000)))
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        public Optional<FluidIngredient> fluidRequirement() {
            return Optional.of(fluid);
        }

        public ItemStack consumeOne(ItemStack stack) {
            if (!test(stack)) throw new IllegalStateException("Insufficient fluid");
            var handler =
                    FluidUtil.getFluidHandler(stack.copy())
                            .orElseThrow(() -> new IllegalStateException("No fluid capability"));
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                var present = handler.getFluidInTank(tank);
                if (fluid.test(present)) {
                    var drained =
                            handler.drain(
                                    new net.minecraftforge.fluids.FluidStack(
                                            present, fluid.amount()),
                                    IFluidHandler.FluidAction.EXECUTE);
                    if (!fluid.test(drained)) throw new IllegalStateException("Fluid drain failed");
                    return handler.getContainer();
                }
            }
            throw new IllegalStateException("Fluid changed");
        }
    }
}
