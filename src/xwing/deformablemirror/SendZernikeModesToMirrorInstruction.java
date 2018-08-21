package xwing.deformablemirror;

import clearcontrol.core.log.LoggingFeature;
import clearcontrol.core.variable.bounded.BoundedVariable;
import clearcontrol.instructions.InstructionBase;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.slms.devices.alpao.AlpaoDMDevice;

public class SendZernikeModesToMirrorInstruction extends InstructionBase implements
        LoggingFeature {
    private AlpaoDMDevice
            mAlpaoMirror;
    private BoundedVariable<Double>[] mZernikeFactorsArray = new BoundedVariable[66];


    public SendZernikeModesToMirrorInstruction(AlpaoDMDevice pAlpaoMirror){
        super("Adaptive optics: Send Zernike Factors to mirror " + pAlpaoMirror.getName());
        mAlpaoMirror = pAlpaoMirror;
        for(int i = 0; i < mZernikeFactorsArray.length; i++) {
            mZernikeFactorsArray[i] = new BoundedVariable<Double>("Zernike Mode " +i,0.0,-10.0,10.0);
        }
    }
    public void sendZernikeMode(double pZernikes[]){
        mAlpaoMirror.setZernikeFactors(pZernikes);
    }
    public boolean initialize() {
//        for(int i = 0; i < mActuatorArray.length; i++) {
//            mActuatorArray[i].set(0.0);
//        }
        return true;
    }
    @Override
    public boolean enqueue(long pTimePoint) {
        double[] lArray = new double[mZernikeFactorsArray.length];
        for(int i = 0; i < mZernikeFactorsArray.length; i++) {
            lArray[i] = mZernikeFactorsArray[i].get();
        }
        sendZernikeMode(lArray);
        return true;
    }
    @Override
    public SendZernikeModesToMirrorInstruction copy() {
        return new SendZernikeModesToMirrorInstruction(mAlpaoMirror);
    }

    public BoundedVariable<Double> getZernikeFactorArray(int i){
        return mZernikeFactorsArray[i];
    }

}