package de.hechler.cometchallenge.gui;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * from:
 * https://alvinalexander.com/blog/post/jfc-swing/complete-java-program-code-open-read-display-image-file/
 */
public class ShowControlledImages implements ImageWindow {

	private String title;
	private ImageController controller;
	
	private JButton btLeft;
	private JButton btRight;
	private JLabel lbCurrentInfo; 
	private JLabel lbImage;
	
	public ShowControlledImages(String title, ImageController controller) {
		this.title = title;
		this.controller = controller;
		
		SwingUtilities.invokeLater(() -> startDialog());
	}

	
	public void startDialog() {
		JFrame window = new JFrame(title);
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 2));
		window.getContentPane().add(buttonPane, BorderLayout.PAGE_START);

		JButton btMode = new JButton("#");
		btMode.addActionListener(ae -> controller.switchMode(ShowControlledImages.this));
		buttonPane.add(btMode);

		buttonPane.add(Box.createHorizontalStrut(10));

		btLeft = new JButton("<");
		btLeft.addActionListener(ae -> controller.left(ShowControlledImages.this));
		buttonPane.add(btLeft);

		btRight = new JButton(">");
		btRight.addActionListener(ae -> controller.right(ShowControlledImages.this));
		buttonPane.add(btRight);

		buttonPane.add(Box.createHorizontalStrut(10));

		JButton btInfo = new JButton("(i)");
		btInfo.addActionListener(ae -> controller.info(ShowControlledImages.this));
		buttonPane.add(btInfo);

		lbCurrentInfo = new JLabel();
		buttonPane.add(lbCurrentInfo);

		ImageIcon imageIcon = new ImageIcon();
		lbImage = new JLabel();
		window.getContentPane().add(lbImage, BorderLayout.CENTER);

		updateControlsASync();
		
		window.pack();
		window.setLocationRelativeTo(null);
		window.setVisible(true);
	}


	
	@Override
	public void updateControls() {
		SwingUtilities.invokeLater(() -> updateControlsASync());
	}

	public void updateControlsASync() {
		btLeft.setEnabled(controller.hasLeft()); 
		btRight.setEnabled(controller.hasRight()); 
		lbCurrentInfo.setText(controller.getCurrentInfo());
		lbImage.setIcon(new ImageIcon(controller.getCurrentImage()));
	}

}