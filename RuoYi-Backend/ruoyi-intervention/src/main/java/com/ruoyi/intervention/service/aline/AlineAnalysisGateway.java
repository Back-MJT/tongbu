package com.ruoyi.intervention.service.aline;

/**
 * B Line owned facade for future A Line body-engine analysis.
 *
 * <p>The real A Line API is not available yet. B Line depends only on this
 * interface, so a future HTTP adapter can replace the stub without changing the
 * mini-program training flow.
 */
public interface AlineAnalysisGateway
{
    AlineAnalysisResult createAnalysis(AlineAnalysisRequest request);

    AlineAnalysisResult getAnalysisResult(String sessionId);

    AlineAnalysisResult getAnalysisStatus(String sessionId);
}
