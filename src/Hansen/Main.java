package Hansen;
/* 
 * 
 * Author: Josh Hansen
 *
 * This program scans the pixel colors in an image and interprets them as tones which are played back to the
 * user, with some controls to modulate output as part of a complete GUI experience.  This will essentially allow a user
 * to “hear” an image and modulate the sound to become a sort of impromptu 8-bit song.
 *
 */

//*********************************************************************************************************************
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import javax.swing.JFileChooser;
import java.io.File; 
import java.io.IOException;
import java.util.Hashtable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage; 
import javax.imageio.ImageIO;

//********************************************************************** Main
public class Main
{
   //the main class only initiates the model, which then starts the GUI
   public static void main(String[] args)
   {
      Model.startImageToTonesClient();
   }
}
//****************************************************************** End Main

//********************************************************************* Model
class Model
{
   //modifiers for the image processing and sound output, to pass to the
      //respective classes
   public static int thisDuration; 
   public static Boolean keyOfCOutput;
   public static int sampleSpan;
   public static double pitchMod;

   //initiates the GUI
   public static void startImageToTonesClient()
   {
      View.startImagetoTonesGUI();
   }

   //the primary method that converts the image to sound
   public static void startConversion() throws LineUnavailableException
   {
      int[] tones; //the array of sounds we will pass to the sound generator

      //set modifiers from user-settings in the GUI
      thisDuration = View.durationSlider.getValue();
      keyOfCOutput = View.keyOfCButton.isSelected();
      sampleSpan = View.spanSlider.getValue();
      pitchMod = View.pitchSlider.getValue();

      //give the user a stop button to cancel
      View.setStopButton();
      
      //prepare new generator and thread for this conversion
      SoundGenerator newSoundGen;
      newSoundGen = new SoundGenerator();
      newSoundGen.thisThread = new Thread(newSoundGen);
      SoundGenerator.setStopped(false);
      
      //start the progress bar
      View.startProgressBar();

      //catches image reading IO exception
      try
      {
         //set the image processor and process image to tones
         ImageProcessor.setSampleSpan(sampleSpan);
         ImageProcessor.setKeyOfCOutput(keyOfCOutput);
         tones = ImageProcessor.processImage();
         
         //finish setting sound generator and start thread
         newSoundGen.setDuration(thisDuration);
         newSoundGen.setTones(tones);
         newSoundGen.setPitchMod(pitchMod);
         newSoundGen.thisThread.start();
      }
      catch (IOException e1)
      {
         //reset conversion button if failed
         System.out.println("Error processing image: " + e1);
         View.setConvertButton();
      }
   }

   //allows the user to choose an image which is assigned to the processor
   public static void getImage()
   {
      //setup and open a file chooser, the user chooses
      File newImageFile;
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
      FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "JPG & PNG Images", "jpg", "jpeg", "png");
      fileChooser.setFileFilter(filter);
      int returnValue = fileChooser.showOpenDialog(null);

      //if the user chose an image, we grab it and try to process it
      if (returnValue == JFileChooser.APPROVE_OPTION)
      {
         newImageFile = fileChooser.getSelectedFile();

         if (!ImageProcessor.setImage(newImageFile))
         {
            View.imageErrorDiaglog();
            return;
         }
         else
         {
            View.showSelectedImage(ImageProcessor.image);
         }
      }
      
      //if we are here then we have a photo ready, set the convert button
      View.setConvertButton();
      
   }

   //conversion was stopped by user
   public static void stopConversion()
   {
      //set back the convert button
      View.setConvertButton();
      //tell the generator to stop
      SoundGenerator.setStopped(true);
   }
}
//***************************************************************** End Model

//********************************************************************** View
class View
{
   //most strings used for this class to avoid hardcoding
   public static final String[] LABEL_STRINGS = {"Image to Tones Converter",
                                                "Convert",
                                                "Stop",
                                                "Image selected",
                                                "Choose an image to convert to tones...",
                                                "Tone duration(ms)",
                                                "Pixel skipping",
                                                "Pitch modifier",
                                                "Output as musical tones",
                                                "Reset to default",
                                                "Controls how long each tone plays.", 
                                                "Controls how many pixels are skipped as tones are generated.",
                                                "Modifies the output pitch, or transposes the key if using musical output.",
                                                "Converts RGB values to musical notes in a single key."};
   
