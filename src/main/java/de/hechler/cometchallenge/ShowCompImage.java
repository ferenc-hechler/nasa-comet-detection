package de.hechler.cometchallenge;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * from: https://alvinalexander.com/blog/post/jfc-swing/complete-java-program-code-open-read-display-image-file/
 */
public class ShowCompImage
{

  public ShowCompImage(final String title, final BufferedImage image, final BufferedImage imageComp)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        JFrame editorFrame = new JFrame(title);
        editorFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        
        ImageIcon imageIcon = new ImageIcon(image);
        JLabel jLabel = new JLabel();
        jLabel.setIcon(imageIcon);
        editorFrame.getContentPane().add(jLabel, BorderLayout.CENTER);

        ImageIcon imageIconComp = new ImageIcon(imageComp);
        JLabel jLabelComp = new JLabel();
        jLabelComp.setIcon(imageIconComp);
        editorFrame.getContentPane().add(jLabelComp, BorderLayout.PAGE_END);

        editorFrame.pack();
        editorFrame.setLocationRelativeTo(null);
        editorFrame.setVisible(true);
      }
    });
  }
}