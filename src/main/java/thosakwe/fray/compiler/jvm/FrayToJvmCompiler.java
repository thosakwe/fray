package thosakwe.fray.compiler.jvm;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import thosakwe.fray.compiler.FrayCompiler;
import thosakwe.fray.grammar.FrayParser;
import thosakwe.fray.pipeline.FrayAsset;
import thosakwe.fray.pipeline.FrayTransformer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class FrayToJvmCompiler extends FrayCompiler {
    private final JvmCompilationContext compilationContext = new JvmCompilationContext();

    public FrayToJvmCompiler(FrayAsset sourceAsset, boolean debug) {
        super("jvm", "class", sourceAsset, debug);
    }

    public ClassWriter compileClass(FrayParser.ClassDefinitionContext ctx) {
        String name = ctx.name.getText();
        ClassWriter clazz = new ClassWriter(compilationContext.getClassIndex(name));
        clazz.visit(Opcodes.V1_1, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);

        // TODO: Inheritance

        // TODO: Constructors
        // Implicit constructor
        MethodVisitor mw = clazz.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        // Push `this`
        mw.visitVarInsn(Opcodes.ALOAD, 0);
        // Invoke `super`
        mw.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mw.visitInsn(Opcodes.RETURN);
        // Max one stack elem, and one local variable
        mw.visitMaxs(1, 1);
        mw.visitEnd();

        // TODO: Class members
        // TODO: Class methods

        return clazz;
    }

    @Override
    public ClassWriter compile(FrayParser.CompilationUnitContext ctx) {
        /* Make main class:
           public class Main
              public static void main(String[] args) {
                  // ...
              }
          }
         */

        // public class Main
        ClassWriter main = new ClassWriter(0);
        main.visit(Opcodes.V1_1, Opcodes.ACC_PUBLIC, "Main", null, "java/lang/Object", null);

        // Implicit constructor
        MethodVisitor mw = main.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        // Push `this`
        mw.visitVarInsn(Opcodes.ALOAD, 0);
        // Invoke `super`
        mw.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mw.visitInsn(Opcodes.RETURN);
        // Max one stack elem, and one local variable
        mw.visitMaxs(1, 1);
        mw.visitEnd();

        // TODO: Create `main` based on existing definition
        // Otherwise, throw an error: "No top-level function 'main' declared in Fray program."
        return main;
    }

    @Override
    public FrayTransformer toTransformer() {
        return new FrayTransformer() {
            @Override
            public boolean claim(FrayAsset asset) {
                return asset.getExtension().equals("fray");
            }

            @Override
            public String getName() {
                return "Fray to JVM Compiler";
            }

            @Override
            public FrayAsset transform(FrayAsset asset) throws IOException {
                final String src = asset.readAsString();
                final FrayParser parser = parse(src);
                ClassWriter cw = compile(parser.compilationUnit());
                return asset
                        .changeInputStream(new ByteArrayInputStream(cw.toByteArray()))
                        .changeExtension(getOutputExtension());
            }
        };
    }
}