   //the GUI elements
   public static JFrame mainFrame;
   public static JPanel topPanel, buttonPanel, imagePanel, progressBarPanel, selectionsPanel;
   public static JButton convertButton, stopButton, chooserButton, selectionDefaultsButton;
   public static JLabel imageSelectedJLabel;
   public static JSlider durationSlider, spanSlider, pitchSlider;
   public static JCheckBox keyOfCButton;
   public static JProgressBar progressBar;

   //The primary GUI setting method, calls other methods to complete the GUI
   public static void startImagetoTonesGUI()
   {
      //overall look and feel
      JFrame.setDefaultLookAndFeelDecorated(true);
      JDialog.setDefaultLookAndFeelDecorated(true);

      //setting a main frame and layout
      mainFrame = new JFrame(LABEL_STRINGS[0]);
      mainFrame.setLayout(new BorderLayout());

      //setting the top panel and progress bar below
      setTopPanel();
      setProgressBarPanel();
      
      //more main frame parameters and setting visible
      mainFrame.setSize(1200, 600);
      mainFrame.setLocationRelativeTo(null);
      mainFrame.setResizable(true);
      mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      mainFrame.setVisible(true);
   }
   
   //arranging the top panel with it's 2 subpanels
   public static void setTopPanel()
   {
      topPanel = new JPanel();
      topPanel.setLayout(new GridLayout(1,2));
      mainFrame.add(topPanel, BorderLayout.CENTER);
      setSelectionsPanel();
      setImagePanel();
   }
   
   //adding panel by which user navigate the GUI
   public static void setSelectionsPanel()
   {
      //setup the framework for the selections panel
      selectionsPanel = new JPanel();
      selectionsPanel.setLayout(new GridLayout(7,1, 10, 10));
      selectionsPanel.setBorder(new EmptyBorder(10,10,10,0));
      
      //add the sliders and image chooser
      setChooserButton();
      setDurationSlider();
      setSpanSlider();
      setPitchSlider();
      
      //setup and add the "pretify" musical conversion limiter button
      keyOfCButton = new JCheckBox(LABEL_STRINGS[8]);
      keyOfCButton.setBorder(new EmptyBorder(0, 15, 0, 0));
      keyOfCButton.setSelected(true);
      selectionsPanel.add(keyOfCButton);
      keyOfCButton.setToolTipText(LABEL_STRINGS[13]); //handy tooltip
      
      //setup and add a button to reset all to defaults
      selectionDefaultsButton = new JButton(LABEL_STRINGS[9]);
      selectionDefaultsButton.setSize(5,5);
      selectionDefaultsButton.setActionCommand("selectionDefaults");
      selectionDefaultsButton.addActionListener(Controller.bp);
      selectionsPanel.add(selectionDefaultsButton);
      
      //setup and add the sub-sub-panel that will hold the convert and stop buttons
      buttonPanel = new JPanel();
      buttonPanel.setLayout(new BorderLayout());
      buttonPanel.setBorder(new EmptyBorder(5,5,5,5));
      selectionsPanel.add(buttonPanel);
      
      //add the whole subpanel to the top panel
      topPanel.add(selectionsPanel);
   }
   
   //setup and add the image panel to the right side of the top panel
   public static void setImagePanel()
   {
      imagePanel = new JPanel();
      imagePanel.setLayout(new BorderLayout());
      imagePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
      imagePanel.setSize(ImageProcessor.IMAGE_WIDTH, ImageProcessor.IMAGE_WIDTH);
      topPanel.add(imagePanel);
   }
   
