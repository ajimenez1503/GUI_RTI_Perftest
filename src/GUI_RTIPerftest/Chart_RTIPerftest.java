package GUI_RTIPerftest;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.swtchart.Chart;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.ISeries.SeriesType;

import GUI_RTIPerftest.GUI_RTIPerftest.ExecutionType;

public class Chart_RTIPerftest {
    private Chart chart;
    private ArrayList<Double> data_instant;
    private ArrayList<Double> data_ave;
    private ILineSeries lineSeries;
    private ILineSeries lineSeriesAve;

    public Chart_RTIPerftest(Composite parent) {
        chart = new Chart(parent, SWT.None);
        chart.getAxisSet().getXAxis(0).getTick().setVisible(false);
        data_instant = new ArrayList<Double>();
        data_ave = new ArrayList<Double>();
    }

    /**
     * update chart
     * 
     * @param type
     *            ExecutionType
     */
    public void setType(ExecutionType type) {
        if (type == ExecutionType.Sub) {
            // set titles
            chart.getTitle().setText("Throughtput");
            chart.getAxisSet().getXAxis(0).getTitle().setText("Time");
            chart.getAxisSet().getYAxis(0).getTitle().setText("Mbps");
            lineSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, "Mbps");
            lineSeriesAve = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, "Mbps Ave");
        } else { // if (type == ExecutionType.Pub){
            // set titles
            chart.getTitle().setText("Latency");
            chart.getAxisSet().getXAxis(0).getTitle().setText("Time");
            chart.getAxisSet().getYAxis(0).getTitle().setText("Us");
            lineSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, "Us");
            lineSeriesAve = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, "Us Ave");
        }
        lineSeriesAve.setLineColor(Display.getDefault().getSystemColor(SWT.COLOR_RED));
        lineSeriesAve.setSymbolType(PlotSymbolType.NONE);
    }

    /**
     * reset chart
     * 
     */
    public void reset() {
        data_instant.clear();
        data_ave.clear();
        if (chart.getSeriesSet().getSeries().length > 0) {
            System.out.println("Reset chart");
            chart.getSeriesSet().deleteSeries(lineSeries.getId());
            chart.getSeriesSet().deleteSeries(lineSeriesAve.getId());
            this.redraw();
        }
    }

    /**
     * update array
     * 
     * @param instant_value
     *            double
     */
    public void update(double instant_value, double ave_value) {
        data_instant.add(instant_value);
        data_ave.add(ave_value);
        // create line series
        lineSeries.setYSeries(toPrimitive(data_instant.toArray(new Double[data_instant.size()])));
        lineSeriesAve.setYSeries(toPrimitive(data_ave.toArray(new Double[data_ave.size()])));
    }

    /**
     * update chart, redraw
     * 
     * @param instant_value
     *            double
     */
    public void redraw() {
        chart.redraw();
        // adjust the axis range
        chart.getAxisSet().adjustRange();
    }

    private double[] toPrimitive(Double[] array) {
        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].doubleValue();
        }
        return result;
    }
}
