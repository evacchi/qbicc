package org.qbicc.plugin.llvm;

import org.qbicc.context.CompilationContext;
import org.qbicc.context.Location;
import org.qbicc.driver.Driver;
import org.qbicc.machine.tool.CCompilerInvoker;
import org.qbicc.machine.tool.CToolChain;
import org.qbicc.machine.tool.ToolMessageHandler;
import org.qbicc.machine.tool.process.InputSource;
import org.qbicc.machine.tool.process.OutputDestination;
import org.qbicc.plugin.linker.Linker;
import org.qbicc.tool.llvm.LlcInvoker;
import org.qbicc.tool.llvm.LlvmToolChain;
import org.qbicc.tool.llvm.OptInvoker;
import org.qbicc.tool.llvm.OptPass;
import org.qbicc.tool.llvm.OutputFormat;
import org.qbicc.tool.llvm.RelocationModel;
import org.qbicc.type.definition.LoadedTypeDefinition;

import java.io.IOException;
import java.nio.file.Path;

public class LLVMCompiler {
    private final LlcInvoker llcInvoker;
    private final OptInvoker optInvoker;
    private final CCompilerInvoker ccInvoker;

    public LLVMCompiler(CompilationContext context, boolean isPie) {
        llcInvoker = createLlcInvoker(context, isPie);
        optInvoker = createOptInvoker(context);
        ccInvoker = createCCompilerInvoker(context);
    }

    public void compileModule(final CompilationContext context, LoadedTypeDefinition typeDefinition, Path modulePath) {
        CToolChain cToolChain = context.getAttachment(Driver.C_TOOL_CHAIN_KEY);

        String moduleName = modulePath.getFileName().toString();
        if (moduleName.endsWith(".ll")) {
            String baseName = moduleName.substring(0, moduleName.length() - 3);
            String objectName = baseName + "." + cToolChain.getPlatform().getObjectType().objectSuffix();
            Path objectPath = modulePath.resolveSibling(objectName);

            ccInvoker.setSource(InputSource.from(modulePath));
            ccInvoker.setSourceLanguage(CCompilerInvoker.SourceLanguage.IR);
            ccInvoker.setOutputPath(objectPath);
            try {
                ccInvoker.invoke();
            } catch (IOException e) {
                context.error("Compiler invocation has failed for %s: %s", modulePath, e.toString());
                return;
            }
            Linker.get(context).addObjectFilePath(typeDefinition, objectPath);
        } else {
            context.warning("Ignoring unknown module file name \"%s\"", modulePath);
        }
    }

    private static CCompilerInvoker createCCompilerInvoker(CompilationContext context) {
        CToolChain cToolChain = context.getAttachment(Driver.C_TOOL_CHAIN_KEY);
        if (cToolChain == null) {
            context.error("No C tool chain is available");
            return null;
        }
        CCompilerInvoker ccInvoker = cToolChain.newCompilerInvoker();
        ccInvoker.setMessageHandler(ToolMessageHandler.reporting(context));
        ccInvoker.setSourceLanguage(CCompilerInvoker.SourceLanguage.ASM);
        return ccInvoker;
    }

    private static OptInvoker createOptInvoker(CompilationContext context) {
        LlvmToolChain llvmToolChain = context.getAttachment(Driver.LLVM_TOOL_KEY);
        if (llvmToolChain == null) {
            context.error("No LLVM tool chain is available");
            return null;
        }
        OptInvoker optInvoker = llvmToolChain.newOptInvoker();
        optInvoker.setMessageHandler(ToolMessageHandler.reporting(context));
        optInvoker.addOptimizationPass(OptPass.RewriteStatepointsForGc);
        optInvoker.addOptimizationPass(OptPass.AlwaysInline);
        return optInvoker;
    }

    private static LlcInvoker createLlcInvoker(CompilationContext context, boolean isPie) {
        LlvmToolChain llvmToolChain = context.getAttachment(Driver.LLVM_TOOL_KEY);
        if (llvmToolChain == null) {
            context.error("No LLVM tool chain is available");
            return null;
        }
        LlcInvoker llcInvoker = llvmToolChain.newLlcInvoker();
        llcInvoker.setMessageHandler(ToolMessageHandler.reporting(context));
        llcInvoker.setOutputFormat(OutputFormat.ASM);
        llcInvoker.setRelocationModel(isPie ? RelocationModel.Pic : RelocationModel.Static);
        return llcInvoker;
    }
}