   //setup and add the duration slider
   public static void setDurationSlider()
   {
      Hashtable<Integer, JLabel> positions = new Hashtable<Integer, JLabel>();
      
      //prepare the slider and set it up
      durationSlider = new JSlider(SoundGenerator.MIN_DURATION,
                                    SoundGenerator.MAX_DURATION, 
                                    SoundGenerator.DEFAULT_DURATION);
      durationSlider.setMajorTickSpacing(20);
      durationSlider.setPaintTicks(true);
      durationSlider.setPaintLabels(true);
      
      // Add positions label in the slider
      positions.put(20, new JLabel("20"));
      positions.put(100, new JLabel("100"));
      positions.put(200, new JLabel("200"));
      positions.put(300, new JLabel("300")); 
      
      // Set the label to be drawn
      durationSlider.setLabelTable(positions); 
      durationSlider.setBorder(BorderFactory.createTitledBorder(LABEL_STRINGS[5]));
      
      //add the slider and a tooltip
      selectionsPanel.add(durationSlider);
      durationSlider.setToolTipText(LABEL_STRINGS[10]);
   }
   
   //setup and add the pixel skipping slider
   public static void setSpanSlider()
   {
      Hashtable<Integer, JLabel> positions = new Hashtable<Integer, JLabel>();
      
      //prepare the slider and set it up
      spanSlider = new JSlider(ImageProcessor.MIN_SAMPLE_SPAN,
                                 ImageProcessor.MAX_SAMPLE_SPAN, 
                                 ImageProcessor.DEFAULT_SAMPLE_SPAN);
      spanSlider.setMajorTickSpacing(20);
      spanSlider.setPaintTicks(true);
      spanSlider.setPaintLabels(true);
      
      // Add positions label in the slider
      positions = new Hashtable<Integer, JLabel>();
      positions.put(1, new JLabel("1"));
      positions.put(21, new JLabel("20"));
      positions.put(41, new JLabel("40"));
      positions.put(61, new JLabel("60"));
      positions.put(81, new JLabel("80"));
      positions.put(99, new JLabel("100"));
      
      // Set the label to be drawn
      spanSlider.setLabelTable(positions); 
      spanSlider.setBorder(BorderFactory.createTitledBorder(LABEL_STRINGS[6]));
      
      //add the slider and a tooltip
      selectionsPanel.add(spanSlider);
      spanSlider.setToolTipText(LABEL_STRINGS[11]);
   }
   
   //setup and add the pitch slider
   public static void setPitchSlider()
   {
      Hashtable<Integer, JLabel> positions = new Hashtable<Integer, JLabel>();
      
      //prepare the slider and set it up
      pitchSlider = new JSlider((int)SoundGenerator.MIN_PITCH_MOD, (int)SoundGenerator.MAX_PITCH_MOD,
            (int)SoundGenerator.DEFAULT_PITCH_MOD);
      pitchSlider.setMajorTickSpacing(1);
      pitchSlider.setPaintTicks(true);
      pitchSlider.setPaintLabels(true);
      
      // Add positions label in the slider
      positions = new Hashtable<Integer, JLabel>();
      positions.put(-20, new JLabel("-20"));
      positions.put(-15, new JLabel("-15"));
      positions.put(-10, new JLabel("-10"));
      positions.put(-5, new JLabel("-5"));
      positions.put(0, new JLabel("0"));
      positions.put(5, new JLabel("5"));
      positions.put(10, new JLabel("10"));
      positions.put(15, new JLabel("15"));
      
      // Set the label to be drawn
      pitchSlider.setLabelTable(positions); 
      pitchSlider.setBorder(BorderFactory.createTitledBorder(LABEL_STRINGS[7]));
      
      //add the slider and a tooltip
      selectionsPanel.add(pitchSlider);
      pitchSlider.setToolTipText(LABEL_STRINGS[12]);
   }
   
   //intialize and add the panel where the progress bar will sit
   public static void setProgressBarPanel()
   {
      progressBarPanel = new JPanel();
      progressBarPanel.setLayout(new BoxLayout(progressBarPanel, 0));
      mainFrame.add(progressBarPanel, BorderLayout.PAGE_END);
   }
   
   //when called will set all default values for user settings and update the GUI
   public static void setSelectionsToDefault()
   {
      pitchSlider.setValue((int)SoundGenerator.DEFAULT_PITCH_MOD);
      spanSlider.setValue(ImageProcessor.DEFAULT_SAMPLE_SPAN);
      durationSlider.setValue(SoundGenerator.DEFAULT_DURATION);
      keyOfCButton.setSelected(true);
      selectionsPanel.repaint();
      selectionsPanel.updateUI();
      mainFrame.setVisible(true); 
   }

