package com.github.sejoslaw.configurabletnt;

import net.fabricmc.api.ModInitializer;

import java.util.HashSet;
import java.util.Set;

public class ConfigurableTNT implements ModInitializer {
    public static final Set<ConfiguredExplosion> EXPLOSIONS = new HashSet<>();
    public static final String MODID = "configurabletnt";

    public void onInitialize() {
        System.out.println("Registering Configurable TNT...");
    }
}
