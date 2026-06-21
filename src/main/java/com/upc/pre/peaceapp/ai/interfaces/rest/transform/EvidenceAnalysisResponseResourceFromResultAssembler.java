package com.upc.pre.peaceapp.ai.interfaces.rest.transform;

import com.upc.pre.peaceapp.ai.domain.model.valueobjects.EvidenceAnalysisResult;
import com.upc.pre.peaceapp.ai.interfaces.rest.resources.EvidenceAnalysisResponseResource;

public class EvidenceAnalysisResponseResourceFromResultAssembler {

    public static EvidenceAnalysisResponseResource toResourceFromResult(EvidenceAnalysisResult result) {
        return new EvidenceAnalysisResponseResource(
                result.summary(),
                result.observedSignals(),
                result.requiresHumanReview(),
                result.mock()
        );
    }
}