   //sets the button in the selections panel for user to upload a photo
   public static void setChooserButton()
   {
      chooserButton = new JButton(LABEL_STRINGS[4]);
      chooserButton.setActionCommand("chooseImage");
      chooserButton.addActionListener(Controller.bp);
      selectionsPanel.add(chooserButton, BorderLayout.PAGE_START);
   }

   //setup and add the convert button to the button panel within selections panel
   public static void setConvertButton()
   {
      buttonPanel.removeAll();
      convertButton = new JButton(LABEL_STRINGS[1]);
      convertButton.setActionCommand(LABEL_STRINGS[1]);
      convertButton.addActionListener(Controller.bp);
      convertButton.setBorder(new EmptyBorder(5,5,5,5));
      convertButton.setBackground(Color.GREEN);
      convertButton.setOpaque(true);
      buttonPanel.add(convertButton);
      buttonPanel.repaint();
      buttonPanel.updateUI();
      buttonPanel.setVisible(true);
   }

   //switch the convert button in the selections panel to a stop button
      //that interupts the output and resets the UI
   public static void setStopButton()
   {
      buttonPanel.removeAll();
      stopButton = new JButton(LABEL_STRINGS[2]);
      stopButton.setActionCommand(LABEL_STRINGS[2]);
      stopButton.addActionListener(Controller.bp);
      stopButton.setBorder(new EmptyBorder(5,5,5,5));
      stopButton.setBackground(Color.RED);
      stopButton.setOpaque(true);
      buttonPanel.add(stopButton);
      buttonPanel.repaint();
      buttonPanel.updateUI();
      mainFrame.setVisible(true);
   }

   //if image selection fails, notify the user
   public static void imageErrorDiaglog()
   {
      JOptionPane.showMessageDialog(mainFrame, "Please choose another image.", "Invalid image", 1);
   }

   //when image has been successfully assigned to the processor, add to the UI
   public static void showSelectedImage(BufferedImage image)
   {
      imagePanel.removeAll();
      ImageIcon newImageIcon = new ImageIcon(image);
      imageSelectedJLabel = new JLabel(newImageIcon);
      imageSelectedJLabel.setBorder(BorderFactory.createTitledBorder(LABEL_STRINGS[3]));
      imagePanel.add(imageSelectedJLabel);
      imagePanel.repaint();
      imagePanel.updateUI();
      mainFrame.setVisible(true);
   }
   
   //initialize the progress bar, ready to be advanced by the sound generator
   public static void startProgressBar()
   {
      progressBar = new JProgressBar(0, 0, 100);
      progressBar.setStringPainted(true);
      progressBar.setForeground(Color.GREEN);
      progressBar.setBorder(new EmptyBorder(0,10,0,10));
      progressBarPanel.add(progressBar);
   }
}
//****************************************************************** End View

//**************************************************************** Controller
class Controller
{
   //instance of action listener for the whole program
   static buttonPressed bp = new buttonPressed();

   //If the button was pressed, intilializes the next round...
   static class buttonPressed implements ActionListener
   {
      public void actionPerformed(ActionEvent e) {

         //all purpose listener for evvery button in the GUI
         switch (e.getActionCommand()) {

         case "Convert": //convert button pressed
            try
            {
               Model.startConversion(); //try to start conversion
            } catch (LineUnavailableException e1)
            {
               System.out.println("Failed to start conversion: " + e);
               //otherwise, notify console
            }
            break;
         case "Stop": //stop button pressed
            Model.stopConversion(); //interrupt conversion
            break;
         case "chooseImage": //image selection button pressed
            Model.getImage(); //start file chooser
            break;
         case "selectionDefaults": //reset to default button pressed
            View.setSelectionsToDefault(); //send reset command to view
            break;
         default:
            break;
         }
      }
   }
}
//************************************************************ End Controller

