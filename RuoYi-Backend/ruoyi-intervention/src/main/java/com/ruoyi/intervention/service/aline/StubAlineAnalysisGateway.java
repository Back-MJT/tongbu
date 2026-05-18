package com.ruoyi.intervention.service.aline;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Default A Line adapter while the real API has not been provided.
 */
@Service
public class StubAlineAnalysisGateway implements AlineAnalysisGateway
{
    private final Map<String, AlineAnalysisResult> sessions = new ConcurrentHashMap<>();

    @Override
    public AlineAnalysisResult createAnalysis(AlineAnalysisRequest request)
    {
        AlineAnalysisResult result = AlineAnalysisResult.unavailable(request);
        if (result.getSessionId() != null && !result.getSessionId().isBlank())
        {
            sessions.put(result.getSessionId(), result);
        }
        return result;
    }

    @Override
    public AlineAnalysisResult getAnalysisResult(String sessionId)
    {
        AlineAnalysisResult result = sessions.get(sessionId);
        if (result != null)
        {
            return result;
        }
        AlineAnalysisRequest request = new AlineAnalysisRequest();
        request.setSessionId(sessionId);
        return AlineAnalysisResult.unavailable(request);
    }

    @Override
    public AlineAnalysisResult getAnalysisStatus(String sessionId)
    {
        return getAnalysisResult(sessionId);
    }
}
