package fr.openent.gar.helper.impl;

import fr.openent.gar.Gar;
import fr.openent.gar.constants.GarConstants;
import fr.openent.gar.helper.XmlExportHelper;
import fr.openent.gar.utils.FileUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

public class XmlExportHelperImpl implements XmlExportHelper {

    private final Integer MAX_NODES;

    private Element currentElement = null;
    private final String ROOT;
    private Document currentDoc = null;
    private final Logger log = LoggerFactory.getLogger(XmlExportHelperImpl.class);
    private int nbElem = 0;
    private int fileIndex = 0;
    private final String exportDir;
    private final String FILE_PREFIX;
    private final JsonArray fileList;
    private final String source;

    /**
     * Initialize helper and first xml
     * @param config vertx container, for logger and config
     * @param root name of the root xml element
     * @param fileParamName param for the name of xml file
     */
    public XmlExportHelperImpl(final String entId, final String source, JsonObject config, String root, String fileParamName, String strDate) {
        ROOT = root;
        this.source = source;
        initNewFile();
        MAX_NODES = config.getInteger("max-nodes", 10000);
        if (Gar.AAF.equals(source)) {
            exportDir = FileUtils.appendPath(config.getString("export-path", ""), entId);
        } else {
            exportDir = FileUtils.appendPath(config.getString("export-path"), entId + GarConstants.EXPORT_1D_SUFFIX);
        }
        FILE_PREFIX = entId + "_GAR-ENT_Complet_" + strDate + getLevelBySource(source) + fileParamName + "_";
        fileList = new fr.wseduc.webutils.collections.JsonArray();
    }

    /**
     * get level from source
     * @param source AFF or AFF1D
     * @return level
     */
    private String getLevelBySource(String source) {
        String level = "2D_";
        if (Gar.AAF1D.equals(source)) {
            level = "1D_";
        }
        return level;
    }

    /**
     * Add static attributes to xml root node
     */
    private void addAttributesToRootNode() {
        if (Gar.AAF1D.equals(this.source)) {
            currentElement.setAttribute("xmlns:men", "http://data.education.fr/ns/gar/1d");
        } else {
            currentElement.setAttribute("xmlns:men", "http://data.education.fr/ns/gar");
        }
        currentElement.setAttribute("xmlns:xalan", "http://xml.apache.org/xalan");
        currentElement.setAttribute("xmlns:xslFormatting", "urn:xslFormatting");
        currentElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        currentElement.setAttribute("Version", "1.0");

        if (Gar.AAF1D.equals(this.source)) {
            currentElement.setAttribute("xsi:schemaLocation", "http://data.education.fr/ns/gar/1d GAR-ENT-1D.xsd");
        } else {
            currentElement.setAttribute("xsi:schemaLocation", "http://data.education.fr/ns/gar GAR-ENT.xsd");
        }
    }

    /**
     * Get list of files created by exporter
     * @return JsonArray of Strings
     */
    @Override
    public JsonArray getFileList() {
        return fileList;
    }

    /**
     * Close current xml file and save to disk
     */
    @Override
    public void closeFile() {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(currentDoc);
            String filename = getExportFileName(fileIndex);
            String pathFile = FileUtils.appendPath(exportDir, filename);
            StreamResult result = new StreamResult(new File(pathFile));
            transformer.transform(source, result);
            fileList.add(pathFile);
            log.info(filename + " saved");
        } catch (TransformerException tfe) {
            log.error(tfe.getMessage());
        }
    }

    /**
     * Save Json object to xml
     * @param key key of xml node
     * @param entry tree content of xml node as JsonObject
     */
    @Override
    public void saveObject(String key, JsonObject entry) {
        saveObject(key, entry, currentElement);
        testNumberOfOccurrences();
    }

    /**
     * Save Json object to xml
     * @param key key of xml node
     * @param entry tree content of xml node as JsonObject
     * @param curElt parent element for new xml node
     */
    private void saveObject(String key, JsonObject entry, Element curElt) {
        Element objectElement = currentDoc.createElement(key);
        for(String jsonKey : entry.fieldNames()) {
            Object o = entry.getValue(jsonKey);
            if ( o==null ) continue;
            saveUnknownObject(jsonKey, o, objectElement);
        }
        curElt.appendChild(objectElement);
        nbElem++;
    }

    /**
     * Save String to xml
     * @param key key of xml node
     * @param value string value of xml node
     * @param curElt parent element for new xml node
     */
    private void saveString(String key, String value, Element curElt) {
        Element elem = currentDoc.createElement(key);
        elem.appendChild(currentDoc.createTextNode(value));
        curElt.appendChild(elem);
        nbElem++;
    }

    /**
     * Save Json array to xml
     * @param key key of xml nodes
     * @param array array of values to save as multiple nodes
     * @param curElt parent element for new xml nodes
     */
    private void saveArray(String key, JsonArray array, Element curElt) {
        for(Object o : array) {
            saveUnknownObject(key, o, curElt);
        }
    }

    /**
     * Save undetermined object to xml
     * Identify correct class and call appropriate function
     * @param key key of xml nodes
     * @param o unknown object to save
     * @param curElt parent element for new xml nodes
     */
    private void saveUnknownObject(String key, Object o, Element curElt) {
        if(o instanceof String) {
            saveString(key, (String)o, curElt);
        }
        if(o instanceof JsonObject) {
            saveObject(key, (JsonObject)o, curElt);
        }
        if(o instanceof JsonArray) {
            saveArray(key, (JsonArray)o, curElt);
        }
        if(o instanceof Number) {
            String strNumber = o.toString();
            saveString(key, strNumber, curElt);
        }
    }

    /**
     * Init new xml
     */
    private void initNewFile() {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        // root elements
        assert docBuilder != null;
        currentDoc = docBuilder.newDocument();
        currentElement = currentDoc.createElement(ROOT);
        currentDoc.appendChild(currentElement);
        nbElem = 1;
        addAttributesToRootNode();
    }

    /**
     * Test if MAX_NODE is reached
     * Then close the file and open a new one
     */
    private void testNumberOfOccurrences() {
        if (nbElem >= MAX_NODES) {
            closeFile();
            fileIndex++;
            initNewFile();
        }
    }

    /**
     * @param fileIndex : it is a number put at the end
     * @return name of the file to save
     */
    private String getExportFileName(int fileIndex){
        String formattedIndex = String.format ("%04d", fileIndex);
        return FILE_PREFIX + formattedIndex + ".xml";
    }

}