//************************************************************ ImageProcessor
class ImageProcessor
{
   public static BufferedImage image = null; // For storing image in RAM 
   //width that input image is converted to
   public static final int IMAGE_WIDTH = 500; 
   //height that input image is converted to
   public static final int IMAGE_HEIGHT = 500; 
   public static final int MIN_SAMPLE_SPAN = 1; //at minimum, convert every pixel
   public static final int MAX_SAMPLE_SPAN = 100; //maximum number of pixels to skip
   public static final int DEFAULT_SAMPLE_SPAN = 21; //default num pixels to skip
   public static final int COLOR_RANGE = 255*3; //max value of color sums
   //set the actual span to default for now
   private static int sampleSpan = DEFAULT_SAMPLE_SPAN; 
   //controls whether we will "prettify" the output to a single musical key
   private static Boolean keyOfCOutput = true; //true by default
   
   //the workhorse functions, converts an image into an int[] tones array
   public static int[] processImage() throws IOException 
   { 
      if (image == null)
         throw new IOException();

      Color nextColor; //temp value for processing colors from pixels
      int nextColorSum; //variable for aggregate of RGB vbalues
      int colorCounter = 0; //index for array of tones
      //the data to return after processing
      int[] colorsAsTones = new int[(IMAGE_WIDTH/sampleSpan)*(IMAGE_HEIGHT/sampleSpan)];

      //the processor, looping through pixels according to span value
      for (int j = 0; j < IMAGE_HEIGHT; j+=sampleSpan)
      {
         for (int k = 0; k < IMAGE_WIDTH; k+=sampleSpan)
         {
            //grab the pixel color
            nextColor = new Color(image.getRGB(k, j)); 
            
            //convert to aggregate RGB value
            nextColorSum = (nextColor.getBlue()+nextColor.getRed()+nextColor.getGreen());

            //convert this color directly to a tone or filtered into the key of C
            if (keyOfCOutput == true)
               colorsAsTones[colorCounter] = cnvtColorToCTone(nextColorSum);
            else
            {
               colorsAsTones[colorCounter] = nextColorSum;
            }
            
            //increase the tones array index
            colorCounter++;
            //escape loop if array is full
            if (colorCounter > (colorsAsTones.length -1))
               break;
         }
         //escape loop if array is full
         if (colorCounter > (colorsAsTones.length -1))
            break;
      }
      //send back the completed tones array
      return colorsAsTones;
   }
   
   //called when image is selected by user with File chooser
   public static Boolean setImage(File fileParam)
   {
      //try to input the image to buffer, resizing to constant width and height
      try
      { 
         image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB); 
         // Reading input file 
         image = ImageIO.read(fileParam); 
         resize(); //for processing and GUI purposes
      } 
      catch(IOException e)
      { 
         return false;
      }

      return true;
   }
   
   //accessor for image
   public static BufferedImage getImage()
   {
      BufferedImage returnImage = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB); 
      returnImage.equals(image);
      return returnImage;
   }

   //resize image to constainments by redrawing with Graphics2D
   public static void resize()
   { 
      Image tempImage = image.getScaledInstance(IMAGE_WIDTH, IMAGE_HEIGHT, Image.SCALE_SMOOTH);
      BufferedImage newImage = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);

      Graphics2D myGraphics2D = newImage.createGraphics();
      myGraphics2D.drawImage(tempImage, 0, 0, null);
      myGraphics2D.dispose();

      image = newImage;
   }

   //sampleSpan mutator, accessor, validity test
   public static Boolean setSampleSpan(int sampleSpanParam)
   {
      if (validSampleSpan(sampleSpanParam))
      {
         sampleSpan = sampleSpanParam;
         return true;
      }
      else
      {
         sampleSpan = DEFAULT_SAMPLE_SPAN;
         return false;
      }
   }
   public static int getSampleSpan()
   {
      return sampleSpan;
   }
   private static Boolean validSampleSpan(int sampleSpanParam)
   {
      if ((sampleSpanParam <= MAX_SAMPLE_SPAN) && (sampleSpanParam >= MIN_SAMPLE_SPAN))
      {
         return true;
      }
      else
      {
         return false;
      }
   }
   
   //Musical key output mutator, accessor
   public static Boolean setKeyOfCOutput(Boolean keyOfCOutputParam)
   {
      keyOfCOutput = keyOfCOutputParam;
      return true;
   }
   public static Boolean getKeyOfCOutput()
   {
      return keyOfCOutput;
   }
   
   //filter an aggregate RGB value into a tone in the key of C from ~C4 to ~C6
   public static int cnvtColorToCTone(int colorSum)
   {
      int[] toneList = {440, 494, 523, 587, 659, 698, 784, 880};
      int colorRangeEigth = COLOR_RANGE/toneList.length;
      
      for (int k = 0; k < toneList.length; k++)
      {
         if (colorSum < colorRangeEigth*(k+1))
            return toneList[k];
      }
      
      return 523;
   }
}
//******************************************************** End ImageProcessor

