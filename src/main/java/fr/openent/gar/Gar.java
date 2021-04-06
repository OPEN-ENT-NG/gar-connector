package fr.openent.gar;

import fr.openent.gar.controller.GarController;
import fr.openent.gar.controller.SettingController;
import fr.openent.gar.export.ExportTask;
import fr.wseduc.cron.CronTrigger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.http.BaseServer;

import java.text.ParseException;
import java.util.Arrays;

public class Gar extends BaseServer {

	public static final String GAR_ADDRESS = "openent.mediacentre";
	public static boolean demo;
	public static JsonObject CONFIG;
	public final static String AAF = "AAF";
	public final static String AAF1D = "AAF1D";

	@Override
	public void start() throws Exception {
		super.start();
		final EventBus eb = getEventBus(vertx);

		final String host = config.getString("host").split("//")[1];

		//adapt the configuration to multi-tenant
		if (config.getValue("id-ent") instanceof String) {
			config.put("id-ent", new JsonObject().put(host, config.getValue("id-ent", "")));
		}

		//default AAF only
		if (!config.containsKey("entid-sources")) {
			final JsonArray jaSources = new JsonArray();
			for (final Object id : config.getJsonObject("id-ent").getMap().values()) {
				if (!(id instanceof String)) continue;
				jaSources.add(id + "-" + AAF);
			}
			config.put("entid-sources", jaSources);
		}

		final JsonObject garRessources = config.getJsonObject("gar-ressources", new JsonObject());
		if (garRessources.containsKey("cert") || garRessources.containsKey("key")) {
			garRessources.put("domains", new JsonObject().put(host,
					new JsonObject().put("cert", garRessources.getString("cert")).put("key", garRessources.getString("key"))));
			garRessources.remove("cert");
			garRessources.remove("key");
		}

		final JsonObject garSftp = config.getJsonObject("gar-sftp", new JsonObject());
		if (garSftp.containsKey("passphrase")) {
			final JsonObject tenants = new JsonObject();
			tenants.put(config.getJsonObject("id-ent").getString(host), new JsonObject()
					.put("username", garSftp.getString("username")).put("passphrase", garSftp.getString("passphrase"))
					.put("sshkey", garSftp.getString("sshkey")).put("dir-dest", garSftp.getString("dir-dest")));

			garSftp.put("tenants", tenants);
			garSftp.remove("username");
			garSftp.remove("passphrase");
			garSftp.remove("sshkey");
			garSftp.remove("dir-dest");
		}

		addController(new GarController(vertx, config));
		addController(new SettingController(eb));

		final String exportCron = config.getString("export-cron", "");
		demo = config.getBoolean("demo", false);
		CONFIG = config;

		vertx.deployVerticle("fr.openent.gar.export.impl.ExportWorker", new DeploymentOptions().setConfig(config)
				.setIsolationGroup("mediacentre_worker_group")
				.setIsolatedClasses(Arrays.asList("fr.openent.mediacentre.export.impl.*",
						"fr.openent.mediacentre.helper.impl.*", "com.sun.org.apache.xalan.internal.xsltc.trax.*"))
				.setWorker(true));

		try{
			new CronTrigger(vertx, exportCron).schedule(new ExportTask(vertx.eventBus()));
		}catch (ParseException e) {
			log.fatal(e.getMessage(), e);
		}
	}

}
