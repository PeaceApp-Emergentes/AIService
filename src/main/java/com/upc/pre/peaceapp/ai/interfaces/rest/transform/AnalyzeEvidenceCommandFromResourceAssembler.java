package com.upc.pre.peaceapp.ai.interfaces.rest.transform;

import com.upc.pre.peaceapp.ai.domain.model.commands.AnalyzeEvidenceCommand;
import com.upc.pre.peaceapp.ai.interfaces.rest.resources.AnalyzeEvidenceRequestResource;
import org.springframework.stereotype.Component;

@Component
public class AnalyzeEvidenceCommandFromResourceAssembler {

    public AnalyzeEvidenceCommand toCommand(AnalyzeEvidenceRequestResource resource) {
        return new AnalyzeEvidenceCommand(
                resource.evidenceUrl(),
                resource.evidenceType(),
                resource.description()
        );
    }
}
