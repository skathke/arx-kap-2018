/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2018 Fabian Prasser and contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.gui.view.impl.masking;

import java.util.List;
import java.util.Set;

import org.deidentifier.arx.gui.Controller;
import org.deidentifier.arx.gui.model.Model;
import org.deidentifier.arx.gui.model.ModelEvent;
import org.deidentifier.arx.gui.model.ModelEvent.ModelPart;
import org.deidentifier.arx.gui.resources.Resources;
import org.deidentifier.arx.gui.view.SWTUtil;
import org.deidentifier.arx.gui.view.def.IView;
import org.deidentifier.arx.gui.view.impl.common.ComponentMultiStack;
import org.deidentifier.arx.gui.view.impl.common.ComponentTitledFolder;
import org.deidentifier.arx.gui.view.impl.menu.DialogError;
import org.deidentifier.arx.masking.MaskingConfiguration;
import org.deidentifier.arx.masking.MaskingType;
import org.deidentifier.arx.masking.variable.RandomVariable;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

/**
 * View providing masking configuration.
 * 
 * @author Sandro Schaeffler
 * @author Peter Bock
 */
public class ViewMaskingConfiguration implements IView {

    /** Resource */
    private static final String[]      COMBO1_MASKINGTYPES = new String[] {
                                                                            Resources.getMessage("MaskingConfigurationView.10"),              //$NON-NLS-1$
                                                                            Resources.getMessage("MaskingConfigurationView.1"),               //$NON-NLS-1$
                                                                            Resources.getMessage("MaskingConfigurationView.2"),               //$NON-NLS-1$
                                                                            Resources.getMessage("MaskingConfigurationView.3"),               //$NON-NLS-1$
                                                                            Resources.getMessage("MaskingConfigurationView.4") };             //$NON-NLS-1$

    /** Resource */
    private static final MaskingType[] COMBO1_TYPES        = new MaskingType[] {
                                                                                 MaskingType.SUPPRESSED,
                                                                                 MaskingType.PSEUDONYMIZATION_MASKING,
                                                                                 MaskingType.NOISE_ADDITION_MASKING,
                                                                                 MaskingType.RANDOM_SHUFFLING_MASKING,
                                                                                 MaskingType.RANDOM_GENERATION_MASKING };

    /** Distributions */
    private static String[]            distributionItems;

    /** Model */
    private String                     attribute           = null;

    /** Widget */
    private final Combo                cmbDistribution;

    /** Widget */
    private final Combo                cmbMasking;

    /** Controller */
    private final Controller           controller;

    /** Model */
    private Model                      model;

    /** Widget. */
    private final ComponentMultiStack  stack;
    
    /** TextField for Pseudonymization */
    private final Text textField;

