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

public class GUI_RTIPerftest {

    /**
     * types of Operating Systems
     */
    private enum OSType {
        Windows, Darwin, Linux, Other
    };

    private static OSType detectedOS; // cached result of OS detection
    private Shell shell;
    private TabFolder folder;
    private ArrayList<Text> listTextCompile; // create list of text elements
    private Map<String, String> mapParameter;// create dictionary with parameter

    /**
     * Constructor
     */
    public GUI_RTIPerftest(Display display) {
        listTextCompile = new ArrayList<Text>();
        mapParameter = new HashMap<String, String>();
        shell = new Shell(display, SWT.SHELL_TRIM | SWT.CENTER);
        folder = new TabFolder(shell, SWT.NONE);
        initUI(display);
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

        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.grabExcessHorizontalSpace = true;

        // Perftest path
        Label labelPerftest = new Label(compositeCompile, SWT.NONE);
        labelPerftest.setText("Perftest path");
        Text textPerftest = new Text(compositeCompile, SWT.BORDER);
        textPerftest.setLayoutData(gridData);
        listTextCompile.add(textPerftest);
        textPerftest.setText("/home/jimenez/Escritorio/PERFTEST/perftest"); // TODO
                                                                            // delete

        // NDDSHOME
        Label labelNDDSHOME = new Label(compositeCompile, SWT.NONE);
        labelNDDSHOME.setText("NDDSHOME");
        Text textNDDSHOME = new Text(compositeCompile, SWT.BORDER);
        textNDDSHOME.setLayoutData(gridData);
        listTextCompile.add(textNDDSHOME);

        // Platform
        Label labelPlaform = new Label(compositeCompile, SWT.NONE);
        labelPlaform.setText("Plaform");
        Text textPlaform = new Text(compositeCompile, SWT.BORDER);
        textPlaform.setLayoutData(gridData);
        listTextCompile.add(textPlaform);
        textPlaform.setText("x64Linux3gcc5.8.2"); // TODO delete

        // four checkboxs for the languages
        Button language_cpp = new Button(compositeCompile, SWT.CHECK);
        language_cpp.setText("CPP");
        language_cpp.setSelection(true);
        Button language_cpp03 = new Button(compositeCompile, SWT.CHECK);
        language_cpp03.setText("CPP03");
        Button language_cs = new Button(compositeCompile, SWT.CHECK);
        language_cs.setText("C#");
        Button language_java = new Button(compositeCompile, SWT.CHECK);
        language_java.setText("JAVA");

        // two radio button for the for the linker
        Button linkerStatic = new Button(compositeCompile, SWT.RADIO);
        linkerStatic.setText("Static linked");
        linkerStatic.setSelection(true);
        Button linkerDynamic = new Button(compositeCompile, SWT.RADIO);
        linkerDynamic.setText("Dynamic linked");

        // two checkboxs for security and debug
        Button security = new Button(compositeCompile, SWT.CHECK);
        security.setText("Enable security");
        Button debug = new Button(compositeCompile, SWT.CHECK);
        debug.setText("Debug libraries");

        // OpenSSL
        Label labelOpenSSL = new Label(compositeCompile, SWT.NONE);
        labelOpenSSL.setText("Path to the openSSL");
        Text textOpenSSL = new Text(compositeCompile, SWT.BORDER);
        textOpenSSL.setLayoutData(gridData);
        listTextCompile.add(textOpenSSL);

        // two buttons for compile and clean
        Button btnCompile = new Button(compositeCompile, SWT.PUSH);
        btnCompile.setText("Compile");
        btnCompile.setLayoutData(gridData);
        Button btnClean = new Button(compositeCompile, SWT.PUSH);
        btnClean.setText("Clean");
        btnClean.setLayoutData(gridData);

        // text command
        Text textCommand = new Text(compositeCompile, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL);
        GridData gridDataTextCommand = new GridData();
        gridDataTextCommand.horizontalSpan = 2;
        gridDataTextCommand.horizontalAlignment = SWT.FILL;
        gridDataTextCommand.grabExcessHorizontalSpace = true;
        textCommand.setLayoutData(gridDataTextCommand);
        textCommand.computeSize(100, 100, true);
        listTextCompile.add(textCommand);

        // text output command
        List listOutput = new List(compositeCompile, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData gridDataListOuput = new GridData();
        gridDataListOuput.horizontalSpan = 2;
        gridDataListOuput.horizontalAlignment = SWT.FILL;
        gridDataListOuput.grabExcessHorizontalSpace = true;
        gridDataListOuput.verticalAlignment = SWT.FILL;
        gridDataListOuput.grabExcessVerticalSpace = true;
        listOutput.setLayoutData(gridDataListOuput);

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
                if (!textPlaform.getText().replaceAll("\\s+", "").equals("")) {
                    mapParameter.put("--platform", " --platform " + textPlaform.getText().replaceAll("\\s+", ""));
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

    private void display_tab_execution() {
        // Tab 2 (execute)
        RowLayout layout = new RowLayout();
        layout.wrap = true;
        layout.pack = true;
        layout.justify = true;
        layout.type = SWT.VERTICAL;

        TabItem tab2 = new TabItem(folder, SWT.NONE);
        tab2.setText("execute");
        Composite group2 = new Composite(folder, SWT.SHADOW_IN);
        group2.setLayout(layout);

        Button quitBtn = new Button(group2, SWT.PUSH);
        quitBtn.setText("Quit");
        quitBtn.setLayoutData(new RowData(80, 30));

        quitBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                shell.getDisplay().dispose();
                System.exit(0);
            }
        });

        tab2.setControl(group2);
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
