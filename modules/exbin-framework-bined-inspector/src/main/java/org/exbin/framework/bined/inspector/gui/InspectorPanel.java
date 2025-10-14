/*
 * Copyright (C) ExBin Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exbin.framework.bined.inspector.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.JComponent;
import org.exbin.bined.operation.command.BinaryDataUndoRedo;
import org.exbin.bined.swing.section.SectCodeArea;
import org.exbin.framework.language.api.LanguageModuleApi;
import org.exbin.framework.utils.WindowUtils;
import org.exbin.framework.App;
import org.exbin.framework.bined.inspector.BinedInspectorModule;
import org.exbin.framework.utils.TestApplication;
import org.exbin.framework.utils.UtilsModule;
import org.exbin.framework.bined.inspector.BinEdInspector;
import org.exbin.framework.bined.inspector.BinEdInspectorManager;
import org.exbin.framework.bined.inspector.BinEdInspectorProvider;
import org.exbin.framework.options.api.OptionsStorage;

/**
 * BinEd inspector right side panel.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class InspectorPanel extends javax.swing.JPanel {

    private final java.util.ResourceBundle resourceBundle = App.getModule(LanguageModuleApi.class).getBundle(InspectorPanel.class);
    private SectCodeArea codeArea;
    private BinaryDataUndoRedo undoRedo;
    private List<BinEdInspector> inspectors = new ArrayList<>();
    private BinEdInspector currentInspector = null;
    private JComponent currentComponent = null;

    public InspectorPanel() {
        initComponents();
        init();
    }

    private void init() {
        Dimension dimension = new java.awt.Dimension(250, 10);
        setMinimumSize(dimension);
        setPreferredSize(dimension);
        BinedInspectorModule binedInspectorModule = App.getModule(BinedInspectorModule.class);
        BinEdInspectorManager inspectorManager = binedInspectorModule.getBinEdInspectorManager();
        List<BinEdInspectorProvider> inspectorProviders = inspectorManager.getInspectorProviders();
        if (inspectorProviders.size() > 1) {
            for (BinEdInspectorProvider inspectorProvider : inspectorProviders) {
                inspectors.add(inspectorProvider.createInspector());
                inspectorComboBox.addItem(inspectorProvider.getName());
            }
            add(inspectorComboBox, BorderLayout.NORTH);
            inspectorComboBox.addItemListener((ItemEvent e) -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    pageChanged(inspectorComboBox.getSelectedIndex());
                }
            });
            pageChanged(inspectorComboBox.getSelectedIndex());
        } else if (inspectorProviders.size() == 1) {
            inspectors.add(inspectorProviders.get(0).createInspector());
            pageChanged(0);
        }
    }

    private void pageChanged(int inspectorIndex) {
        if (currentComponent != null) {
            remove(currentComponent);
            currentComponent = null;
        }
        if (currentInspector != null) {
            if (codeArea != null) {
                currentInspector.deactivateSync();
            }
            currentInspector = null;
        }
        if (!inspectors.isEmpty()) {
            currentInspector = inspectors.get(inspectorIndex);
            currentComponent = currentInspector.getComponent();
            add(currentComponent, BorderLayout.CENTER);
            if (codeArea != null) {
                currentInspector.setCodeArea(codeArea, undoRedo);
                currentInspector.activateSync();
            }
        }
        revalidate();
        repaint();
    }

    public void setCodeArea(SectCodeArea codeArea, @Nullable BinaryDataUndoRedo undoRedo) {
        this.codeArea = codeArea;
        this.undoRedo = undoRedo;

        if (currentInspector != null) {
            currentInspector.setCodeArea(codeArea, undoRedo);
            currentInspector.activateSync();
        }
    }

    public void activateSync() {
        if (currentInspector != null) {
            currentInspector.activateSync();
        }
    }

    public void deactivateSync() {
        if (currentInspector != null) {
            currentInspector.deactivateSync();
        }
    }

    public void onInitFromOptions(OptionsStorage options) {
        for (BinEdInspector inspector : inspectors) {
            inspector.onInitFromOptions(options);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        inspectorComboBox = new javax.swing.JComboBox<>();

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Test method for this panel.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        TestApplication testApplication = UtilsModule.createTestApplication();
        testApplication.launch(() -> {
            testApplication.addModule(org.exbin.framework.language.api.LanguageModuleApi.MODULE_ID, new org.exbin.framework.language.api.utils.TestLanguageModule());
            WindowUtils.invokeWindow(new InspectorPanel());
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> inspectorComboBox;
    // End of variables declaration//GEN-END:variables

    @Nullable
    public <T extends BinEdInspector> T getInspector(Class<T> clazz) {
        for (BinEdInspector inspector : inspectors) {
            if (clazz.isInstance(inspector)) {
                return clazz.cast(inspector);
            }
        }
        return null;
    }
}
