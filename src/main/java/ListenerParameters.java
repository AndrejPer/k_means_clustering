import org.jxmapviewer.JXMapViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static java.lang.Integer.parseInt;

public class ListenerParameters implements ActionListener {

    JButton run;
    JRadioButton seq, par;
    JTextField clusters, sites;
    JPanel mapPanel;
    JXMapViewer mapViewer;
    JTextArea report;
    String[] args;

    public ListenerParameters(String[] args, JRadioButton seq, JRadioButton par, JButton run, JTextField c, JTextField s, JPanel mapPanel, JXMapViewer mapViewer, JTextArea report) {
        this.seq = seq;
        this.par = par;
        this.run = run;
        clusters = c;
        sites = s;
        this.mapPanel = mapPanel;
        this.mapViewer = mapViewer;
        this.report = report;
        this.args = args;

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(run == e.getSource()) {
            int clusterCount = parseInt(clusters.getText());
            int siteCount = parseInt(sites.getText());
            if(seq.isSelected()) {
                Computation computation = new Computation(clusterCount, siteCount);
                computation.compute();
                MapLoader.paintClusters(computation.getClusters(), computation.getSitePoints(), mapViewer);
                mapPanel.updateUI();
                report.setText("Clusters calculated in: " + computation.getTime() + "ms. Performed " + computation.getLoopCounter() + " loops.");
            }

            else if(par.isSelected()) {
                ParallelComputation parallelComputation = new ParallelComputation(clusterCount, siteCount);
                parallelComputation.compute(args);
                MapLoader.paintClusters(parallelComputation.getClusters(), parallelComputation.getSetPoints(), mapViewer);
                mapPanel.updateUI();
                report.setText("Clusters calculated in: " + parallelComputation.getTime() + "ms. Performed " + parallelComputation.getLoopCounter() + " loops.");
            }
        }
    }
}
