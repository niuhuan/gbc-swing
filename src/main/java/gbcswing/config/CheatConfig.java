package gbcswing.config;

import gbc.Cheat;
import gbcswing.tools.FileUtils;
import gbcswing.tools.StringUtils;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class CheatConfig {

    private static final DocumentBuilderFactory dbf;
    private static final DocumentBuilder db;
    private static final TransformerFactory formerFactory;
    private static final Transformer transformer;

    static {
        try {
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            formerFactory = TransformerFactory.newInstance();
            transformer = formerFactory.newTransformer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveItems(String path, List<CheatItem> list) {
        Document doc = db.newDocument();
        Element root = doc.createElement("root");
        doc.appendChild(root);
        for (CheatItem cheatItem : list) {
            Element element = doc.createElement("cheat");
            element.setAttribute("enabled", String.valueOf(cheatItem.enabled));
            element.setAttribute("codes", StringUtils.mLines(cheatItem.codes));
            element.setAttribute("description", cheatItem.description);
            root.appendChild(element);
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            transformer.transform(new DOMSource(doc), new StreamResult(fos));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<CheatItem> loadItems(String path) {
        ArrayList<CheatItem> list = new ArrayList<CheatItem>();
        byte[] buff = FileUtils.readFile(path);
        if (buff != null) {
            ByteArrayInputStream bis = null;
            try {
                bis = new ByteArrayInputStream(buff);
                Document doc = db.parse(bis);
                NodeList els = doc.getElementsByTagName("cheat");
                E:
                for (int i = 0; i < els.getLength(); i++) {
                    Node el = els.item(i);
                    NamedNodeMap attrs = el.getAttributes();
                    Node enabledNode = attrs.getNamedItem("enabled");
                    Node codeNode = attrs.getNamedItem("codes");
                    Node descriptionNode = attrs.getNamedItem("description");
                    CheatItem cheatItem = new CheatItem();
                    cheatItem.enabled = enabledNode == null ? false : Boolean.parseBoolean(enabledNode.getNodeValue());
                    cheatItem.codes = codeNode == null ? new ArrayList<String>() : StringUtils.mLines(codeNode.getNodeValue());
                    cheatItem.description = descriptionNode == null ? "" : descriptionNode.getNodeValue();
                    for (String code : cheatItem.codes) {
                        if (Cheat.newCheat(code) == null) {
                            continue E;
                        }
                    }
                    list.add(cheatItem);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return list;
    }

}
