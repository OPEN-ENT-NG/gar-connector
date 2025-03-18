package fr.openent.gar.export;

import io.vertx.core.Handler;

public interface PurgeAssignmentService {
    void raPurge(final String entId, final String source, final Handler<String> handler);
    void raAssignment(final String entId, final String source, final Handler<String> handler);
    void raPurgeAssignment(final String entId, final String source, final Handler<String> handler);
}
