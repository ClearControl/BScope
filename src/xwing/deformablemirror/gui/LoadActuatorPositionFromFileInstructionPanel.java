package xwing.deformablemirror.gui;

import clearcontrol.core.log.LoggingFeature;
import clearcontrol.gui.jfx.custom.gridpane.CustomGridPane;
import clearcontrol.gui.jfx.var.file.VariableFileChooser;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.instructions.LoadMirrorModesFromFolderInstruction;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class LoadActuatorPositionFromFileInstructionPanel extends
        CustomGridPane
        implements LoggingFeature
{
    public LoadActuatorPositionFromFileInstructionPanel(LoadMirrorModesFromFolderInstruction pLoadMirrorModesFromFolderScheduler) {


        int lRow = 0;
        {
            this.add(new Label("This will reconfigure a deformable mirror using a matrix from the given folder every ... time points as configured."), 0, lRow);

            lRow++;
        }

        {
            VariableFileChooser lRootFolderChooser =
                    new VariableFileChooser("Folder:",
                            pLoadMirrorModesFromFolderScheduler.getRootFolderVariable(),
                            true);
            GridPane.setColumnSpan(lRootFolderChooser.getLabel(),
                    Integer.valueOf(1));
            GridPane.setColumnSpan(lRootFolderChooser.getTextField(),
                    Integer.valueOf(2));
            GridPane.setColumnSpan(lRootFolderChooser.getButton(),
                    Integer.valueOf(1));
            this.add(lRootFolderChooser.getLabel(), 0, lRow);
            this.add(lRootFolderChooser.getTextField(), 1, lRow);
            this.add(lRootFolderChooser.getButton(), 3, lRow);

            lRow++;
        }

    }
}