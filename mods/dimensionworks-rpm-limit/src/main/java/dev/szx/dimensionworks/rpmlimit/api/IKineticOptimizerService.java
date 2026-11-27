package dev.szx.dimensionworks.rpmlimit.api;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

public interface IKineticOptimizerService {
    Float cachedStress(KineticNetwork network);

    void beginStressRebuild(KineticNetwork network);

    void recordStressContribution(KineticNetwork network, KineticBlockEntity member, float contribution);

    void finishStressRebuild(KineticNetwork network, float totalStress);

    boolean updateStressContribution(KineticNetwork network, KineticBlockEntity member);

    boolean updateCapacityContribution(KineticNetwork network, KineticBlockEntity source);

    void onMemberPruned(KineticNetwork network, KineticBlockEntity member);

    void invalidate(KineticNetwork network);

    void invalidateAndRefreshFor(KineticBlockEntity member);

    void beginBatch();

    void endBatch();

    void abortBatch();
}
