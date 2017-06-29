# GUI_RTI_Perftest
Simple GUI in Java for *RTI PErftest* where you can compile and execute and see the result

# Execute in Eclipse

## Clone the project

```
git clone https://github.com/softwarejimenez/GUI_RTI_Perftest.git
```

## Import Java Project.

* Click in File -> Import.  
* Select General -> Projects from Folder or Archive
* Find directory *GUI_RTI_Perftest*
* Click on Finish


## Download the necessary package

* [GUI SWT CHART](http://www.java2s.com/Code/Jar/o/Downloadorgswtchart060jar.htm)
* [APACHE EXECUTOR](http://www.java2s.com/Code/Jar/c/Downloadcommonsexec13jar.htm)

Unzip the packages.

## Configure Eclipse to use SWT

* Download SWT stable release for Eclipse in [Eclipse SWT Project Page](http://www.eclipse.org/swt/)
* Download a zip file that contains our org.eclipes.swt project
* Inside Eclipse, select Import / Existing Projects into Workspace.

## Add Java build path

In eclipse, go to the properties of the Java project and to the tab "Java Build Path"

* Go to Library tab, then *Add External JARs..* and add GUI SWT CHART and APACHE EXECUTOR.

* Go to Project tab, press on *Add.* and select org.eclipse.swt.

##Execute

Click on *Run GUI_RTIPerftest*