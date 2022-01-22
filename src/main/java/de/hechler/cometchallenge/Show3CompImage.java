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
public class Show3CompImage
{

  public Show3CompImage(final String title, final BufferedImage imagePrevious, final BufferedImage imageThis, final BufferedImage imageNext)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        JFrame editorFrame = new JFrame(title);
        editorFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        
        ImageIcon imageIconPrevious = new ImageIcon(imagePrevious);
        JLabel jLabelPrevious = new JLabel();
        jLabelPrevious.setIcon(imageIconPrevious);
        editorFrame.getContentPane().add(jLabelPrevious, BorderLayout.PAGE_START);

        ImageIcon imageIconThis = new ImageIcon(imageThis);
        JLabel jLabelThis = new JLabel();
        jLabelThis.setIcon(imageIconThis);
        editorFrame.getContentPane().add(jLabelThis, BorderLayout.CENTER);

        ImageIcon imageIconNext = new ImageIcon(imageNext);
        JLabel jLabelNext = new JLabel();
        jLabelNext.setIcon(imageIconNext);
        editorFrame.getContentPane().add(jLabelNext, BorderLayout.PAGE_END);

        editorFrame.pack();
        editorFrame.setLocationRelativeTo(null);
        editorFrame.setVisible(true);
      }
    });
  }
}