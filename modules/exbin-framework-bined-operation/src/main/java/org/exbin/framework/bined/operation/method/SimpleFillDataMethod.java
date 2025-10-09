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
package org.exbin.framework.bined.operation.method;

import java.awt.Component;
import java.awt.Dialog;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.exbin.auxiliary.binary_data.BinaryData;
import org.exbin.auxiliary.binary_data.array.ByteArrayEditableData;
import org.exbin.auxiliary.binary_data.EditableBinaryData;
import org.exbin.bined.CodeAreaUtils;
import org.exbin.bined.EditOperation;
import org.exbin.bined.operation.swing.command.CodeAreaCommand;
import org.exbin.bined.swing.CodeAreaCore;
import org.exbin.framework.App;
import org.exbin.framework.bined.BinedModule;
import org.exbin.framework.bined.operation.method.gui.SimpleFillDataPanel;
import org.exbin.framework.bined.search.SearchCondition;
import org.exbin.framework.bined.search.gui.BinaryMultilinePanel;
import org.exbin.framework.window.api.WindowModuleApi;
import org.exbin.framework.language.api.LanguageModuleApi;
import org.exbin.framework.window.api.gui.DefaultControlPanel;
import org.exbin.framework.window.api.controller.DefaultControlController;
import org.exbin.framework.bined.operation.api.InsertDataMethod;
import org.exbin.framework.bined.operation.api.PreviewDataHandler;
import org.exbin.framework.bined.operation.command.InsertFromProviderCommand;
import org.exbin.framework.bined.operation.InsertFromProviderOperation;
import org.exbin.framework.bined.operation.ReplaceDataOperation;
import org.exbin.framework.bined.operation.InsertionDataProvider;
import org.exbin.framework.bined.operation.command.ReplaceDataCommand;
import org.exbin.framework.bined.operation.gui.BinaryPreviewPanel;
import org.exbin.framework.window.api.WindowHandler;

