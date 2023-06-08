package club.mcams.carpet.util.AutoMixinAuditExecutor;

import club.mcams.carpet.AmsServer;
import club.mcams.carpet.util.MixinUtil;

import net.fabricmc.loader.api.FabricLoader;

public class AutoMixinAuditExecutor {
    //#if MC<=11900
    private static final String KEYWORD_PROPERTY = "carpetamsaddition.mixin_audit";
    public static void run() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment() && "true".equals(System.getProperty(KEYWORD_PROPERTY))) {
            AmsServer.LOGGER.info("Triggered auto mixin audit");
            boolean ok = MixinUtil.audit(null);
            AmsServer.LOGGER.info("Mixin audit result: " + (ok ? "successful" : "failed"));
            System.exit(ok ? 0 : 1);
        }
    }
    //#endif
}