    /**
     * Creates an instance.
     * 
     * @param parent
     * @param controller
     */
    public ViewMaskingConfiguration(final Composite parent, final Controller controller) {

        this.controller = controller;

        // Title bar
        ComponentTitledFolder folder = new ComponentTitledFolder(parent, controller, null, null);
        folder.setLayoutData(SWTUtil.createFillGridData());

        // First tab
        Composite composite = folder.createItem(Resources.getMessage("MaskingView.2"), null); //$NON-NLS-1$
        composite.setLayout(SWTUtil.createGridLayout(1));
        folder.setSelection(0);

        // These events are triggered when data is imported or attribute configuration changes
        this.controller.addListener(ModelPart.INPUT, this); // TODO: Is this actually needed? Can data be imported with an attribute being set as identifying?
        this.controller.addListener(ModelPart.MODEL, this);
        this.controller.addListener(ModelPart.ATTRIBUTE_TYPE, this);
        this.controller.addListener(ModelPart.SELECTED_ATTRIBUTE, this);

        // Listens to whenever the list of distributions changes
        this.controller.addListener(ModelPart.RANDOM_VARIABLE, this);

        // Group
        Composite innerGroup = new Composite(composite, SWT.NULL);
        innerGroup.setLayoutData(SWTUtil.createFillHorizontallyGridData());
        final GridLayout typeInputGridLayout = new GridLayout();
        typeInputGridLayout.numColumns = 2;
        typeInputGridLayout.marginLeft = 0;
        typeInputGridLayout.marginRight = 0;
        typeInputGridLayout.marginWidth = 0;
        typeInputGridLayout.marginHeight = 0;
        innerGroup.setLayout(typeInputGridLayout);

        // Combo for Masking type
        final Label kLabel = new Label(innerGroup, SWT.PUSH);
        kLabel.setText(Resources.getMessage("MaskingConfigurationView.0")); //$NON-NLS-1$
        cmbMasking = new Combo(innerGroup, SWT.READ_ONLY);
        cmbMasking.setLayoutData(SWTUtil.createFillGridData());
        cmbMasking.setItems(COMBO1_MASKINGTYPES);
        
        
        cmbMasking.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                if ((cmbMasking.getSelectionIndex() != -1) && (attribute != null)) {
                    boolean identified = false;
                    Set<String> identifyingAttributes = controller.getModel().getInputDefinition().getIdentifyingAttributes();
                    for (String att : identifyingAttributes) {
                        if (att.equals(attribute)) {
                            MaskingType maskingType = COMBO1_TYPES[cmbMasking.getSelectionIndex()];
                            actionMaskingTypeChanged(attribute, maskingType, false);
                            refreshLayers(cmbMasking.getSelectionIndex());
                            identified = true;
                            cmbMasking.select(cmbMasking.getSelectionIndex());
                            cmbMasking.setEnabled(true);
                            break;
                        }

                    }
                    if (!identified)
                        cmbMasking.select(0);
                }
            }
        });

        // Create multistack
        stack = new ComponentMultiStack(innerGroup);

        // First column
        Composite first = stack.create(SWTUtil.createGridData());
        Composite compositeEmpty = new Composite(first, SWT.NONE);
        Composite compositeString = new Composite(first, SWT.NONE);
        GridLayout compositeLabelMinLayout = new GridLayout();
        compositeLabelMinLayout.numColumns = 1;
        compositeLabelMinLayout.marginLeft = 0;
        compositeLabelMinLayout.marginRight = 0;
        compositeLabelMinLayout.marginWidth = 0;
        compositeLabelMinLayout.marginHeight = 0;
        compositeString.setLayout(compositeLabelMinLayout);
        compositeEmpty.setLayout(compositeLabelMinLayout);
        Label labelStrLength = new Label(compositeString, SWT.PUSH);
        labelStrLength.setText(Resources.getMessage("MaskingConfigurationView.5")); //$NON-NLS-1$
        Composite compositeString2 = new Composite(first, SWT.NONE);
        compositeString2.setLayout(compositeLabelMinLayout);
        final Label labelProbDist = new Label(compositeString2, SWT.PUSH);
        labelProbDist.setText(Resources.getMessage("MaskingConfigurationView.6")); //$NON-NLS-1$

        // Second column
        Composite second = stack.create(SWTUtil.createFillHorizontallyGridData());
        Composite compositeEmpty2 = new Composite(second, SWT.NONE);
        compositeEmpty2.setLayout(compositeLabelMinLayout);
        Composite compositetf = new Composite(second, SWT.NONE);
        compositetf.setLayout(typeInputGridLayout);
        final int maxLength = 20;
        textField = new Text(compositetf, SWT.SINGLE | SWT.BORDER);
        if (MaskingConfiguration.getMaskingType(attribute) == MaskingType.PSEUDONYMIZATION_MASKING) {
        	textField.setText(MaskingConfiguration.getMapping().get(attribute).getStringLength() + "");
        	textField.setToolTipText(MaskingConfiguration.getMapping().get(attribute).getStringLength() + "");
        }
        else {
        	textField.setText("15"); //$NON-NLS-1$
            textField.setToolTipText("15"); //$NON-NLS-1$
        }
        textField.setLayoutData(SWTUtil.createFillHorizontallyGridData());
        textField.setEditable(false);
        // Button for updating
        Button btn1 = new Button(compositetf, SWT.PUSH);
        btn1.setText(Resources.getMessage("ViewPopulationModel.0")); //$NON-NLS-1$
        btn1.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {

                if (model == null || model.getInputConfig() == null || model.getInputConfig().getInput() == null)
                    return;
                String _value = controller.actionShowInputDialog(parent.getShell(), Resources.getMessage("MaskingConfigurationView.8"), //$NON-NLS-1$
                                                                 Resources.getMessage("MaskingConfigurationView.9") + maxLength, //$NON-NLS-1$
                                                                 textField.getToolTipText(), new IInputValidator() {
                                                                     @Override
                                                                     public String isValid(String arg0) {
                                                                         int value = 0;
                                                                         try {
                                                                             value = Integer.valueOf(arg0);
                                                                         } catch (Exception e) {
                                                                             return Resources.getMessage("ViewPopulationModel.11"); //$NON-NLS-1$
                                                                         }
                                                                         if (value <= maxLength && value >= 0) {
                                                                             return null;
                                                                         } else {
                                                                             return Resources.getMessage("ViewPopulationModel.12"); //$NON-NLS-1$
                                                                         }
                                                                     }
                                                                 });
                if (_value != null) {
                	MaskingConfiguration.getMapping().get(attribute).setStringLength(Integer.parseInt(_value, 10)); 
                    textField.setText(_value);
                    textField.setToolTipText(_value);
                }
            }
        });
        Composite compositecmb = new Composite(second, SWT.NONE);
        cmbDistribution = new Combo(compositecmb, SWT.READ_ONLY);
        compositecmb.setLayout(compositeLabelMinLayout);
        cmbDistribution.setLayoutData(SWTUtil.createFillGridData());
        cmbDistribution.setItems(new String[] { Resources.getMessage("MaskingConfigurationView.11") }); //$NON-NLS-1$ }
        cmbDistribution.select(0);
        cmbDistribution.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                if ((cmbDistribution.getSelectionIndex() != -1) && (attribute != null)) {
                    MaskingConfiguration.addDistribution(attribute, cmbDistribution.getSelectionIndex());                   
                }
                actionMaskingTypeChanged(attribute, MaskingConfiguration.getMaskingType(attribute), true);
            }
        });

        // Collect info about children in stack
        stack.pack();
        stack.setLayer(0);

    }

    /**
     * Masking type changed
     * 
     * @param attribute
     * @param maskingType
     * @param distrOnly
     */
    private void actionMaskingTypeChanged(String attribute, MaskingType maskingType, boolean distrOnly) {
        if (maskingType != null)
            MaskingConfiguration.addMasking(attribute, maskingType);
        // sets the ComboBox to the appropriate Distribution, only if set to RandomGeneration or NoiseAddition
        if (!distrOnly && (maskingType == MaskingType.RANDOM_GENERATION_MASKING || maskingType == MaskingType.NOISE_ADDITION_MASKING)) {
            int index = MaskingConfiguration.getDistributionIndex(attribute);
            if (index <= cmbDistribution.getItemCount() - 1)
                cmbDistribution.select(index);
            else {
                // removes Distributions that were deleted already
                MaskingConfiguration.removeMasking(attribute);
                cmbDistribution.select(0);
            }
        }
        if (maskingType == MaskingType.PSEUDONYMIZATION_MASKING) {
            if (MaskingConfiguration.getMaskingType(attribute) == MaskingType.PSEUDONYMIZATION_MASKING) {
            	textField.setText(MaskingConfiguration.getMapping().get(attribute).getStringLength() + "");
            	textField.setToolTipText(MaskingConfiguration.getMapping().get(attribute).getStringLength() + "");
            }
            else {
            	textField.setText("15"); //$NON-NLS-1$
                textField.setToolTipText("15"); //$NON-NLS-1$
            }
        }
        
        if (maskingType == MaskingType.NOISE_ADDITION_MASKING) {
        	if (cmbDistribution.getSelectionIndex() != 0) { //Not Identity
        		RandomVariable var = controller.getModel().getMaskingModel().getRandomVariables().get(cmbDistribution.getSelectionIndex() - 1);
        		controller.update(new ModelEvent(this, ModelPart.MASKING_CONFIGURATION, var));
        	} else {
        		controller.update(new ModelEvent(this, ModelPart.MASKING_CONFIGURATION, null));
        	}
        } else if (maskingType == MaskingType.RANDOM_GENERATION_MASKING){
        	if (cmbDistribution.getSelectionIndex() != 0) { //Not Identity
        		RandomVariable var = controller.getModel().getMaskingModel().getRandomVariables().get(cmbDistribution.getSelectionIndex() - 1);
        		controller.update(new ModelEvent(this, ModelPart.MASKING_CONFIGURATION, var));
        	} else {
        		controller.update(new ModelEvent(this, ModelPart.MASKING_CONFIGURATION, null));
        	}
        	
        } else if (maskingType == MaskingType.PSEUDONYMIZATION_MASKING){
        	try {
        		controller.update(new ModelEvent(this, ModelPart.MASKING_CONFIGURATION, Integer.parseInt(textField.getText())));
        	} catch (ParseException e){
        		DialogError d = new DialogError(controller.getResources().getShell(), controller, "Invalid string length", "Please enter an integer for the string length");
        	}
        } else if (maskingType == MaskingType.RANDOM_SHUFFLING_MASKING || maskingType == MaskingType.SUPPRESSED){
        	controller.update(new ModelEvent(this, ModelPart.MASKING_CONFIGURATION, null));
        }
    }
    
    

    /*
     * (non-Javadoc)
     * 
     * @see org.deidentifier.arx.gui.view.def.IView#dispose()
     */
    @Override
    public void dispose() {
        controller.removeListener(this);
    }

    /**
     * Refresh layers.
     * 
     * @param selection
     */
    private void refreshLayers(int selection) {
        if (selection == 0) {
            stack.setLayer(0);
        } else if (selection == 1) {
            stack.setLayer(1);
        } else if (selection == 2) {
            stack.setLayer(2);
        } else if (selection == 3) {
            stack.setLayer(0);
        } else if (selection == 4) {
            stack.setLayer(2);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.deidentifier.arx.gui.view.def.IView#reset()
     */
    @Override
    public void reset() {
        // Nothing to do
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.deidentifier.arx.gui.view.def.IView#update(org.deidentifier.arx.gui.model.ModelEvent)
     */
    @Override
    public void update(final ModelEvent event) {
        if (event.part == ModelPart.SELECTED_ATTRIBUTE) {
            if (model != null) {
                attribute = model.getSelectedAttribute();
                updateMaskingType();
            }
        } else if (event.part == ModelPart.ATTRIBUTE_TYPE) {
            updateMaskingType();
            if (controller.getModel().getInputDefinition().getIdentifyingAttributes().size() == 0) {
            	cmbMasking.setEnabled(false);
            	cmbMasking.select(0);
            	stack.setLayer(0);
            	actionMaskingTypeChanged(attribute, MaskingType.SUPPRESSED, false);
            	}
            else
            	cmbMasking.setEnabled(true);
            	if (!controller.getModel().getInputDefinition().getIdentifyingAttributes().contains("attribute")) {
            		cmbMasking.select(0);
                	stack.setLayer(0);
                	actionMaskingTypeChanged(attribute, MaskingType.SUPPRESSED, false);
            	}
            // TODO disable/hide masking configuration when no attribute is selected in the
            // attribute configuration table (currently selected attribute is not identifying)
        } else if (event.part == ModelPart.MODEL) {
            model = (Model) event.data;
            attribute = model.getSelectedAttribute();
        }
        // gets called whenever a distribution is added/deleted, refreshes ComboButton and selects appropriate distribution
        else if (event.part == ModelPart.RANDOM_VARIABLE) {
            List<RandomVariable> variables = controller.getModel().getMaskingModel().getRandomVariables();
            distributionItems = new String[variables.size() + 1];
            distributionItems[0] = Resources.getMessage("MaskingConfigurationView.11"); //$NON-NLS-1$
            for (int i = 1; i < variables.size() + 1; i++)
                distributionItems[i] = variables.get(i - 1).getName();
            cmbDistribution.setItems(distributionItems);
            cmbDistribution.select(MaskingConfiguration.getDistributionIndex(attribute));
        }
    }

    /**
     * Update masking type.
     */
    private void updateMaskingType() {
        if (model == null || model.getInputConfig() == null || model.getInputDefinition() == null) {
            reset();
            return;
        }
        MaskingType maskingType = MaskingConfiguration.getMaskingType(attribute);
        if (maskingType == MaskingType.PSEUDONYMIZATION_MASKING) { //Set String length for  pseudonymization masking
        	textField.setText(MaskingConfiguration.getMapping().get(attribute).getStringLength() + "");
        	textField.setToolTipText(MaskingConfiguration.getMapping().get(attribute).getStringLength() + "");
        }
        // sets the ComboBox to the appropriate Distribution, only if set to RandomGeneration or NoiseAddition
        if (maskingType == MaskingType.RANDOM_GENERATION_MASKING || maskingType == MaskingType.NOISE_ADDITION_MASKING) {
            int index = MaskingConfiguration.getDistributionIndex(attribute);
            if (index <= cmbDistribution.getItemCount() - 1)
                cmbDistribution.select(index);
            else {
                // removes Distributions that were deleted already
                MaskingConfiguration.removeMasking(attribute);
                cmbDistribution.select(0);
            }
        }
        for (int i = 0; i < COMBO1_TYPES.length; i++) {
            if (maskingType == COMBO1_TYPES[i]) {
                cmbMasking.select(i);
                refreshLayers(i);
                return;
            }
        }
        cmbMasking.select(0);
        stack.setLayer(0);
    }

}