//************************************************************ SoundGenerator
class SoundGenerator implements Runnable //the sound player for the program, runs in it's own thread
{
   
   public static final float BASE_FREQUENCY = 41000; //44100 sample points per 1 second
   public static final int MAX_DURATION = 300; //max ms that we play each tone
   public static final int MIN_DURATION = 20; //min ms that we play each tone
   public static final int DEFAULT_DURATION = 100; //default ms that we play each tone
   public static final int MAX_TONE = 7902; //max tone in hz that we will actually play
   public static final int MIN_TONE = 28; //min tone in hz that we will actually play
   //the most that we will decrease the base frequency
   public static final double MIN_PITCH_MOD = -20; 
   //the most that we will increase the base frequency
   public static final double MAX_PITCH_MOD = 15;
   public static final double DEFAULT_PITCH_MOD = 0; //default leaves pitch/frequency at base
   private int duration; //the actual ms per tone value
   //the format with which to create the data line
   private AudioFormat myAudioFormat = new AudioFormat(BASE_FREQUENCY, 16, 1, true, false);
   //the data line which will directly play the tones
   private SourceDataLine mySourceDataLine;
   //volume value to modulate and soften the sounds a bit
   private FloatControl volume;
   //value to interrupt the playback
   private static Boolean stopped = false;
   //the passed in value processed from image processor to loop through
   private int[] tones;
   //the passed-in pitch mod value for the actual playback
   private double pitchMod;
   //the new thread on which the playback will run
   public Thread thisThread;

   //no parameter constructor only sets up data line and sets the duration to default
   public SoundGenerator() throws LineUnavailableException
   {   
      mySourceDataLine = AudioSystem.getSourceDataLine(myAudioFormat);
      duration = DEFAULT_DURATION;
   }

   //constructor with duration parameter sets this and prepares the data line
   public SoundGenerator(int durationParam) throws LineUnavailableException
   {    
      mySourceDataLine = AudioSystem.getSourceDataLine(myAudioFormat);
      setDuration(durationParam);
   }
   
   //the thread call that starts the sequence or notifies the user of failure
   @Override
   public void run()
   {
      try
      {
         playSequence();
      }
      catch (LineUnavailableException e)
      {
         System.out.println("Could not play sequence: " + e);
      }
   }

   //the workhorse function that loops through the tones array and plays it
   private void playSequence() throws LineUnavailableException
   {      

      //preparing the data line and frequency
      mySourceDataLine.open();
      mySourceDataLine.start();
      float newFrequency = BASE_FREQUENCY;

      //if valid, pitchMod adjusts base frequency to allow for many different octaves and keys
      if (validPitchMod(pitchMod))
         newFrequency = newFrequency - (BASE_FREQUENCY*(float)(pitchMod*.059)); 

      //initiliazing the volume value to soften the tone playback
      volume = (FloatControl) mySourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);

      //the actual playback loop, playing each tone in the loop
      for (int k = 0; k < tones.length; k++)
      {
         if(stopped) //the interruption if user stops the playback
            break;
         
         //try to play a single tone and update the progress bar
            //or catch and notify user if playback throws exception
         try
         {
            writeSingleTone(tones[k], (float)newFrequency);
            View.progressBar.setValue((int)(((double)k/(double)tones.length)*(double)100));
         }
         catch (NullPointerException e)
         {
            System.out.println("Could not write tone at" + k + ": "  + e); 
         }
      }   
      
