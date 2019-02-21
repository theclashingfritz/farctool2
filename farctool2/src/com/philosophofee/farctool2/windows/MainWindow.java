package com.philosophofee.farctool2.windows;

import com.bulenkov.darcula.DarculaLaf;
import com.philosophofee.farctool2.streams.CustomPrintStream;
import com.philosophofee.farctool2.algorithms.KMPMatch;
import com.philosophofee.farctool2.parsers.MapParser;
import com.philosophofee.farctool2.streams.TextAreaOutputStream;
import com.philosophofee.farctool2.utilities.MiscUtils;
import com.philosophofee.farctool2.utilities.ZlibUtils;
import com.philosophofee.farctool2.utilities.FarcUtils;
import java.awt.FileDialog;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import static java.lang.Class.forName;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.riversun.bigdoc.bin.BigFileSearcher;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import tv.porst.jhexview.*;
import tv.porst.jhexview.JHexView.DefinitionStatus;

public class MainWindow extends javax.swing.JFrame {

    public File bigBoy = null;
    public File bigBoyFarc = null;
    public String currSHA1[] = null;
    public String currGUID[] = null;
    public String currFileName[] = null;
    public String currSize[] = null;
    public static Boolean DeveloperMode = false;
    public TreePath[] selectedPaths = null;

    public MainWindow() {
        initComponents();
        PreviewLabel.setVisible(false);
        TextPrevScroll.setVisible(false);
        TextPreview.setVisible(false);
        setIconImage(new ImageIcon(getClass().getResource("resources/farctool2_icon.png")).getImage());
        aboutWindow.setIconImage(new ImageIcon(getClass().getResource("resources/farctool2_icon.png")).getImage());

        mapTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        mapTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) mapTree.getLastSelectedPathComponent();
                selectedPaths = mapTree.getSelectionPaths();
                if (selectedPaths != null) {
                    currSHA1 = new String[selectedPaths.length];
                    currGUID = new String[selectedPaths.length];
                    currFileName = new String[selectedPaths.length];
                    currSize = new String[selectedPaths.length];
                } else {
                    currSHA1 = null;
                    currGUID = null;
                    currFileName = null;
                    currSize = null;
                    selectedPaths = new TreePath[0];
                }

