package org.deidentifier.arx.gui.view.impl.wizard.sharingwizard.evaluation;

import java.util.ArrayList;
import java.util.List;

import org.deidentifier.arx.gui.Controller;
import org.deidentifier.arx.gui.view.SWTUtil;
import org.deidentifier.arx.gui.view.impl.common.ComponentRiskMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import org.deidentifier.arx.gui.view.impl.wizard.sharingwizard.checklist.*;

/**
 * the monitor visualization
 *
 */
public class MonitorVisualization extends Visualization {
	private List<ComponentRiskMonitor> monitors;
	private ComponentRiskMonitor totalMonitor;
	
	public MonitorVisualization(Composite parent, Controller controller, Checklist checklist) {
		super(parent, controller, checklist);
	}
	
	protected void createVisualization() {
		Section sections[] = this.checklist.getSections();
		monitors = new ArrayList<ComponentRiskMonitor>();
		
		//Composite rootComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = SWTUtil.createGridLayoutWithEqualWidth(sections.length);
		layout.marginHeight = 0;
		layout.marginTop = 0;
		layout.marginBottom = 0;
//		layout.verticalSpacing = 0;
		layout.makeColumnsEqualWidth = true;
		this.setLayout(layout);
		
		for(int i = 0; i < sections.length; i++) {
			String title = sections[i].getTitle();

			ComponentRiskMonitor riskMonitor = new ComponentRiskMonitor(this, this.controller, title, title);
			riskMonitor.setLayoutData(SWTUtil.createFillGridData());
			riskMonitor.setRisk(0.5);
			monitors.add(riskMonitor);
		}

		totalMonitor = new ComponentRiskMonitor(this, this.controller, "Total", "Total");
		totalMonitor.setLayoutData(SWTUtil.createFillGridData());
		totalMonitor.setRisk(0.5);

		GridData gridData = new GridData();
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.verticalAlignment = SWT.FILL;
		gridData.minimumHeight  = 150;
		gridData.horizontalSpan = layout.numColumns;
		totalMonitor.setLayoutData(gridData);
	}
	
	public void updateWeights() {
		Section sections[] = checklist.getSections();
		for(int i = 0; i < sections.length; i++) {
			Section s = sections[i];

			ComponentRiskMonitor riskMonitor = monitors.get(i);
			riskMonitor.setRisk(1.0 - ((s.getScore() / 2.0) + 0.5));
			//				System.out.println("Setup");
		}

		totalMonitor.setRisk(1.0 - ((checklist.getScore() / 2.0) + 0.5));
	}
	
}
