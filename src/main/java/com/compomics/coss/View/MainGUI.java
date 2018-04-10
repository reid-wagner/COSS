package com.compomics.coss.View;

import com.compomics.coss.Controller.MainFrameController;
import com.compomics.coss.Model.ComparisonResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;

/**
 * Main GUI of the project
 *
 * @author Genet
 */
public class MainGUI extends JFrame {
    
    SettingPanel settings;
    ResultPanel result;
    MainFrameController control;
    TargetDB_View pnlTargView;

    /**
     * constructor of the class
     *
     * @param settings setting panel to be added on the main GUI
     * @param result result panel to be added on the main GUI
     * @param targetView
     * @param controler controller class responsible for coordinating the
     * process
     */
    public MainGUI(SettingPanel settings, ResultPanel result, TargetDB_View targetView, MainFrameController controler) {
        super("COSS");
        this.control = controler;
        this.settings = settings;
        this.result = result;        
        this.pnlTargView = targetView;
        initComponents();
    }

    //<editor-fold defaultstate="colapsed" desc="Initialize Component">
    /**
     * Initialize components of the GUI
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        setName("test view");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //icon images
        ImageIcon iconSetting = new ImageIcon("settingIcon.png");

        //Initialize components
        menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu settingMenu = new JMenu("Setting");
        JMenu helpMenu = new JMenu("Help");

        // Mnemonic
        fileMenu.setMnemonic(KeyEvent.VK_F);
        editMenu.setMnemonic(KeyEvent.VK_E);
        helpMenu.setMnemonic(KeyEvent.VK_F1);
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(settingMenu);
        menuBar.add(helpMenu);

        // Items of the menue
        open = new JMenuItem("Open", KeyEvent.VK_N);
        save = new JMenuItem("Save", KeyEvent.VK_S);
        exit = new JMenuItem("Exit", KeyEvent.VK_X);
        copy = new JMenuItem("Copy", KeyEvent.VK_C);
        paste = new JMenuItem("Paste", KeyEvent.VK_P);
        about = new JMenuItem("About", KeyEvent.VK_A);
        JMenuItem configSystem = new JMenuItem("Configure Library", KeyEvent.VK_L);
        
        fileMenu.add(open);
        fileMenu.add(save);
        fileMenu.add(exit);
        editMenu.add(copy);
        editMenu.add(paste);
        helpMenu.add(about);
        settingMenu.add(configSystem);
        
        pnlsetting = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlsetting.setLayout(new BorderLayout());
        
        pnlresult = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlresult.setLayout(new BorderLayout());
        
        pnlCommands = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlCommands.setLayout(new BorderLayout());
        
        pnlLog = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlLog.setLayout(new BorderLayout());
        
        txtlog = new JTextArea();
        scrLogArea = new JScrollPane();
        txtlog.setColumns(20);
        txtlog.setRows(5);
        scrLogArea.setViewportView(txtlog);

        //this.setSize(dmnsn);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        tab = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
        //tab.addTab("Settings", iconSetting,  new JScrollPane(pnlsetting));
        tab.addTab("Settings", new JScrollPane(pnlsetting));        
        tab.addTab("Result", new JScrollPane(pnlresult));
        //tab.setSize(WIDTH, WIDTH);

        srchCmdPnl = new SearchCommandPnl(control);
        valdtCmdPnl = new ValidationCommandPanel(control);
        valdHistPnl=new ValidationHistogramPanel(null, null);
        srchCmdPnl.prgProgress.setStringPainted(true);
        srchCmdPnl.prgProgress.setForeground(Color.BLUE);
        pnlCommands.add(srchCmdPnl);

        //the upper split panel holding main tabed panels and the info panel
        JPanel pnlUpper = new JPanel();
        pnlUpper.setLayout(new GridLayout(1, 2));
        pnlUpper.add(tab);
        pnlUpper.add(pnlTargView);
        
        JPanel pnlLower = new JPanel();
        pnlLower.setLayout(new GridLayout(1, 2));
        pnlLower.add(pnlCommands);
        pnlLower.add(pnlLog);

        //SettingPanel settings = new SettingPanel();
        pnlsetting.add(settings);

        //ResultPanel result = new ResultPanel();
        pnlresult.add(result);
        
        pnlLog.add(scrLogArea);

        //Control Events
        //main window listener for window closing
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent we
            ) {
                //System.exit(0);

                int selectionOption = JOptionPane.showConfirmDialog(null,
                        "Are you sure to close this window?", "Really Closing?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                
                if (selectionOption == JOptionPane.YES_OPTION) {
                    if (control.isBussy) {
                        control.stopSearch();
                        
                    }
                    dispose();
                    System.exit(0);
                }
            }
        }
        );
        
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    control.saveResult();
                } catch (InterruptedException ex) {
                    Logger.getLogger(MainGUI.class.getName()).log(Level.SEVERE, "not saved, null value", ex);
                }
            }
        });
        
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                int selectionOption = JOptionPane.showConfirmDialog(null,
                        "Are you sure to close this window?", "Really Closing?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                
                if (selectionOption == JOptionPane.YES_OPTION) {
                    if (control.isBussy) {
                        control.stopSearch();
                    }
                    System.exit(0);
                }
            }
        });
        
        configSystem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //control.chooseLibraryFile();      
                // frmNewLibrary.setVisible(true);

            }
        }
        );
        
        tab.addChangeListener((ChangeEvent e) -> {
            if (e.getSource() instanceof JTabbedPane) {
                JTabbedPane pane = (JTabbedPane) e.getSource();
                int ind = pane.getSelectedIndex();
                if (ind == 0) {
                    pnlCommands.removeAll();
                    pnlCommands.add(srchCmdPnl);
                    pnlCommands.revalidate();
                    pnlCommands.repaint();
                    
                } else {
                    pnlCommands.removeAll();
                    pnlCommands.add(valdtCmdPnl, BorderLayout.EAST);
                    //if (valdHistPnl != null) {
                    pnlCommands.add(valdHistPnl, BorderLayout.CENTER);
                   // }
                   
                    pnlCommands.revalidate();
                    pnlCommands.repaint();
                    
                }
                
            }            
        });
        
        setJMenuBar(menuBar);        
        BorderLayout layout = new BorderLayout();
        getContentPane().setLayout(layout);
        add(new JScrollPane(pnlUpper), BorderLayout.CENTER);
        add(new JScrollPane(pnlLower), BorderLayout.SOUTH);
        
        pack();
        
    }
//</editor-fold>
    
    public void setResults(List<ArrayList<ComparisonResult>> resT, List<ArrayList<ComparisonResult>> resD) {
        valdHistPnl = new ValidationHistogramPanel(resT, resD);
        //valdHistPnl.setPreferredSize(new Dimension(300,200));
        valdHistPnl.revalidate();
        valdHistPnl.repaint();
        
    }
    
    public void setProgressValue(int v) {
        srchCmdPnl.prgProgress.setValue(v);
    }

    public void setProgressValue(String s) {
        srchCmdPnl.prgProgress.setString(s);
    }

    public void searchBtnActive(boolean b) {
        srchCmdPnl.btnStartSearch.setEnabled(b);
    }
    
    public void readerBtnActive(boolean b) {
        srchCmdPnl.btnConfigReader.setEnabled(b);
    }
    
    
    
    
    private ValidationHistogramPanel valdHistPnl;
    private SearchCommandPnl srchCmdPnl;
    private ValidationCommandPanel valdtCmdPnl;
    
    private JMenuBar menuBar;
    private JMenuItem open;
    private JMenuItem save;
    private JMenuItem exit;
    private JMenuItem copy;
    private JMenuItem paste;
    private JMenuItem about;
    private JTabbedPane tab;    
    public JPanel pnlsetting;
    public JPanel pnlresult;
    
    public JPanel pnlCommands;
    private JPanel pnlLog;
    
    public JTextArea txtlog;
    private JScrollPane scrLogArea;
   
}