                for (int currentTreeNode = 0; currentTreeNode < selectedPaths.length; currentTreeNode++) {

                    if (node == null) {
                        return;
                    }
                    node = (DefaultMutableTreeNode) selectedPaths[currentTreeNode].getLastPathComponent();
                    if (node.getChildCount() > 0) {
                        currSHA1[currentTreeNode] = null;
                        currGUID[currentTreeNode] = null;
                        currFileName[currentTreeNode] = null;
                        currSize[currentTreeNode] = null;
                        continue;
                    }
                    if (mapTree.getSelectionPath().getPathCount() == 1) {
                        System.out.println("Root");
                        return;
                    }
                    String[] test = new String[selectedPaths[currentTreeNode].getPathCount()];
                    for (int i = 1; i < selectedPaths[currentTreeNode].getPathCount(); i++) {
                        test[i] = selectedPaths[currentTreeNode].getPathComponent(i).toString();
                    }
                    String finalString = new String();
                    for (int i = 1; i < selectedPaths[currentTreeNode].getPathCount(); i++) {
                        finalString += test[i];
                        if (i != selectedPaths[currentTreeNode].getPathCount() - 1) {
                            finalString += "/";
                        }
                    }

                    if (finalString.contains(".")) {
                        //System.out.println("You currently have selected " + finalString); //this is annoying
                        currFileName[currentTreeNode] = finalString;
                        EditorPanel.setValueAt(finalString, 0, 1);
                        KMPMatch matcher = new KMPMatch();

                        try {
                            long offset = 0;
                            boolean lbp3map = false;
                            offset = matcher.indexOf(Files.readAllBytes(bigBoy.toPath()), finalString.getBytes());
                            try (RandomAccessFile mapAccess = new RandomAccessFile(bigBoy, "rw")) {
                                mapAccess.seek(0);
                                if (mapAccess.readInt() == 21496064) {
                                    lbp3map = true;
                                }

                                mapAccess.seek(offset);
                                offset += finalString.length();
                                mapAccess.seek(offset);

                                if (lbp3map == false) {
                                    offset += 4;
                                    mapAccess.seek(offset);
                                }

                                //Get timestamp
                                String fileTimeStamp = "";
                                for (int i = 0; i < 4; i++) {
                                    fileTimeStamp += String.format("%02X", mapAccess.readByte());
                                    offset += 1;
                                    mapAccess.seek(offset);
                                }
                                EditorPanel.setValueAt(fileTimeStamp, 1, 2); //set hex timestamp
                                Date readableDate = new Date();
                                readableDate.setTime((long) Integer.parseInt(fileTimeStamp, 16) * 1000);
                                EditorPanel.setValueAt(readableDate.toString(), 1, 1); //set readable timestamp

                                //Get size
                                String fileSize = "";
                                for (int i = 0; i < 4; i++) {
                                    fileSize += String.format("%02X", mapAccess.readByte());
                                    offset += 1;
                                    mapAccess.seek(offset);
                                }
                                currSize[currentTreeNode] = fileSize;
                                EditorPanel.setValueAt(fileSize, 2, 2); //set hex filesize
                                EditorPanel.setValueAt(Integer.parseInt(fileSize, 16), 2, 1); //set readable filesize

                                //Get hash
                                String fileHash = "";
                                for (int i = 0; i < 20; i++) {
                                    fileHash += String.format("%02X", mapAccess.readByte());
                                    offset += 1;
                                    mapAccess.seek(offset);
                                }
                                EditorPanel.setValueAt(fileHash, 3, 2); //set hex hash
                                currSHA1[currentTreeNode] = fileHash;
                                EditorPanel.setValueAt(fileHash, 3, 1); //set readable hash (redundant)

                                //Get guid
                                String fileGUID = "";
                                for (int i = 0; i < 4; i++) {
                                    fileGUID += String.format("%02X", mapAccess.readByte());
                                    offset += 1;
                                    mapAccess.seek(offset);
                                }
                                currGUID[currentTreeNode] = fileGUID;
                                EditorPanel.setValueAt(fileGUID, 4, 2); //set hex guid
                                EditorPanel.setValueAt("g" + Integer.parseInt(fileGUID, 16), 4, 1); //set readable guid
                            } catch (IOException ex) {
                                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }


                if (bigBoyFarc != null && selectedPaths.length != 0 && currFileName[0] != null) {
                    PreviewLabel.setVisible(true);
                    PreviewLabel.setIcon(null);
                    PreviewLabel.setText("No preview available");
                    TextPrevScroll.setVisible(false);
                    TextPreview.setVisible(false);

                    byte[] workWithData = FarcUtils.pullFromFarc(currSHA1[currFileName.length - 1], bigBoyFarc);
                    if (workWithData == null) {
                        System.out.println("As a result, I wasn't able to preview anything...");
                        hexViewer.setData(null);
                        hexViewer.setDefinitionStatus(DefinitionStatus.UNDEFINED);
                        hexViewer.setEnabled(false);
                        return;
                    }
                    if (
                        workWithData[3] == 0x74 ||
                        currFileName[currFileName.length - 1].contains(".nws") ||
                        currFileName[currFileName.length - 1].contains(".txt") ||
                        currFileName[currFileName.length - 1].contains(".rlst") ||
                        currFileName[currFileName.length - 1].contains(".xml") ||
                        currFileName[currFileName.length - 1].contains(".cha")
                    ) {
                        //Text file we can read with the text preview pane
                        PreviewLabel.setVisible(false);
                        TextPrevScroll.setVisible(true);
                        TextPreview.setVisible(true);
                        TextPreview.setText(new String(workWithData));
                        TextPreview.setCaretPosition(0);
                    }
                    if (currFileName[currFileName.length - 1].contains(".tex")) {
                        try {
                            ZlibUtils.decompressThis(workWithData);
                            PreviewLabel.setVisible(true);
                            TextPrevScroll.setVisible(false);
                            TextPreview.setVisible(false);
                            PreviewLabel.setText(null);
                            PreviewLabel.setIcon(MiscUtils.createDDSIcon("temp_prev_tex"));
                        } catch (DataFormatException ex) {
                            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    if (currFileName[currFileName.length - 1].contains(".png") || currFileName[currFileName.length - 1].contains(".jpg")) {
                        try {
                            InputStream in = new ByteArrayInputStream(workWithData);
                            BufferedImage image = ImageIO.read( in );
                            PreviewLabel.setVisible(true);
                            TextPrevScroll.setVisible(false);
                            TextPreview.setVisible(false);
                            PreviewLabel.setText(null);
                            PreviewLabel.setIcon(MiscUtils.getScaledImage(image)); in .close();
                        } catch (IOException ex) {
                            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }

                    if (currFileName[currFileName.length - 1].contains(".dds")) {
                        PreviewLabel.setVisible(true);
                        TextPrevScroll.setVisible(false);
                        TextPreview.setVisible(false);
                        PreviewLabel.setText(null);
                        PreviewLabel.setIcon(MiscUtils.createImageIconFromDDS(workWithData));
                    }


                    hexViewer.setData(new SimpleDataProvider(workWithData));
                    hexViewer.setDefinitionStatus(DefinitionStatus.DEFINED);
                    hexViewer.setEnabled(true);

                }
            }



        });
        PrintStream out = new CustomPrintStream(new TextAreaOutputStream(OutputTextArea));
        System.setOut(out);
        System.setErr(out);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        jFrame1 = new javax.swing.JFrame();
        PopUpMessage = new javax.swing.JOptionPane();
        aboutWindow = new javax.swing.JFrame();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextArea2 = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jSplitPane1 = new javax.swing.JSplitPane();
        MapPanel = new javax.swing.JScrollPane();
        mapTree = new javax.swing.JTree();
        RightHandStuff = new javax.swing.JSplitPane();
        pnlOutput = new javax.swing.JPanel();
        mapLoadingBar = new javax.swing.JProgressBar();
        jScrollPane2 = new javax.swing.JScrollPane();
        OutputTextArea = new javax.swing.JTextArea();
        jSplitPane2 = new javax.swing.JSplitPane();
        ToolsPanel = new javax.swing.JPanel();
        jSplitPane3 = new javax.swing.JSplitPane();
        PreviewPanel = new javax.swing.JPanel();
        hexViewer = new tv.porst.jhexview.JHexView();
        ToolsPanel2 = new javax.swing.JPanel();
        PreviewLabel = new javax.swing.JLabel();
        TextPrevScroll = new javax.swing.JScrollPane();
        TextPreview = new javax.swing.JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();
        EditorPanel = new javax.swing.JTable();
        jMenuBar1 = new javax.swing.JMenuBar();
        FileMenu = new javax.swing.JMenu();
        jMenu4 = new javax.swing.JMenu();
        Open = new javax.swing.JMenuItem();
        OpenFarc = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        Exit = new javax.swing.JMenuItem();
        ToolsMenu = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        ExportTEXtoPNG = new javax.swing.JMenuItem();
        ExportTEXtoJPG = new javax.swing.JMenuItem();
        ExportTEXtoDDS = new javax.swing.JMenuItem();
        ExportTRANStoXML = new javax.swing.JMenuItem();
        ExportMOLto3DS = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        AddFileToFARC = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        jMenu1 = new javax.swing.JMenu();
        ExtractRaw = new javax.swing.JMenuItem();
        ExtractDecompressed = new javax.swing.JMenuItem();
        ReplaceSelected = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        AddEntry = new javax.swing.JMenuItem();
        RemoveEntry = new javax.swing.JMenuItem();
        ZeroEntry = new javax.swing.JMenuItem();
        jMenu7 = new javax.swing.JMenu();
        MAPtoRLST = new javax.swing.JMenuItem();
        PrintDependenciesButton = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        InstallMod = new javax.swing.JMenuItem();
        PackagePLAN = new javax.swing.JMenuItem();
        HelpMenu = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        DEV = new javax.swing.JMenu();
        DEV.setVisible(false);
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();

        javax.swing.GroupLayout jFrame1Layout = new javax.swing.GroupLayout(jFrame1.getContentPane());
        jFrame1.getContentPane().setLayout(jFrame1Layout);
        jFrame1Layout.setHorizontalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jFrame1Layout.setVerticalGroup(
            jFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        aboutWindow.setTitle("farctool2 :)");
        aboutWindow.setAlwaysOnTop(true);
        aboutWindow.setMinimumSize(new java.awt.Dimension(450, 225));
        aboutWindow.setResizable(false);

        jTextArea2.setEditable(false);
        jTextArea2.setColumns(20);
        jTextArea2.setFont(new java.awt.Font("Courier New", 0, 10)); // NOI18N
        jTextArea2.setRows(5);
        jTextArea2.setText("Special thanks to:\n\n \"The man in the shadows\"\n                          Aluigi (http://zenhax.com)\n  http://xentax.com\n                              Jon, TBA, and friends\n     npedotnet (NPESDK_GWT) licensed under MIT license\n                 riversun (bigdoc) licensed under MIT license\n  Sporst (JHexView, splib) licensed under GPL 2.0");
        jTextArea2.setFocusable(false);
        jScrollPane4.setViewportView(jTextArea2);

        jLabel1.setFont(new java.awt.Font("Courier New", 0, 18)); // NOI18N
        jLabel1.setText("farctool2!");

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/philosophofee/farctool2/windows/resources/1002.gif"))); // NOI18N

        jLabel3.setFont(new java.awt.Font("Courier New", 0, 11)); // NOI18N
        jLabel3.setText("written by Philosophofee");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(28, 28, 28)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel2))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 384, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel3)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout aboutWindowLayout = new javax.swing.GroupLayout(aboutWindow.getContentPane());
        aboutWindow.getContentPane().setLayout(aboutWindowLayout);
        aboutWindowLayout.setHorizontalGroup(
            aboutWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 450, Short.MAX_VALUE)
            .addGroup(aboutWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, aboutWindowLayout.createSequentialGroup()
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        aboutWindowLayout.setVerticalGroup(
            aboutWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 225, Short.MAX_VALUE)
            .addGroup(aboutWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, aboutWindowLayout.createSequentialGroup()
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("farctool2");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        jSplitPane1.setDividerLocation(150);

        mapTree.setModel(null);
        MapPanel.setViewportView(mapTree);

        jSplitPane1.setLeftComponent(MapPanel);

        RightHandStuff.setDividerLocation(400);
        RightHandStuff.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        OutputTextArea.setEditable(false);
        OutputTextArea.setColumns(20);
        OutputTextArea.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        OutputTextArea.setLineWrap(true);
        OutputTextArea.setRows(5);
        jScrollPane2.setViewportView(OutputTextArea);

        javax.swing.GroupLayout pnlOutputLayout = new javax.swing.GroupLayout(pnlOutput);
        pnlOutput.setLayout(pnlOutputLayout);
        pnlOutputLayout.setHorizontalGroup(
            pnlOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mapLoadingBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 820, Short.MAX_VALUE)
        );
        pnlOutputLayout.setVerticalGroup(
            pnlOutputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlOutputLayout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 106, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mapLoadingBar, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        RightHandStuff.setRightComponent(pnlOutput);

        jSplitPane2.setDividerLocation(256);
        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jSplitPane3.setDividerLocation(256);

        javax.swing.GroupLayout PreviewPanelLayout = new javax.swing.GroupLayout(PreviewPanel);
        PreviewPanel.setLayout(PreviewPanelLayout);
        PreviewPanelLayout.setHorizontalGroup(
            PreviewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(hexViewer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        PreviewPanelLayout.setVerticalGroup(
            PreviewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(hexViewer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jSplitPane3.setRightComponent(PreviewPanel);

        PreviewLabel.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        PreviewLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        PreviewLabel.setAlignmentX(0.5f);

        TextPreview.setColumns(20);
        TextPreview.setFont(new java.awt.Font("Arial", 0, 10)); // NOI18N
        TextPreview.setRows(5);
        TextPrevScroll.setViewportView(TextPreview);

        javax.swing.GroupLayout ToolsPanel2Layout = new javax.swing.GroupLayout(ToolsPanel2);
        ToolsPanel2.setLayout(ToolsPanel2Layout);
        ToolsPanel2Layout.setHorizontalGroup(
            ToolsPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 255, Short.MAX_VALUE)
            .addGroup(ToolsPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(PreviewLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE))
            .addGroup(ToolsPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(TextPrevScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE))
        );
        ToolsPanel2Layout.setVerticalGroup(
            ToolsPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 253, Short.MAX_VALUE)
            .addGroup(ToolsPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(PreviewLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 253, Short.MAX_VALUE))
            .addGroup(ToolsPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(TextPrevScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 253, Short.MAX_VALUE))
        );

        jSplitPane3.setLeftComponent(ToolsPanel2);

        javax.swing.GroupLayout ToolsPanelLayout = new javax.swing.GroupLayout(ToolsPanel);
        ToolsPanel.setLayout(ToolsPanelLayout);
        ToolsPanelLayout.setHorizontalGroup(
            ToolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 818, Short.MAX_VALUE)
        );
        ToolsPanelLayout.setVerticalGroup(
            ToolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane3)
        );

        jSplitPane2.setTopComponent(ToolsPanel);

        EditorPanel.setModel(new javax.swing.table.DefaultTableModel(
            new Object[][] {
                {
                    "Filename",
                    null,
                    null
                }, {
                    "Time Created",
                    null,
                    null
                }, {
                    "Size",
                    null,
                    null
                }, {
                    "Hash",
                    null,
                    null
                }, {
                    "GUID",
                    null,
                    null
                }
            },
            new String[] {
                "Variable",
                "Value",
                "Value (HEX)"
            }
        ) {
            Class[] types = new Class[] {
                java.lang.Object.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean[] {
                false,
                true,
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jScrollPane1.setViewportView(EditorPanel);

        jSplitPane2.setRightComponent(jScrollPane1);

        RightHandStuff.setLeftComponent(jSplitPane2);

        jSplitPane1.setRightComponent(RightHandStuff);

        FileMenu.setText("File");

        jMenu4.setText("Load");

        Open.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        Open.setText(".MAP");
        Open.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OpenActionPerformed(evt);
            }
        });
        jMenu4.add(Open);

        OpenFarc.setText(".FARC");
        OpenFarc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OpenFarcActionPerformed(evt);
            }
        });
        jMenu4.add(OpenFarc);

        FileMenu.add(jMenu4);
        FileMenu.add(jSeparator6);

        Exit.setText("Exit");
        Exit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExitActionPerformed(evt);
            }
        });
        FileMenu.add(Exit);

        jMenuBar1.add(FileMenu);

        ToolsMenu.setText("Export");
        ToolsMenu.setToolTipText("");

        jMenu2.setText("Export .TEX");

        ExportTEXtoPNG.setText(".PNG");
        ExportTEXtoPNG.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExportTEXtoPNGActionPerformed(evt);
            }
        });
        jMenu2.add(ExportTEXtoPNG);

        ExportTEXtoJPG.setText(".JPG");
        ExportTEXtoJPG.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExportTEXtoJPGActionPerformed(evt);
            }
        });
        jMenu2.add(ExportTEXtoJPG);

        ExportTEXtoDDS.setText(".DDS");
        ExportTEXtoDDS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExportTEXtoDDSActionPerformed(evt);
            }
        });
        jMenu2.add(ExportTEXtoDDS);

        ToolsMenu.add(jMenu2);

        ExportTRANStoXML.setText("Export .TRANS as .XML");
        ExportTRANStoXML.setEnabled(false);
        ExportTRANStoXML.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExportTRANStoXMLActionPerformed(evt);
            }
        });
        ToolsMenu.add(ExportTRANStoXML);

        ExportMOLto3DS.setText("Export .MOL as .3DS (LBP1)");
        ExportMOLto3DS.setEnabled(false);
        ExportMOLto3DS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExportMOLto3DSActionPerformed(evt);
            }
        });
        ToolsMenu.add(ExportMOLto3DS);

        jMenuBar1.add(ToolsMenu);

        jMenu5.setText("FARC");

        AddFileToFARC.setText("Add File...");
        AddFileToFARC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddFileToFARCActionPerformed(evt);
            }
        });
        jMenu5.add(AddFileToFARC);
        jMenu5.add(jSeparator5);

        jMenu1.setText("Extract Selected...");

        ExtractRaw.setText("Raw");
        ExtractRaw.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExtractRawActionPerformed(evt);
            }
        });
        jMenu1.add(ExtractRaw);

        ExtractDecompressed.setText("Decompressed");
        ExtractDecompressed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExtractDecompressedActionPerformed(evt);
            }
        });
        jMenu1.add(ExtractDecompressed);

        jMenu5.add(jMenu1);

        ReplaceSelected.setText("Replace Selected...");
        ReplaceSelected.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ReplaceSelectedActionPerformed(evt);
            }
        });
        jMenu5.add(ReplaceSelected);

        jMenuBar1.add(jMenu5);

        jMenu6.setText("MAP");

        AddEntry.setText("Add Entry...");
        AddEntry.setFocusCycleRoot(true);
        AddEntry.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                AddEntryActionPerformed(evt);
            }
        });
        jMenu6.add(AddEntry);

        RemoveEntry.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        RemoveEntry.setText("Remove Entry");
        RemoveEntry.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RemoveEntryActionPerformed(evt);
            }
        });
        jMenu6.add(RemoveEntry);

        ZeroEntry.setText("Zero Entry");
        ZeroEntry.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ZeroEntryActionPerformed(evt);
            }
        });
        jMenu6.add(ZeroEntry);

        jMenuBar1.add(jMenu6);

        jMenu7.setText("Tools");

        MAPtoRLST.setText(".RLST from .MAP");
        MAPtoRLST.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MAPtoRLSTActionPerformed(evt);
            }
        });
        jMenu7.add(MAPtoRLST);

        PrintDependenciesButton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        PrintDependenciesButton.setText("Print Dependencies");
        PrintDependenciesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PrintDependenciesButtonActionPerformed(evt);
            }
        });
        jMenu7.add(PrintDependenciesButton);

        jMenuBar1.add(jMenu7);

        jMenu3.setText("Mods");

        InstallMod.setText("Install Mod...");
        InstallMod.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                InstallModActionPerformed(evt);
            }
        });
        jMenu3.add(InstallMod);

        PackagePLAN.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.CTRL_MASK));
        PackagePLAN.setText("Package Mod from .PLAN");
        PackagePLAN.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PackagePLANActionPerformed(evt);
            }
        });
        jMenu3.add(PackagePLAN);

        jMenuBar1.add(jMenu3);

        HelpMenu.setText("Help");

        jMenuItem2.setText("About");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        HelpMenu.add(jMenuItem2);

        jMenuBar1.add(HelpMenu);

        DEV.setText("*DEV*");

        jMenuItem1.setText("Check if GUID exists");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        DEV.add(jMenuItem1);

        jMenuItem3.setText("Byte to Int");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        DEV.add(jMenuItem3);

        jMenuBar1.add(DEV);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 547, Short.MAX_VALUE)
        );

        pack();
    } // </editor-fold>//GEN-END:initComponents


    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed


    }//GEN-LAST:event_formWindowClosed

    private void PrintDependenciesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PrintDependenciesButtonActionPerformed
        if (bigBoy == null) {
            showUserDialog("A bit of advice", "You kind of need a .MAP file opened to do anything with this.");
            return;
        }
        if (bigBoyFarc == null) {
            showUserDialog("A bit of advice", "You kind of need a .FARC file opened to do anything with this.");
            return;
        }
        try {
            for (int pathCount = 0; pathCount < currSHA1.length; pathCount++) {
                if (currFileName[pathCount] == null) continue;
                byte[] bytesToRead = FarcUtils.pullFromFarc(currSHA1[pathCount], bigBoyFarc);
                ByteArrayInputStream fileAccess = new ByteArrayInputStream(bytesToRead);
                fileAccess.skip(8);
                //Get dependencies offset
                byte[] offsetDependenciesByte = new byte[4];
                fileAccess.read(offsetDependenciesByte);
                int offsetDependencies = Integer.parseInt(MiscUtils.byteArrayToHexString(offsetDependenciesByte), 16);
                //System.out.println("Dependencies offset in hex: " + MiscUtils.byteArrayToHexString(offsetDependenciesByte));
                System.out.println("Dependencies offset: " + offsetDependencies);

                fileAccess.skip(offsetDependencies - 12);

                byte[] dependenciesCountByte = new byte[4];
                fileAccess.read(dependenciesCountByte);
                int dependenciesCount = Integer.parseInt(MiscUtils.byteArrayToHexString(dependenciesCountByte), 16);

                System.out.println("Dependencies count: " + dependenciesCount);

                byte[] dependencyKindByte = new byte[1];
                byte[] dependencyGUIDByte = new byte[4];
                int dependencyGUID = 0;
                byte[] dependencyTypeByte = new byte[4];
                int dependencyType = 0;
                boolean levelFail = false;

                for (int i = 0; i < dependenciesCount; i++) {
                    fileAccess.read(dependencyKindByte);
                    if (dependencyKindByte[0] == 0x01) {
                        fileAccess.skip(20); //sha1
                        fileAccess.skip(4);
                    }
                    if (dependencyKindByte[0] == 0x02) {
                        fileAccess.read(dependencyGUIDByte);
                        dependencyGUID = Integer.parseInt(MiscUtils.byteArrayToHexString(dependencyGUIDByte), 16);

                        fileAccess.read(dependencyTypeByte);
                        dependencyType = Integer.parseInt(MiscUtils.byteArrayToHexString(dependencyTypeByte), 16);
                        String fileNameNew = MiscUtils.getFileNameFromGUID(MiscUtils.byteArrayToHexString(dependencyGUIDByte), bigBoy);
                        if (fileNameNew.contains("Error")) {
                            levelFail = true;
                        }
                        System.out.println((i + 1) + ": " + fileNameNew + " | " + "g" + dependencyGUID + " | " + dependencyType);
                    }

                }
                if (levelFail == true) {
                    System.out.println("ERROR! The level contains at least one dependency that does not exist. You will have problems.");
                }

            }
        } catch (IOException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_PrintDependenciesButtonActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        aboutWindow.setVisible(true);
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void ExtractRawActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExtractRawActionPerformed
        if (bigBoy == null) {
            showUserDialog("A bit of advice", "You kind of need a .MAP file opened to do anything with this.");
            return;
        }
        if (bigBoyFarc == null) {
            showUserDialog("A bit of advice", "You kind of need a .FARC file opened to do anything with this.");
            return;
        }
        File outputFile;
        String outputFileName;

        if (currSHA1.length == 1) {
            outputFileName = currFileName[0].substring(currFileName[0].lastIndexOf("/") + 1);
            outputFile = new File(outputFileName);
            fileChooser.setSelectedFile(outputFile);
        } else {
            fileChooser.setCurrentDirectory(new java.io.File("."));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);

        }

        fileChooser.setFileFilter(null);
        int returnVal = fileChooser.showSaveDialog(this);

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            System.out.println("File access cancelled by user.");
            return;
        }

        for (int pathCount = 0; pathCount < currSHA1.length; pathCount++) {
            if (currFileName[pathCount] == null) continue;
            byte[] bytesToSave = FarcUtils.pullFromFarc(currSHA1[pathCount], bigBoyFarc);

            if (currSHA1.length == 1) outputFile = fileChooser.getSelectedFile();
            else {
                outputFileName = fileChooser.getSelectedFile().getAbsolutePath();
                System.out.println(outputFileName);
                outputFileName = outputFileName + "\\" + currFileName[pathCount].substring(currFileName[pathCount].lastIndexOf("/") + 1);
                System.out.println(outputFileName);
                outputFile = new File(outputFileName);
            }
            System.out.println("Gonna try extracting now!");

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(bytesToSave);
            } catch (IOException ex) {}
        }
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    }//GEN-LAST:event_ExtractRawActionPerformed

    private void ExportMOLto3DSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExportMOLto3DSActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_ExportMOLto3DSActionPerformed

    private void ExportTEXtoPNGActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExportTEXtoPNGActionPerformed
        if (bigBoy == null) {
            showUserDialog("A bit of advice", "You kind of need a .MAP file opened to do anything with this.");
            return;
        }
        if (bigBoyFarc == null) {
            showUserDialog("A bit of advice", "You kind of need a .FARC file opened to do anything with this.");
            return;
        }
        File outputFile;
        String outputFileName;

        if (currSHA1.length == 1) {
            outputFileName = currFileName[0].substring(currFileName[0].lastIndexOf("/") + 1) + ".png";
            outputFile = new File(outputFileName);
            fileChooser.setSelectedFile(outputFile);
        } else {
            fileChooser.setCurrentDirectory(new java.io.File("."));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);

        }

        fileChooser.setFileFilter(null);
        int returnVal = fileChooser.showSaveDialog(this);

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            System.out.println("File access cancelled by user.");
            return;
        }

        for (int pathCount = 0; pathCount < currSHA1.length; pathCount++) {
            if (currFileName[pathCount] == null) continue;
            if (!currFileName[pathCount].contains(".tex")) {
                System.out.println("This is not a .TEX file!");
                continue;
            }
            try {
                byte[] bytesToSave = FarcUtils.pullFromFarc(currSHA1[pathCount], bigBoyFarc);

                if (currSHA1.length == 1) outputFile = fileChooser.getSelectedFile();
                else {
                    outputFileName = fileChooser.getSelectedFile().getAbsolutePath();
                    outputFileName = outputFileName + "\\" + currFileName[pathCount].substring(currFileName[pathCount].lastIndexOf("/") + 1);
                    outputFileName = outputFileName.substring(0, outputFileName.length() - 4) + ".png";
                    System.out.println(outputFileName);
                    outputFile = new File(outputFileName);
                }
                System.out.println("Gonna try extracting now!");

                byte[] buffer = ZlibUtils.decompressThis(bytesToSave);
                MiscUtils.DDStoStandard(buffer, "png", outputFile);
            } catch (DataFormatException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NullPointerException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    }//GEN-LAST:event_ExportTEXtoPNGActionPerformed

    private void ExportTEXtoDDSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExportTEXtoDDSActionPerformed
        if (bigBoy == null) {
            showUserDialog("A bit of advice", "You kind of need a .MAP file opened to do anything with this.");
            return;
        }
        if (bigBoyFarc == null) {
            showUserDialog("A bit of advice", "You kind of need a .FARC file opened to do anything with this.");
            return;
        }
        File outputFile;
        String outputFileName;

        if (currSHA1.length == 1) {
            outputFileName = currFileName[0].substring(currFileName[0].lastIndexOf("/") + 1) + ".dds";
            outputFile = new File(outputFileName);
            fileChooser.setSelectedFile(outputFile);
        } else {
            fileChooser.setCurrentDirectory(new java.io.File("."));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);

        }

        fileChooser.setFileFilter(null);
        int returnVal = fileChooser.showSaveDialog(this);

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            System.out.println("File access cancelled by user.");
            return;
        }

        for (int pathCount = 0; pathCount < currSHA1.length; pathCount++) {
            if (currFileName[pathCount] == null) continue;
            if (!currFileName[pathCount].contains(".tex")) {
                System.out.println("This is not a .TEX file!");
                continue;
            }
            try {
                byte[] bytesToSave = ZlibUtils.decompressThis(FarcUtils.pullFromFarc(currSHA1[pathCount], bigBoyFarc));

                if (currSHA1.length == 1) outputFile = fileChooser.getSelectedFile();
                else {
                    outputFileName = fileChooser.getSelectedFile().getAbsolutePath();
                    System.out.println(outputFileName);
                    outputFileName = outputFileName + "\\" + currFileName[pathCount].substring(currFileName[pathCount].lastIndexOf("/") + 1);
                    outputFileName = outputFileName.substring(0, outputFileName.length() - 4) + ".dds";
                    System.out.println(outputFileName);
                    outputFile = new File(outputFileName);
                }
                System.out.println("Gonna try extracting now!");

                FileOutputStream fos = new FileOutputStream(outputFile);
                fos.write(bytesToSave);

            } catch (FileNotFoundException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (DataFormatException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    }//GEN-LAST:event_ExportTEXtoDDSActionPerformed

    private void ExportTEXtoJPGActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExportTEXtoJPGActionPerformed
        if (bigBoy == null) {
            showUserDialog("A bit of advice", "You kind of need a .MAP file opened to do anything with this.");
            return;
        }
        if (bigBoyFarc == null) {
            showUserDialog("A bit of advice", "You kind of need a .FARC file opened to do anything with this.");
            return;
        }
        File outputFile;
        String outputFileName;

        if (currSHA1.length == 1) {
            outputFileName = currFileName[0].substring(currFileName[0].lastIndexOf("/") + 1) + ".jpg";
            outputFile = new File(outputFileName);
            fileChooser.setSelectedFile(outputFile);
        } else {
            fileChooser.setCurrentDirectory(new java.io.File("."));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);

        }

        fileChooser.setFileFilter(null);
        int returnVal = fileChooser.showSaveDialog(this);

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            System.out.println("File access cancelled by user.");
            return;
        }

        for (int pathCount = 0; pathCount < currSHA1.length; pathCount++) {
            if (currFileName[pathCount] == null) continue;
            if (!currFileName[pathCount].contains(".tex")) {
                System.out.println("This is not a .TEX file!");
                continue;
            }
            try {
                byte[] bytesToSave = FarcUtils.pullFromFarc(currSHA1[pathCount], bigBoyFarc);

                if (currSHA1.length == 1) outputFile = fileChooser.getSelectedFile();
                else {
                    outputFileName = fileChooser.getSelectedFile().getAbsolutePath();
                    System.out.println(outputFileName);
                    outputFileName = outputFileName + "\\" + currFileName[pathCount].substring(currFileName[pathCount].lastIndexOf("/") + 1);
                    outputFileName = outputFileName.substring(0, outputFileName.length() - 4) + ".jpg";
                    System.out.println(outputFileName);
                    outputFile = new File(outputFileName);
                }
                System.out.println("Gonna try extracting now!");

                MiscUtils.DDStoStandard(ZlibUtils.decompressThis(bytesToSave), "jpg", outputFile);
            } catch (DataFormatException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NullPointerException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    }//GEN-LAST:event_ExportTEXtoJPGActionPerformed

    private void ExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExitActionPerformed
        System.out.println("Shutting down. Goodbye!");
        System.exit(0);
    }//GEN-LAST:event_ExitActionPerformed

    private void OpenFarcActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OpenFarcActionPerformed
        if (bigBoy == null) {
            showUserDialog("Warning", "Please keep in mind, opening a .FARC file alone will not display anything within farctool2. A .MAP file is required for most functions.");
        }
        FileFilter ff = new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else if (f.getName().endsWith(".farc")) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return "FARC Files";
            }
        };
        fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
        fileChooser.setFileFilter(ff);
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            bigBoyFarc = fileChooser.getSelectedFile();
            System.out.println("Sucessfully opened " + bigBoyFarc.getName());
        }
    }//GEN-LAST:event_OpenFarcActionPerformed

    private void OpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OpenActionPerformed
        System.out.println("A haiku for the impatient:\n" +
            "Map parsing takes time.\n" +
            "I might freeze, I have not crashed.\n" +
            "Wait, please bear with me!");
        FileFilter ff = new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else if (f.getName().endsWith(".map")) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return "Map Files";
            }
        };
        fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
        fileChooser.setFileFilter(ff);
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            bigBoy = fileChooser.getSelectedFile();
            System.out.println("Sucessfully opened " + bigBoy.getName());

            MapParser self = new MapParser();
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(bigBoy.getName());
            mapTree.setModel(null);
            DefaultTreeModel model = self.parseMapIntoMemory(root, bigBoy);

            mapTree.setModel(model);

            //self.loadMap(file);
            //self.printHtml(System.out);
        } else {
            System.out.println("...nevermind, you cancelled!");
        }
    }//GEN-LAST:event_OpenActionPerformed

    private void AddEntryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddEntryActionPerformed

        if (bigBoy == null) {
            showUserDialog("A bit of advice", "You kind of need a .MAP file opened to do anything with this.");
            return;
        }
        EntryAdditionWindow EntryWindow = new EntryAdditionWindow(this);
        EntryWindow.setVisible(true);
    }//GEN-LAST:event_AddEntryActionPerformed

    private void ZeroEntryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ZeroEntryActionPerformed
        if (bigBoy == null) {
            showUserDialog("A bit of advice", "You kind of need a .MAP file opened to do anything with this.");
            return;
        }
        for (int pathCount = 0; pathCount < currFileName.length; pathCount++) {
            if (currFileName[pathCount] == null) continue;
            KMPMatch matcher = new KMPMatch();
            long offset = 0;
            boolean lbp3map = false;
            try {
                offset = matcher.indexOf(Files.readAllBytes(bigBoy.toPath()), currFileName[pathCount].getBytes());
            } catch (IOException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
            try (RandomAccessFile mapAccess = new RandomAccessFile(bigBoy, "rw")) {
                mapAccess.seek(0);
                if (mapAccess.readInt() == 21496064) lbp3map = true;
                mapAccess.seek(offset);
                offset += currFileName[pathCount].length();
                mapAccess.seek(offset);
                if (lbp3map == false) {
                    offset += 4;
                    mapAccess.seek(offset);
                }
                byte buffer[] = new byte[28];
                mapAccess.write(buffer, 0, 28);
                mapAccess.close();


                System.out.println("Successfully zeroed entry!");
            } catch (Exception e) {}
        }
    }//GEN-LAST:event_ZeroEntryActionPerformed

    private void RemoveEntryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RemoveEntryActionPerformed
        if (bigBoy == null) {
            showUserDialog("A bit of advice", "You kind of need a .MAP file opened to do anything with this.");
            return;
        }
        for (int pathCount = 0; pathCount < currFileName.length; pathCount++) {
            if (currFileName[pathCount] == null) continue;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPaths[pathCount].getLastPathComponent();
            if (node.getChildCount() > 0) {
                System.out.println("You can't delete a folder that has contents in it!");
                return;
            }
            KMPMatch matcher = new KMPMatch();
            long offset = 0;
            boolean lbp3map = false;
            try {
                offset = matcher.indexOf(Files.readAllBytes(bigBoy.toPath()), currFileName[pathCount].getBytes());
            } catch (IOException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
            try (RandomAccessFile mapAccess = new RandomAccessFile(bigBoy, "rw")) {
                mapAccess.seek(0);
                if (mapAccess.readInt() == 21496064) lbp3map = true;
                mapAccess.seek(offset);

                mapAccess.seek(0);
                byte prev[] = new byte[(int) offset - 1];
                mapAccess.read(prev);

                Boolean lastEntry = mapAccess.length() - (offset + currFileName[pathCount].length()) == 32;

                byte next[] = null;
                if (!lastEntry) {
                    offset += currFileName[pathCount].length() + (lbp3map ? 33 : 39);
                    mapAccess.seek(offset);

                    next = new byte[(int) mapAccess.length() - (int) offset];
                    mapAccess.read(next);
                }


                mapAccess.setLength(0);
                if (!lastEntry)
                    mapAccess.setLength(prev.length + next.length);
                else mapAccess.setLength(prev.length);

                mapAccess.write(prev);
                if (!lastEntry)
                    mapAccess.write(next);

                mapAccess.seek(4);
                int entries = mapAccess.readInt();
                entries -= 1;

                mapAccess.seek(4);
                mapAccess.writeInt(entries);

                mapAccess.close();

                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                node.removeFromParent();

                Boolean parentNodes = true;
                while (parentNodes) {
                    if (parent.isLeaf()) {
                        node = (DefaultMutableTreeNode) parent.getParent();
                        parent.removeFromParent();
                        parent = node;
                    } else parentNodes = false;
                }
                mapTree.updateUI();


            } catch (Exception e) {}
            System.out.println("Successfully removed file!");
        }
    }//GEN-LAST:event_RemoveEntryActionPerformed

    private void ExtractDecompressedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExtractDecompressedActionPerformed
        if (bigBoy == null) {
            showUserDialog("A bit of advice", "You kind of need a .MAP file opened to do anything with this.");
            return;
        }
        if (bigBoyFarc == null) {
            showUserDialog("A bit of advice", "You kind of need a .FARC file opened to do anything with this.");
            return;
        }
        File outputFile;
        String outputFileName;

        if (currSHA1.length == 1) {
            outputFileName = currFileName[0].substring(currFileName[0].lastIndexOf("/") + 1);
            outputFile = new File(outputFileName);
            fileChooser.setSelectedFile(outputFile);
        } else {
            fileChooser.setCurrentDirectory(new java.io.File("."));
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);

        }

        fileChooser.setFileFilter(null);
        int returnVal = fileChooser.showSaveDialog(this);

        if (returnVal != JFileChooser.APPROVE_OPTION) {
            System.out.println("File access cancelled by user.");
            return;
        }

        for (int pathCount = 0; pathCount < currSHA1.length; pathCount++) {
            if (currFileName[pathCount] == null) continue;
            byte[] bytesToSave = null;
            try {
                bytesToSave = ZlibUtils.decompressThis(FarcUtils.pullFromFarc(currSHA1[pathCount], bigBoyFarc));
            } catch (DataFormatException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (currSHA1.length == 1) outputFile = fileChooser.getSelectedFile();
            else {
                outputFileName = fileChooser.getSelectedFile().getAbsolutePath();
                System.out.println(outputFileName);
                outputFileName = outputFileName + "\\" + currFileName[pathCount].substring(currFileName[pathCount].lastIndexOf("/") + 1);
                if (outputFileName.contains(".tex"))
                    outputFileName = outputFileName.substring(0, outputFileName.length() - 4) + ".dds";
                System.out.println(outputFileName);
                outputFile = new File(outputFileName);
            }
            System.out.println("Gonna try extracting now!");

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(bytesToSave);
            } catch (IOException ex) {}
        }
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    }//GEN-LAST:event_ExtractDecompressedActionPerformed

    private void ReplaceSelectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ReplaceSelectedActionPerformed
        if (bigBoyFarc == null) {
            showUserDialog("A bit of advice", "You kind of need a .FARC file opened to do anything with this.");
            return;
        }

        fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());
        int returnVal = fileChooser.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                File newFile = fileChooser.getSelectedFile();
                System.out.println("Sucessfully opened " + newFile.getName());
                System.out.println("Attempting to inject " + newFile.getName() + " into " + bigBoyFarc.getName());
                FarcUtils.addFile(newFile, bigBoyFarc);
                byte[] SHA1 = MiscUtils.getSHA1(newFile);
                String fileName = "";
                for (int pathCount = 0; pathCount < currSHA1.length; pathCount++) {
                    if (currFileName[pathCount] == null) continue;
                    fileName = currFileName[pathCount];
                    if (currFileName[pathCount].contains(".tex") && !newFile.getName().contains(".tex")) {
                        if (fileName != null) {
                            fileName = fileName.substring(0, fileName.length() - 3);
                        }
                        if (newFile.getName() != null) {
                            String path = newFile.getName();
                            String[] paths = path.split("[.]");
                            fileName += paths[paths.length - 1];
                        }
                    }
                    System.out.println(currGUID[pathCount]);
                    MiscUtils.replaceEntryByGUID(currGUID[pathCount], fileName, Integer.toString((int) newFile.length()), MiscUtils.byteArrayToHexString(SHA1), this);
                }
                System.out.println("File successfully replaced!");
            } catch (Exception ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
            ((DefaultTreeModel) mapTree.getModel()).reload((DefaultMutableTreeNode) mapTree.getModel().getRoot());
            mapTree.updateUI();
        }
    }//GEN-LAST:event_ReplaceSelectedActionPerformed

    private void ExportTRANStoXMLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExportTRANStoXMLActionPerformed
        if (!currFileName[0].contains(".trans")) {
            System.out.println("This tool requires a .TRANS file!");
            return;
        }

        String outputFileName = currFileName[0].substring(currFileName[0].lastIndexOf("/") + 1);
        File outputFile = new File(outputFileName.substring(0, outputFileName.length() - 5) + "lam");

        fileChooser.setFileFilter(null);
        fileChooser.setSelectedFile(outputFile);
        int returnVal = fileChooser.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            outputFile = fileChooser.getSelectedFile();
            byte language[] = FarcUtils.pullFromFarc(currSHA1[0], bigBoyFarc);

            try (FileOutputStream output = new FileOutputStream(new File("trans_temp"))) {
                try {
                    output.write(language);
                } catch (IOException ex) {
                    Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (FileNotFoundException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }

            try (RandomAccessFile trans = new RandomAccessFile("trans_temp", "rw")) {
                Boolean isEnd = false;
                int stringEnd;
                int stringOffset;
                byte[] buffer = null;
                int offset = 4;
                int entries = trans.readInt();
                String[] strings = new String[entries];
                int[] keys = new int[entries];

                for (int i = 0; i < entries; i++) {
                    keys[i] = trans.readInt();
                    stringOffset = trans.readInt() + ((entries * 8) + 6);
                    trans.seek(stringOffset);

                    stringEnd = stringOffset;
                    if (i == (entries - 1)) {
                        while (!isEnd) {
                            stringEnd++;
                            buffer = new byte[1];
                            trans.read(buffer);
                            if (MiscUtils.byteArrayToHexString(buffer).equals("FE"))
                                isEnd = true;
                        }
                    } else stringEnd = (int) trans.length();

                    trans.seek(stringOffset);


                    buffer = new byte[stringEnd - stringOffset];
                    trans.read(buffer);
                    strings[i] = new String(buffer, "UTF-8");

                    offset += 8;
                    trans.seek(offset);

                }
                String output = "";
                for (int i: keys) {
                    output += keys[i] + "\n " + strings[i] + "\n";
                }
                PrintWriter out = new PrintWriter(outputFile.getAbsolutePath());
                out.println(output);

            } catch (FileNotFoundException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_ExportTRANStoXMLActionPerformed

    private void AddFileToFARCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_AddFileToFARCActionPerformed
        if (bigBoyFarc == null) {
            showUserDialog("A bit of advice", "You kind of need a .FARC file opened to do anything with this.");
            return;
        }

        fileChooser.setFileFilter(fileChooser.getAcceptAllFileFilter());
        int returnVal = fileChooser.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                File newFile = fileChooser.getSelectedFile();
                System.out.println("Sucessfully opened " + newFile.getName());
                System.out.println("Attempting to inject " + newFile.getName() + " into " + bigBoyFarc.getName());
                FarcUtils.addFile(newFile, bigBoyFarc);
            } catch (Exception ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_AddFileToFARCActionPerformed

    private void InstallModActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_InstallModActionPerformed
        if (bigBoyFarc == null) {
            showUserDialog("A bit of advice", "You kind of need a .FARC file opened to do anything with this.");
            return;
        }
        if (bigBoy == null) {
            showUserDialog("A bit of advice", "You kind of need a .MAP file opened to do anything with this.");
            return;
        }
        FileFilter ff = new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else if (f.getName().endsWith(".xml")) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return "XML Files";
            }
        };
        fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
        fileChooser.setFileFilter(ff);
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                File modInfo = fileChooser.getSelectedFile();
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder;
                dBuilder = dbFactory.newDocumentBuilder();
                Document mod = dBuilder.parse(modInfo);
                mod.getDocumentElement().normalize();
                String directory = fileChooser.getSelectedFile().getParent() + "/";

                ModInstaller installer = new ModInstaller(mod, directory, this);
            } catch (ParserConfigurationException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SAXException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_InstallModActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        long offset = MiscUtils.findGUIDOffset("000139EE00", bigBoy);
        try {
            if (offset == 0) return;

            RandomAccessFile map = new RandomAccessFile(bigBoy, "rw");
            Boolean beginning = false;

            offset -= 33;
            while (!beginning) {
                offset -= 2;
                map.seek(offset);

                byte[] buffer = new byte[1];
                map.read(buffer);

                if (MiscUtils.byteArrayToHexString(buffer).equals("00"))
                    beginning = true;
            }
            offset++;
            System.out.println(offset);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        }


    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        System.out.println((MiscUtils.hexStringToByteArray("FE")));
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void PackagePLANActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PackagePLANActionPerformed
        if (bigBoyFarc == null) {
            showUserDialog("A bit of advice", "You kind of need a .FARC file opened to do anything with this.");
            return;
        }
        if (bigBoy == null) {
            showUserDialog("A bit of advice", "You kind of need a .MAP file opened to do anything with this.");
            return;
        }
        if (!currFileName[0].contains(".plan")) return;
        try {
            byte[] bytesToRead = FarcUtils.pullFromFarc(currSHA1[0], bigBoyFarc);
            ByteArrayInputStream fileAccess = new ByteArrayInputStream(bytesToRead);
            fileAccess.skip(8);

            byte[] offsetDependenciesByte = new byte[4];
            fileAccess.read(offsetDependenciesByte);
            int offsetDependencies = Integer.parseInt(MiscUtils.byteArrayToHexString(offsetDependenciesByte), 16);

            fileAccess.skip(offsetDependencies - 12);

            byte[] dependenciesCountByte = new byte[4];
            fileAccess.read(dependenciesCountByte);
            int dependenciesCount = Integer.parseInt(MiscUtils.byteArrayToHexString(dependenciesCountByte), 16);

            System.out.println("Dependencies count: " + dependenciesCount);

            byte[] dependencyKindByte = new byte[1];
            byte[] dependencyGUIDByte = new byte[4];
            int dependencyGUID = 0;
            byte[] dependencyTypeByte = new byte[4];
            int dependencyType = 0;
            boolean levelFail = false;

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElement("mod");

            doc.appendChild(rootElement);

            Attr description = doc.createAttribute("description");
            description.setValue("Automatically packed mod : " + currFileName[0].substring(currFileName[0].lastIndexOf("/") + 1));

            Attr game = doc.createAttribute("game");
            game.setValue("any");

            Attr icon = doc.createAttribute("icon");
            icon.setValue("icon.png");

            Attr name = doc.createAttribute("name");

            String nameOfNode = currFileName[0].substring(currFileName[0].lastIndexOf("/") + 1);
            name.setValue(nameOfNode.substring(0, nameOfNode.length() - 5));

            rootElement.setAttributeNode(name);
            rootElement.setAttributeNode(description);
            rootElement.setAttributeNode(icon);
            rootElement.setAttributeNode(game);

            byte[] buffer = FarcUtils.pullFromFarc(currSHA1[0], bigBoyFarc);
            File fileToPack = new File("mods/" + nameOfNode.substring(0, nameOfNode.length() - 5) + "/files/" + nameOfNode);
            fileToPack.getParentFile().mkdirs();
            fileToPack.createNewFile();
            FileOutputStream fileToPackStream = new FileOutputStream(fileToPack);
            fileToPackStream.write(buffer);
            fileToPackStream.close();

            Element fileNode = doc.createElement("file");
            rootElement.appendChild(fileNode);

            Attr guid = doc.createAttribute("guid");
            guid.setValue(currGUID[0]);
            fileNode.setAttributeNode(guid);

            Element path = doc.createElement("path");
            path.appendChild(doc.createTextNode("files/" + nameOfNode));

            fileNode.appendChild(path);

            Element mapNode = doc.createElement("map");
            mapNode.appendChild(doc.createTextNode(currFileName[0]));

            fileNode.appendChild(mapNode);

            for (int i = 0; i < dependenciesCount; i++) {
                fileAccess.read(dependencyKindByte);
                if (dependencyKindByte[0] == 0x01) {
                    fileAccess.skip(20); //sha1
                    fileAccess.skip(4);
                }
                if (dependencyKindByte[0] == 0x02) {
                    fileAccess.read(dependencyGUIDByte);
                    dependencyGUID = Integer.parseInt(MiscUtils.byteArrayToHexString(dependencyGUIDByte), 16);

                    fileAccess.read(dependencyTypeByte);
                    dependencyType = Integer.parseInt(MiscUtils.byteArrayToHexString(dependencyTypeByte), 16);
                    String fileNameNew = MiscUtils.getFileNameFromGUID(MiscUtils.byteArrayToHexString(dependencyGUIDByte), bigBoy);
                    String Hash = MiscUtils.getHashFromGUID(MiscUtils.byteArrayToHexString(dependencyGUIDByte), bigBoy);
                    if (Hash == "NULL")
                        continue;
                    byte[] file = FarcUtils.pullFromFarc(Hash, bigBoyFarc);
                    if (file == null)
                        continue;
                    if (fileNameNew.contains(".gmat")) {
                        byte[] innerBytesToRead = FarcUtils.pullFromFarc(Hash, bigBoyFarc);
                        ByteArrayInputStream innerFileAccess = new ByteArrayInputStream(innerBytesToRead);
                        innerFileAccess.skip(8);

                        byte[] innerOffsetDependenciesByte = new byte[4];
                        innerFileAccess.read(innerOffsetDependenciesByte);
                        int innerOffsetDependencies = Integer.parseInt(MiscUtils.byteArrayToHexString(innerOffsetDependenciesByte), 16);

                        innerFileAccess.skip(innerOffsetDependencies - 12);

                        byte[] innerDependenciesCountByte = new byte[4];
                        innerFileAccess.read(innerDependenciesCountByte);
                        int innerDependenciesCount = Integer.parseInt(MiscUtils.byteArrayToHexString(innerDependenciesCountByte), 16);
                        byte[] innerDependencyKindByte = new byte[1];
                        byte[] innerDependencyGUIDByte = new byte[4];
                        int innerDependencyGUID = 0;
                        byte[] innerDependencyTypeByte = new byte[4];
                        int innerDependencyType = 0;
                        for (int j = 0; j < innerDependenciesCount; j++) {
                            innerFileAccess.read(innerDependencyKindByte);
                            if (dependencyKindByte[0] == 0x01) {
                                innerFileAccess.skip(20); //sha1
                                innerFileAccess.skip(4);
                            }
                            if (innerDependencyKindByte[0] == 0x02) {
                                innerFileAccess.read(innerDependencyGUIDByte);
                                innerDependencyGUID = Integer.parseInt(MiscUtils.byteArrayToHexString(innerDependencyGUIDByte), 16);

                                innerFileAccess.read(innerDependencyTypeByte);
                                innerDependencyType = Integer.parseInt(MiscUtils.byteArrayToHexString(innerDependencyTypeByte), 16);
                                String innerFileNameNew = MiscUtils.getFileNameFromGUID(MiscUtils.byteArrayToHexString(innerDependencyGUIDByte), bigBoy);
                                String innerHash = MiscUtils.getHashFromGUID(MiscUtils.byteArrayToHexString(innerDependencyGUIDByte), bigBoy);
                                if (innerHash == "NULL")
                                    continue;
                                byte[] innerFile = FarcUtils.pullFromFarc(innerHash, bigBoyFarc);
                                if (innerFile == null)
                                    continue;
                                System.out.println(innerFileNameNew);
                                String innerOutputFileName = "mods/" + currFileName[0].substring(currFileName[0].lastIndexOf("/") + 1);
                                innerOutputFileName = innerOutputFileName.substring(0, innerOutputFileName.length() - 5) + "/files/" + innerFileNameNew.substring(innerFileNameNew.lastIndexOf("/") + 1);

                                if (!innerFileNameNew.contains(".tex")) {
                                    File innerMyFile = new File(innerOutputFileName);
                                    innerMyFile.getParentFile().mkdirs();
                                    innerMyFile.createNewFile();
                                    FileOutputStream innerOutputFile = new FileOutputStream(innerMyFile);
                                    innerOutputFile.write(innerFile);
                                    innerOutputFile.close();
                                } else {
                                    innerOutputFileName = innerOutputFileName.substring(0, innerOutputFileName.length() - 3) + "png";
                                    ImageIO.write(MiscUtils.DDStoPNG(ZlibUtils.decompressThis(innerFile)), "png", new File(innerOutputFileName));
                                }

                                fileNode = doc.createElement("file");
                                rootElement.appendChild(fileNode);

                                guid = doc.createAttribute("guid");
                                guid.setValue(MiscUtils.byteArrayToHexString(innerDependencyGUIDByte));
                                fileNode.setAttributeNode(guid);

                                path = doc.createElement("path");
                                String innerPathName = "files/" + innerFileNameNew.substring(innerFileNameNew.lastIndexOf("/") + 1);
                                if (innerPathName.contains(".tex"))
                                    innerPathName = innerPathName.substring(0, innerPathName.length() - 3) + "png";
                                path.appendChild(doc.createTextNode(innerPathName));

                                fileNode.appendChild(path);

                                mapNode = doc.createElement("map");
                                if (innerFileNameNew.contains(".tex"))
                                    innerFileNameNew = innerFileNameNew.substring(0, innerFileNameNew.length() - 3) + "png";
                                mapNode.appendChild(doc.createTextNode(innerFileNameNew));

                                fileNode.appendChild(mapNode);
                            }
                        }

                    }

                    System.out.println(fileNameNew);
                    String outputFileName = "mods/" + currFileName[0].substring(currFileName[0].lastIndexOf("/") + 1);
                    outputFileName = outputFileName.substring(0, outputFileName.length() - 5) + "/files/" + fileNameNew.substring(fileNameNew.lastIndexOf("/") + 1);

                    if (!fileNameNew.contains(".tex")) {
                        File myFile = new File(outputFileName);
                        myFile.getParentFile().mkdirs();
                        myFile.createNewFile();
                        FileOutputStream outputFile = new FileOutputStream(myFile);
                        outputFile.write(file);
                        outputFile.close();
                    } else {
                        outputFileName = outputFileName.substring(0, outputFileName.length() - 3) + "png";
                        ImageIO.write(MiscUtils.DDStoPNG(ZlibUtils.decompressThis(file)), "png", new File(outputFileName));
                    }

                    fileNode = doc.createElement("file");
                    rootElement.appendChild(fileNode);

                    guid = doc.createAttribute("guid");
                    guid.setValue(MiscUtils.byteArrayToHexString(dependencyGUIDByte));
                    fileNode.setAttributeNode(guid);

                    path = doc.createElement("path");
                    String pathName = "files/" + fileNameNew.substring(fileNameNew.lastIndexOf("/") + 1);
                    if (pathName.contains(".tex"))
                        pathName = pathName.substring(0, pathName.length() - 3) + "png";
                    path.appendChild(doc.createTextNode(pathName));

                    fileNode.appendChild(path);

                    mapNode = doc.createElement("map");
                    if (fileNameNew.contains(".tex"))
                        fileNameNew = fileNameNew.substring(0, fileNameNew.length() - 3) + "png";
                    mapNode.appendChild(doc.createTextNode(fileNameNew));

                    fileNode.appendChild(mapNode);

                }

            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            String outputFileName = "mods/" + currFileName[0].substring(currFileName[0].lastIndexOf("/") + 1);
            outputFileName = outputFileName.substring(0, outputFileName.length() - 5);
            StreamResult result = new StreamResult(new File(outputFileName + "/mod.xml"));
            transformer.transform(source, result);

            InputStream fis = getClass().getResourceAsStream("resources/icon.png");
            FileOutputStream fos = new FileOutputStream(outputFileName + "/icon.png");
            byte[] imageBuffer = fis.readAllBytes();
            fos.write(imageBuffer);
            fos.close();
            fis.close();
            System.out.println("Successfully packed .PLAN into a moddable format.");
        } catch (IOException | NullPointerException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DataFormatException ex) {
            Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
        }

    }//GEN-LAST:event_PackagePLANActionPerformed

    private void MAPtoRLSTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MAPtoRLSTActionPerformed
        if (bigBoy == null) {
            showUserDialog("A bit of advice", "You kind of need a .MAP file opened to do anything with this.");
            return;
        }
        String output = "";
        try {
            try (
                // Open the selected file. // 
                DataInputStream mapAccess = new DataInputStream(new FileInputStream(bigBoy))) {

                // Initialize Variables // 
                long begin = System.currentTimeMillis();
                boolean lbp3map = false;

                // Detect from which game the .MAP originates. //
                int header = mapAccess.readInt();
                switch (header) {
                    case 256:
                        break;
                    case 21496064:
                        lbp3map = true;
                        break;
                    case 936:
                        break;
                    default:
                        throw new IOException("Error reading 4 bytes - not a valid .map file");
                }

                // Get the amount of entries present in the .MAP File //
                int mapEntries = mapAccess.readInt();

                // Enumerate each entry detected. //
                int fileNameLength = 0;
                String fileName = "";
                for (int i = 0; i < mapEntries; i++) {

                    // Seek 2 bytes if the .MAP originates from LBP1/2. //
                    if (!lbp3map)
                        mapAccess.skip(2);

                    // Get path of entry //
                    fileNameLength = mapAccess.readShort();
                    byte[] fileNameBytes = new byte[fileNameLength];
                    mapAccess.read(fileNameBytes);
                    fileName = new String(fileNameBytes);

                    if (fileName.contains(".plan") || fileName.contains(".pal"))
                        output += fileName + "\n";

                    // Seek 4 bytes if the .MAP originates from LBP1/2. (Padding) //
                    if (!lbp3map)
                        mapAccess.skip(4);

                    // Skip the rest of the data as it's obtained at a future point in time. //
                    mapAccess.skip(32);


                }
                PrintWriter out = new PrintWriter("inventory.rlst");
                out.println(output);
                out.close();
                System.out.println("Success!");

            }

        } catch (FileNotFoundException ex) {} catch (IOException ex) {}

    }//GEN-LAST:event_MAPtoRLSTActionPerformed


    public void showUserDialog(String title, String message) {
        if ("Warning".equals(title)) {
            JOptionPane.showMessageDialog(PopUpMessage, message, title, JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(PopUpMessage, message, title, JOptionPane.PLAIN_MESSAGE);
        }
    }



    public static void main(String args[]) {
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */

        java.awt.EventQueue.invokeLater(() -> {
            BasicLookAndFeel darcula = new DarculaLaf();
            try {
                UIManager.setLookAndFeel(darcula);
            } catch (UnsupportedLookAndFeelException ex) {
                Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
            MainWindow myWindow = new MainWindow();
            myWindow.setVisible(true);

            if (args.length != 0) {
                if (args[0].equals("--dev")) {
                    System.out.println("FARC Tool has been started in Developer Mode, this is intended for testing purposes only.");
                    myWindow.setTitle("FARC Tool 2 | Developer");
                    myWindow.DeveloperMode = true;
                    myWindow.DEV.setVisible(true);
                }
            }

    });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem AddEntry;
    private javax.swing.JMenuItem AddFileToFARC;
    private javax.swing.JMenu DEV;
    private javax.swing.JTable EditorPanel;
    private javax.swing.JMenuItem Exit;
    private javax.swing.JMenuItem ExportMOLto3DS;
    private javax.swing.JMenuItem ExportTEXtoDDS;
    private javax.swing.JMenuItem ExportTEXtoJPG;
    private javax.swing.JMenuItem ExportTEXtoPNG;
    private javax.swing.JMenuItem ExportTRANStoXML;
    private javax.swing.JMenuItem ExtractDecompressed;
    private javax.swing.JMenuItem ExtractRaw;
    private javax.swing.JMenu FileMenu;
    private javax.swing.JMenu HelpMenu;
    private javax.swing.JMenuItem InstallMod;
    private javax.swing.JMenuItem MAPtoRLST;
    private javax.swing.JScrollPane MapPanel;
    private javax.swing.JMenuItem Open;
    private javax.swing.JMenuItem OpenFarc;
    private javax.swing.JTextArea OutputTextArea;
    private javax.swing.JMenuItem PackagePLAN;
    private javax.swing.JOptionPane PopUpMessage;
    private javax.swing.JLabel PreviewLabel;
    private javax.swing.JPanel PreviewPanel;
    private javax.swing.JMenuItem PrintDependenciesButton;
    private javax.swing.JMenuItem RemoveEntry;
    private javax.swing.JMenuItem ReplaceSelected;
    private javax.swing.JSplitPane RightHandStuff;
    private javax.swing.JScrollPane TextPrevScroll;
    private javax.swing.JTextArea TextPreview;
    private javax.swing.JMenu ToolsMenu;
    private javax.swing.JPanel ToolsPanel;
    private javax.swing.JPanel ToolsPanel2;
    private javax.swing.JMenuItem ZeroEntry;
    private javax.swing.JFrame aboutWindow;
    private javax.swing.JFileChooser fileChooser;
    private tv.porst.jhexview.JHexView hexViewer;
    private javax.swing.JFrame jFrame1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenu jMenu7;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JSplitPane jSplitPane3;
    private javax.swing.JTextArea jTextArea2;
    private javax.swing.JProgressBar mapLoadingBar;
    public javax.swing.JTree mapTree;
    private javax.swing.JPanel pnlOutput;
    // End of variables declaration//GEN-END:variables
}