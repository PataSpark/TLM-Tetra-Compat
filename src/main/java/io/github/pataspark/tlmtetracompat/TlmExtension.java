package io.github.pataspark.tlmtetracompat;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;

@LittleMaidExtension
public class TlmExtension implements ILittleMaid {

    @Override
    public void addMaidTask(TaskManager manager) {
        TlmTetraCompat.LOGGER.info(
                "TLM-Tetra-Compat: TLM extension loaded."
        );

        TetraBowAttack tetraBowAttack = new TetraBowAttack();

        TlmTetraCompat.LOGGER.info(
                "TLM-Tetra-Compat: Registering task {}",
                tetraBowAttack.getUid()
        );

        manager.add(tetraBowAttack);

        // Register Tetra crossbow task
        TetraCrossbowAttack tetraCrossbowAttack =
                new TetraCrossbowAttack();

        TlmTetraCompat.LOGGER.info(
                "TLM-Tetra-Compat: Registering task {}",
                tetraCrossbowAttack.getUid()
        );

        manager.add(tetraCrossbowAttack);
    }
}