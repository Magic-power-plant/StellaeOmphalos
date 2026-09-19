package com.mpp.stellaeomphalos.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import static org.junit.jupiter.api.Assertions.*;

class ArchitectureTest {
    private static final String ROOT = "com/mpp/stellaeomphalos/";
    private static final Map<String, Integer> LAYERS = Map.ofEntries(Map.entry("core", 0), Map.entry("data", 0), Map.entry("network", 0),
            Map.entry("lumen", 1), Map.entry("constellation", 2), Map.entry("structure", 3), Map.entry("ritual", 4),
            Map.entry("crafting", 5), Map.entry("knowledge", 6), Map.entry("player", 6), Map.entry("content", 7), Map.entry("client", 8));
    @Test void bytecodeDependenciesObeyLayersAndPhysicalSide() throws Exception {
        var violations = new HashSet<String>();
        try (var files = Files.walk(Path.of("build/classes/java/main"))) {
            for (var file : files.filter(path -> path.toString().endsWith(".class")).toList()) {
                var reader = new ClassReader(Files.readAllBytes(file)); var refs = new HashSet<String>();
                reader.accept(new ClassVisitor(Opcodes.ASM9) {
                    private void descriptor(String descriptor) {
                        if (descriptor == null) return;
                        var matcher = java.util.regex.Pattern.compile("L([A-Za-z0-9_/$]+)").matcher(descriptor);
                        while (matcher.find()) refs.add(matcher.group(1));
                    }
                    @Override public void visit(int version, int access, String name, String signature, String parent, String[] interfaces) {
                        if (parent != null) refs.add(parent); refs.addAll(java.util.List.of(interfaces)); descriptor(signature);
                    }
                    @Override public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                        descriptor(desc); descriptor(signature); return null;
                    }
                    @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                        descriptor(desc); descriptor(signature);
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override public void visitTypeInsn(int opcode, String type) { refs.add(type); }
                            @Override public void visitFieldInsn(int opcode, String owner, String name, String desc) { refs.add(owner); descriptor(desc); }
                            @Override public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) { refs.add(owner); descriptor(desc); }
                            @Override public void visitLdcInsn(Object value) { if (value instanceof Type type) descriptor(type.getDescriptor()); }
                            @Override public void visitInvokeDynamicInsn(String name, String desc, org.objectweb.asm.Handle handle, Object... arguments) {
                                descriptor(desc); refs.add(handle.getOwner());
                                for (Object argument : arguments) {
                                    if (argument instanceof org.objectweb.asm.Handle method) { refs.add(method.getOwner()); descriptor(method.getDesc()); }
                                    if (argument instanceof Type type) descriptor(type.getDescriptor());
                                }
                            }
                        };
                    }
                }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                String origin = reader.getClassName().substring(ROOT.length()).split("/")[0];
                for (String ref : refs) {
                    if (!origin.equals("client") && (ref.startsWith("net/minecraft/client/") || ref.startsWith(ROOT + "client/")))
                        violations.add(reader.getClassName() + " -> " + ref);
                    if (ref.startsWith(ROOT)) {
                        String target = ref.substring(ROOT.length()).split("/")[0];
                        if (LAYERS.containsKey(origin) && LAYERS.containsKey(target) && LAYERS.get(target) > LAYERS.get(origin))
                            violations.add(origin + " -> " + target);
                        if (Set.of("knowledge", "player").contains(origin) && Set.of("knowledge", "player").contains(target) && !origin.equals(target))
                            violations.add(origin + " -> " + target);
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
    }
}