/**
 * Simple fill data method.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class SimpleFillDataMethod implements InsertDataMethod {

    private java.util.ResourceBundle resourceBundle = App.getModule(LanguageModuleApi.class).getBundle(SimpleFillDataPanel.class);

    private PreviewDataHandler previewDataHandler;
    private BinaryPreviewPanel previewPanel;
    private long previewLengthLimit = 0;

    @Nonnull
    @Override
    public String getName() {
        return resourceBundle.getString("method.name");
    }

    @Nonnull
    @Override
    public Component createComponent() {
        SimpleFillDataPanel component = new SimpleFillDataPanel();
        WindowModuleApi windowModule = App.getModule(WindowModuleApi.class);
        component.setSampleBinaryData(new ByteArrayEditableData());
        component.setController(() -> {
            BinedModule binedModule = App.getModule(BinedModule.class);
            final BinaryMultilinePanel multilinePanel = new BinaryMultilinePanel();
//            final WindowHandler dialog = windowModule.createWindow(dialogPanel, codeArea, "", Dialog.ModalityType.APPLICATION_MODAL);
            SearchCondition searchCondition = new SearchCondition();
            EditableBinaryData conditionData = new ByteArrayEditableData();
            EditableBinaryData sampleBinaryData = component.getSampleBinaryData();
            conditionData.insert(0, sampleBinaryData);
            searchCondition.setBinaryData(conditionData);
            searchCondition.setSearchMode(SearchCondition.SearchMode.BINARY);
            multilinePanel.setCondition(searchCondition);
            multilinePanel.setCodeAreaPopupMenuHandler(binedModule.createCodeAreaPopupMenuHandler(BinedModule.PopupMenuVariant.BASIC));
            DefaultControlPanel controlPanel = new DefaultControlPanel();
            JPanel dialogPanel = windowModule.createDialogPanel(multilinePanel, controlPanel);
            final WindowHandler multilineDialog = windowModule.createDialog(component, Dialog.ModalityType.APPLICATION_MODAL, dialogPanel);
            windowModule.setWindowTitle(multilineDialog, multilinePanel.getResourceBundle());
            controlPanel.setController((DefaultControlController.ControlActionType actionType) -> {
                if (actionType == DefaultControlController.ControlActionType.OK) {
                    SearchCondition condition = multilinePanel.getCondition();
                    sampleBinaryData.clear();
                    sampleBinaryData.insert(0, condition.getBinaryData());
                    component.setFillWith(FillWithType.SAMPLE);
                    long dataLength = component.getDataLength();
                    if (dataLength < sampleBinaryData.getDataSize()) {
                        component.setDataLength(sampleBinaryData.getDataSize());
                    }
                    component.setSampleBinaryData(sampleBinaryData);
                }

                multilineDialog.close();
                multilineDialog.dispose();
            });
            multilineDialog.showCentered(component);
            multilinePanel.detachMenu();
        });
        return component;
    }

    @Override
    public void initFocus(Component component) {
        ((SimpleFillDataPanel) component).initFocus();
    }

    @Nonnull
    @Override
    public CodeAreaCommand createInsertCommand(Component component, CodeAreaCore codeArea, long position, EditOperation editOperation) {
        SimpleFillDataPanel panel = (SimpleFillDataPanel) component;
        long length = panel.getDataLength();
        FillWithType fillWithType = panel.getFillWithType();

        InsertionDataProvider dataOperationDataProvider = (EditableBinaryData binaryData, long insertPosition) -> {
            generateData(binaryData, fillWithType, insertPosition, length, panel.getSampleBinaryData());
        };

        if (editOperation == EditOperation.OVERWRITE) {
            return new ReplaceDataCommand(codeArea, new ReplaceDataOperation(position, length, dataOperationDataProvider));
        } else {
            return new InsertFromProviderCommand(codeArea, new InsertFromProviderOperation(position, length, dataOperationDataProvider));
        }
    }

    public void generateData(EditableBinaryData binaryData, FillWithType fillWithType, long position, long length, BinaryData sampleBinaryData) throws IllegalStateException {
        switch (fillWithType) {
            case EMPTY: {
                for (long pos = position; pos < position + length; pos++) {
                    binaryData.setByte(pos, (byte) 0x0);
                }
                break;
            }
            case SPACE: {
                for (long pos = position; pos < position + length; pos++) {
                    binaryData.setByte(pos, (byte) 0x20);
                }
                break;
            }
            case SAMPLE: {
                if (sampleBinaryData.isEmpty()) {
                    for (long pos = position; pos < position + length; pos++) {
                        binaryData.setByte(pos, (byte) 0xff);
                    }
                } else {
                    long sampleDataSize = sampleBinaryData.getDataSize();
                    long pos = position;
                    long remain = length;
                    while (remain > 0) {
                        long segmentLength = Math.min(remain, sampleDataSize);
                        binaryData.replace(pos, sampleBinaryData, 0, segmentLength);
                        pos += segmentLength;
                        remain -= segmentLength;
                    }
                }

                break;
            }
            default:
                throw CodeAreaUtils.getInvalidTypeException(fillWithType);
        }
    }

    @Override
    public void requestPreview(PreviewDataHandler previewDataHandler, Component component, long lengthLimit) {
        this.previewDataHandler = previewDataHandler;
        this.previewLengthLimit = lengthLimit;
        SimpleFillDataPanel panel = (SimpleFillDataPanel) component;
        panel.setResultChangeListener(() -> {
            fillPreviewData(panel);
        });
        fillPreviewData(panel);
    }

    private void fillPreviewData(SimpleFillDataPanel panel) {
        previewPanel = new BinaryPreviewPanel();
        previewDataHandler.setPreviewComponent(previewPanel);
        SwingUtilities.invokeLater(() -> {
            FillWithType fillWithType = panel.getFillWithType();
            long dataLength = panel.getDataLength();
            if (dataLength > previewLengthLimit) {
                dataLength = previewLengthLimit;
            }
            EditableBinaryData sampleBinaryData = panel.getSampleBinaryData();

            EditableBinaryData previewBinaryData = new ByteArrayEditableData();
            previewBinaryData.insertUninitialized(0, dataLength);
            generateData(previewBinaryData, fillWithType, 0, dataLength, sampleBinaryData);
            previewPanel.setPreviewData(previewBinaryData);
        });
    }

    public enum FillWithType {
        EMPTY,
        SPACE,
        SAMPLE
    }
}
