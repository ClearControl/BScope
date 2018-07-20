package xwing.deformablemirror.gui;

import clearcontrol.gui.jfx.custom.gridpane.CustomGridPane;
import xwing.deformablemirror.SendActuatorPositionsToMirrorInstruction;
import xwing.deformablemirror.SendActuatorPositionsToMirrorInstruction;


public class SendActuatorPositionsToMirrorInstructionPanel extends CustomGridPane {
    public SendActuatorPositionsToMirrorInstructionPanel(SendActuatorPositionsToMirrorInstruction pInstruction)
    {
        for(int i = 0; i<97;i++) {
            addDoubleField(pInstruction.getActuatorArray(i), i);
        }
    }
}
