package io.github.pataspark.tlmtetracompat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TlmTetraCompat.MOD_ID)
public class TlmTetraCompat {

    public static final String MOD_ID = "tlm_tetra_compat";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TlmTetraCompat() {
        LOGGER.info("TLM-Tetra-Compat initialized.");
    }
}
