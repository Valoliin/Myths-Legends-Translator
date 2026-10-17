package io.github.valoliin.mythslegendstranslator.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Marque le point d’entrée ; la traduction ASM est effectuée par le plugin Mixin. */
@Pseudo
@Mixin(targets = "com.github.d0ctorleon.mythsandlegends.utils.ForceSpawningUtils", remap = false)
public abstract class ForceSpawningUtilsMixin { }
