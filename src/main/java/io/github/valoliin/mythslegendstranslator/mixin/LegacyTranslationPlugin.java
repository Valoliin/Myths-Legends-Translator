package io.github.valoliin.mythslegendstranslator.mixin;

import io.github.valoliin.mythslegendstranslator.MythsLegendsTranslator;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Réécriture de liaisons Java 1.7 -> 1.8 dans ForceSpawningUtils seulement.
 * Les méthodes de validation, la sélection de SpawnDetail et les effets sont conservés.
 */
public final class LegacyTranslationPlugin implements IMixinConfigPlugin {
    private static final String OLD = "com/cobblemon/mod/common/api/spawning/SpawnBucket";
    private static final String BRIDGE = "io/github/valoliin/mythslegendstranslator/compatibility/LegacyApiBridge";
    private static final String CONFIG = "com/cobblemon/mod/common/api/spawning/preset/BestSpawnerConfig";
    private static final String SPAWNER = "com/cobblemon/mod/common/api/spawning/spawner/PlayerSpawner";
    private static final String DETAIL = "com/cobblemon/mod/common/api/spawning/detail/SpawnDetail";
    private static final String TARGET = "com.github.d0ctorleon.mythsandlegends.utils.ForceSpawningUtils";

    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) { return true; }
    @Override public void acceptTargets(Set<String> my, Set<String> other) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }

    @Override
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (!TARGET.equals(target)) return;
        int patched = 0;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("forceSpawnInternal") && !method.name.startsWith("lambda$forceSpawnInternal$")) continue;
            method.desc = changeType(method.desc);
            if (method.localVariables != null) for (LocalVariableNode local : method.localVariables) {
                local.desc = changeType(local.desc);
                if (local.signature != null) local.signature = changeType(local.signature);
            }
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof FrameNode frame) {
                    replaceFrameTypes(frame.local);
                    replaceFrameTypes(frame.stack);
                } else if (insn instanceof TypeInsnNode typeInsn && typeInsn.desc.equals(OLD)) {
                    typeInsn.desc = "java/lang/String";
                    patched++;
                } else if (insn instanceof MethodInsnNode call) {
                    String translated = bridgeMethod(call.owner, call.name, call.desc);
                    if (translated != null) {
                        // Passe implicitement le receveur original comme premier argument du helper statique.
                        call.desc = helperDescriptor(translated, call.owner, call.desc);
                        call.owner = BRIDGE;
                        call.name = translated;
                        call.setOpcode(Opcodes.INVOKESTATIC);
                        call.itf = false;
                        if (translated.equals("chooseSpawn")) method.instructions.insert(call,
                            new TypeInsnNode(Opcodes.CHECKCAST, "com/cobblemon/mod/common/api/spawning/detail/SpawnAction"));
                        patched++;
                    } else {
                        call.desc = changeType(call.desc);
                    }
                } else if (insn instanceof InvokeDynamicInsnNode dyn) {
                    dyn.desc = changeType(dyn.desc);
                    for (int i=0; i<dyn.bsmArgs.length; i++) {
                        Object arg = dyn.bsmArgs[i];
                        if (arg instanceof Type t && t.getSort() == Type.METHOD)
                            dyn.bsmArgs[i] = Type.getMethodType(changeType(t.getDescriptor()));
                        else if (arg instanceof Handle h) {
                            dyn.bsmArgs[i] = new Handle(h.getTag(), h.getOwner(), h.getName(), changeType(h.getDesc()), h.isInterface());
                        }
                    }
                }
            }
        }
        if (patched < 6) throw new IllegalStateException("Myths Translator : seulement " + patched + " appels remplacés ; version du mod inattendue");
        MythsLegendsTranslator.LOG.info("Traducteur API: {} substitutions appliquées à {}", patched, target);
    }

    private static void replaceFrameTypes(List<Object> entries) {
        if (entries == null) return;
        for (int i=0; i<entries.size(); i++) if (OLD.equals(entries.get(i)))
            entries.set(i, "java/lang/String");
    }

    private static String helperDescriptor(String helper, String owner, String original) {
        return switch (helper) {
            case "configuredBuckets" -> "(Ljava/lang/Object;)Ljava/util/List;";
            case "chooseBucket" -> "(Ljava/lang/Object;Ljava/lang/Object;Ljava/util/List;)Ljava/lang/String;";
            case "matchingSpawns" -> "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Ljava/util/List;";
            case "detailBucket" -> "(Ljava/lang/Object;)Ljava/lang/String;";
            case "chooseSpawn" -> "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;";
            case "bucketName" -> "(Ljava/lang/String;)Ljava/lang/String;";
            default -> throw new IllegalArgumentException(helper);
        };
    }
    private static String changeType(String desc) { return desc.replace("L" + OLD + ";", "Ljava/lang/String;"); }

    private static String bridgeMethod(String owner, String name, String desc) {
        if (owner.equals(CONFIG) && name.equals("getBuckets") && desc.equals("()Ljava/util/List;")) return "configuredBuckets";
        if (owner.equals(SPAWNER) && name.equals("chooseBucket") && desc.endsWith(")L" + OLD + ";")) return "chooseBucket";
        if (owner.equals(SPAWNER) && name.equals("getMatchingSpawns") && desc.contains("L" + OLD + ";")) return "matchingSpawns";
        if (owner.equals(DETAIL) && name.equals("getBucket") && desc.equals("()L" + OLD + ";")) return "detailBucket";
        if (owner.equals(DETAIL) && name.equals("choose") && desc.contains("L" + OLD + ";")) return "chooseSpawn";
        if (owner.equals(OLD) && name.equals("getName") && desc.equals("()Ljava/lang/String;")) return "bucketName";
        return null;
    }
}
