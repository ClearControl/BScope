package xwing.deformablemirror.gui;

import clearcontrol.gui.jfx.custom.gridpane.CustomGridPane;
import xwing.deformablemirror.SendZernikeModesToMirrorInstruction;

public class SendZernikeModesToMirrorInstructionPanel extends CustomGridPane {
    public SendZernikeModesToMirrorInstructionPanel(SendZernikeModesToMirrorInstruction pInstruction)
    {
        for(int i = 0; i<66;i++) {
            addDoubleField(pInstruction.getZernikeFactorArray(i), i);
        }
    }
}
