package com.upc.pre.peaceapp.ai.domain.services;

import com.upc.pre.peaceapp.ai.domain.model.commands.AnalyzeEvidenceCommand;
import com.upc.pre.peaceapp.ai.domain.model.commands.ChatbotCommand;
import com.upc.pre.peaceapp.ai.domain.model.commands.ClassifyIncidentCommand;
import com.upc.pre.peaceapp.ai.domain.model.valueobjects.ChatbotResult;
import com.upc.pre.peaceapp.ai.domain.model.valueobjects.EvidenceAnalysisResult;
import com.upc.pre.peaceapp.ai.domain.model.valueobjects.IncidentClassificationResult;

public interface AiCommandService {
    ChatbotResult handle(ChatbotCommand command);
    IncidentClassificationResult handle(ClassifyIncidentCommand command);
    EvidenceAnalysisResult handle(AnalyzeEvidenceCommand command);
}
