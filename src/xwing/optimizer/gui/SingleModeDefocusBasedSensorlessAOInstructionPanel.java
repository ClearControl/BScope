package xwing.optimizer.gui;

import clearcontrol.gui.jfx.custom.gridpane.CustomGridPane;
import xwing.optimizer.SingleModeDefocusBasedSensorlessAOInstruction;

public class SingleModeDefocusBasedSensorlessAOInstructionPanel extends CustomGridPane{
    public SingleModeDefocusBasedSensorlessAOInstructionPanel(SingleModeDefocusBasedSensorlessAOInstruction pInstruction){
        addDoubleField(pInstruction.getPositionZ(),0);
        addDoubleField(pInstruction.getstepSize(), 1);
        addIntegerField(pInstruction.getZernikeFator(),2);
        addDoubleField(pInstruction.getDefocusStepSize(),3);

    }
}
