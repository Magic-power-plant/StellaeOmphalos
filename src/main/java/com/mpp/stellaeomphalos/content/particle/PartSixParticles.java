package com.mpp.stellaeomphalos.content.particle;

import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModParticles;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Part-6 粒子类型的声明类。本类**只负责服务端/注册表侧的登记**：客户端渲染
 * （provider 绑定、贴图、顶点色）属于 Part-7 工作流，因此这里不得出现任何 client 包引用。
 *
 * <p>{@code floating_cube} 使用 {@code new SimpleParticleType(true)}：它的贴图由运行时
 * 从流体材质推导（"总是显示"），其余 6 个使用 {@code false}（由调用方按需触发）。
 */
public final class PartSixParticles {

    /** 光斑上升粒子。 */
    public static final RegistrationGuard<SimpleParticleType> GLOW_MOTE = declare("glow_mote", false);

    /** 星点特效。 */
    public static final RegistrationGuard<SimpleParticleType> SPARKLE_STAR = declare("sparkle_star", false);

    /** 贴图朝向精灵（星象连线/曜灵）。 */
    public static final RegistrationGuard<SimpleParticleType> FACING_SPRITE = declare("facing_sprite", false);

    /** 以流体贴图为材质的漂浮立方体：alwaysShow = true。 */
    public static final RegistrationGuard<SimpleParticleType> FLOATING_CUBE = declare("floating_cube", true);

    /** 半透明下落方块（充能镐探矿）。 */
    public static final RegistrationGuard<SimpleParticleType> TRANSLUCENT_FALLING_BLOCK =
            declare("translucent_falling_block", false);

    /** 光柱/连线。 */
    public static final RegistrationGuard<SimpleParticleType> LIGHTBEAM = declare("lightbeam", false);

    /** 环绕轨道粒子。 */
    public static final RegistrationGuard<SimpleParticleType> ORBITAL = declare("orbital", false);

    private PartSixParticles() {}

    /** 幂等初始化：强制类加载，使上面的 DeferredRegister 声明落在登记窗口内。 */
    public static void initialize() {
        // 类初始化即完成声明；此方法只作为装配点的显式调用入口。
    }

    private static RegistrationGuard<SimpleParticleType> declare(String id, boolean alwaysShow) {
        return ModParticles.ENTRIES.declare(id, () -> new SimpleParticleType(alwaysShow));
    }

    /** 只读自检：返回全部已声明粒子类型守卫，供装配期校验使用。 */
    public static java.util.List<RegistrationGuard<? extends ParticleType<?>>> all() {
        return java.util.List.of(
                GLOW_MOTE,
                SPARKLE_STAR,
                FACING_SPRITE,
                FLOATING_CUBE,
                TRANSLUCENT_FALLING_BLOCK,
                LIGHTBEAM,
                ORBITAL);
    }
}
