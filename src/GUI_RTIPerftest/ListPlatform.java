package GUI_RTIPerftest;

import GUI_RTIPerftest.OSType;
import java.io.InputStreamReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ListPlatform {
    private LinkedHashMap<String, List<String>> supportedPlatforms;
    private String NDDSHOME;
    private String file;
    private String ERR_PARSING_FILE;
    private boolean ready;
    private OSType detectedOS;
    private List<String> possibleOS;

    public ListPlatform(OSType _detectedOS) {
        supportedPlatforms = new LinkedHashMap<String, List<String>>();
        file = "supportedPlatforms.xml";
        ERR_PARSING_FILE = "We coulnd't retreive the supported platforms from Code Generator";
        ready = false;
        detectedOS = _detectedOS;
        possibleOS = new ArrayList<String>();
        possibleOS.addAll(Arrays.asList("C++", "C++03", "C#", "Java"));
    }

    public void setNDDSHOME(String _NDDSHOME) {
        NDDSHOME = _NDDSHOME;
    }

    // Must be called from a non-UI thread, or the executeCommand will hang
    public void getPlatform() {
        List<String> availableListPlaform = scanPlatforms();
        if (!availableListPlaform.isEmpty()) {
            try {
                // First we will need to create a temporal directory
                Path tmpPath = Files.createTempDirectory("");

                try {
                    // Run codegen in that path in order to get the platforms
                    Process proc = Runtime.getRuntime().exec(NDDSHOME + File.separator + "bin" + File.separator
                            + "rtiddsgen -printSupportedPlatforms -d " + tmpPath.toString());
                    new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    try {
                        proc.waitFor();
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                    // Open the file that has been generated and start to parse
                    // it.
                    if (!parsePlatforms(tmpPath.toString() + File.separator + file, availableListPlaform)) {
                        System.out.println(ERR_PARSING_FILE);
                        ready = false;
                    } else {
                        ready = true;
                    }

                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            } catch (IOException e) {
                System.out.println(ERR_PARSING_FILE);
            }
        }
    }

    public boolean parsePlatforms(String fileName, List<String> availableListPlaform) {
        File xmlFile = new File(fileName);
        if (!xmlFile.exists()) {
            return false;
        }

        if (!xmlFile.canRead()) {
            return false;
        }
        supportedPlatforms.clear();

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);

            doc.getDocumentElement().normalize();

            NodeList languageNodes = doc.getElementsByTagName("language");

            for (int i = 0; i < languageNodes.getLength(); i++) {
                Node languageNode = languageNodes.item(i);
                if (languageNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                if (possibleOS.contains(languageNode.getAttributes().item(0).getNodeValue())) {
                    NodeList platformNodes = languageNode.getChildNodes();
                    List<String> platforms = new ArrayList<String>();

                    for (int j = 0; j < platformNodes.getLength(); j++) {
                        Node platformNode = platformNodes.item(j);
                        if (platformNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (platformNode.getFirstChild().getNodeValue().contains(detectedOS.name())
                                && availableListPlaform.contains(platformNode.getFirstChild().getNodeValue())) {
                            platforms.add(platformNode.getFirstChild().getNodeValue());
                        }
                    }
                    supportedPlatforms.put(languageNode.getAttributes().item(0).getNodeValue(), platforms);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public List<String> getListPlaform(String language) {
        while (!ready) { // TODO max time to wait
            System.out.println("wait for platform");
        }
        if (possibleOS.contains(language)) {
            return supportedPlatforms.get(language);
        } else {
            System.out.println("The language " + language + " is not available.");
            return new ArrayList<String>();
        }
    }

    /**
     * Returns the list of installed platforms.
     *
     * The function determines the list by scanning the NDDSHOME lib directory.
     *
     * @return List<String>
     */
    private List<String> scanPlatforms() {

        List<String> listPlaform = new ArrayList<String>();
        File libsDir = new File(NDDSHOME + File.separator + "lib");
        if (libsDir.isDirectory()) {
            String[] libs = libsDir.list();

            for (int i = 0; i < libs.length; ++i) {
                // Do not add hidden files to the list
                if (libs[i].startsWith(".")) {
                    continue;
                }
                // Skip the "java" directory if present
                if (libs[i].equals("java")) {
                    continue;
                }
                listPlaform.add(libs[i]);
            }
        }
        return listPlaform;
    }

}