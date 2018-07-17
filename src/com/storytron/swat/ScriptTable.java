package com.storytron.swat;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import com.storytron.enginecommon.Pair;
import com.storytron.enginecommon.Triplet;
import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;

public final class ScriptTable {
	private Swat swat;
//**********************************************************************	
	public ScriptTable(Swat swat) {
		this.swat = swat;
	}
//**********************************************************************	
	public void displayScriptOwners(Dialog frame,ArrayList<Triplet<Script.Type,String[],String>> data) {
		JDialog scriptFrame = new JDialog(frame,"Scripts");
		scriptFrame.setSize(800, 500);
		scriptFrame.setLocation(100,200);
		scriptFrame.setVisible(true);
		scriptFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		scriptFrame.getContentPane().setBackground(Color.red);
		scriptFrame.getContentPane().setLayout(new BoxLayout(scriptFrame.getContentPane(), BoxLayout.Y_AXIS));
		
		Box bigBox = Box.createHorizontalBox();
		Box leftBox = Box.createVerticalBox();
		Box rightBox = Box.createVerticalBox();
		for (int i=0; (i<data.size()); ++i) {
			final Pair<ScriptPath,Script> s = swat.dk.getScriptPath(data.get(i).first, data.get(i).second);
			JButton scriptButton = new JButton(s.first.getPath(s.second));
			scriptButton.setMaximumSize(new Dimension(500,20));
			scriptButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					swat.showScript(s.first,s.second);
				}
			});

			leftBox.add(scriptButton);
			JLabel scriptLabel = new JLabel(data.get(i).third);
			scriptLabel.setPreferredSize(new Dimension(400,25));
			rightBox.add(scriptLabel);
		}
		leftBox.add(Box.createVerticalGlue());
		rightBox.add(Box.createVerticalGlue());
		bigBox.add(leftBox);
		bigBox.add(Box.createHorizontalStrut(20));
		bigBox.add(rightBox);
		JScrollPane jsp = new JScrollPane(bigBox);
		scriptFrame.setContentPane(jsp);
//		scriptFrame.pack();
	}

}
