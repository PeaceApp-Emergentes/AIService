package com.upc.pre.peaceapp.ai.interfaces.rest.transform;

import com.upc.pre.peaceapp.ai.domain.model.valueobjects.IncidentClassificationResult;
import com.upc.pre.peaceapp.ai.interfaces.rest.resources.IncidentClassificationResponseResource;

public class IncidentClassificationResponseResourceFromResultAssembler {

    public static IncidentClassificationResponseResource toResourceFromResult(IncidentClassificationResult result) {
        return new IncidentClassificationResponseResource(
                result.incidentType(),
                result.severity(),
                result.summary(),
                result.suggestedTitle(),
                result.suggestedDescription(),
                result.recommendedActions(),
                result.valid(),
                result.mock()
        );
    }
}
