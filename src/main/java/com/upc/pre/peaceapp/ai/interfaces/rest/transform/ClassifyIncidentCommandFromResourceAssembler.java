package com.upc.pre.peaceapp.ai.interfaces.rest.transform;

import com.upc.pre.peaceapp.ai.domain.model.commands.ClassifyIncidentCommand;
import com.upc.pre.peaceapp.ai.interfaces.rest.resources.ClassifyIncidentRequestResource;
import org.springframework.stereotype.Component;

@Component
public class ClassifyIncidentCommandFromResourceAssembler {

    public ClassifyIncidentCommand toCommand(ClassifyIncidentRequestResource resource) {
        return new ClassifyIncidentCommand(
                resource.description(),
                resource.location(),
                resource.district()
        );
    }
}
