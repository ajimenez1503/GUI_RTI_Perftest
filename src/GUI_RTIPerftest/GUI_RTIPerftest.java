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

public class GUI_RTIPerftest {
 public GUI_RTIPerftest(Display display) {
  initUI(display);
 }

 private void cleanInput(List listOutput, ArrayList < Text > text) {
  listOutput.removeAll();
  for (int i = 0; i < text.size(); i++) {
   text.get(i).setText("");
  }
 }

 private Boolean compile(Map < String, String > mapCompileParameter, Text textCommand, List listOutput) {

  //create parameter
  String commnad = " --nddshome " + mapCompileParameter.get("--nddshome");
  commnad += " --platform " + mapCompileParameter.get("--platform");

  //check if linux or win or Darwin
  System.out.println("PLATFORM:" + mapCompileParameter.get("-platform"));
  if (mapCompileParameter.get("--platform").toLowerCase().toLowerCase().contains("linux")) {
   commnad = mapCompileParameter.get("Perftest") + "/build.sh" + commnad;
  } else if (mapCompileParameter.get("--platform").toLowerCase().toLowerCase().contains("win")) {
   commnad = mapCompileParameter.get("Perftest") + "/build.bat" + commnad;
  } else if (mapCompileParameter.get("--platform").toLowerCase().toLowerCase().contains("darwin")) {
   commnad = mapCompileParameter.get("Perftest") + "/build.sh" + commnad;
  } else {
   return false;
  }


  //print command to run
  System.out.println(commnad);
  textCommand.setText(commnad);

  try {
   Process proc = Runtime.getRuntime().exec(commnad);
   BufferedReader read = new BufferedReader(new InputStreamReader(
    proc.getInputStream()));
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

 private void initUI(Display display) {

  Shell shell = new Shell(display, SWT.SHELL_TRIM | SWT.CENTER);

  shell.setLayout(new FillLayout());

  TabFolder folder = new TabFolder(shell, SWT.NONE);

  RowLayout layout = new RowLayout();
  layout.wrap = true;
  layout.pack = true;
  layout.justify = true;
  layout.type = SWT.VERTICAL;

  //Tab 1 (compile)
  //create dictionary with parameter
  Map < String, String > mapCompileParameter = new HashMap < String, String > ();

  //create list of text elements
  ArrayList listTextCompile = new ArrayList();

  TabItem tabCompile = new TabItem(folder, SWT.NONE);
  tabCompile.setText("compile");

  Composite compositeCompile = new Composite(folder, SWT.NONE);
  compositeCompile.setLayout(new GridLayout(2, false));

  GridData gridData = new GridData();
  gridData.horizontalAlignment = SWT.FILL;
  gridData.grabExcessHorizontalSpace = true;

  //Perftest path
  Label labelPerftest = new Label(compositeCompile, SWT.NONE);
  labelPerftest.setText("Perftest path");
  Text textPerftest = new Text(compositeCompile, SWT.BORDER);
  textPerftest.setLayoutData(gridData);
  listTextCompile.add(textPerftest);

  //NDDSHOME
  Label labelNDDSHOME = new Label(compositeCompile, SWT.NONE);
  labelNDDSHOME.setText("NDDSHOME");
  Text textNDDSHOME = new Text(compositeCompile, SWT.BORDER);
  textNDDSHOME.setLayoutData(gridData);
  listTextCompile.add(textNDDSHOME);

  //NDDSHOME
  Label labelPlaform = new Label(compositeCompile, SWT.NONE);
  labelPlaform.setText("Plaform");
  Text textPlaform = new Text(compositeCompile, SWT.BORDER);
  textPlaform.setLayoutData(gridData);
  listTextCompile.add(textPlaform);


  Button btnCompile = new Button(compositeCompile, SWT.PUSH);
  btnCompile.setText("Compile");
  btnCompile.setLayoutData(gridData);
  //btn.addListener(SWT.Selection, event -> System.out.println("Button clicked"));

  Button btnClean = new Button(compositeCompile, SWT.PUSH);
  btnClean.setText("Clean");
  btnClean.setLayoutData(gridData);


  // text command
  Text textCommand = new Text(compositeCompile, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
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


  btnCompile.addSelectionListener(new SelectionAdapter() {
   @Override
   public void widgetSelected(SelectionEvent e) {
    System.out.println("Button compile clicked");
    mapCompileParameter.put("Perftest", textPerftest.getText().replaceAll("\\s+", ""));
    mapCompileParameter.put("--nddshome", textNDDSHOME.getText().replaceAll("\\s+", ""));
    mapCompileParameter.put("--platform", textPlaform.getText().replaceAll("\\s+", ""));
    //TODO cleanInput(listOutput,listTextCompile);
    if (!compile(mapCompileParameter, textCommand, listOutput)) {
     MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING);
     messageBox.setText("Warning");
     messageBox.setMessage("You must specify a correct platform");
     messageBox.open();
     System.out.println("You must specify a correct platform");
    }
   }
  });


  btnClean.addSelectionListener(new SelectionAdapter() {
   @Override
   public void widgetSelected(SelectionEvent e) {
    System.out.println("Button clean clicked");
    //TODO
   }
  });

  tabCompile.setControl(compositeCompile);

  //Tab 2 (execute)
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



  shell.pack();
  shell.open();

  shell.setText("RTI perftest");
  shell.setSize(900, 900);


  while (!shell.isDisposed()) {
   if (!display.readAndDispatch()) {
    display.sleep();

   }
  }
 }


 @SuppressWarnings("unused")
 public static void main(String[] args) {
  //System.out.println(SWT.getPlatform());
  //System.out.println(SWT.getVersion());
  Display display = new Display();
  GUI_RTIPerftest ex = new GUI_RTIPerftest(display);
  display.dispose();
 }

}