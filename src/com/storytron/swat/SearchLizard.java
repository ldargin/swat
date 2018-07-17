package com.storytron.swat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import com.storytron.enginecommon.Utils;
import com.storytron.uber.Role;
import com.storytron.uber.Script;
import com.storytron.uber.ScriptPath;
import com.storytron.uber.Verb;
import com.storytron.uber.Script.Node;
import com.storytron.uber.operator.Operator;
import com.storytron.uber.operator.ParameterOperator;

public final class SearchLizard {
	private static final long serialVersionUID = 1L;
	private Swat swat;
	private JDialog myFrame;
	private Map<String,ArrayList<SearchEntry>> operatorScripts = new TreeMap<String,ArrayList<SearchEntry>>(new Comparator<String>(){
		public int compare(String o1, String o2) {
			return o1.compareToIgnoreCase(o2);
		}
	});
	private Map<Operator.Type,Map<String,ArrayList<SearchEntry>>> constantScripts = new EnumMap<Operator.Type,Map<String,ArrayList<SearchEntry>>>(Operator.Type.class);
//**********************************************************************
	SearchLizard(Swat sw) {
		swat = sw;

		searchScripts();

		JTabbedPane tabbedPane = new JTabbedPane(); 
		
		tabbedPane.addTab("non-constant operators",createTab(operatorScripts));
		for(Map.Entry<Operator.Type,Map<String,ArrayList<SearchEntry>>> e:constantScripts.entrySet())
			tabbedPane.addTab(e.getKey()+" constants",createTab(e.getValue()));
		
		myFrame = new JDialog(swat.getMyFrame(),"Operator Search Lizard");
		myFrame.getContentPane().add(tabbedPane);
		myFrame.setSize(400, 500);
		myFrame.setLocation(50,200);
		myFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		myFrame.setVisible(true);
		myFrame.validate();
	}
	
	private JComponent createTab(Map<String,ArrayList<SearchEntry>> opMap){
		JComponent searchPanel = Box.createVerticalBox();
		searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		for (final Map.Entry<String,ArrayList<SearchEntry>> entry: opMap.entrySet()) {
			Box linePanel = Box.createHorizontalBox();
			linePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			linePanel.add(Box.createHorizontalStrut(10));
			// here we must insert the code that will bring up a specific operator
			JButton operatorButton = new JButton(entry.getKey());
			operatorButton.setToolTipText(Utils.toHtmlTooltipFormat("Click to see a list of all Scripts using this Operator."));
			operatorButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					displayOperatorDetails(entry.getKey(),entry.getValue());
				}
			});
			operatorButton.setEnabled(true);
			
			linePanel.add(operatorButton);
			linePanel.add(Box.createHorizontalGlue());
			linePanel.add(new JLabel(String.valueOf(entry.getValue().size())));
			linePanel.add(Box.createHorizontalStrut(10));
			searchPanel.add(linePanel);
		}
		JScrollPane scrollPane = new JScrollPane(searchPanel);
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);

		return scrollPane;
	}
	
//**********************************************************************
	private void searchScripts() {
		swat.dk.traverseScripts(new Script.NodeTraverser(){
			public boolean traversing(Script s,Node n) {
				logOperator(n, verb,role,option,s);
				return true;
			}
		});
	}
//**********************************************************************
	private void logOperator(Node n, Verb v, Role.Link r, Role.Option o,Script s) {
		Operator tOperator = n.getOperator();
		if (tOperator != null){
			if (!(tOperator instanceof ParameterOperator)) {
				final boolean constant = Operator.OpType.Constant==tOperator.getOperatorType();
				final String name = constant ? n.getConstant().toString():tOperator.getLabel();
				Map<String,ArrayList<SearchEntry>> opMap;
				if (constant) {
					opMap = constantScripts.get(tOperator.getDataType());
					if (opMap==null) {
						opMap = new TreeMap<String,ArrayList<SearchEntry>>(); 
						constantScripts.put(tOperator.getDataType(), opMap);
					}
				} else
					opMap = operatorScripts;
				
				ArrayList<SearchEntry> scripts = opMap.get(name);
				
				if (scripts==null && !name.equals("Returned value")) {
					scripts = new ArrayList<SearchEntry>();
					opMap.put(name,scripts);
				}
				if (scripts!=null && (scripts.size()==0 || scripts.get(scripts.size()-1).s!=s)) 
					scripts.add(new SearchEntry(new ScriptPath(v,r,o),s));
			}
		} else 
			System.out.println("SearchLizard.logOperator(): null Operator");
	}
//**********************************************************************
	public void displayOperatorDetails(String tOperator,ArrayList<SearchEntry> scripts) {
		JDialog frame = new JDialog(myFrame,tOperator);
		DefaultListModel list = new DefaultListModel();
		for (SearchEntry s: scripts) 
			list.addElement(s);
		JList jlist = new JList(list);
		jlist.setCellRenderer(new DefaultListCellRenderer(){
			private static final long serialVersionUID = 1L;
			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected,cellHasFocus);
				((JComponent)c).setToolTipText(Utils.toHtmlTooltipFormat("Double-click to jump to this Script."));
				return c;
			}
		});
		jlist.addMouseListener(new OperatorDetailsMouseAdapter(jlist));

		JScrollPane operatorScrollPane =  new JScrollPane(jlist,
		                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
		                    JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		operatorScrollPane.getVerticalScrollBar().setUnitIncrement(20);
		frame.getContentPane().add(operatorScrollPane);
		frame.setSize(600, 300);
		frame.setLocation(50,400);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
		frame.validate();
	}
//**********************************************************************
	private class OperatorDetailsMouseAdapter extends MouseAdapter {
        JList list;
		public OperatorDetailsMouseAdapter(JList l){
			this.list = l;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				swat.setEditorInFocus(Swat.EditorEnum.VerbEditorHasFocus);
				int index = list.locationToIndex(e.getPoint());
				if (index!=-1) {
					SearchEntry s = ((SearchEntry)list.getModel().getElementAt(index));
					swat.showScript(s.sp,s.s);
				}
			}
		}
	}
	
	private static class SearchEntry {
		public ScriptPath sp;
		public Script s;
		public SearchEntry(ScriptPath sp,Script s){
			this.sp = sp;
			this.s = s;
		}
		public String toString(){
			return sp.getPath(s);
		}
	}
}
