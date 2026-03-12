package fr.openent.gar.controller;

import fr.openent.gar.export.ExportTask;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

public class TaskController extends BaseController {
	protected static final Logger log = LoggerFactory.getLogger(TaskController.class);

	final ExportTask exportTask;

	public TaskController(ExportTask exportTask) {
		this.exportTask = exportTask;
	}

	@Post("api/internal/export")
	@SecuredAction(value = "", type = ActionType.RESOURCE)
	public void export(HttpServerRequest request) {
		log.info("Triggered export task");
		exportTask.handle(0L);
		render(request, null, 202);
	}
}
