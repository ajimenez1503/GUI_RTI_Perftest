package GUI_RTIPerftest;

import org.eclipse.swt.SWT;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.custom.StyledText;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import GUI_RTIPerftest.ListPlatform;
import GUI_RTIPerftest.Chart_RTIPerftest;
import GUI_RTIPerftest.OSType;

public class GUI_RTIPerftest {

    /**
     * types of Execution
     */
    protected enum ExecutionType {
        Compile, Pub, Sub
    };

    /**
     * types of Supported Languages
     */
    public enum Language {
        cpp, cpp03, cs, java
    };

    private static OSType detectedOS; // cached result of OS detection
    private Shell shell;
    private TabFolder folder, folder_output;
    private ArrayList<Text> listTextParameter; // create list of text elements
    private Map<String, String> mapParameter;// create dictionary with parameter
    private String[] possiblePlatform;
    private Map<String, String> listDurability;
    private String[] listFlowController;
    private DefaultExecutor exec;
    private ListPlatform listPlatform;

    /**
     * Constructor
     */
    public GUI_RTIPerftest() {
        listPlatform = new ListPlatform(getOperatingSystemType());
        exec = new DefaultExecutor();
        set_all_possible_platform();
        listTextParameter = new ArrayList<Text>();
        set_listDurability();
        listFlowController = new String[] { "default", "10Gbps", "1Gbps" };
        mapParameter = new HashMap<String, String>();
        shell = new Shell(Display.getCurrent(), SWT.SHELL_TRIM | SWT.CENTER);
        folder = new TabFolder(shell, SWT.NONE);
        initUI();
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
     * Check is a path exists in the OS
     */
    private boolean path_exists(String path) {
        Path folder = Paths.get(path);
        return Files.exists(folder);
    }

    /**
     * Set the list of Platform according to the OS
     */
    private void set_all_possible_platform() {
        if (System.getenv("NDDSHOME") != null && System.getenv("NDDSHOME").contains("5.3")) {
            // TODO check that it is 5.3.0 >=
            // get platform
            listPlatform.setNDDSHOME(System.getenv("NDDSHOME"));
            listPlatform.getPlatform();
            possiblePlatform = (String[]) listPlatform.getListPlaform("C++").toArray(new String[0]);
        } else {
            // TODO type here the most important Platform
            System.out.format("NDDSHOME is not define");
            if (getOperatingSystemType() == OSType.Linux) {
                possiblePlatform = new String[] { "", "x64Linux3gcc5.4.0", "x64Linux3gcc4.8.2" };
            } else if (getOperatingSystemType() == OSType.Win) {
                possiblePlatform = new String[] { "", "x64Win64VS2015" };
            } else if (getOperatingSystemType() == OSType.Darwin) {
                possiblePlatform = new String[] { "", "x64Darwin16clang8.0" };
            } else {
                possiblePlatform = new String[] { "", "x64Win64VS2015", "x64Darwin16clang8.0", "x64Linux3gcc5.4.0",
                        "x64Linux3gcc4.8.2" };
            }
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
                detectedOS = OSType.Win;
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
     * @param outputTextField
     * @param texts
     */
    private void cleanInput(StyledText outputTextField, ArrayList<Text> listText) {
        outputTextField.setText("");
        for (int i = 0; i < listText.size(); i++) {
            listText.get(i).setText("");
        }
    }

    private void execute_command(String command, String workingDirectory, StyledText outputTextField,
            ExecutionType executionType, Chart_RTIPerftest chart, boolean display_real_time) {
        CommandLine cl = CommandLine.parse(command);
        kill_job(); // kill in the case that there are already a job
        exec.setWorkingDirectory(new File(workingDirectory));
        if (executionType == ExecutionType.Compile) {
            exec.setStreamHandler(new PumpStreamHandler(new StyledTextOutputStreamCompile(outputTextField)));
        } else { // if (executionType == ExecutionType.Pub || executionType ==
                 // ExecutionType.Sub)
            chart.reset();
            exec.setStreamHandler(new PumpStreamHandler(new StyledTextOutputStreamExecution(outputTextField, chart,
                    executionType, display_real_time, folder_output)));
        }
        exec.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    exec.execute(cl);
                } catch (ExecuteException e) {
                    // e.printStackTrace();
                    System.out.println(e.toString());
                } catch (IOException e) {
                    // e.printStackTrace();
                    System.out.println(e.toString());
                }
            }

        }).start();
    }

    /**
     * Run the command compile in the OS
     *
     * @param textCommand
     * @param outputTextField
     *
     * @returns - True if the commands works, False if the OS is not found
     */
    private Boolean compile(Text textCommand, StyledText outputTextField) {
        outputTextField.setText("");
        // create parameter
        String command = "";

        // check if Linux or Win or Darwin
        if (getOperatingSystemType() == OSType.Linux || get_paramenter("--platform").toLowerCase().contains("linux")) {
            command = "." + File.separator + "build.sh";
        } else if (getOperatingSystemType() == OSType.Darwin
                || get_paramenter("--platform").toLowerCase().contains("darwin")) {
            command = "." + File.separator + "build.sh";
        } else if (getOperatingSystemType() == OSType.Win
                || get_paramenter("--platform").toLowerCase().contains("win")) {
            command = "build.bat";
            command += get_paramenter("--skip-cs-build");
            // C# just in win
        } else {
            show_error("You must specify a correct platform");
            return false;
        }

        command += get_paramenter("--nddshome");
        command += get_paramenter("--platform");
        command += get_paramenter("--skip-cpp-build");
        command += get_paramenter("--skip-cpp03-build");
        command += get_paramenter("--skip-java-build");
        command += get_paramenter("--debug");
        command += get_paramenter("--secure");
        command += get_paramenter("--openssl-home");

        // Print command to run
        System.out.println(command);
        textCommand.setText(command);

        execute_command(command, get_paramenter("Perftest"), outputTextField, ExecutionType.Compile, null, false);
        return true;
    }

    /**
     * Run the command compile in the OS
     *
     * @param textCommand
     * @param outputTextField
     *
     * @returns - True if the commands works, False if the OS is not found
     */
    private Boolean executePerftest(Text textCommand, StyledText outputTextField, Language language, ExecutionType type,
            Chart_RTIPerftest chart, boolean display_real_time) {
        outputTextField.setText("");
        // create parameter
        String command = "." + File.separator + "bin" + File.separator;

        // check if Linux or Win or Darwin
        if (getOperatingSystemType() == OSType.Linux || get_paramenter("--platform").toLowerCase().contains("linux")) {
            switch (language) {
            case cpp:
                command += get_paramenter("platform") + File.separator + "release" + File.separator + "perftest_cpp";
                break;
            case cpp03:
                command += get_paramenter("platform") + File.separator + "release" + File.separator + "perftest_cpp03";
                break;
            case cs:
                return false;
            case java:
                command += "Release" + File.separator + "perftest_java.sh";
                break;
            }
        } else if (getOperatingSystemType() == OSType.Darwin
                || get_paramenter("--platform").toLowerCase().contains("darwin")) {
            switch (language) {
            case cpp:
                command += get_paramenter("platform") + File.separator + "release" + File.separator + "perftest_cpp";
                break;
            case cpp03:
                command += get_paramenter("platform") + File.separator + "release" + File.separator + "perftest_cpp03";
                break;
            case cs:
                return false;
            case java:
                command += "Release" + File.separator + "perftest_java.bat";
                break;
            }
        } else if (getOperatingSystemType() == OSType.Win
                || get_paramenter("--platform").toLowerCase().contains("win")) {
            switch (language) {
            case cpp:
                command += get_paramenter("platform") + File.separator + "release" + File.separator + "perftest_cpp";
                break;
            case cpp03:
                command += get_paramenter("platform") + File.separator + "release" + File.separator + "perftest_cpp03";
                break;
            case cs:
                command += get_paramenter("platform") + File.separator + "release" + File.separator + "perftest_cs";
                break;
            case java:
                command += "Release" + File.separator + "perftest_java.bat";
                break;
            }
        } else {
            show_error("You must specify a correct platform");
            return false;
        }

        // TODO improve, it could not work on Window
        if (!path_exists(command.replace("./", get_paramenter("Perftest") + "/"))) {
            show_error("The path '" + command.replace("./", get_paramenter("Perftest") + "/")
                    + "' to execute perftest does not exists.");
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
        execute_command(command, get_paramenter("Perftest"), outputTextField, type, chart, display_real_time);
        return true;
    }

    /**
     * Run the command compile --clean in the OS
     *
     * @param textCommand
     * @param outputTextField
     *
     * @returns - True if the commands works, False if the OS is not found
     */
    private Boolean compile_clean(Text textCommand, StyledText outputTextField) {
        outputTextField.setText("");
        String command = "";

        // check if Linux or Win or Darwin
        if (getOperatingSystemType() == OSType.Linux || get_paramenter("--platform").toLowerCase().contains("linux")) {
            command = "." + File.separator + "build.sh --clean";
        } else if (getOperatingSystemType() == OSType.Darwin
                || get_paramenter("--platform").toLowerCase().contains("darwin")) {
            command = "." + File.separator + "build.sh --clean";
        } else if (getOperatingSystemType() == OSType.Win
                || get_paramenter("--platform").toLowerCase().contains("win")) {
            command = "build.bat --clean";
        } else {
            show_error("You must specify a correct platform");
            return false;
        }

        // print command to run
        System.out.println(command);
        textCommand.setText(command);
        execute_command(command, get_paramenter("Perftest"), outputTextField, ExecutionType.Compile, null, false);

        return true;
    }

    /**
     * Run the GUI with two tabs
     *
     */
    private void initUI() {
        shell.setLayout(new FillLayout());
        display_tab_compile(); // Tab 1 (compile)
        display_tab_execution(); // Tab 2 (execute)
        display_menu();

        shell.open();
        shell.pack();
        shell.setText("RTI Perftest");
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
            if (!shell.getDisplay().readAndDispatch()) {
                shell.getDisplay().sleep();
            }
        }
        // Exit execution
        kill_job();
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
     * Open URL in the default browser
     *
     * @param urlString
     */
    private void openWebpage(String urlString) {
        URL url;
        if (getOperatingSystemType() == OSType.Linux || getOperatingSystemType() == OSType.Darwin) {
            try {
                Process proc = Runtime.getRuntime().exec("xdg-open " + urlString);
                BufferedReader read = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                try {
                    proc.waitFor();
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }
                while (read.ready()) {
                    String output = read.readLine();
                    System.out.println(output);
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } else {
            try {
                url = new URL(urlString);
                Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
                if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                    try {
                        desktop.browse(url.toURI());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                show_error("Cannot display url: '" + urlString + "'");
                return;
            }
        }

    }

    /**
     * Display the menu with: exit, documentation Perftest, new window, help
     * execution, help compile
     *
     */
    private void display_menu() {

        Menu menuBar = new Menu(shell, SWT.BAR);
        shell.setMenuBar(menuBar);

        // menu exit
        Menu exitMenu = new Menu(shell, SWT.DROP_DOWN);
        MenuItem exitMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        exitMenuHeader.setText("&Menu");
        exitMenuHeader.setMenu(exitMenu);
        MenuItem itemNewWindow = new MenuItem(exitMenu, SWT.PUSH);
        itemNewWindow.setText("&New Window");
        itemNewWindow.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                new GUI_RTIPerftest();
            }
        });
        MenuItem itemExit = new MenuItem(exitMenu, SWT.PUSH);
        itemExit.setText("&Exit");
        itemExit.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                kill_job();
                shell.dispose();
            }
        });

        // Menu help
        Menu helpMenu = new Menu(shell, SWT.DROP_DOWN);
        MenuItem helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        helpMenuHeader.setText("&Help");
        helpMenuHeader.setMenu(helpMenu);

        // documentation RTI Perftest
        MenuItem itemRTIPerftest = new MenuItem(helpMenu, SWT.PUSH);
        itemRTIPerftest.setText("&Documentation RTI Perftest");
        itemRTIPerftest.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                openWebpage("https://github.com/rticommunity/rtiperftest/wiki");
            }
        });

        // documentation help execution
        MenuItem itemHelpExecution = new MenuItem(helpMenu, SWT.PUSH);
        itemHelpExecution.setText("&Help Execution");
        itemHelpExecution.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                openWebpage("https://github.com/rticommunity/rtiperftest/blob/master/srcDoc/md/test_parameters.md");
            }
        });

        // documentation help compile
        MenuItem itemHelpCompule = new MenuItem(helpMenu, SWT.PUSH);
        itemHelpCompule.setText("&Help Compile");
        itemHelpCompule.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                openWebpage(
                        "https://github.com/rticommunity/rtiperftest/blob/master/srcDoc/md/code_generation_and_compilation.md");
            }
        });

    }

    /**
     * Display the tab of the compile
     *
     */
    private void display_tab_compile() {
        TabItem tabCompile = new TabItem(folder, SWT.NONE);
        tabCompile.setText("Compile");

        Composite compositeCompile = new Composite(folder, SWT.NONE);
        compositeCompile.setLayout(new GridLayout(2, false));
        tabCompile.setControl(compositeCompile);

        Group groupGeneral = new Group(compositeCompile, SWT.NONE);
        groupGeneral.setLayout(new GridLayout(2, false));
        groupGeneral.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 3));

        // PerftestPath
        Group groupPerftestPath = new Group(groupGeneral, SWT.NONE);
        groupPerftestPath.setLayout(new GridLayout(3, false));
        groupPerftestPath.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
        Label labelPerftest = new Label(groupPerftestPath, SWT.NONE);
        labelPerftest.setText("Perftest path");
        labelPerftest.setToolTipText("Path to the RTI perftest bundle");
        Text textPerftest = new Text(groupPerftestPath, SWT.BORDER);
        textPerftest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        textPerftest.setMessage("/home/ajimenez/github/rtiperftest_test");
        listTextParameter.add(textPerftest);
        textPerftest.setText("/home/ajimenez/github/rtiperftest_test");
        Button openPerftestPath = new Button(groupPerftestPath, SWT.PUSH);
        openPerftestPath.setText("Open");
        openPerftestPath.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                // User has selected to open a single file
                DirectoryDialog dlgPerftestPath = new DirectoryDialog(shell, SWT.OPEN);
                String folderPerftestPath = dlgPerftestPath.open();
                if (folderPerftestPath != null) {
                    textPerftest.setText(folderPerftestPath);
                }
            }
        });

        // NDDSHOME
        Group groupNDDSHOME = new Group(groupGeneral, SWT.NONE);
        groupNDDSHOME.setLayout(new GridLayout(3, false));
        groupNDDSHOME.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
        Label labelNDDSHOME = new Label(groupNDDSHOME, SWT.NONE);
        labelNDDSHOME.setText("NDDSHOME");
        labelNDDSHOME.setToolTipText(
                "Path to the RTI Connext DDS installation. If this parameter is not present, the $NDDSHOME variable should be.");
        Text textNDDSHOME = new Text(groupNDDSHOME, SWT.BORDER);
        textNDDSHOME.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textNDDSHOME);
        if (System.getenv("NDDSHOME") != null) {
            textNDDSHOME.setText(System.getenv("NDDSHOME"));
        } else {
            System.out.format("NDDSHOME is not define");
        }
        Button openNDDSHOME = new Button(groupNDDSHOME, SWT.PUSH);
        openNDDSHOME.setText("Open");
        openNDDSHOME.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                // User has selected to open a single file
                DirectoryDialog dlgNDDSHOME = new DirectoryDialog(shell, SWT.OPEN);
                String folderNDDSHOME = dlgNDDSHOME.open();
                if (folderNDDSHOME != null) {
                    textNDDSHOME.setText(folderNDDSHOME);
                }
            }
        });

        // Platform
        Label labelPlatform = new Label(groupGeneral, SWT.NONE);
        labelPlatform.setText("Platform");
        labelPlatform.setToolTipText("Architecture/Platform for which build.sh is going to compile RTI Perftest.");
        Combo comboPlatform = new Combo(groupGeneral, SWT.READ_ONLY);
        comboPlatform.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        comboPlatform.setItems(possiblePlatform);
        comboPlatform.setText(possiblePlatform[possiblePlatform.length - 1]);

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
        Button language_java = new Button(groupLanguage, SWT.CHECK);
        language_java.setText("JAVA");
        Button language_cs = new Button(groupLanguage, SWT.CHECK);
        language_cs.setText("C#");
        language_cs.setEnabled(getOperatingSystemType() == OSType.Win);

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
        groupSecurity.setLayout(new GridLayout(4, false));
        groupSecurity.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        groupSecurity.setText("Security");
        Label labelOpenSSL = new Label(groupSecurity, SWT.NONE);
        labelOpenSSL.setText("Path to the openSSL");
        labelOpenSSL.setToolTipText(
                "Path to the openSSL home directory. Needed when compiling using the --secure option and statically.");
        labelOpenSSL.setEnabled(false);
        Text textOpenSSL = new Text(groupSecurity, SWT.BORDER);
        textOpenSSL.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        textOpenSSL.setEnabled(false);
        listTextParameter.add(textOpenSSL);
        Button openOpenSSL = new Button(groupSecurity, SWT.PUSH);
        openOpenSSL.setText("Open");
        openOpenSSL.setEnabled(false);
        openOpenSSL.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                // User has selected to open a single file
                DirectoryDialog dlgOpenSSL = new DirectoryDialog(shell, SWT.OPEN);
                String folderOpenSSL = dlgOpenSSL.open();
                if (folderOpenSSL != null) {
                    textOpenSSL.setText(folderOpenSSL);
                }
            }
        });
        Button security = new Button(groupSecurity, SWT.CHECK);
        security.setToolTipText(
                "Enable the compilation of the Perfest code specific for security and adds the RTI Connext DDS Security Libraries in the linking step (if compiling statically). ");
        security.setText("Enable security");
        security.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                labelOpenSSL.setEnabled(security.getSelection());
                textOpenSSL.setEnabled(security.getSelection());
                openOpenSSL.setEnabled(security.getSelection());
            }
        });

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
        StyledText outputTextField = new StyledText(compositeCompile,
                SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        outputTextField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
        outputTextField.setFont(new Font(shell.getDisplay(), "Courier New", 12, SWT.NORMAL));
        // listener button compile
        btnCompile.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                System.out.println("Button compile clicked");
                if (textPerftest.getText().replaceAll("\\s+", "").equals("")) {
                    show_error("The path to the build of perftest is necessary.");
                    return;
                } else {
                    if (path_exists(textPerftest.getText().replaceAll("\\s+", ""))) {
                        mapParameter.put("Perftest", textPerftest.getText().replaceAll("\\s+", ""));
                    } else {
                        show_error("The path '" + textPerftest.getText().replaceAll("\\s+", "")
                                + "' to the build of perftest does not exists.");
                        return;
                    }
                }
                if (!textNDDSHOME.getText().replaceAll("\\s+", "").equals("")) {
                    if (path_exists(textNDDSHOME.getText().replaceAll("\\s+", ""))) {
                        mapParameter.put("--nddshome", " --nddshome " + textNDDSHOME.getText().replaceAll("\\s+", ""));
                    } else {
                        show_error("The path '" + textNDDSHOME.getText().replaceAll("\\s+", "")
                                + "' to the NDDSHOME does not exists.");
                        return;
                    }
                } else {
                    mapParameter.put("--nddshome", "");
                }
                if (!comboPlatform.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("--platform", " --platform " + comboPlatform.getText().replaceAll("\\s+", ""));
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
                        if (path_exists(textPerftest.getText().replaceAll("\\s+", ""))) {
                            // static and secure
                            mapParameter.put("--openssl-home",
                                    " --openssl-home " + textOpenSSL.getText().replaceAll("\\s+", ""));
                        } else {
                            show_error("The path '" + textOpenSSL.getText().replaceAll("\\s+", "")
                                    + "' to Open SSL does not exists.");
                            return;
                        }
                    } else {
                        show_error("OpenSSL needs when compiling using the secure option and statically linker.");
                        return;
                    }
                } else {
                    mapParameter.put("--openssl-home", "");
                }
                // TODO cleanInput(outputTextField,listTextCompile);
                compile(textCommand, outputTextField);

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
                } else {
                    if (path_exists(textPerftest.getText().replaceAll("\\s+", ""))) {
                        mapParameter.put("Perftest", textPerftest.getText().replaceAll("\\s+", ""));
                    } else {
                        show_error("The path '" + textPerftest.getText().replaceAll("\\s+", "")
                                + "' to the build of perftest does not exists.");
                        return;
                    }
                }
                mapParameter.put("Perftest", textPerftest.getText().replaceAll("\\s+", ""));
                // TODO cleanInput(outputTextField,listTextCompile);
                compile_clean(textCommand, outputTextField);
            }
        });

        SelectionAdapter languageListenerCompile = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                System.out.println("Button language clicked Compile");
                if (System.getenv("NDDSHOME") != null && System.getenv("NDDSHOME").contains("5.3")) {
                    // TODO check that it is 5.3.0 >=
                    List<String> newListPlaform = new ArrayList<String>();
                    if (language_cpp.getSelection()) {
                        if (newListPlaform.isEmpty()) {
                            newListPlaform.addAll(listPlatform.getListPlaform("C++"));
                        } else {
                            newListPlaform.retainAll(listPlatform.getListPlaform("C++"));
                        }
                    }
                    if (language_cpp03.getSelection()) {
                        if (newListPlaform.isEmpty()) {
                            newListPlaform.addAll(listPlatform.getListPlaform("C++03"));
                        } else {
                            newListPlaform.retainAll(listPlatform.getListPlaform("C++03"));
                        }
                    }
                    if (language_java.getSelection()) {
                        if (newListPlaform.isEmpty()) {
                            newListPlaform.addAll(listPlatform.getListPlaform("Java"));
                        } else {
                            newListPlaform.retainAll(listPlatform.getListPlaform("Java"));
                        }

                    }
                    if (language_cs.getSelection()) {
                        if (newListPlaform.isEmpty()) {
                            newListPlaform.addAll(listPlatform.getListPlaform("C#"));
                        } else {
                            newListPlaform.retainAll(listPlatform.getListPlaform("C#"));
                        }
                    }
                    if (!newListPlaform.isEmpty()) {
                        possiblePlatform = (String[]) newListPlaform.toArray(new String[0]);
                        Arrays.sort(possiblePlatform);
                        comboPlatform.setItems(possiblePlatform);
                        comboPlatform.setText(possiblePlatform[possiblePlatform.length - 1]);
                    } else {
                        possiblePlatform = new String[] {};
                        comboPlatform.setItems(possiblePlatform);
                    }
                }
            }
        };

        // listener language_cpp, language_cpp03, language_cs, language_java
        language_cpp.addSelectionListener(languageListenerCompile);
        language_cpp03.addSelectionListener(languageListenerCompile);
        language_cs.addSelectionListener(languageListenerCompile);
        language_java.addSelectionListener(languageListenerCompile);
    }

    /**
     * Display new window with the Advanced Options of the Subscriber
     *
     */
    private void display_sub_advanced_options() {

        Shell shellAdvancedOptionsSub = new Shell(shell.getDisplay(), SWT.CLOSE);
        shellAdvancedOptionsSub.setText("Subscriber Advanced Options");
        shellAdvancedOptionsSub.setLayout(new GridLayout(2, false));

        // numPublishers
        Label labelNumPublishers = new Label(shellAdvancedOptionsSub, SWT.NONE);
        labelNumPublishers.setText("Number of publisher");
        labelNumPublishers.setToolTipText(
                "The subscribing application will wait for this number of publishing applications to start.");
        Text textNumPublishers = new Text(shellAdvancedOptionsSub, SWT.BORDER);
        textNumPublishers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textNumPublishers);
        textNumPublishers.setMessage("1");

        // sidMultiSubTest
        Label labelSidMultiSubTest = new Label(shellAdvancedOptionsSub, SWT.NONE);
        labelSidMultiSubTest.setText("ID of Subscriber");
        labelSidMultiSubTest.setToolTipText("ID of the subscriber in a multi-subscriber test.");
        Text textSidMultiSubTest = new Text(shellAdvancedOptionsSub, SWT.BORDER);
        textSidMultiSubTest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textSidMultiSubTest);
        textSidMultiSubTest.setMessage("0");

        shellAdvancedOptionsSub.open();
        shellAdvancedOptionsSub.pack();
        shellAdvancedOptionsSub.addListener(SWT.Close, new Listener() {
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
                shellAdvancedOptionsSub.dispose();
            }
        });
    }

    /**
     * Display new window with the Advanced Options of the Publisher
     *
     */
    private void display_pub_advanced_options() {

        Shell shellAdvancedOptionsPub = new Shell(shell.getDisplay(), SWT.CLOSE);
        shellAdvancedOptionsPub.setText("Publisher Advanced Options");
        shellAdvancedOptionsPub.setLayout(new GridLayout(3, false));

        // NumIter LatencyCount LatencyTest Scan writerStats
        Group groupLatencyTest = new Group(shellAdvancedOptionsPub, SWT.NONE);
        groupLatencyTest.setLayout(new GridLayout(7, false));
        groupLatencyTest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
        Label labelNumIter = new Label(groupLatencyTest, SWT.NONE);
        labelNumIter.setText("Number of samples");
        labelNumIter.setToolTipText("Number of samples to send.");
        Text textNumIter = new Text(groupLatencyTest, SWT.BORDER);
        textNumIter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        textNumIter.setMessage("100000000");
        listTextParameter.add(textNumIter);
        Label labelLatencyCount = new Label(groupLatencyTest, SWT.NONE);
        labelLatencyCount.setText("Latency Count");
        labelLatencyCount.setToolTipText("Number samples to send before a latency ping packet is sent.");
        Text textLatencyCount = new Text(groupLatencyTest, SWT.BORDER);
        textLatencyCount.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        textLatencyCount.setMessage("1");
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
        Group groupBatchSize = new Group(shellAdvancedOptionsPub, SWT.NONE);
        groupBatchSize.setLayout(new GridLayout(2, false));
        groupBatchSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelBatchSize = new Label(groupBatchSize, SWT.NONE);
        labelBatchSize.setText("Batch Size");
        labelBatchSize.setToolTipText("Enable batching and set the maximum batched message size.");
        Text textBatchSize = new Text(groupBatchSize, SWT.BORDER);
        textBatchSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        textBatchSize.setMessage("8192");
        listTextParameter.add(textBatchSize);

        // Execution Time
        Group groupExecuteionTime = new Group(shellAdvancedOptionsPub, SWT.NONE);
        groupExecuteionTime.setLayout(new GridLayout(2, false));
        groupExecuteionTime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelExecuteionTime = new Label(groupExecuteionTime, SWT.NONE);
        labelExecuteionTime.setText("Execution Time");
        labelExecuteionTime.setToolTipText(
                "Allows you to limit the test duration by specifying the number of seconds to run the test.");
        Text textExecuteionTime = new Text(groupExecuteionTime, SWT.BORDER);
        textExecuteionTime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        textExecuteionTime.setMessage("120");
        listTextParameter.add(textExecuteionTime);

        // SendQueueSize
        Group groupSendQueueSize = new Group(shellAdvancedOptionsPub, SWT.NONE);
        groupSendQueueSize.setLayout(new GridLayout(2, false));
        groupSendQueueSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelSendQueueSize = new Label(groupSendQueueSize, SWT.NONE);
        labelSendQueueSize.setText("Send Queue Size");
        labelSendQueueSize.setToolTipText("Size of the send queue.");
        Text textSendQueueSize = new Text(groupSendQueueSize, SWT.BORDER);
        textSendQueueSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        textSendQueueSize.setMessage("50");
        listTextParameter.add(textSendQueueSize);

        // HeartbeatPeriod
        Group groupHeartbeatPeriod = new Group(shellAdvancedOptionsPub, SWT.NONE);
        groupHeartbeatPeriod.setLayout(new GridLayout(2, false));
        groupHeartbeatPeriod.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelHeartbeatPeriod = new Label(groupHeartbeatPeriod, SWT.NONE);
        labelHeartbeatPeriod.setText("Heartbeat Period");
        labelHeartbeatPeriod.setToolTipText("The period at which the publishing application will send heartbeats.");
        Text textHeartbeatPeriod = new Text(groupHeartbeatPeriod, SWT.BORDER);
        textHeartbeatPeriod.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        textHeartbeatPeriod.setMessage("0:10000000");
        listTextParameter.add(textHeartbeatPeriod);

        // two check button EnableAutoThrottle EnableTurboMode
        Group groupEnableAutoThrottleEnableTurboMode = new Group(shellAdvancedOptionsPub, SWT.NONE);
        groupEnableAutoThrottleEnableTurboMode.setLayout(new GridLayout(2, false));
        groupEnableAutoThrottleEnableTurboMode.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Button enableAutoThrottle = new Button(groupEnableAutoThrottleEnableTurboMode, SWT.CHECK);
        enableAutoThrottle.setText("EnableAutoThrottle");
        enableAutoThrottle.setToolTipText("Enable the Auto Throttling feature.");
        Button enableTurboMode = new Button(groupEnableAutoThrottleEnableTurboMode, SWT.CHECK);
        enableTurboMode.setText("EnableTurboMode");
        enableTurboMode.setToolTipText("Enables the Turbo Mode feature.");

        // PidMultiPubTest
        Group groupPidMultiPubTest = new Group(shellAdvancedOptionsPub, SWT.NONE);
        groupPidMultiPubTest.setLayout(new GridLayout(2, false));
        groupPidMultiPubTest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelPidMultiPubTest = new Label(groupPidMultiPubTest, SWT.NONE);
        labelPidMultiPubTest.setText("ID of Publisher");
        labelPidMultiPubTest.setToolTipText("ID of the subscriber in a multi-subscriber test.");
        Text textPidMultiPubTest = new Text(groupPidMultiPubTest, SWT.BORDER);
        textPidMultiPubTest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textPidMultiPubTest);
        textPidMultiPubTest.setMessage("0");

        // PubRate Sleep Spin
        Group groupPubRate = new Group(shellAdvancedOptionsPub, SWT.NONE);
        groupPubRate.setLayout(new GridLayout(6, false));
        groupPubRate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        Label labelPubRate = new Label(groupPubRate, SWT.NONE);
        labelPubRate.setText("Publication Rate");
        labelPubRate.setToolTipText("Limit the throughput to the specified number of samples per second.");
        Text textPubRate = new Text(groupPubRate, SWT.BORDER);
        textPubRate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        textPubRate.setMessage("100");
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
        Group groupNumSubscribers = new Group(shellAdvancedOptionsPub, SWT.NONE);
        groupNumSubscribers.setLayout(new GridLayout(2, false));
        groupNumSubscribers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelNumSubscribers = new Label(groupNumSubscribers, SWT.NONE);
        labelNumSubscribers.setText("Number of Subscribers");
        labelNumSubscribers.setToolTipText(
                "The subscribing application will wait for this number of publishing applications to start.");
        Text textNumSubscribers = new Text(groupNumSubscribers, SWT.BORDER);
        textNumSubscribers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textNumSubscribers);
        textNumSubscribers.setMessage("1");

        shellAdvancedOptionsPub.open();
        shellAdvancedOptionsPub.pack();
        shellAdvancedOptionsPub.addListener(SWT.Close, new Listener() {
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
                shellAdvancedOptionsPub.dispose();
            }
        });
    }

    /**
     * Display new window with the Advanced Option of the Subscriber
     *
     */
    private void display_execution_advanced_options() {

        Shell shellAdvancedOptionsExecution = new Shell(shell.getDisplay(), SWT.CLOSE);
        shellAdvancedOptionsExecution.setText("Advanced Options");
        shellAdvancedOptionsExecution.setLayout(new GridLayout(4, false));

        // Date Length
        Group groupDataLen = new Group(shellAdvancedOptionsExecution, SWT.NONE);
        groupDataLen.setLayout(new GridLayout(4, false));
        groupDataLen.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelDataLen = new Label(groupDataLen, SWT.NONE);
        labelDataLen.setText("Data Length");
        labelDataLen.setToolTipText("Length of payload in bytes for each send.");
        Text textDataLen = new Text(groupDataLen, SWT.BORDER);
        textDataLen.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textDataLen);
        textDataLen.setMessage("100");

        // Three radio for the transport
        Group groupTransport = new Group(shellAdvancedOptionsExecution, SWT.NONE);
        groupTransport.setLayout(new GridLayout(4, false));
        groupTransport.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        groupTransport.setText("Transport");
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
        Group groupDynamic = new Group(shellAdvancedOptionsExecution, SWT.NONE);
        groupDynamic.setLayout(new GridLayout(4, false));
        groupDynamic.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Button dynamic = new Button(groupDynamic, SWT.CHECK);
        dynamic.setText("Use Dynamic data API");
        dynamic.setSelection(false);
        dynamic.setToolTipText("Run using the Dynamic Data API functions instead of the rtiddsgen generated calls.");

        // Combo Durability
        Group groupDurability = new Group(shellAdvancedOptionsExecution, SWT.NONE);
        groupDurability.setLayout(new GridLayout(2, false));
        groupDurability.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelDurability = new Label(groupDurability, SWT.NONE);
        labelDurability.setText("Durability");
        labelDurability.setToolTipText("Sets the Durability kind");
        Combo comboDurability = new Combo(groupDurability, SWT.READ_ONLY);
        comboDurability.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        comboDurability.setItems((String[]) listDurability.keySet().toArray(new String[listDurability.size()]));
        comboDurability.setText(listDurability.keySet().toArray(new String[listDurability.size()])[0]);

        // one CHECK button for the multicast and a label and input for
        // multicast address
        Group groupMulticast = new Group(shellAdvancedOptionsExecution, SWT.NONE);
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
        textMulticast.setMessage("127.0.0.1");
        listTextParameter.add(textMulticast);

        // Nic
        Group groupNic = new Group(shellAdvancedOptionsExecution, SWT.NONE);
        groupNic.setLayout(new GridLayout(2, false));
        groupNic.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelNic = new Label(groupNic, SWT.NONE);
        labelNic.setText("Nic");
        labelNic.setToolTipText(
                "Restrict RTI Connext DDS to sending output through this interface. This can be the IP address of any available network interface on the machine.");
        Text textNic = new Text(groupNic, SWT.BORDER);
        textNic.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        textNic.setMessage("127.0.0.1");
        listTextParameter.add(textNic);

        // check button for the useReadThread and for CPU
        Group groupUseReadThreadAndCpu = new Group(shellAdvancedOptionsExecution, SWT.NONE);
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
        Group groupFlowController = new Group(shellAdvancedOptionsExecution, SWT.NONE);
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
        Group groupInitalPeers = new Group(shellAdvancedOptionsExecution, SWT.NONE);
        groupInitalPeers.setLayout(new GridLayout(2, false));
        groupInitalPeers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelInitalPeers = new Label(groupInitalPeers, SWT.NONE);
        labelInitalPeers.setText("Inital Peers");
        labelInitalPeers.setToolTipText(
                "Adds a peer to the peer host address list. This argument may be repeated to indicate multiple peers.");
        Text textInitalPeers = new Text(groupInitalPeers, SWT.BORDER);
        textInitalPeers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        textInitalPeers.setMessage("127.0.0.1");
        listTextParameter.add(textInitalPeers);

        shellAdvancedOptionsExecution.open();
        shellAdvancedOptionsExecution.pack();
        shellAdvancedOptionsExecution.addListener(SWT.Close, new Listener() {
            @Override
            public void handleEvent(Event event) {
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
                shellAdvancedOptionsExecution.dispose();
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

        // PerftestPath
        Group groupGeneral = new Group(compositeExecution, SWT.NONE);
        groupGeneral.setLayout(new GridLayout(4, false));
        groupGeneral.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 2));

        Group groupPerftestPath = new Group(groupGeneral, SWT.NONE);
        groupPerftestPath.setLayout(new GridLayout(3, false));
        groupPerftestPath.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
        Label labelPerftest = new Label(groupPerftestPath, SWT.NONE);
        labelPerftest.setText("Perftest path");
        labelPerftest.setToolTipText("Path to the RTI perftest bundle");
        Text textPerftest = new Text(groupPerftestPath, SWT.BORDER);
        textPerftest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textPerftest);
        textPerftest.setText("/home/ajimenez/github/rtiperftest_test");
        textPerftest.setMessage("/home/ajimenez/github/rtiperftest_test");
        Button openPerftestPath = new Button(groupPerftestPath, SWT.PUSH);
        openPerftestPath.setText("Open");
        openPerftestPath.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                // User has selected to open a single file
                DirectoryDialog dlgPerftestPath = new DirectoryDialog(shell, SWT.OPEN);
                String folderPerftestPath = dlgPerftestPath.open();
                if (folderPerftestPath != null) {
                    textPerftest.setText(folderPerftestPath);
                }
            }
        });

        // two radio button for publisher or subscriber
        Group groupPubSub = new Group(groupGeneral, SWT.NONE);
        groupPubSub.setLayout(new GridLayout(2, false));
        groupPubSub.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Button pub = new Button(groupPubSub, SWT.RADIO);
        pub.setText("Run Publisher");
        pub.setToolTipText("Set test to be a publisher.");
        pub.setSelection(true);
        Button sub = new Button(groupPubSub, SWT.RADIO);
        sub.setText("Run Subscriber");
        sub.setToolTipText("Set test to be a subscriber.");

        // Platform
        Group groupPlatform = new Group(groupGeneral, SWT.NONE);
        groupPlatform.setLayout(new GridLayout(2, false));
        groupPlatform.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelPlatform = new Label(groupPlatform, SWT.NONE);
        labelPlatform.setText("Platform");
        labelPlatform.setToolTipText("Architecture/Platform for which build.sh is going to compile RTI Perftest.");
        Combo comboPlatform = new Combo(groupPlatform, SWT.READ_ONLY);
        comboPlatform.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        comboPlatform.setItems(possiblePlatform);
        comboPlatform.setText(possiblePlatform[possiblePlatform.length - 1]);

        // domain id
        Group groupDomain = new Group(groupGeneral, SWT.NONE);
        groupDomain.setLayout(new GridLayout(2, false));
        groupDomain.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Label labelDomain = new Label(groupDomain, SWT.NONE);
        labelDomain.setText("Domain");
        labelDomain.setToolTipText(
                "The publisher and subscriber applications must use the same domain ID in order to communicate.");
        Text textDomain = new Text(groupDomain, SWT.BORDER);
        textDomain.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textDomain);
        textDomain.setMessage("0");

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
        Button language_java = new Button(groupLanguage, SWT.RADIO);
        language_java.setText("JAVA");
        Button language_cs = new Button(groupLanguage, SWT.RADIO);
        language_cs.setText("C#");
        language_cs.setEnabled(getOperatingSystemType() == OSType.Win);

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
        groupKey.setText("Key data");
        Label labelInstances = new Label(groupKey, SWT.NONE);
        labelInstances.setText("Instances");
        labelInstances.setToolTipText(
                "Set the number of instances to use in the test. The publishing and subscribing applications must specify the same number of instances.This option only makes sense when testing a keyed data type.");
        Text textInstances = new Text(groupKey, SWT.BORDER);
        textInstances.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textInstances);
        textInstances.setMessage("1");
        Button keyed = new Button(groupKey, SWT.CHECK);
        keyed.setText("keyed");
        keyed.setToolTipText("Specify the use of a keyed type.");

        // five buttons for compile and advanced options and security option
        Group groupButtons = new Group(compositeExecution, SWT.NONE);
        groupButtons.setLayout(new GridLayout(4, false));
        groupButtons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
        Button btnAdvancedOptionsExecution = new Button(groupButtons, SWT.PUSH);
        btnAdvancedOptionsExecution.setText("Advanced Options");
        btnAdvancedOptionsExecution.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Button btnAdvancedOptionsPub = new Button(groupButtons, SWT.PUSH);
        btnAdvancedOptionsPub.setText("Advanced Options Pub");
        btnAdvancedOptionsPub.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Button btnAdvancedOptionsSub = new Button(groupButtons, SWT.PUSH);
        btnAdvancedOptionsSub.setText("Advanced Options Sub");
        btnAdvancedOptionsSub.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        btnAdvancedOptionsSub.setEnabled(false);
        Button btnSecureOption = new Button(groupButtons, SWT.PUSH);
        btnSecureOption.setText("Secure Options");
        btnSecureOption.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        // two buttons for run and stop and a checkbox for the real time display
        // data
        Group groupButton3s = new Group(groupButtons, SWT.NONE);
        groupButton3s.setLayout(new GridLayout(5, false));
        groupButton3s.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
        groupButton3s.setText("Execution");

        Button btnExecute = new Button(groupButton3s, SWT.PUSH);
        btnExecute.setText("Run");
        btnExecute.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        Group groupButton2s = new Group(groupButton3s, SWT.NONE);
        groupButton2s.setLayout(new GridLayout(1, false));
        groupButton2s.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Button display_real_time = new Button(groupButton2s, SWT.CHECK);
        display_real_time.setText("Real time");
        display_real_time
                .setToolTipText("Display all the output real time, else display at the end in order to use less CPU.");
        display_real_time.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 2, 1));
        display_real_time.setSelection(true);

        Button btnStop = new Button(groupButton3s, SWT.PUSH);
        btnStop.setText("Stop");
        btnStop.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

        // text command
        Text textCommand = new Text(compositeExecution, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL);
        textCommand.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
        textCommand.computeSize(100, 100, true);
        listTextParameter.add(textCommand);

        folder_output = new TabFolder(compositeExecution, SWT.NONE);
        folder_output.setLayout(new GridLayout(4, false));
        folder_output.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
        folder_output.setBackground(new Color(Display.getCurrent(), 225, 225, 234));

        // text output command
        TabItem tabExecuteOutputText = new TabItem(folder_output, SWT.NONE);
        tabExecuteOutputText.setText("Text Output");
        Composite compositeExecutionOutputText = new Composite(folder_output, SWT.NONE);
        compositeExecutionOutputText.setLayout(new GridLayout(4, false));
        tabExecuteOutputText.setControl(compositeExecutionOutputText);

        StyledText outputTextField = new StyledText(compositeExecutionOutputText,
                SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        outputTextField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));

        // Chart output command
        TabItem tabExecuteOutputChart = new TabItem(folder_output, SWT.NONE);
        tabExecuteOutputChart.setText("Chart Output");

        Composite compositeExecutionOutputChart = new Composite(folder_output, SWT.NONE);
        compositeExecutionOutputChart.setLayout(new FillLayout());
        tabExecuteOutputChart.setControl(compositeExecutionOutputChart);
        Chart_RTIPerftest chart = new Chart_RTIPerftest(compositeExecutionOutputChart);

        // listener button compile
        btnExecute.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Language language = Language.cpp; // by default
                System.out.println("Button compile clicked");
                if (textPerftest.getText().replaceAll("\\s+", "").equals("")) {
                    show_error("The path to the build of perftest is necessary.");
                    return;
                } else {
                    if (path_exists(textPerftest.getText().replaceAll("\\s+", ""))) {
                        mapParameter.put("Perftest", textPerftest.getText().replaceAll("\\s+", ""));
                    } else {
                        show_error("The path '" + textPerftest.getText().replaceAll("\\s+", "")
                                + "' to the build of perftest does not exists.");
                        return;
                    }
                }

                if (!language_java.getSelection() && comboPlatform.getText().replaceAll("\\s+", "").equals("")) {
                    show_error("The platform of the execution is necessary.");
                    return;
                } else {
                    mapParameter.put("platform", comboPlatform.getText().replaceAll("\\s+", ""));
                }
                if (language_cpp.getSelection()) {
                    language = Language.cpp;
                } else if (language_cpp03.getSelection()) {
                    language = Language.cpp03;
                } else if (language_cs.getSelection()) {
                    if (comboPlatform.getText().replaceAll("\\s+", "").contains("Linux")
                            || comboPlatform.getText().replaceAll("\\s+", "").contains("Darwin")) {
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
                // TODO cleanInput(outputTextField,listTextCompile);
                folder_output.setBackground(new Color(Display.getCurrent(), 209, 209, 224)); // Default
                                                                                             // color
                if (!get_paramenter("-pub").isEmpty()) {
                    executePerftest(textCommand, outputTextField, language, ExecutionType.Pub, chart,
                            display_real_time.getSelection());
                    chart.setType(ExecutionType.Pub);
                } else { // if (!get_paramenter("-sub").isEmpty()) {
                    executePerftest(textCommand, outputTextField, language, ExecutionType.Sub, chart,
                            display_real_time.getSelection());
                    chart.setType(ExecutionType.Sub);
                }
            }
        });

        // listener button advanced options execution
        btnAdvancedOptionsExecution.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                System.out.println("Button advanced options execution clicked");
                display_execution_advanced_options();
            }
        });

        // listener button advanced options pub
        btnAdvancedOptionsPub.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                System.out.println("Button advanced options pub clicked");
                display_pub_advanced_options();
            }
        });

        // listener button advanced options sub
        btnAdvancedOptionsSub.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                System.out.println("Button advanced options sub clicked");
                display_sub_advanced_options();
            }
        });

        // listener button SecureOption
        btnSecureOption.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                System.out.println("Button btnSecureOption clicked");
                show_error("Security paramters are not implemented yet");
            }
        });

        // Activate Pub advanced options
        pub.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                System.out.println("Button pub clicked");
                btnAdvancedOptionsSub.setEnabled(false);
                btnAdvancedOptionsPub.setEnabled(true);
            }
        });

        // Activate Sub advanced options
        sub.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                System.out.println("Button sub clicked");
                btnAdvancedOptionsPub.setEnabled(false);
                btnAdvancedOptionsSub.setEnabled(true);
            }
        });

        // Stop job
        btnStop.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                kill_job();
                outputTextField.setText("");
            }
        });

        SelectionAdapter languageListenerExecution = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                System.out.println("Button language clicked execution");
                if (System.getenv("NDDSHOME") != null && System.getenv("NDDSHOME").contains("5.3")) {
                    // TODO check that it is 5.3.0 >=
                    List<String> newListPlaform = new ArrayList<String>();
                    if (language_cpp.getSelection()) {
                        newListPlaform = listPlatform.getListPlaform("C++");
                    } else if (language_cpp03.getSelection()) {
                        newListPlaform = listPlatform.getListPlaform("C++03");
                    } else if (language_java.getSelection()) {
                        newListPlaform = listPlatform.getListPlaform("Java");
                    } else if (language_cs.getSelection()) {
                        newListPlaform = listPlatform.getListPlaform("C#");
                    }
                    if (!newListPlaform.isEmpty()) {
                        possiblePlatform = (String[]) newListPlaform.toArray(new String[0]);
                        Arrays.sort(possiblePlatform);
                        comboPlatform.setItems(possiblePlatform);
                        comboPlatform.setText(possiblePlatform[possiblePlatform.length - 1]);
                    } else {
                        possiblePlatform = new String[] {};
                        comboPlatform.setItems(possiblePlatform);
                    }
                }
            }
        };

        // Listener language_cpp, language_cpp03, language_cs, language_java
        language_cpp.addSelectionListener(languageListenerExecution);
        language_cpp03.addSelectionListener(languageListenerExecution);
        language_cs.addSelectionListener(languageListenerExecution);
        language_java.addSelectionListener(languageListenerExecution);
    }

    /**
     * kill the job after the execution. First check if there are a job running.
     * 
     * @return boolean, true if job is killed
     */
    private boolean kill_job() {
        if (exec.getWatchdog() != null) {
            // TODO check if it is necessary -
            // if(exec.getWatchdog().isWatching()) {
            exec.getWatchdog().destroyProcess();
            return true;
        }
        return false;
    }

    private static class StyledTextOutputStreamCompile extends LogOutputStream {
        private StyledText outputControl;
        private Map<String, Color> colors;// create dictionary with parameter

        private int c;

        public StyledTextOutputStreamCompile(StyledText _outputControl) {
            outputControl = _outputControl;
            colors = new HashMap<String, Color>();
            colors.put("[ERROR]:", Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
            colors.put("[INFO]:", Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
            colors.put("\033[0;31m", Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
            colors.put("\033[0;32m", Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
            colors.put("\033[0;33m", Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
            colors.put("\033[0m", Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
            c = 0;

        }

        @Override
        protected void processLine(String line, int level) {
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    String lineCopy = line;
                    Color specific_color = null;
                    for (Map.Entry<String, Color> color : colors.entrySet()) {
                        if (lineCopy.contains(color.getKey())) {
                            lineCopy = lineCopy.replace(color.getKey(), "");
                            specific_color = color.getValue();
                        }
                    }
                    outputControl.append(lineCopy + "\n");
                    if (specific_color != null) {
                        outputControl.setLineBackground(c, 1, specific_color);
                    }
                    c++;
                }
            });
            System.out.println(line);
        }
    }

    private static class StyledTextOutputStreamExecution extends LogOutputStream {
        private StyledText outputControl;
        private Chart_RTIPerftest chart;
        private ExecutionType type;
        private boolean display_real_time;
        private boolean display_end;
        private Double instant_value;
        private Double ave_value;
        private boolean incompatibility;
        private TabFolder folder_output;
        private String[] key_error;

        public StyledTextOutputStreamExecution(StyledText _outputControl, Chart_RTIPerftest _chart, ExecutionType _type,
                boolean _display_real_time, TabFolder _folder_output) {
            outputControl = _outputControl;
            chart = _chart;
            type = _type;
            display_real_time = _display_real_time;
            display_end = false;
            instant_value = -1.0;
            ave_value = -1.0;
            incompatibility = false;
            folder_output = _folder_output;
            key_error = new String[] { "incompatible", "Problem", "ERROR", "inconsistent" };
        }

        @Override
        protected void processLine(String line, int level) {
            instant_value = -1.0;
            ave_value = -1.0;
            if (type == ExecutionType.Pub) {
                if (line.contains("One way Latency:")) {
                    // One way Latency: 42 us Ave 321 us Std 173.6 us Min 39 us
                    // Max 636
                    instant_value = Double.parseDouble(line
                            .substring(line.indexOf("Latency:") + 8, line.indexOf("us  Ave")).replaceAll("\\s+", ""));
                    ave_value = Double.parseDouble(
                            line.substring(line.indexOf("Ave") + 3, line.indexOf("us  Std")).replaceAll("\\s+", ""));
                    chart.add(instant_value, ave_value);
                }

            } else { // if (type == ExecutionType.Sub) {
                if (line.contains("Packets/s:")) {
                    // Packets: 2097033 Packets/s: 32791 Packets/s(ave): 51858
                    // Mbps: 26.2 Mbps(ave): 41.5 Lost: 0
                    instant_value = Double.parseDouble(line
                            .substring(line.indexOf("Mbps:") + 5, line.indexOf("Mbps(ave):")).replaceAll("\\s+", ""));
                    ave_value = Double.parseDouble(line
                            .substring(line.indexOf("Mbps(ave):") + 10, line.indexOf("Lost:")).replaceAll("\\s+", ""));
                    chart.add(instant_value, ave_value);
                }

            }
            if (line.contains("Length:")) { // Always display at the end
                display_end = true;
            }
            if (display_real_time) {
                for (String element : key_error) {
                    if (line.contains(element)) {
                        incompatibility = true;
                        break;
                    }
                }
            }
            if (display_real_time || display_end) {
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        String lineCopy = line;
                        chart.update();
                        chart.redraw();
                        outputControl.append(lineCopy + "\n");
                        outputControl.setTopIndex(outputControl.getLineCount() - 1);
                        if (incompatibility) {
                            folder_output.setBackground(new Color(Display.getCurrent(), 255, 102, 102));
                        }
                    }
                });
            }

            System.out.println(line);
        }
    }

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        GUI_RTIPerftest GuiRtiPerftest = new GUI_RTIPerftest();
    }
}
