/*
 * Forge's public item-stack hook runs after the complete BreakEvent dispatch.
 * Ordinary items cannot override it after an infusion changes their stack NBT.
 * Keep the original hook intact and let the bounded TC handler claim only an
 * actually infused ordinary stack. TC item overrides retain their own path.
 */
function initializeCoreMod() {
    return {
        'thaumcraft_infusion_tools': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraftforge.common.extensions.IForgeItemStack',
                'methodName': 'onBlockStartBreak',
                'methodDesc': '(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Z'
            },
            'transformer': function(method) {
                var Opcodes = Java.type('org.objectweb.asm.Opcodes');
                var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
                var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
                var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
                var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
                var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
                var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
                var FrameNode = Java.type('org.objectweb.asm.tree.FrameNode');
                var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
                var owner = 'thaumcraft/equipment/tools/ToolMining';
                var name = 'beforeVanillaBlockStart';
                var descriptor = '(Ljava/lang/Object;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Z';
                if (String(method.name) !== 'onBlockStartBreak' ||
                    String(method.desc) !== '(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Z' ||
                    (method.access & Opcodes.ACC_STATIC) !== 0) {
                    throw new Error('Thaumcraft: unexpected public Forge item-stack hook signature');
                }
                for (var node = method.instructions.getFirst(); node !== null; node = node.getNext()) {
                    if (node.getOpcode() === Opcodes.INVOKESTATIC && String(node.owner) === owner && String(node.name) === name) {
                        throw new Error('Thaumcraft: duplicate infusion item-stack hook');
                    }
                }
                var original = new LabelNode();
                var prefix = new InsnList();
                prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
                prefix.add(new VarInsnNode(Opcodes.ALOAD, 1));
                prefix.add(new VarInsnNode(Opcodes.ALOAD, 2));
                prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, descriptor, false));
                prefix.add(new JumpInsnNode(Opcodes.IFEQ, original));
                prefix.add(new InsnNode(Opcodes.ICONST_1));
                prefix.add(new InsnNode(Opcodes.IRETURN));
                prefix.add(original);
                prefix.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
                method.instructions.insert(prefix);
                method.maxStack = Math.max(method.maxStack, 3);
                ASMAPI.log('INFO', 'THAUMCRAFT_INFUSION_TOOL_BRIDGE_APPLIED: native Forge item-stack hook');
                return method;
            }
        }
    };
}
