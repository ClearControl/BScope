package xwing.deformablemirror;

import clearcontrol.core.log.LoggingFeature;
import clearcontrol.core.variable.bounded.BoundedVariable;
import clearcontrol.instructions.InstructionBase;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.slms.devices.alpao.AlpaoDMDevice;

public class SendActuatorPositionsToMirrorInstruction extends InstructionBase implements
        LoggingFeature {
    private AlpaoDMDevice
            mAlpaoMirror;
    private BoundedVariable<Double>[] mActuatorArray = new BoundedVariable[97];


    public SendActuatorPositionsToMirrorInstruction(AlpaoDMDevice pAlpaoMirror){
        super("Adaptive optics: Send actuator position to mirror " + pAlpaoMirror.getName());
        mAlpaoMirror = pAlpaoMirror;
        for(int i = 0; i < mActuatorArray.length; i++) {
            mActuatorArray[i] = new BoundedVariable<Double>("Actuator Position " +i,0.0,-1.0,1.0);
//            mActuatorArray[i].setMinMax(-1,1);
//            mActuatorArray[i].set(0.0);
        }
    }
    public void sendActuatorPositions(double positions[]){
        mAlpaoMirror.setActuatorPositions(positions);
    }
    public boolean initialize() {
        for(int i = 0; i < mActuatorArray.length; i++) {
            mActuatorArray[i].set(0.0);
        }
        return true;
    }
    @Override
    public boolean enqueue(long pTimePoint) {
        double[] lArray = new double[97];
        for(int i = 0; i < 97; i++) {
            lArray[i] = mActuatorArray[i].get();
        }
        sendActuatorPositions(lArray);
        return true;
    }
    @Override
    public SendActuatorPositionsToMirrorInstruction copy() {
        return new SendActuatorPositionsToMirrorInstruction(mAlpaoMirror);
    }

    public BoundedVariable<Double> getActuatorArray(int i){
        return mActuatorArray[i];
    }

}
