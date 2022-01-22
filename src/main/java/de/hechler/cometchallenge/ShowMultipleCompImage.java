package de.hechler.cometchallenge;

import java.awt.GridLayout;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * from: https://alvinalexander.com/blog/post/jfc-swing/complete-java-program-code-open-read-display-image-file/
 */
public class ShowMultipleCompImage
{

  public ShowMultipleCompImage(final String title, final int rows, final int cols, final BufferedImage[] images)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(rows, cols, 5, 5));
        
        for (BufferedImage image:images) {
        	ImageIcon imageIcon = new ImageIcon(image);
        	JLabel jLabel = new JLabel();
        	jLabel.setIcon(imageIcon);
        	frame.getContentPane().add(jLabel);
        }

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
      }
    });
  }
}