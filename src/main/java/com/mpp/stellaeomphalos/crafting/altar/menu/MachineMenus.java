package com.mpp.stellaeomphalos.crafting.altar.menu;

import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModMenus;
import com.mpp.stellaeomphalos.crafting.special.WorkbenchAnchorMenu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;

import java.util.*;

public final class MachineMenus {
    private static final Map<String, RegistrationGuard<MenuType<AsterismMenu>>> TYPES =
            new LinkedHashMap<>();
    public static final RegistrationGuard<MenuType<WorkbenchAnchorMenu>> WORKBENCH;

    static {
        for (String name : List.of("asterism", "lumen_infuser", "grindwheel", "lumen_well"))
            TYPES.put(
                    name,
                    ModMenus.ENTRIES.declare(
                            name,
                            () ->
                                    IForgeMenuType.create(
                                            (id, inv, buf) ->
                                                    new AsterismMenu(id, inv, buf, name))));
        WORKBENCH =
                ModMenus.ENTRIES.declare(
                        "lumen_workbench",
                        () ->
                                IForgeMenuType.create(
                                        (id, inv, buf) ->
                                                new WorkbenchAnchorMenu(
                                                        id, inv, buf.readBlockPos())));
    }

    private MachineMenus() {}

    public static void initialize() {}

    public static MenuType<AsterismMenu> type(String kind) {
        return TYPES.get(kind).get();
    }
}
