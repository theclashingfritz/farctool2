package com.philosophofee.farctool2.parsers;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

public class MapParser {

 public DefaultTreeModel parseMapIntoMemory(TreeNode root, File file) {
  DefaultTreeModel model = new DefaultTreeModel(root);
  try {
   try ( 
    // Open the selected file. // 
    DataInputStream mapAccess = new DataInputStream(new FileInputStream(file))) {
    
    // Initialize Variables // 
    long begin = System.currentTimeMillis();
    boolean lbp3map = false;
    
    // Detect from which game the .MAP originates. //
    int header = mapAccess.readInt();
    switch (header)
    {
        case 256:
            System.out.println("Detected: LBP 1/2 .MAP File");
            break;
        case 21496064:
               System.out.println("Detected: LBP3 .MAP File");
               lbp3map = true;
               break;
        case 936:
            System.out.println("Detected: LBP Vita .MAP File");
            break;
        default:
            throw new IOException("Error reading 4 bytes - not a valid .map file");
    }
    
    // Get the amount of entries present in the .MAP File //
    int mapEntries = mapAccess.readInt();
    System.out.println("There are " + mapEntries + " entries present in the .MAP file");
    
    // Enumerate each entry detected. //
    int fileNameLength = 0;
    String fileName = "";
    for (int i = 0; i < mapEntries; i++) 
    {

        // Seek 2 bytes if the .MAP originates from LBP1/2. //
        if (!lbp3map)
            mapAccess.skip(2);
        
        // Get path of entry //
        fileNameLength = mapAccess.readShort();
        byte[] fileNameBytes = new byte[fileNameLength];
        mapAccess.read(fileNameBytes);
        fileName = new String(fileNameBytes);

        // Seek 4 bytes if the .MAP originates from LBP1/2. (Padding) //
        if (!lbp3map) 
            mapAccess.skip(4);
        
        // Skip the rest of the data as it's obtained at a future point in time. //
        mapAccess.skip(32);
        
        if (fileName.contains(".fsb") || fileName.contains(".farc") || fileName.contains(".sdat") || fileName.contains(".edat") || fileName.contains(".bik") || fileName.contains(".fnt") || fileName.contains(".fev"))
            continue;

        // Build the node for the JTree. //
        buildTreeFromString(model, fileName);

    }
    
    // Print the amount of time it took to process. //
    long end = System.currentTimeMillis();
    long timeTook = end - begin;
    System.out.println(".MAP parsed in " + (timeTook / 1000) + " seconds (" + timeTook + "ms)");
   }

  } catch (FileNotFoundException ex) {} catch (IOException ex) {}
  
  return model;
 }

 public void buildTreeFromString(final DefaultTreeModel model, final String str) {
  DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();

  String[] strings = str.split("/");

  DefaultMutableTreeNode node = root;

  for (String s: strings) {
   int index = childIndex(node, s);

   if (index < 0) {
    DefaultMutableTreeNode newChild = new DefaultMutableTreeNode(s);
    node.insert(newChild, node.getChildCount());
    node = newChild;
   } else {
    node = (DefaultMutableTreeNode) node.getChildAt(index);
   }
  }
 }

 private int childIndex(final DefaultMutableTreeNode node, final String childValue) {
  Enumeration < TreeNode > children = node.children();
  DefaultMutableTreeNode child = null;
  int index = -1;

  while (children.hasMoreElements() && index < 0) {
   child = (DefaultMutableTreeNode) children.nextElement();

   if (child.getUserObject() != null && childValue.equals(child.getUserObject())) {
    index = node.getIndex(child);
   }
  }

  return index;
 }
 
 public DefaultMutableTreeNode sort(DefaultMutableTreeNode node) {

    //sort alphabetically
    for(int i = 0; i < node.getChildCount() - 1; i++) {
        DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
        String nt = child.getUserObject().toString();

        for(int j = i + 1; j <= node.getChildCount() - 1; j++) {
            DefaultMutableTreeNode prevNode = (DefaultMutableTreeNode) node.getChildAt(j);
            String np = prevNode.getUserObject().toString();

            System.out.println(nt + " " + np);
            if(nt.compareToIgnoreCase(np) > 0) {
                node.insert(child, j);
                node.insert(prevNode, i);
            }
        }
        if(child.getChildCount() > 0) {
            sort(child);
        }
    }

    //put folders first - normal on Windows and some flavors of Linux but not on Mac OS X.
    for(int i = 0; i < node.getChildCount() - 1; i++) {
        DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
        for(int j = i + 1; j <= node.getChildCount() - 1; j++) {
            DefaultMutableTreeNode prevNode = (DefaultMutableTreeNode) node.getChildAt(j);

            if(!prevNode.isLeaf() && child.isLeaf()) {
                node.insert(child, j);
                node.insert(prevNode, i);
            }
        }
    }

    return node;

}
 
 
}