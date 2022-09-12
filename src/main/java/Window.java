import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.Arrays;

import org.jxmapviewer.JXMapViewer;

public class Window extends JFrame {
    //map
    JXMapViewer map;
    JPanel mapPanel;
    //report
    JTextArea report;
    JScrollPane scroll;

    public Window(String[] args){
        setVisible(true);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //creating a panel for parameters selection
        JPanel parametersPanel = new JPanel();
        parametersPanel.setLayout(new BoxLayout(parametersPanel, BoxLayout.Y_AXIS));
        //mode buttons
        JRadioButton seq = new JRadioButton();
        seq.setText("Sequential");
        seq.setSelected(true);
        JRadioButton par = new JRadioButton();
        par.setText("Parallel");
        par.setEnabled(true);

        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(seq);
        modeGroup.add(par);

        //number input//
        JTextField clusterNumber = new JTextField("3");
        clusterNumber.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel clusterLabel = new JLabel("Number of clusters:");
        JTextField siteNumber = new JTextField("50");
        siteNumber.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel siteLabel = new JLabel("Number of sites:");

        //report
        report = new JTextArea();
        report.setLineWrap(true);


        //map
        map = MapLoader.getInstance(this);
        mapPanel = new JPanel();
        mapPanel.setLayout(new BorderLayout(3, 3));
        //mapPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        mapPanel.add(map);
        this.add(mapPanel, BorderLayout.CENTER);


        //######
        Border border = BorderFactory.createLineBorder(Color.BLUE);
        //######
        JButton run = new JButton("Run");
        ListenerParameters lParameters = new ListenerParameters(args, seq, par, run, clusterNumber, siteNumber, mapPanel, map, report);
        seq.setBorder(border);
        parametersPanel.add(seq);
        parametersPanel.add(par);
        parametersPanel.add(clusterLabel);
        parametersPanel.add(clusterNumber);
        parametersPanel.add(siteLabel);
        parametersPanel.add(siteNumber);
        clusterNumber.addActionListener(lParameters);
        parametersPanel.add(run);
        run.addActionListener(lParameters);
        parametersPanel.add(report);

        this.add(parametersPanel, BorderLayout.EAST);


    }

    public JXMapViewer getMap() {
        return map;
    }

    public JPanel getMapPanel() {
        return mapPanel;
    }

    public JTextArea getReport() {
        return report;
    }
}
