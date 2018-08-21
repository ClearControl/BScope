package xwing.deformablemirror;

import clearcontrol.core.log.LoggingFeature;
import clearcontrol.core.variable.Variable;
import clearcontrol.microscope.lightsheet.LightSheetMicroscope;
import clearcontrol.microscope.lightsheet.instructions.LightSheetMicroscopeInstructionBase;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.instructions.LoadMirrorModesFromFolderInstruction;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.io.DenseMatrix64FReader;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.slms.ZernikeModeFactorBasedSpatialPhaseModulatorBase;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.slms.devices.alpao.AlpaoDMDevice;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.zernike.TransformMatrices;
import org.ejml.data.DenseMatrix64F;

import java.io.File;

public class LoadActuatorPositionFromFileInstruction extends LightSheetMicroscopeInstructionBase implements
        LoggingFeature
{
    private Variable<File> mRootFolderVariable =
            new Variable("RootFolder",
                    (Object) null);

    private AlpaoDMDevice mAlpaoMirror;

    public LoadActuatorPositionFromFileInstruction(AlpaoDMDevice pAlpaoMirror, LightSheetMicroscope pLightSheetMicroscope) {
        super("Adaptive optics: Load actuator postion sequentially from folder and send to " + pAlpaoMirror.getName(), pLightSheetMicroscope);

        mAlpaoMirror = pAlpaoMirror;
    }



    public Variable<File> getRootFolderVariable()
    {
        return mRootFolderVariable;
    }

    private int mTimePointCount = 0;

    @Override public boolean initialize()
    {
        mTimePointCount = 0;
        return true;
    }

    @Override public boolean enqueue(long pTimePoint)
    {



        File lFolder = mRootFolderVariable.get();
        if (lFolder == null || !lFolder.isDirectory()) {
            warning("Error: given root folder is no directory");
            return false;
        }
        long lFileIndex = mTimePointCount;

        File lFile = lFolder.listFiles()[(int)lFileIndex];

        getLightSheetMicroscope().getTimelapse().log("Loading " + lFile);

        info("Loading " + lFile);
        DenseMatrix64F lMatrix = new DenseMatrix64FReader(lFile).getMatrix();

        double[] lArray = TransformMatrices.convertDense64MatrixTo1DDoubleArray(lMatrix);
        if(lArray.length != mAlpaoMirror.getNumberOfActuatorVariable().get()){
            System.out.println("Less than "+mAlpaoMirror.getNumberOfActuatorVariable().get()+" actuators");
            return true;
        }
        for(int i=0;i<lArray.length;i++){
            if(lArray[i]>1.0 || lArray[i]<-1.0){
                System.out.println("There is an error in actuator position" + i+1);
                return true;
            }
        }
        info("Sending matrix to mirror");

        mAlpaoMirror.setActuatorPositions(lArray);

        info("Sent. Scheduler done");

        mTimePointCount++;
        if(mTimePointCount >= lFolder.listFiles().length)
        {
            mTimePointCount = 0;
        }
        return true;
    }

    @Override
    public LoadActuatorPositionFromFileInstruction copy() {
        return new LoadActuatorPositionFromFileInstruction(mAlpaoMirror, getLightSheetMicroscope());
    }
}
