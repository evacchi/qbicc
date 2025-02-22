package org.qbicc.plugin.nativeimage;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class FeaturePatcher {

    private static final AttachmentKey<FeaturePatcher> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final Set<String> runtimeInitializedClasses = ConcurrentHashMap.newKeySet();

    private FeaturePatcher(CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static FeaturePatcher get(CompilationContext ctxt) {
        FeaturePatcher patcher = ctxt.getAttachment(KEY);
        if (patcher == null) {
            patcher = new FeaturePatcher(ctxt);
            FeaturePatcher appearing = ctxt.putAttachmentIfAbsent(KEY, patcher);
            if (appearing != null) {
                patcher = appearing;
            }
        }
        return patcher;
    }

    void addRuntimeInitializedClass(String internalName) {
        runtimeInitializedClasses.add(internalName);
    }

    boolean isRuntimeInitializedClass(String internalName) {
        return runtimeInitializedClasses.contains(internalName);
    }
}
