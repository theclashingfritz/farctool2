package com.philosophofee.farctool2.utilities;

import com.philosophofee.farctool2.windows.MainWindow;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.riversun.bigdoc.bin.BigFileSearcher;

public class FarcUtils {

 //public static byte[] pullFromFarcFileName(String filename) {

 //}
 public static byte[] pullFromFarc(String currSHA1, File bigBoyFarc) {
  System.out.println("Converting " + currSHA1 + " to byte array...");

  //If the farc provided is nonexistant, boot us out
  if (bigBoyFarc == null) {
   return null;
  }

  //Grab file count
  int fileCount = 0;
  long tableOffset = 0;
  try {
   RandomAccessFile farcAccess = new RandomAccessFile(bigBoyFarc, "rw");
   farcAccess.seek(bigBoyFarc.length() - 8);
   fileCount = farcAccess.readInt();
   tableOffset = (bigBoyFarc.length() - 8 - (fileCount * 28));

   farcAccess.close();
  } catch (IOException ex) {}

  //Go to offset where SHA1's start
  BigFileSearcher searcher = new BigFileSearcher();
  long fileTableOffset = searcher.indexOf(bigBoyFarc, MiscUtils.hexStringToByteArray(currSHA1), tableOffset);
  if (fileTableOffset == -1) {
   System.out.println("This SHA1 isn't in the farc, dummy!");
   return null;
  }
  //System.out.println("entry position in table: " + fileTableOffset);

  //Let's do some extraction
  long newFileSize = 0;
  long newFileOffset = 0;
  byte[] newSHA1 = new byte[20];
  try {;
   RandomAccessFile farcAccess = new RandomAccessFile(bigBoyFarc, "rw");

   //go to the file table, and grab the hash for verification later
   farcAccess.seek(fileTableOffset);
   farcAccess.readFully(newSHA1);
   System.out.println("entry SHA1 in farc: " + MiscUtils.byteArrayToHexString(newSHA1));

   //seek past the sha1 and grab the offset to know where to extract the file
   farcAccess.seek(fileTableOffset + 20);
   byte bytes[] = new byte[8];
   farcAccess.read(bytes, 4, 4);
   newFileOffset = MiscUtils.getLong(bytes);
   System.out.println("entry offset: " + newFileOffset);

   //get file size so we can know how much data to pull later
   farcAccess.seek(fileTableOffset + 24);
   newFileSize = farcAccess.readInt();
   System.out.println("entry size: " + newFileSize);


   System.out.println("Gonna try extracting now!");
   long begin = System.currentTimeMillis();
   FileInputStream fin = new FileInputStream(bigBoyFarc);
   fin.skip(newFileOffset);
   byte[] outputbytes = new byte[(int) newFileSize];
   int output = 0;
   output = fin.read(outputbytes);

   fin.close();

   //System.out.println("Done in " + (timeTook / 1000) + " seconds (" + timeTook + "ms). ");
   //This whole process takes like 10 milliseconds so timing it is useless
   farcAccess.close();

   //finally, return our bytes!
   return outputbytes;

  } catch (IOException ex) {}
  return null; //something messed up
 }
 
  public static void addFile(File newFile, File bigBoyFarc) {
  try {
   byte[] SHA1 = MiscUtils.getSHA1(newFile);
   FileInputStream fis = new FileInputStream(newFile);
   byte[] file = fis.readAllBytes();
   fis.close();

   RandomAccessFile farc = new RandomAccessFile(bigBoyFarc, "rw");

   farc.seek(farc.length() - 8);
   long fileCount = farc.readInt();
   long tableOffset = (farc.length() - 8 - (fileCount * 28));

   farc.seek(tableOffset);
   byte[] table = new byte[(int)(farc.length() - tableOffset)];
   farc.read(table);


   farc.seek(tableOffset);
   farc.write(file);

   farc.write(table);
   farc.seek(farc.length() - 8);
   farc.write(SHA1);
   farc.write(ByteBuffer.allocate(4).putInt((int) tableOffset).array());
   farc.write(ByteBuffer.allocate(4).putInt(file.length).array());
   farc.write(ByteBuffer.allocate(4).putInt((int)(fileCount + 1)).array());
   farc.write("FARC".getBytes());

   farc.close();

  } catch (FileNotFoundException ex) {
   Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
  } catch (IOException ex) {
   Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
  } catch (Exception ex) {
   Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
  }
  System.out.println("Successfully injected file into .FARC!");
 }
  
  public static byte[] pullFromFAR4(String Offset, String Size, File FAR4)
  {
     try {
         byte[] file;
         try (RandomAccessFile FAR4Access = new RandomAccessFile(FAR4, "rw")) {
             FAR4Access.seek(Integer.parseInt(Offset, 16));
             file = new byte[Integer.parseInt(Size, 16)];
             FAR4Access.read(file);
         }
         return file;
     } catch (FileNotFoundException ex) {
         Logger.getLogger(FarcUtils.class.getName()).log(Level.SEVERE, null, ex);
     } catch (IOException ex) {
         Logger.getLogger(FarcUtils.class.getName()).log(Level.SEVERE, null, ex);
     }
     return null;
  }
}