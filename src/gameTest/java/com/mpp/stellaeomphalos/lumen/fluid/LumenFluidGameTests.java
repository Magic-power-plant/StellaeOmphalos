package com.mpp.stellaeomphalos.lumen.fluid;

import com.mpp.stellaeomphalos.Omphalos;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Omphalos.MODID)
@PrefixGameTestTemplate(false)
public final class LumenFluidGameTests {
    @GameTest(template = "foundation_empty")
    public static void molten_lumen_registration_and_type_properties(GameTestHelper helper) {
        var type = MoltenLumenContent.TYPE.get();
        helper.assertTrue(type.getTemperature() == 120 && type.getLightLevel() == 15, "FluidType parameters wrong");
        helper.assertTrue(!type.canSwim(null) && type.canDrownIn(null), "Swim/drown semantics wrong");
        helper.assertTrue(((BucketItem) MoltenLumenContent.BUCKET.get()).getFluid() == MoltenLumenContent.STILL.get(),
                "Bucket not bound to still fluid");
        helper.assertTrue(MoltenLumenContent.BLOCK.get().getFluid() == MoltenLumenContent.STILL.get(),
                "Liquid block not bound to still fluid");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void molten_lumen_freezes_beside_cold_fluid(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlock(pos.west(), Blocks.WATER.defaultBlockState(), 3);
        level.setBlock(pos, MoltenLumenContent.BLOCK.get().defaultBlockState(), 3);
        helper.assertTrue(level.getBlockState(pos).is(Blocks.ICE), "Cold neighbor did not freeze molten lumen");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void molten_lumen_solidifies_to_sand_beside_hot_fluid(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlock(pos.north(), Blocks.LAVA.defaultBlockState(), 3);
        level.setBlock(pos, MoltenLumenContent.BLOCK.get().defaultBlockState(), 3);
        helper.assertTrue(level.getBlockState(pos).is(Blocks.SAND), "Hot neighbor did not solidify molten lumen to sand");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void molten_lumen_grants_night_vision_to_players(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(BlockPos.ZERO);
        var state = MoltenLumenContent.BLOCK.get().defaultBlockState();
        level.setBlock(pos, state, 3);
        var player = helper.makeMockPlayer();
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        state.entityInside(level, pos, player);
        var effect = player.getEffect(MobEffects.NIGHT_VISION);
        helper.assertTrue(effect != null && effect.getDuration() == LumenFluidInteractions.NIGHT_VISION_DURATION
                && effect.isAmbient() && effect.isVisible(), "Night vision not applied with expected flags");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void molten_lumen_infuses_log_item_entities(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(BlockPos.ZERO);
        var state = MoltenLumenContent.BLOCK.get().defaultBlockState();
        level.setBlock(pos, state, 3);
        var logs = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.OAK_LOG, 5));
        level.addFreshEntity(logs);
        state.entityInside(level, pos, logs);
        helper.assertTrue(logs.getItem().getCount() == 4, "Log stack not decremented by one");
        var infused = level.getEntities(EntityType.ITEM, new AABB(pos).inflate(1),
                entity -> entity.getItem().is(MoltenLumenContent.INFUSED_LOG_ITEM.get()));
        helper.assertTrue(infused.size() == 1 && infused.get(0).getItem().getCount() == 1, "Infused log not produced in place");
        helper.succeed();
    }
}