      //playback is complete, end the data line
      mySourceDataLine.drain();
      mySourceDataLine.stop();
      
      //interrupt thread, remove the progress bar, put back the convert button
      View.progressBarPanel.removeAll();
      View.setConvertButton();
      thisThread.interrupt();
   }
   
   //each call to this method plays a single tone during a conversion/playback session
   private void writeSingleTone(int tone, float frequency) throws LineUnavailableException
   {
      float numSamplesInFullSine = frequency / tone; //number of points on the sine wave to sample
      short maxSineDuration; //the max value that the sine wave will hit
      double angle; //creating the sine wave itself
      byte[] bytesBuffer = new byte[2]; //grabbing values from the sine wave to pass to data line
      //the length of each sine wave which we pass to the data line bit by bit to create the sound
      float sineWaveDuration = duration * ((float) frequency / 1000);
      
      //skip this tone if out of range
      if (!validTone(tone))
         return;

      for (int i = 0; i < sineWaveDuration; i++) //1000 ms in 1 second
      { 
         angle = i / (numSamplesInFullSine/ 2.0) * Math.PI; 
         maxSineDuration = (short) (Math.sin(angle) * 32767); //maximum sample value
         bytesBuffer[0] = (byte) (maxSineDuration & 0xFF); //first half of sine wave
         bytesBuffer[1] = (byte) (maxSineDuration >> 8); //second half of sine wave

         //modulating the volume to soften playback
         if (i < 1)
            volume.setValue(-80.0F);
         else if (i >= 1)
            volume.setValue(0.0F);
         else if (i >= (sineWaveDuration - 1))
            volume.setValue(-80.0F);

         //play the tone immediately by writing tone,
         //now converted to wave data as bits, to the SourceDataLine
         mySourceDataLine.write(bytesBuffer, 0, 2);

      }
   }

   //accessor and mutator for loop interrupter
   public static Boolean getStopped()
   {
      return stopped;
   }
   public static Boolean setStopped(Boolean stopCommand)
   {
      stopped = stopCommand;
      return true;
   }

   //duration mutator, accessor, validity test
   public Boolean setDuration(int durationParam)
   {
      if (validDuration(durationParam))
      {
         duration = durationParam;
         return true;
      }
      else
      {
         duration = DEFAULT_DURATION;
         return false;
      }
   }
   public int getDuration()
   {
      return duration;
   }
   private Boolean validDuration(int durationParam)
   {
      if ((durationParam <= MAX_DURATION) && (durationParam >= MIN_DURATION))
      {
         return true;
      }
      else
      {
         return false;
      }
   }

   //pitchMod mutator, accessor, validity test
   public Boolean setPitchMod(double pitchModParam)
   {
      if (validPitchMod(pitchModParam))
      {
         pitchMod = pitchModParam;
         return true;
      }
      else
      {
         pitchMod = DEFAULT_PITCH_MOD;
         return false;
      }
   }
   public double getPitchMod()
   {
      return pitchMod;
   }
   private Boolean validPitchMod(double pitchMod)
   {
      if ((pitchMod > MAX_PITCH_MOD) || (pitchMod < MIN_PITCH_MOD))
      {
         return false;
      }

      return true;
   }

   //tones array mutator, accessor, validity test
   public Boolean setTones(int[] tonesParam)
   {
      tones = tonesParam;
      return true;
   }
   public int[] getTones()
   {
      return tones;
   }
   private Boolean validTone(int toneParam)
   {
      if ((toneParam <= MAX_TONE) && (toneParam >= MIN_TONE))
      {
         return true;
      }
      else
      {
         return false;
      }
   }
   
   //required toString and equals methods, not very useful
   public SoundGenerator equals(SoundGenerator inputGen) throws LineUnavailableException
   {
      SoundGenerator returnGenerator = new SoundGenerator(inputGen.getDuration());

      return returnGenerator;
   }
   
   public String toString()
   {
      return ("Duration: " + duration + "\nSourceDataLine: " + mySourceDataLine.toString());
   }
}
//******************************************************* End SoundGenerator