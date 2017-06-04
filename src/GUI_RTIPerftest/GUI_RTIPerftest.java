package GUI_RTIPerftest;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Arrays;

public class GUI_RTIPerftest {

    /**
     * types of Operating Systems
     */
    private enum OSType {
        Windows, Darwin, Linux, Other
    };

    /**
     * types of Supported Languages
     */
    private enum Language {
        cpp, cpp03, cs, java
    };

    private static OSType detectedOS; // cached result of OS detection
    private Shell shell;
    private TabFolder folder;
    private ArrayList<Text> listTextParameter; // create list of text elements
    private Map<String, String> mapParameter;// create dictionary with parameter
    private String[] possiblePlatform;
    private Display display;
    private Map<String, String> listDurability;
    private String[] listFlowController;

    /**
     * Constructor
     */
    public GUI_RTIPerftest(Display _display) {
        display = _display;
        set_all_possible_platform();
        listTextParameter = new ArrayList<Text>();
        set_listDurability();
        listFlowController = new String[] { "default", "10Gbps", "1Gbps" };
        mapParameter = new HashMap<String, String>();
        shell = new Shell(display, SWT.SHELL_TRIM | SWT.CENTER);
        folder = new TabFolder(shell, SWT.NONE);
        initUI(display);
    }

    /**
     * Fill the list of Durability
     */
    private void set_listDurability() {
        listDurability = new HashMap<String, String>();
        listDurability.put("0 - VOLATILE (Default)", "0");
        listDurability.put("1 - TRANSIENT LOCAL", "1");
        listDurability.put("2 - TRANSIENT", "2");
        listDurability.put("3 - PERSISTENT", "3");
    }

    /**
     * Set the list of Platform according to the OS
     */
    private String get_paramenter(String parameter) {
        if (!mapParameter.containsKey(parameter)) {
            return "";
        } else {
            return mapParameter.get(parameter);
        }
    }

    /**
     * Set the list of Platform according to the OS
     */
    private void set_all_possible_platform() {
        if (getOperatingSystemType() == OSType.Linux) {
            possiblePlatform = new String[] { "", "x64Linux3gcc5.4.0", "x64Linux3gcc4.8.2" };
        } else if (getOperatingSystemType() == OSType.Windows) {
            possiblePlatform = new String[] { "", "x64Win64VS2015" };
        } else if (getOperatingSystemType() == OSType.Darwin) {
            possiblePlatform = new String[] { "", "x64Darwin16clang8.0" };
        } else {
            possiblePlatform = new String[] { "", "x64Win64VS2015", "x64Darwin16clang8.0", "x64Linux3gcc5.4.0",
                    "x64Linux3gcc4.8.2" };
        }
        Arrays.sort(possiblePlatform);
    }

