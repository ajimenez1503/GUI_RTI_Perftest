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
    String[] possiblePlatform;
    Display display;

    
    /**
     * Constructor
     */
    public GUI_RTIPerftest(Display _display) {
        display=_display;
        set_all_possible_platform();
        listTextParameter = new ArrayList<Text>();
        mapParameter = new HashMap<String, String>();
        shell = new Shell(display, SWT.SHELL_TRIM | SWT.CENTER);
        folder = new TabFolder(shell, SWT.NONE);
        initUI(display);
    }
    
    
    /**
     * Set the list of Platform according to the OS
     */
    private void set_all_possible_platform() {
        if (getOperatingSystemType() == OSType.Linux) {
            possiblePlatform = new String[] { "", "x64Linux3gcc5.4.0", "x64Linux3gcc4.8.2"};
        } else if (getOperatingSystemType() == OSType.Windows) {
            possiblePlatform = new String[] { "", "x64Win64VS2015"};
        } else if (getOperatingSystemType() == OSType.Darwin) {
            possiblePlatform = new String[] { "", "x64Darwin16clang8.0"};
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
        String command = mapParameter.get("--nddshome");
        command += mapParameter.get("--platform");
        command += mapParameter.get("--skip-cpp-build");
        command += mapParameter.get("--skip-cpp03-build");
        command += mapParameter.get("--skip-java-build");
        command += mapParameter.get("--debug");
        command += mapParameter.get("--secure");
        command += mapParameter.get("--openssl-home");

        // check if Linux or Win or Darwin
        if (getOperatingSystemType() == OSType.Linux
                || mapParameter.get("--platform").toLowerCase().contains("linux")) {
            command = mapParameter.get("Perftest") + "/build.sh" + command;
        } else if (getOperatingSystemType() == OSType.Windows
                || mapParameter.get("--platform").toLowerCase().contains("win")) {
            command = mapParameter.get("Perftest") + "/build.bat" + command;
            command += mapParameter.get("--skip-cs-build");
            // C# just in win
        } else if (getOperatingSystemType() == OSType.Darwin
                || mapParameter.get("--platform").toLowerCase().contains("darwin")) {
            command = mapParameter.get("Perftest") + "/build.sh" + command;
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
        String command = mapParameter.get("Perftest") + "/bin/";

        // check if Linux or Win or Darwin
        if (getOperatingSystemType() == OSType.Linux
                || mapParameter.get("--platform").toLowerCase().contains("linux")) {
            switch (language) {
            case cpp:
                command += mapParameter.get("platform") + "/release/perftest_cpp";
                break;
            case cpp03:
                command += mapParameter.get("platform") + "/release/perftest_cpp";
                break;
            case cs:
                return false;
            case java:
                command += "Release/perftest_java.sh";
                break;
            }
        } else if (getOperatingSystemType() == OSType.Windows
                || mapParameter.get("--platform").toLowerCase().contains("win")) {
            switch (language) {
            case cpp:
                command += mapParameter.get("platform") + "/release/perftest_cpp";
                break;
            case cpp03:
                command += mapParameter.get("platform") + "/release/perftest_cpp";
                break;
            case cs:
                command += mapParameter.get("platform") + "/release/perftest_cs";
                break;
            case java:
                command += "Release/perftest_java.bat";
                break;
            }
        } else if (getOperatingSystemType() == OSType.Darwin
                || mapParameter.get("--platform").toLowerCase().contains("darwin")) {
            switch (language) {
            case cpp:
                command += mapParameter.get("platform") + "/release/perftest_cpp";
                break;
            case cpp03:
                command += mapParameter.get("platform") + "/release/perftest_cpp";
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

        command += mapParameter.get("-pub");
        command += mapParameter.get("-sub");
        command += mapParameter.get("-domain");
        command += mapParameter.get("-bestEffort");
        command += mapParameter.get("-dataLen");
        command += mapParameter.get("-executionTime");
        command += mapParameter.get("-pubRate");
        command += mapParameter.get("-sendQueueSize");
        command += mapParameter.get("-numPublishers");
        command += mapParameter.get("-sidMultiSubTest");
        command += " -noXmlQos";// always use the QoS from the String

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
        if (getOperatingSystemType() == OSType.Linux
                || mapParameter.get("--platform").toLowerCase().contains("linux")) {
            command = mapParameter.get("Perftest") + "/build.sh --clean";
        } else if (getOperatingSystemType() == OSType.Windows
                || mapParameter.get("--platform").toLowerCase().contains("win")) {
            command = mapParameter.get("Perftest") + "/build.bat --clean";
        } else if (getOperatingSystemType() == OSType.Darwin
                || mapParameter.get("--platform").toLowerCase().contains("darwin")) {
            command = mapParameter.get("Perftest") + "/build.sh --clean";
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

        // Perftest path
        Label labelPerftest = new Label(compositeCompile, SWT.NONE);
        labelPerftest.setText("Perftest path");
        labelPerftest.setToolTipText("Path to the RTI perftest bundle");
        Text textPerftest = new Text(compositeCompile, SWT.BORDER);
        textPerftest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textPerftest);
        textPerftest.setText("/home/ajimenez/github/rtiperftest_test"); // TODO
                                                                        // delete

        // NDDSHOME
        Label labelNDDSHOME = new Label(compositeCompile, SWT.NONE);
        labelNDDSHOME.setText("NDDSHOME");
        labelNDDSHOME.setToolTipText(
                "Path to the RTI Connext DDS installation. If this parameter is not present, the $NDDSHOME variable should be.");
        Text textNDDSHOME = new Text(compositeCompile, SWT.BORDER);
        textNDDSHOME.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textNDDSHOME);
        textNDDSHOME.setText("/home/ajimenez/rti_connext_dds-5.2.7");

        // Platform
        Label labelPlaform = new Label(compositeCompile, SWT.NONE);
        labelPlaform.setText("Plaform");
        labelPlaform.setToolTipText("Architecture/Platform for which build.sh is going to compile RTI Perftest.");
        Combo comboPlaform = new Combo(compositeCompile, SWT.READ_ONLY);
        comboPlaform.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        comboPlaform.setItems(possiblePlatform);
        comboPlaform.setText(possiblePlatform[possiblePlatform.length - 1]);

        // four radio for the languages
        Group groupLanguage = new Group(compositeCompile, SWT.NONE);
        groupLanguage.setLayout(new GridLayout(4, true));
        groupLanguage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
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

        // two radio button for the linker
        Button linkerStatic = new Button(compositeCompile, SWT.RADIO);
        linkerStatic.setText("Static linked");
        linkerStatic.setToolTipText("Compile using the RTI Connext DDS Static libraries.");
        linkerStatic.setSelection(true);
        Button linkerDynamic = new Button(compositeCompile, SWT.RADIO);
        linkerDynamic.setToolTipText("Compile using the RTI Connext DDS Dynamic libraries.");
        linkerDynamic.setText("Dynamic linked");

        // two checkboxs for security and debug
        Button security = new Button(compositeCompile, SWT.CHECK);
        security.setToolTipText(
                "Enable the compilation of the Perfest code specific for security and adds the RTI Connext DDS Security Libraries in the linking step (if compiling statically). ");
        security.setText("Enable security");
        Button debug = new Button(compositeCompile, SWT.CHECK);
        debug.setToolTipText("Compile using the RTI Connext DDS debug libraries.");
        debug.setText("Debug libraries");

        // OpenSSL
        Label labelOpenSSL = new Label(compositeCompile, SWT.NONE);
        labelOpenSSL.setText("Path to the openSSL");
        labelOpenSSL.setToolTipText(
                "Path to the openSSL home directory. Needed when compiling using the --secure option and statically.");
        Text textOpenSSL = new Text(compositeCompile, SWT.BORDER);
        textOpenSSL.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textOpenSSL);

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
        labelNumPublishers.setToolTipText("The subscribing application will wait for this number of publishing applications to start.");
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
        
        shellAdvancedOptionSub.setSize(200, 100); 
        shellAdvancedOptionSub.open(); 
        shellAdvancedOptionSub.pack(); 
        shellAdvancedOptionSub.addListener(SWT.Close, new Listener() 
        { 
           @Override 
           public void handleEvent(Event event) 
           { 
               if (!textNumPublishers.getText().replaceAll("\\s+", "").equals("")) {
                   mapParameter.put("-numPublishers", " -numPublishers " + textNumPublishers.getText().replaceAll("\\s+", ""));
               } else {
                   mapParameter.put("-numPublishers", "");
               }
               if (!textSidMultiSubTest.getText().replaceAll("\\s+", "").equals("")) {
                   mapParameter.put("-sidMultiSubTest", " -sidMultiSubTest " + textSidMultiSubTest.getText().replaceAll("\\s+", ""));
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
        shellAdvancedOptionPub.setLayout(new GridLayout(4, false));

        // PubRate
        Label labelPubRate = new Label(shellAdvancedOptionPub, SWT.NONE);
        labelPubRate.setText("Publication Rate");
        labelPubRate.setToolTipText("Limit the throughput to the specified number of samples per second.");
        Text textPubRate = new Text(shellAdvancedOptionPub, SWT.BORDER);
        textPubRate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textPubRate);

        // SendQueueSize
        Label labelSendQueueSize = new Label(shellAdvancedOptionPub, SWT.NONE);
        labelSendQueueSize.setText("Send Queue Size");
        labelSendQueueSize.setToolTipText("Size of the send queue.");
        Text textSendQueueSize = new Text(shellAdvancedOptionPub, SWT.BORDER);
        textSendQueueSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textSendQueueSize);

        shellAdvancedOptionPub.open(); 
        shellAdvancedOptionPub.pack();
        shellAdvancedOptionPub.setSize(400, 400); 
        shellAdvancedOptionPub.addListener(SWT.Close, new Listener() 
        { 
           @Override 
           public void handleEvent(Event event) 
           { 
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
              shellAdvancedOptionPub.dispose(); 
           } 
        }); 
    }
    
  
    /**
     * Display the execution tab
     *
     */
    private void display_tab_execution() {
        TabItem tabExecute = new TabItem(folder, SWT.NONE);
        tabExecute.setText("Execution");

        Composite compositeExecution = new Composite(folder, SWT.NONE);
        compositeExecution.setLayout(new GridLayout(4, false));
        tabExecute.setControl(compositeExecution);

        // Perftest path
        Label labelPerftest = new Label(compositeExecution, SWT.NONE);
        labelPerftest.setText("Perftest path");
        labelPerftest.setToolTipText("Path to the RTI perftest bundle");
        Text textPerftest = new Text(compositeExecution, SWT.BORDER);
        textPerftest.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textPerftest);
        textPerftest.setText("/home/ajimenez/github/rtiperftest_test"); // TODO
                                                                        // delete

        // two radio button for publisher or subscriber
        Button pub = new Button(compositeExecution, SWT.RADIO);
        pub.setText("Run Publisher");
        pub.setToolTipText("Set test to be a publisher.");
        pub.setSelection(true);
        Button sub = new Button(compositeExecution, SWT.RADIO);
        sub.setText("Run Subscriber");
        sub.setToolTipText("Set test to be a subscriber.");

        // Platform
        Label labelPlaform = new Label(compositeExecution, SWT.NONE);
        labelPlaform.setText("Plaform");
        labelPlaform.setToolTipText("Architecture/Platform for which build.sh is going to compile RTI Perftest.");
        Combo comboPlaform = new Combo(compositeExecution, SWT.READ_ONLY);
        comboPlaform.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        comboPlaform.setItems(possiblePlatform);
        comboPlaform.setText(possiblePlatform[possiblePlatform.length - 1]);

        // domain id
        Label labelDomain = new Label(compositeExecution, SWT.NONE);
        labelDomain.setText("Domain");
        labelDomain.setToolTipText(
                "The publisher and subscriber applications must use the same domain ID in order to communicate.");
        Text textDomain = new Text(compositeExecution, SWT.BORDER);
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
        reliabiliy.setToolTipText("Use best-effort communication.");

        // two radio button for the keyed and unkeyed
        Group groupKey = new Group(compositeExecution, SWT.NONE);
        groupKey.setLayout(new GridLayout(2, false));
        groupKey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        Button unkeyed = new Button(groupKey, SWT.RADIO);
        unkeyed.setToolTipText("Specify the use of a unkeyed type.");
        unkeyed.setText("keyed");
        unkeyed.setSelection(true);
        Button keyed = new Button(groupKey, SWT.RADIO);
        keyed.setText("keyed");
        keyed.setToolTipText("Specify the use of a keyed type.");

        // Date Length
        Label labelDataLen = new Label(compositeExecution, SWT.NONE);
        labelDataLen.setText("Data Length");
        keyed.setToolTipText("Length of payload in bytes for each send.");
        Text textDataLen = new Text(compositeExecution, SWT.BORDER);
        textDataLen.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        listTextParameter.add(textDataLen);
        textDataLen.setText("100");

        // Execution Time
        Label labelExecuteionTime = new Label(compositeExecution, SWT.NONE);
        labelExecuteionTime.setText("Execution Time");
        labelExecuteionTime.setToolTipText(
                "Allows you to limit the test duration by specifying the number of seconds to run the test.");
        Text textExecuteionTime = new Text(compositeExecution, SWT.BORDER);
        textExecuteionTime.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        textExecuteionTime.setText("120");
        listTextParameter.add(textExecuteionTime);
        
        // four radio for the languages
        Group groupTransport = new Group(compositeExecution, SWT.NONE);
        groupTransport.setLayout(new GridLayout(4, false));
        groupTransport.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        groupTransport.setText("Language to execute");
        Button UDP = new Button(groupTransport, SWT.RADIO);
        UDP.setText("UDP");
        UDP.setSelection(true);
        Button ShareMemory = new Button(groupTransport, SWT.RADIO);
        ShareMemory.setText("Share Memory");
        Button TCP = new Button(groupTransport, SWT.RADIO);
        TCP.setText("TCP");
      

        // three buttons for compile and advance option and security option
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
                        show_error("You must specify a correct platform");
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
                if (!textDataLen.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-dataLen", " -dataLen " + textDataLen.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-dataLen", "");
                }
                if (!textExecuteionTime.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("-executionTime",
                            " -executionTime " + textExecuteionTime.getText().replaceAll("\\s+", ""));
                } else {
                    mapParameter.put("-executionTime", "");
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