    /**
     * Detect the operating system from the os.name System property and cache
     * the result
     *
     * @returns - the operating system detected
     */
    private OSType getOperatingSystemType() {
        if (detectedOS == null) {
            String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
            if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
                detectedOS = OSType.Darwin;
            } else if (OS.indexOf("win") >= 0) {
                detectedOS = OSType.Windows;
            } else if (OS.indexOf("nux") >= 0) {
                detectedOS = OSType.Linux;
            } else {
                detectedOS = OSType.Other;
            }
        }
        return detectedOS;
    }

    /**
     * Clean all the input. The List of output and the list of text
     * 
     * @param listOutput
     * @param texts
     */
    private void cleanInput(List listOutput, ArrayList<Text> listText) {
        listOutput.removeAll();
        for (int i = 0; i < listText.size(); i++) {
            listText.get(i).setText("");
        }
    }

    /**
     * Run the command compile in the OS
     *
     * @param textCommand
     * @param listOutput
     *
     * @returns - True if the commands works, False if the OS is not found
     */
    private Boolean compile(Text textCommand, List listOutput) {
        listOutput.removeAll();
        // create parameter
        String command = get_paramenter("--nddshome");
        command += get_paramenter("--platform");
        command += get_paramenter("--skip-cpp-build");
        command += get_paramenter("--skip-cpp03-build");
        command += get_paramenter("--skip-java-build");
        command += get_paramenter("--debug");
        command += get_paramenter("--secure");
        command += get_paramenter("--openssl-home");

        // check if Linux or Win or Darwin
        if (getOperatingSystemType() == OSType.Linux || get_paramenter("--platform").toLowerCase().contains("linux")) {
            command = get_paramenter("Perftest") + "/build.sh" + command;
        } else if (getOperatingSystemType() == OSType.Windows
                || get_paramenter("--platform").toLowerCase().contains("win")) {
            command = get_paramenter("Perftest") + "/build.bat" + command;
            command += get_paramenter("--skip-cs-build");
            // C# just in win
        } else if (getOperatingSystemType() == OSType.Darwin
                || get_paramenter("--platform").toLowerCase().contains("darwin")) {
            command = get_paramenter("Perftest") + "/build.sh" + command;
        } else {
            return false;
        }

        // print command to run
        System.out.println(command);
        textCommand.setText(command);

        try {
            Process proc = Runtime.getRuntime().exec(command);
            BufferedReader read = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            try {
                proc.waitFor();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
                listOutput.add(e.getMessage());
            }
            while (read.ready()) {
                String output = read.readLine();
                System.out.println(output);
                listOutput.add(output);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            listOutput.add(e.getMessage());
        }
        return true;
    }

    /**
     * Run the command compile in the OS
     *
     * @param textCommand
     * @param listOutput
     *
     * @returns - True if the commands works, False if the OS is not found
     */
    private Boolean execute(Text textCommand, List listOutput, Language language) {
        listOutput.removeAll();
        // create parameter
        String command = get_paramenter("Perftest") + "/bin/";

        // check if Linux or Win or Darwin
        if (getOperatingSystemType() == OSType.Linux || get_paramenter("--platform").toLowerCase().contains("linux")) {
            switch (language) {
            case cpp:
                command += get_paramenter("platform") + "/release/perftest_cpp";
                break;
            case cpp03:
                command += get_paramenter("platform") + "/release/perftest_cpp03";
                break;
            case cs:
                return false;
            case java:
                command += "Release/perftest_java.sh";
                break;
            }
        } else if (getOperatingSystemType() == OSType.Windows
                || get_paramenter("--platform").toLowerCase().contains("win")) {
            switch (language) {
            case cpp:
                command += get_paramenter("platform") + "/release/perftest_cpp";
                break;
            case cpp03:
                command += get_paramenter("platform") + "/release/perftest_cpp03";
                break;
            case cs:
                command += get_paramenter("platform") + "/release/perftest_cs";
                break;
            case java:
                command += "Release/perftest_java.bat";
                break;
            }
        } else if (getOperatingSystemType() == OSType.Darwin
                || get_paramenter("--platform").toLowerCase().contains("darwin")) {
            switch (language) {
            case cpp:
                command += get_paramenter("platform") + "/release/perftest_cpp03";
                break;
            case cpp03:
                command += get_paramenter("platform") + "/release/perftest_cpp";
                break;
            case cs:
                return false;
            case java:
                command += "Release/perftest_java.bat";
                break;
            }
        } else {
            return false;
        }

        command += get_paramenter("-domain");
        command += get_paramenter("-bestEffort");
        command += get_paramenter("-dataLen");
        command += get_paramenter("-keyed");
        command += get_paramenter("-instances");
        command += get_paramenter("-enableSharedMemory");
        command += get_paramenter("-enableTcp");
        command += get_paramenter("-dynamicData");
        command += get_paramenter("-durability");
        command += get_paramenter("-multicast");
        command += get_paramenter("-multicastAddress");
        command += get_paramenter("-nic");
        command += get_paramenter("-multicast");
        command += get_paramenter("-multicastAddress");
        command += get_paramenter("-useReadThread");
        command += get_paramenter("-flowController");
        command += get_paramenter("-cpu");
        command += get_paramenter("-peer");
        command += " -noXmlQos";// always use the QoS from the String

        // pub
        command += get_paramenter("-pub");
        command += get_paramenter("-batchSize");
        command += get_paramenter("-enableAutoThrottle");
        command += get_paramenter("-enableTurboMode");
        command += get_paramenter("-executionTime");
        command += get_paramenter("-heartbeatPeriod");
        command += get_paramenter("-latencyCount");
        command += get_paramenter("-latencyTest");
        command += get_paramenter("-numIter");
        command += get_paramenter("-numSubscribers");
        command += get_paramenter("-pidMultiPubTest");
        command += get_paramenter("-pubRate");
        command += get_paramenter("-scan");
        command += get_paramenter("-sendQueueSize");
        command += get_paramenter("-sleep");
        command += get_paramenter("-spin");
        command += get_paramenter("-writerStats");

        // Sub
        command += get_paramenter("-sub");
        command += get_paramenter("-numPublishers");
        command += get_paramenter("-sidMultiSubTest");

        // print command to run
        System.out.println(command);
        textCommand.setText(command);

        try {
            Process proc = Runtime.getRuntime().exec(command);
            BufferedReader read = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            try {
                proc.waitFor();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
                listOutput.add(e.getMessage());
            }
            while (read.ready()) {
                String output = read.readLine();
                System.out.println(output);
                listOutput.add(output);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            listOutput.add(e.getMessage());
        }
        return true;
    }

    /**
     * Run the command compile --clean in the OS
     *
     * @param textCommand
     * @param listOutput
     *
     * @returns - True if the commands works, False if the OS is not found
     */
    private Boolean compile_clean(Text textCommand, List listOutput) {
        listOutput.removeAll();
        String command = "";
        // check if Linux or Win or Darwin
        if (getOperatingSystemType() == OSType.Linux || get_paramenter("--platform").toLowerCase().contains("linux")) {
            command = get_paramenter("Perftest") + "/build.sh --clean";
        } else if (getOperatingSystemType() == OSType.Windows
                || get_paramenter("--platform").toLowerCase().contains("win")) {
            command = get_paramenter("Perftest") + "/build.bat --clean";
        } else if (getOperatingSystemType() == OSType.Darwin
                || get_paramenter("--platform").toLowerCase().contains("darwin")) {
            command = get_paramenter("Perftest") + "/build.sh --clean";
        } else {
            return false;
        }

        // print command to run
        System.out.println(command);
        textCommand.setText(command);
        try {
            Process proc = Runtime.getRuntime().exec(command);
            BufferedReader read = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            try {
                proc.waitFor();
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
            while (read.ready()) {
                String output = read.readLine();
                System.out.println(output);
                listOutput.add(output);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return true;
    }

    /**
     * Run the GUI with two tabs
     * 
     * @param display
     *
     */
    private void initUI(Display display) {
        shell.setLayout(new FillLayout());
        display_tab_compile(); // Tab 1 (compile)
        display_tab_execution(); // Tab 2 (execute)
        shell.open();
        shell.pack();
        shell.setText("RTI perftest");
        shell.setSize(900, 900);

        shell.addListener(SWT.Traverse, new Listener() {
            public void handleEvent(Event event) {
                switch (event.detail) {
                case SWT.TRAVERSE_ESCAPE:
                    shell.close();
                    event.detail = SWT.TRAVERSE_NONE;
                    event.doit = false;
                    break;
                }
            }
        });

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    /**
     * Display an error in another windows
     * 
     * @param shell
     * @param message
     */
    private void show_error(String message) {
        MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING);
        messageBox.setText("Warning");
        messageBox.setMessage(message);
        messageBox.open();
        System.out.println(message);
    }

    /**
     * Display the tab of the compile
     *
     */
    private void display_tab_compile() {
        TabItem tabCompile = new TabItem(folder, SWT.NONE);
        tabCompile.setText("compile");

        Composite compositeCompile = new Composite(folder, SWT.NONE);
        compositeCompile.setLayout(new GridLayout(2, false));
        tabCompile.setControl(compositeCompile);

        Group groupGeneral = new Group(compositeCompile, SWT.NONE);
        groupGeneral.setLayout(new GridLayout(2, false));
        groupGeneral.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 3));

        // Perftest path
        Label labelPerftest = new Label(groupGeneral, SWT.NONE);
        labelPerftest.setText("Perftest path");
        labelPerftest.setToolTipText("Path to the RTI perftest bundle");
        Text textPerftest = new Text(groupGeneral, SWT.BORDER);
        textPerftest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textPerftest);
        textPerftest.setText("/home/ajimenez/github/rtiperftest_test"); // TODO
                                                                        // delete

        // NDDSHOME
        Label labelNDDSHOME = new Label(groupGeneral, SWT.NONE);
        labelNDDSHOME.setText("NDDSHOME");
        labelNDDSHOME.setToolTipText(
                "Path to the RTI Connext DDS installation. If this parameter is not present, the $NDDSHOME variable should be.");
        Text textNDDSHOME = new Text(groupGeneral, SWT.BORDER);
        textNDDSHOME.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textNDDSHOME);
        textNDDSHOME.setText("/home/ajimenez/rti_connext_dds-5.2.7");

        // Platform
        Label labelPlaform = new Label(groupGeneral, SWT.NONE);
        labelPlaform.setText("Plaform");
        labelPlaform.setToolTipText("Architecture/Platform for which build.sh is going to compile RTI Perftest.");
        Combo comboPlaform = new Combo(groupGeneral, SWT.READ_ONLY);
        comboPlaform.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        comboPlaform.setItems(possiblePlatform);
        comboPlaform.setText(possiblePlatform[possiblePlatform.length - 1]);

        // four radio for the languages
        Group groupLanguage = new Group(compositeCompile, SWT.NONE);
        groupLanguage.setLayout(new GridLayout(4, true));
        groupLanguage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        groupLanguage.setText("Language to compile");
        Button language_cpp = new Button(groupLanguage, SWT.CHECK);
        language_cpp.setText("CPP");
        language_cpp.setSelection(true);
        Button language_cpp03 = new Button(groupLanguage, SWT.CHECK);
        language_cpp03.setText("CPP03");
        Button language_cs = new Button(groupLanguage, SWT.CHECK);
        language_cs.setText("C#");
        Button language_java = new Button(groupLanguage, SWT.CHECK);
        language_java.setText("JAVA");

        // two radio button for the linker and one check for the debug
        Group groupLinker = new Group(compositeCompile, SWT.NONE);
        groupLinker.setLayout(new GridLayout(3, false));
        groupLinker.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        groupLinker.setText("Linker");
        Button linkerStatic = new Button(groupLinker, SWT.RADIO);
        linkerStatic.setText("Static linked");
        linkerStatic.setToolTipText("Compile using the RTI Connext DDS Static libraries.");
        linkerStatic.setSelection(true);
        Button linkerDynamic = new Button(groupLinker, SWT.RADIO);
        linkerDynamic.setToolTipText("Compile using the RTI Connext DDS Dynamic libraries.");
        linkerDynamic.setText("Dynamic linked");
        Button debug = new Button(groupLinker, SWT.CHECK);
        debug.setToolTipText("Compile using the RTI Connext DDS debug libraries.");
        debug.setText("Debug libraries");

        // Security and OpenSSL
        Group groupSecurity = new Group(compositeCompile, SWT.NONE);
        groupSecurity.setLayout(new GridLayout(3, false));
        groupSecurity.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        groupSecurity.setText("Security");
        Label labelOpenSSL = new Label(groupSecurity, SWT.NONE);
        labelOpenSSL.setText("Path to the openSSL");
        labelOpenSSL.setToolTipText(
                "Path to the openSSL home directory. Needed when compiling using the --secure option and statically.");
        Text textOpenSSL = new Text(groupSecurity, SWT.BORDER);
        textOpenSSL.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textOpenSSL);
        Button security = new Button(groupSecurity, SWT.CHECK);
        security.setToolTipText(
                "Enable the compilation of the Perfest code specific for security and adds the RTI Connext DDS Security Libraries in the linking step (if compiling statically). ");
        security.setText("Enable security");

        // two buttons for compile and clean
        Button btnCompile = new Button(compositeCompile, SWT.PUSH);
        btnCompile.setText("Compile");
        btnCompile.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Button btnClean = new Button(compositeCompile, SWT.PUSH);
        btnClean.setText("Clean");
        btnClean.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        // text command
        Text textCommand = new Text(compositeCompile, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL);
        textCommand.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        textCommand.computeSize(100, 100, true);
        listTextParameter.add(textCommand);

        // text output command
        List listOutput = new List(compositeCompile, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        listOutput.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

        // listener button compile
        btnCompile.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                System.out.println("Button compile clicked");
                if (textPerftest.getText().replaceAll("\\s+", "").equals("")) {
                    show_error("The path to the build of perftest is necessary.");
                    return;
                }
                mapParameter.put("Perftest", textPerftest.getText().replaceAll("\\s+", ""));

                if (!textNDDSHOME.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("--nddshome", " --nddshome " + textNDDSHOME.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("--nddshome", "");
                }
                if (!comboPlaform.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("--platform", " --platform " + comboPlaform.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("--platform", "");
                }
                if (!language_cpp.getSelection()) {
                    mapParameter.put("--skip-cpp-build", " --skip-cpp-build");
                } else {
                    mapParameter.put("--skip-cpp-build", "");
                }
                if (!language_cpp03.getSelection()) {
                    mapParameter.put("--skip-cpp03-build", " --skip-cpp03-build");
                } else {
                    mapParameter.put("--skip-cpp03-build", "");
                }
                if (!language_cs.getSelection()) {
                    mapParameter.put("--skip-cs-build", " --skip-cs-build");
                } else {
                    mapParameter.put("--skip-cs-build", "");
                }
                if (!language_java.getSelection()) {
                    mapParameter.put("--skip-java-build", " --skip-java-build");
                } else {
                    mapParameter.put("--skip-java-build", "");
                }
                if (linkerDynamic.getSelection()) {
                    mapParameter.put("--dynamic", " --dynamic");
                } else {
                    mapParameter.put("--dynamic", "");
                }
                if (debug.getSelection()) {
                    mapParameter.put("--debug", " --debug");
                } else {
                    mapParameter.put("--debug", "");
                }
                if (security.getSelection()) {
                    if (linkerDynamic.getSelection()) {
                        show_error("Secure and dynamic library together is not sopported");
                        return;
                    } else {
                        mapParameter.put("--secure", " --secure");
                    }
                } else {
                    mapParameter.put("--secure", "");
                }
                if (!textOpenSSL.getText().replaceAll("\\s+", "").equals("")) {
                    if (!linkerDynamic.getSelection() && security.getSelection()) {
                        // static and secure
                        mapParameter.put("--openssl-home",
                                " --openssl-home " + textOpenSSL.getText().replaceAll("\\s+", ""));
                    } else {
                        show_error("OpenSSL needs when compiling using the secure option and statically linker.");
                        return;
                    }
                } else {
                    mapParameter.put("--openssl-home", "");
                }
                // TODO cleanInput(listOutput,listTextCompile);
                if (!compile(textCommand, listOutput)) {
                    show_error("You must specify a correct platform");
                    return;
                }
            }
        });

        // listener button compile clean
        btnClean.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                System.out.println("Button compile clean clicked");
                if (textPerftest.getText().replaceAll("\\s+", "").equals("")) {
                    show_error("The path to the build of perftest is necessary.");
                    return;
                }
                mapParameter.put("Perftest", textPerftest.getText().replaceAll("\\s+", ""));
                // TODO cleanInput(listOutput,listTextCompile);
                if (!compile_clean(textCommand, listOutput)) {
                    show_error("You must specify a correct platform");
                    return;
                }
            }
        });
    }

    /**
     * Display new window with the Advanced Option of the Subscriber
     *
     */
    private void display_sub_advanced_option() {

        Shell shellAdvancedOptionSub = new Shell(display, SWT.CLOSE);
        shellAdvancedOptionSub.setText("Subscriber Advanced Option");
        shellAdvancedOptionSub.setLayout(new GridLayout(2, false));

        // numPublishers
        Label labelNumPublishers = new Label(shellAdvancedOptionSub, SWT.NONE);
        labelNumPublishers.setText("Number of publisher");
        labelNumPublishers.setToolTipText(
                "The subscribing application will wait for this number of publishing applications to start.");
        Text textNumPublishers = new Text(shellAdvancedOptionSub, SWT.BORDER);
        textNumPublishers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textNumPublishers);
        textNumPublishers.setText("1");

        // sidMultiSubTest
        Label labelSidMultiSubTest = new Label(shellAdvancedOptionSub, SWT.NONE);
        labelSidMultiSubTest.setText("ID of Subscriber");
        labelSidMultiSubTest.setToolTipText("ID of the subscriber in a multi-subscriber test.");
        Text textSidMultiSubTest = new Text(shellAdvancedOptionSub, SWT.BORDER);
        textSidMultiSubTest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textSidMultiSubTest);
        textSidMultiSubTest.setText("0");

        shellAdvancedOptionSub.open();
        shellAdvancedOptionSub.pack();
        shellAdvancedOptionSub.addListener(SWT.Close, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (!textNumPublishers.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-numPublishers",
                            " -numPublishers " + textNumPublishers.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-numPublishers", "");
                }
                if (!textSidMultiSubTest.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-sidMultiSubTest",
                            " -sidMultiSubTest " + textSidMultiSubTest.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-sidMultiSubTest", "");
                }
                shellAdvancedOptionSub.dispose();
            }
        });
    }

    /**
     * Display new window with the Advanced Option of the Publisher
     *
     */
    private void display_pub_advanced_option() {

        Shell shellAdvancedOptionPub = new Shell(display, SWT.CLOSE);
        shellAdvancedOptionPub.setText("Subscriber Advanced Option");
        shellAdvancedOptionPub.setLayout(new GridLayout(3, false));

        // NumIter LatencyCount LatencyTest Scan writerStats
        Group groupLatencyTest = new Group(shellAdvancedOptionPub, SWT.NONE);
        groupLatencyTest.setLayout(new GridLayout(7, false));
        groupLatencyTest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
        Label labelNumIter = new Label(groupLatencyTest, SWT.NONE);
        labelNumIter.setText("Number of samples");
        labelNumIter.setToolTipText("Number of samples to send.");
        Text textNumIter = new Text(groupLatencyTest, SWT.BORDER);
        textNumIter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textNumIter);
        Label labelLatencyCount = new Label(groupLatencyTest, SWT.NONE);
        labelLatencyCount.setText("Latency Count");
        labelLatencyCount.setToolTipText("Number samples to send before a latency ping packet is sent.");
        Text textLatencyCount = new Text(groupLatencyTest, SWT.BORDER);
        textLatencyCount.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textLatencyCount);
        Button latencyTest = new Button(groupLatencyTest, SWT.CHECK);
        latencyTest.setText("Latency Test");
        latencyTest.setToolTipText("Run a latency test consisting of a ping-pong.");
        Button scan = new Button(groupLatencyTest, SWT.CHECK);
        scan.setText("scan");
        scan.setToolTipText("Run test in scan mode, traversing a range of sample data sizes from 32 to 63,000 bytes.");
        Button writerStats = new Button(groupLatencyTest, SWT.CHECK);
        writerStats.setText("writerStats");
        writerStats.setToolTipText(
                "Enable extra messages showing the Pulled Sample Count of the Writer in the Publisher side.");

        // BatchSize
        Group groupBatchSize = new Group(shellAdvancedOptionPub, SWT.NONE);
        groupBatchSize.setLayout(new GridLayout(2, false));
        groupBatchSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelBatchSize = new Label(groupBatchSize, SWT.NONE);
        labelBatchSize.setText("Batch Size");
        labelBatchSize.setToolTipText("Enable batching and set the maximum batched message size.");
        Text textBatchSize = new Text(groupBatchSize, SWT.BORDER);
        textBatchSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textBatchSize);

        // Execution Time
        Group groupExecuteionTime = new Group(shellAdvancedOptionPub, SWT.NONE);
        groupExecuteionTime.setLayout(new GridLayout(2, false));
        groupExecuteionTime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelExecuteionTime = new Label(groupExecuteionTime, SWT.NONE);
        labelExecuteionTime.setText("Execution Time");
        labelExecuteionTime.setToolTipText(
                "Allows you to limit the test duration by specifying the number of seconds to run the test.");
        Text textExecuteionTime = new Text(groupExecuteionTime, SWT.BORDER);
        textExecuteionTime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        textExecuteionTime.setText("120");
        listTextParameter.add(textExecuteionTime);

        // SendQueueSize
        Group groupSendQueueSize = new Group(shellAdvancedOptionPub, SWT.NONE);
        groupSendQueueSize.setLayout(new GridLayout(2, false));
        groupSendQueueSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelSendQueueSize = new Label(groupSendQueueSize, SWT.NONE);
        labelSendQueueSize.setText("Send Queue Size");
        labelSendQueueSize.setToolTipText("Size of the send queue.");
        Text textSendQueueSize = new Text(groupSendQueueSize, SWT.BORDER);
        textSendQueueSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textSendQueueSize);

        // HeartbeatPeriod
        Group groupHeartbeatPeriod = new Group(shellAdvancedOptionPub, SWT.NONE);
        groupHeartbeatPeriod.setLayout(new GridLayout(2, false));
        groupHeartbeatPeriod.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelHeartbeatPeriod = new Label(groupHeartbeatPeriod, SWT.NONE);
        labelHeartbeatPeriod.setText("Heartbeat Period");
        labelHeartbeatPeriod.setToolTipText("The period at which the publishing application will send heartbeats.");
        Text textHeartbeatPeriod = new Text(groupHeartbeatPeriod, SWT.BORDER);
        textHeartbeatPeriod.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        textHeartbeatPeriod.setText("0:10000000");
        listTextParameter.add(textHeartbeatPeriod);

        // two check button EnableAutoThrottle EnableTurboMode
        Group groupEnableAutoThrottleEnableTurboMode = new Group(shellAdvancedOptionPub, SWT.NONE);
        groupEnableAutoThrottleEnableTurboMode.setLayout(new GridLayout(2, false));
        groupEnableAutoThrottleEnableTurboMode.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Button enableAutoThrottle = new Button(groupEnableAutoThrottleEnableTurboMode, SWT.CHECK);
        enableAutoThrottle.setText("EnableAutoThrottle");
        enableAutoThrottle.setToolTipText("Enable the Auto Throttling feature.");
        Button enableTurboMode = new Button(groupEnableAutoThrottleEnableTurboMode, SWT.CHECK);
        enableTurboMode.setText("EnableTurboMode");
        enableTurboMode.setToolTipText("Enables the Turbo Mode feature.");

        // PidMultiPubTest
        Group groupPidMultiPubTest = new Group(shellAdvancedOptionPub, SWT.NONE);
        groupPidMultiPubTest.setLayout(new GridLayout(2, false));
        groupPidMultiPubTest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelPidMultiPubTest = new Label(groupPidMultiPubTest, SWT.NONE);
        labelPidMultiPubTest.setText("ID of Subscriber");
        labelPidMultiPubTest.setToolTipText("ID of the subscriber in a multi-subscriber test.");
        Text textPidMultiPubTest = new Text(groupPidMultiPubTest, SWT.BORDER);
        textPidMultiPubTest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textPidMultiPubTest);
        textPidMultiPubTest.setText("0");

        // PubRate Sleep Spin
        Group groupPubRate = new Group(shellAdvancedOptionPub, SWT.NONE);
        groupPubRate.setLayout(new GridLayout(6, false));
        groupPubRate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        Label labelPubRate = new Label(groupPubRate, SWT.NONE);
        labelPubRate.setText("Publication Rate");
        labelPubRate.setToolTipText("Limit the throughput to the specified number of samples per second.");
        Text textPubRate = new Text(groupPubRate, SWT.BORDER);
        textPubRate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textPubRate);
        Label labelSleep = new Label(groupPubRate, SWT.NONE);
        labelSleep.setText("Sleep");
        labelSleep.setToolTipText("Time to sleep between each send.");
        Text textSleep = new Text(groupPubRate, SWT.BORDER);
        textSleep.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textSleep);
        Label labelSpin = new Label(groupPubRate, SWT.NONE);
        labelSpin.setText("Spin");
        labelSpin.setToolTipText("Number of times to run in a spin loop between each send.");
        Text textSpin = new Text(groupPubRate, SWT.BORDER);
        textSpin.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textSpin);

        // NumSubscribers
        Group groupNumSubscribers = new Group(shellAdvancedOptionPub, SWT.NONE);
        groupNumSubscribers.setLayout(new GridLayout(2, false));
        groupNumSubscribers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelNumSubscribers = new Label(groupNumSubscribers, SWT.NONE);
        labelNumSubscribers.setText("Number of Subscribers");
        labelNumSubscribers.setToolTipText(
                "The subscribing application will wait for this number of publishing applications to start.");
        Text textNumSubscribers = new Text(groupNumSubscribers, SWT.BORDER);
        textNumSubscribers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textNumSubscribers);
        textNumSubscribers.setText("1");

        shellAdvancedOptionPub.open();
        shellAdvancedOptionPub.pack();
        shellAdvancedOptionPub.addListener(SWT.Close, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (!textBatchSize.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-batchSize", " -batchSize " + textBatchSize.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-batchSize", "");
                }
                if (!textPubRate.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-pubRate", " -pubRate " + textPubRate.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-pubRate", "");
                }
                if (!textSendQueueSize.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-sendQueueSize",
                            " -sendQueueSize " + textSendQueueSize.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-sendQueueSize", "");
                }
                if (!textExecuteionTime.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-executionTime",
                            " -executionTime " + textExecuteionTime.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-executionTime", "");
                }
                if (!textNumIter.getText().replaceAll("\\s+", "").equals("") && !scan.getSelection()) {
                    mapParameter.put("-numIter", " -numIter " + textNumIter.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-numIter", "");
                }
                if (!textLatencyCount.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-latencyCount",
                            " -latencyCount " + textLatencyCount.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-latencyCount", "");
                }
                if (latencyTest.getSelection() && textPidMultiPubTest.getText().replaceAll("\\s+", "").equals("0")) {
                    mapParameter.put("-latencyTest", " -latencyTest");
                } else {
                    mapParameter.put("-latencyTest", "");
                }
                if (scan.getSelection()) {
                    mapParameter.put("-scan", " -scan");
                } else {
                    mapParameter.put("-scan", "");
                }
                if (writerStats.getSelection()) {
                    mapParameter.put("-writerStats", " -writerStats");
                } else {
                    mapParameter.put("-writerStats", "");
                }
                if (!textHeartbeatPeriod.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-heartbeatPeriod",
                            " -heartbeatPeriod " + textHeartbeatPeriod.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-heartbeatPeriod", "");
                }
                if (enableAutoThrottle.getSelection()) {
                    mapParameter.put("-enableAutoThrottle", " -enableAutoThrottle");
                } else {
                    mapParameter.put("-enableAutoThrottle", "");
                }
                if (enableTurboMode.getSelection()) {
                    mapParameter.put("-enableTurboMode", " -enableTurboMode");
                } else {
                    mapParameter.put("-enableTurboMode", "");
                }
                if (!textPidMultiPubTest.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-pidMultiPubTest",
                            " -pidMultiPubTest " + textPidMultiPubTest.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-pidMultiPubTest", "");
                }
                if (!textSleep.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-sleep", " -sleep " + textSleep.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-sleep", "");
                }
                if (!textSpin.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-spin", " -spin " + textSpin.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-spin", "");
                }
                if (!textNumSubscribers.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-numSubscribers",
                            " -numSubscribers " + textNumSubscribers.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-numSubscribers", "");
                }
                shellAdvancedOptionPub.dispose();
            }
        });
    }

    /**
     * Display the execution tab with all the parameter. However we have a list
     * of parameter which are not added: verbosity, instanceHashBuckets,
     * keepDurationUsec, noDirectCommunication, noPositiveAcks,
     * waitsetDelayUsec, waitsetEventCount, asynchronous, unbounded
     */
    private void display_tab_execution() {
        TabItem tabExecute = new TabItem(folder, SWT.NONE);
        tabExecute.setText("Execution");

        Composite compositeExecution = new Composite(folder, SWT.NONE);
        compositeExecution.setLayout(new GridLayout(4, false));
        tabExecute.setControl(compositeExecution);

        Group groupGeneral = new Group(compositeExecution, SWT.NONE);
        groupGeneral.setLayout(new GridLayout(4, false));
        groupGeneral.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 2));

        // Perftest path
        Label labelPerftest = new Label(groupGeneral, SWT.NONE);
        labelPerftest.setText("Perftest path");
        labelPerftest.setToolTipText("Path to the RTI perftest bundle");
        Text textPerftest = new Text(groupGeneral, SWT.BORDER);
        textPerftest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textPerftest);
        textPerftest.setText("/home/ajimenez/github/rtiperftest_test"); // TODO
                                                                        // delete

        // two radio button for publisher or subscriber
        Button pub = new Button(groupGeneral, SWT.RADIO);
        pub.setText("Run Publisher");
        pub.setToolTipText("Set test to be a publisher.");
        pub.setSelection(true);
        Button sub = new Button(groupGeneral, SWT.RADIO);
        sub.setText("Run Subscriber");
        sub.setToolTipText("Set test to be a subscriber.");

        // Platform
        Label labelPlaform = new Label(groupGeneral, SWT.NONE);
        labelPlaform.setText("Plaform");
        labelPlaform.setToolTipText("Architecture/Platform for which build.sh is going to compile RTI Perftest.");
        Combo comboPlaform = new Combo(groupGeneral, SWT.READ_ONLY);
        comboPlaform.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        comboPlaform.setItems(possiblePlatform);
        comboPlaform.setText(possiblePlatform[possiblePlatform.length - 1]);

        // domain id
        Label labelDomain = new Label(groupGeneral, SWT.NONE);
        labelDomain.setText("Domain");
        labelDomain.setToolTipText(
                "The publisher and subscriber applications must use the same domain ID in order to communicate.");
        Text textDomain = new Text(groupGeneral, SWT.BORDER);
        textDomain.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textDomain);
        textDomain.setText("0");

        // four radio for the languages
        Group groupLanguage = new Group(compositeExecution, SWT.NONE);
        groupLanguage.setLayout(new GridLayout(4, false));
        groupLanguage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        groupLanguage.setText("Language to execute");
        Button language_cpp = new Button(groupLanguage, SWT.RADIO);
        language_cpp.setText("CPP");
        language_cpp.setSelection(true);
        Button language_cpp03 = new Button(groupLanguage, SWT.RADIO);
        language_cpp03.setText("CPP03");
        Button language_cs = new Button(groupLanguage, SWT.RADIO);
        language_cs.setText("C#");
        Button language_java = new Button(groupLanguage, SWT.RADIO);
        language_java.setText("JAVA");

        // two radio button for the delivery
        Group groupDelivery = new Group(compositeExecution, SWT.NONE);
        groupDelivery.setLayout(new GridLayout(2, false));
        groupDelivery.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        groupDelivery.setText("Delivery");
        Button reliabiliy = new Button(groupDelivery, SWT.RADIO);
        reliabiliy.setText("Reliabiliy");
        reliabiliy.setToolTipText("Use Reliabiliy communication.");
        reliabiliy.setSelection(true);
        Button bestEffort = new Button(groupDelivery, SWT.RADIO);
        bestEffort.setText("Best Effort");
        bestEffort.setToolTipText("Use best-effort communication.");

        // A check button for the keyed and a label and input for
        // instances
        Group groupKey = new Group(compositeExecution, SWT.NONE);
        groupKey.setLayout(new GridLayout(4, false));
        groupKey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelInstances = new Label(groupKey, SWT.NONE);
        labelInstances.setText("Instances");
        labelInstances.setToolTipText(
                "Set the number of instances to use in the test. The publishing and subscribing applications must specify the same number of instances.This option only makes sense when testing a keyed data type.");
        Text textInstances = new Text(groupKey, SWT.BORDER);
        textInstances.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textInstances);
        textInstances.setText("1");
        Button keyed = new Button(groupKey, SWT.CHECK);
        keyed.setText("keyed");
        keyed.setToolTipText("Specify the use of a keyed type.");

        // Date Length
        Group groupDataLen = new Group(compositeExecution, SWT.NONE);
        groupDataLen.setLayout(new GridLayout(4, false));
        groupDataLen.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelDataLen = new Label(groupDataLen, SWT.NONE);
        labelDataLen.setText("Data Length");
        labelDataLen.setToolTipText("Length of payload in bytes for each send.");
        Text textDataLen = new Text(groupDataLen, SWT.BORDER);
        textDataLen.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textDataLen);
        textDataLen.setText("100");

        // three radio for the transport
        Group groupTransport = new Group(compositeExecution, SWT.NONE);
        groupTransport.setLayout(new GridLayout(4, false));
        groupTransport.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        groupTransport.setText("Language to execute");
        Button UDP = new Button(groupTransport, SWT.RADIO);
        UDP.setText("UDP");
        UDP.setToolTipText("Enable the UDP transport.");
        UDP.setSelection(true);
        Button shareMemory = new Button(groupTransport, SWT.RADIO);
        shareMemory.setText("Share Memory");
        shareMemory.setToolTipText("Enable the Share Memory transport.");
        Button TCP = new Button(groupTransport, SWT.RADIO);
        TCP.setText("TCP");
        TCP.setToolTipText("Enable the TCP transport.");

        // check button for the dynamic
        Group groupDynamic = new Group(compositeExecution, SWT.NONE);
        groupDynamic.setLayout(new GridLayout(4, false));
        groupDynamic.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Button dynamic = new Button(groupDynamic, SWT.CHECK);
        dynamic.setText("Use Dynamic data API");
        dynamic.setSelection(false);
        dynamic.setToolTipText("Run using the Dynamic Data API functions instead of the rtiddsgen generated calls.");

        // Combo Durability
        Group groupDurability = new Group(compositeExecution, SWT.NONE);
        groupDurability.setLayout(new GridLayout(2, false));
        groupDurability.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelDurability = new Label(groupDurability, SWT.NONE);
        labelDurability.setText("Durability");
        labelDurability.setToolTipText("Sets the Durability kind");
        Combo comboDurability = new Combo(groupDurability, SWT.READ_ONLY);
        comboDurability.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        comboDurability.setItems((String[]) listDurability.keySet().toArray(new String[listDurability.size()]));

        // one CHECK button for the multicast and a label and input for
        // multicast address
        Group groupMulticast = new Group(compositeExecution, SWT.NONE);
        groupMulticast.setLayout(new GridLayout(3, false));
        groupMulticast.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        Button multicast = new Button(groupMulticast, SWT.CHECK);
        multicast.setToolTipText(
                "Use multicast to receive data. In addition, the Datawriter heartbeats will be sent using multicast instead of unicast.");
        multicast.setText("Multicast");
        multicast.setSelection(false);
        Label labelMulticast = new Label(groupMulticast, SWT.NONE);
        labelMulticast.setText(" Address");
        labelMulticast.setToolTipText("Specify the multicast receive address for receiving user data.");
        Text textMulticast = new Text(groupMulticast, SWT.BORDER);
        textMulticast.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textMulticast);

        // Nic
        Group groupNic = new Group(compositeExecution, SWT.NONE);
        groupNic.setLayout(new GridLayout(2, false));
        groupNic.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelNic = new Label(groupNic, SWT.NONE);
        labelNic.setText("Nic");
        labelNic.setToolTipText(
                "Restrict RTI Connext DDS to sending output through this interface. This can be the IP address of any available network interface on the machine.");
        Text textNic = new Text(groupNic, SWT.BORDER);
        textNic.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textNic);

        // check button for the useReadThread and for CPU
        Group groupUseReadThreadAndCpu = new Group(compositeExecution, SWT.NONE);
        groupUseReadThreadAndCpu.setLayout(new GridLayout(2, false));
        groupUseReadThreadAndCpu.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        Button useReadThread = new Button(groupUseReadThreadAndCpu, SWT.CHECK);
        useReadThread.setText("Use Read Thread");
        useReadThread.setSelection(false);
        useReadThread.setToolTipText("Use a separate thread (instead of a callback) to read data.");
        Button cpu = new Button(groupUseReadThreadAndCpu, SWT.CHECK);
        cpu.setText("CPU");
        cpu.setSelection(false);
        cpu.setToolTipText("Display the cpu used by the RTI Perftest process.");

        // Combo FlowController
        Group groupFlowController = new Group(compositeExecution, SWT.NONE);
        groupFlowController.setLayout(new GridLayout(2, false));
        groupFlowController.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        Label labelFlowController = new Label(groupFlowController, SWT.NONE);
        labelFlowController.setText("Flow Controller");
        labelFlowController.setToolTipText(
                "Specify the name of the flow controller that will be used by the DataWriters. This will only have effect if the DataWriter uses Asynchronous Publishing either because it is using samples greater than 63000 Bytes or because the -asynchronous option is present.");
        Combo comboFlowController = new Combo(groupFlowController, SWT.READ_ONLY);
        comboFlowController.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        comboFlowController.setItems(listFlowController);
        comboFlowController.setText(listFlowController[0]);

        // Initial peers
        // TODO add several initials peers, like a list
        Group groupInitalPeers = new Group(compositeExecution, SWT.NONE);
        groupInitalPeers.setLayout(new GridLayout(2, false));
        groupInitalPeers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelInitalPeers = new Label(groupInitalPeers, SWT.NONE);
        labelInitalPeers.setText("Inital Peers");
        labelInitalPeers.setToolTipText(
                "Adds a peer to the peer host address list. This argument may be repeated to indicate multiple peers.");
        Text textInitalPeers = new Text(groupInitalPeers, SWT.BORDER);
        textInitalPeers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textInitalPeers);

        // four buttons for compile and advance option and security option
        Group groupButtons = new Group(compositeExecution, SWT.NONE);
        groupButtons.setLayout(new GridLayout(4, false));
        groupButtons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
        Button btnExecute = new Button(groupButtons, SWT.PUSH);
        btnExecute.setText("Run");
        btnExecute.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Button btnAdvancedOptionPub = new Button(groupButtons, SWT.PUSH);
        btnAdvancedOptionPub.setText("Advanced Option Pub");
        btnAdvancedOptionPub.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Button btnAdvancedOptionSub = new Button(groupButtons, SWT.PUSH);
        btnAdvancedOptionSub.setText("Advanced Option Sub");
        btnAdvancedOptionSub.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        btnAdvancedOptionSub.setEnabled(false);
        Button btnSecureOption = new Button(groupButtons, SWT.PUSH);
        btnSecureOption.setText("Secure Option");
        btnSecureOption.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        // text command
        Text textCommand = new Text(compositeExecution, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL);
        textCommand.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
        textCommand.computeSize(100, 100, true);
        listTextParameter.add(textCommand);

        // text output command
        List listOutput = new List(compositeExecution, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        listOutput.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));

        // listener button compile
        btnExecute.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Language language = Language.cpp; // by default
                System.out.println("Button compile clicked");
                if (textPerftest.getText().replaceAll("\\s+", "").equals("")) {
                    show_error("The path to the build of perftest is necessary.");
                    return;
                }
                mapParameter.put("Perftest", textPerftest.getText().replaceAll("\\s+", ""));

                if (!language_java.getSelection() && comboPlaform.getText().replaceAll("\\s+", "").equals("")) {
                    show_error("The platfomr of the execution is necessary.");
                    return;
                } else {
                    mapParameter.put("platform", comboPlaform.getText().replaceAll("\\s+", ""));
                }
                if (language_cpp.getSelection()) {
                    language = Language.cpp;
                } else if (language_cpp03.getSelection()) {
                    language = Language.cpp03;
                } else if (language_cs.getSelection()) {
                    if (comboPlaform.getText().replaceAll("\\s+", "").contains("Linux")
                            || comboPlaform.getText().replaceAll("\\s+", "").contains("Darwin")) {
                        show_error("You must specify a correct platform. C# is not compatible with Linux or Darwin");
                        return;
                    } else {
                        language = Language.cs;
                    }
                } else if (language_java.getSelection()) {
                    language = Language.java;
                }

                if (pub.getSelection()) {
                    mapParameter.put("-pub", " -pub");
                } else {
                    mapParameter.put("-pub", "");
                }
                if (sub.getSelection()) {
                    mapParameter.put("-sub", " -sub");
                } else {
                    mapParameter.put("-sub", "");
                }
                if (!textDomain.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-domain", " -domain " + textDomain.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-domain", "");
                }
                if (bestEffort.getSelection()) {
                    mapParameter.put("-bestEffort", " -bestEffort");
                } else {
                    mapParameter.put("-bestEffort", "");
                }
                if (keyed.getSelection()) {
                    mapParameter.put("-keyed", " -keyed");
                } else {
                    mapParameter.put("-keyed", "");
                }
                if (!textInstances.getText().replaceAll("\\s+", "").equals("") && keyed.getSelection()) {
                    mapParameter.put("-instances", " -instances " + textInstances.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-instances", "");
                }
                if (TCP.getSelection()) {
                    mapParameter.put("-enableTcp", " -enableTcp");
                } else {
                    mapParameter.put("-enableTcp", "");
                }
                if (shareMemory.getSelection()) {
                    mapParameter.put("-enableSharedMemory", " -enableSharedMemory");
                } else {
                    mapParameter.put("-enableSharedMemory", "");
                }
                if (!textDataLen.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-dataLen", " -dataLen " + textDataLen.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-dataLen", "");
                }
                if (dynamic.getSelection()) {
                    mapParameter.put("-dynamicData", " -dynamicData ");
                } else {
                    mapParameter.put("-dynamicData", "");
                }
                if (!comboDurability.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-durability", " -durability " + listDurability.get(comboDurability.getText()));
                } else {
                    mapParameter.put("-durability", "");
                }
                if (multicast.getSelection()) {
                    mapParameter.put("-multicast", " -multicast");
                } else {
                    mapParameter.put("-multicast", "");
                }
                if (!textMulticast.getText().replaceAll("\\s+", "").equals("") && multicast.getSelection()) {
                    mapParameter.put("-multicastAddress",
                            " -multicastAddress " + textMulticast.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-multicastAddress", "");
                }
                if (!textNic.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-nic", " -nic " + textNic.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-nic", "");
                }
                if (!textInitalPeers.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-peer", " -peer " + textInitalPeers.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-peer", "");
                }
                if (useReadThread.getSelection()) {
                    mapParameter.put("-useReadThread", " -useReadThread");
                } else {
                    mapParameter.put("-useReadThread", "");
                }
                if (cpu.getSelection()) {
                    mapParameter.put("-cpu", " -cpu");
                } else {
                    mapParameter.put("-cpu", "");
                }
                if (!comboFlowController.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-flowController", " -flowController " + comboFlowController.getText());
                } else {
                    mapParameter.put("-flowController", "");
                }
                // TODO cleanInput(listOutput,listTextCompile);
                if (!execute(textCommand, listOutput, language)) {
                    show_error("Error in the execution.");
                    return;
                }
            }
        });

        // listener button advance option pub
        btnAdvancedOptionPub.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                System.out.println("Button advance option pub clicked");
                display_pub_advanced_option();
            }
        });

        // listener button advance option sub
        btnAdvancedOptionSub.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                System.out.println("Button advance option sub clicked");
                display_sub_advanced_option();
            }
        });

        // Activate Pub advance option
        pub.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                System.out.println("Button pub clicked");
                btnAdvancedOptionSub.setEnabled(false);
                btnAdvancedOptionPub.setEnabled(true);
            }
        });

        // Activate Sub advance option
        sub.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                System.out.println("Button sub clicked");
                btnAdvancedOptionPub.setEnabled(false);
                btnAdvancedOptionSub.setEnabled(true);
            }
        });
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        // System.out.println(SWT.getPlatform());
        // System.out.println(SWT.getVersion());
        // System.out.println(System.getProperty("os.name"));
        Display display = new Display();
        GUI_RTIPerftest ex = new GUI_RTIPerftest(display);
        display.dispose();
    }
}
