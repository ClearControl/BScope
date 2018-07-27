package xwing.optimizer;

import clearcl.ClearCLImage;
import clearcl.imagej.ClearCLIJ;
import clearcl.imagej.kernels.Kernels;
import clearcontrol.core.log.LoggingFeature;
import clearcontrol.core.variable.bounded.BoundedVariable;
import clearcontrol.devices.imagej.ImageJFeature;
import clearcontrol.microscope.lightsheet.LightSheetMicroscope;
import clearcontrol.microscope.lightsheet.imaging.SingleViewPlaneImager;
import clearcontrol.microscope.lightsheet.imaging.singleview.WriteSingleLightSheetImageAsTifToDiscInstruction;
import clearcontrol.microscope.lightsheet.instructions.LightSheetMicroscopeInstructionBase;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.instructions.SequentialZernikesInstruction;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.slms.SpatialPhaseModulatorDeviceInterface;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.slms.ZernikeModeFactorBasedSpatialPhaseModulatorBase;
import clearcontrol.microscope.lightsheet.state.InterpolatedAcquisitionState;
import clearcontrol.microscope.state.AcquisitionStateManager;
import clearcontrol.stack.OffHeapPlanarStack;
import clearcontrol.stack.StackInterface;

import java.lang.reflect.Array;
import java.util.Arrays;

public class SingleModeDeocusBasedSensorlessAOInstruction extends LightSheetMicroscopeInstructionBase implements ImageJFeature, LoggingFeature {


    private final SpatialPhaseModulatorDeviceInterface mSpatialPhaseModulatorDeviceInterface;

    private BoundedVariable<Double> mPositionZ = new BoundedVariable<Double>("position Z",
            50.0,0.0,100.0);
    private BoundedVariable<Double> mStepSize = new BoundedVariable<Double>("Step size for Gradient Descent",
            0.25, 0.0, 2.0, 0.0000000001);
    private BoundedVariable<Integer> mZernikeFactor = new BoundedVariable<Integer>("Zernike Factor",
            3,0,66);


    private BoundedVariable<Double> mDefocusStepSize = new BoundedVariable<Double>("Defocus Step Size",
            0.1,0.0,2.0);


    private ClearCLIJ clij = ClearCLIJ.getInstance();

    private double[] mZernikes;
    private int mTileHeight = 0;
    private int mTileWidth = 0;

    WriteSingleLightSheetImageAsTifToDiscInstruction lWrite =  new WriteSingleLightSheetImageAsTifToDiscInstruction(
            0, 0, getLightSheetMicroscope());

    public SingleModeDeocusBasedSensorlessAOInstruction(LightSheetMicroscope pLightSheetMicroscope,
                                                 SpatialPhaseModulatorDeviceInterface pSpatialPhaseModulatorDeviceInterface)
    {
        super("Adaptive optics: Sensorless Single PLane AO optimizer for " +
                pSpatialPhaseModulatorDeviceInterface.getName(), pLightSheetMicroscope);
        this.mSpatialPhaseModulatorDeviceInterface = pSpatialPhaseModulatorDeviceInterface;
        mStepSize.set(0.25);


        mDefocusStepSize.set(0.1);

        mPositionZ.set(50.0);
    }

    @Override
    public boolean initialize() {
        //showImageJ();
        mZernikes = mSpatialPhaseModulatorDeviceInterface.getZernikeFactors();
        for(int i = 0; i< Array.getLength(mZernikes); i++){
            mZernikes[i] = 0;
        }
        mSpatialPhaseModulatorDeviceInterface.setZernikeFactors(mZernikes);
        return true;
    }

    @Override
    public boolean enqueue(long pTimePoint) {
        mZernikes = mSpatialPhaseModulatorDeviceInterface.getZernikeFactors();

        try {
            optimize();
        } catch (InterruptedException e) {
            System.out.println("Sleeping Error");
        }
        return false;
    }


    public boolean optimize() throws InterruptedException {

        mZernikes[mZernikeFactor.get()] = 0;
        //double[] lErrorMatrix = new double[];

        // Unchanged Zernike factor Imager
        mSpatialPhaseModulatorDeviceInterface.setZernikeFactors(mZernikes);
        Thread.sleep(mSpatialPhaseModulatorDeviceInterface.getRelaxationTimeInMilliseconds());
        double lDefaultQuality = determineQuality(mZernikes);


        // decrease Zernike factor by step size
        double[] zernikesFactorDecreased = new double[mZernikes.length];
        System.arraycopy(mZernikes, 0, zernikesFactorDecreased, 0, mZernikes.length);
        zernikesFactorDecreased[mZernikeFactor.get()] -= mStepSize.get();
        mSpatialPhaseModulatorDeviceInterface.setZernikeFactors(zernikesFactorDecreased);
        Thread.sleep(mSpatialPhaseModulatorDeviceInterface.getRelaxationTimeInMilliseconds());
        double lFactorDecreasedQuality = determineQuality(zernikesFactorDecreased);


        // increase Zernike factor by step size
        double[] zernikesFactorIncreased = new double[mZernikes.length];
        System.arraycopy(mZernikes, 0, zernikesFactorIncreased, 0, mZernikes.length);
        zernikesFactorIncreased[mZernikeFactor.get()] += mStepSize.get();
        mSpatialPhaseModulatorDeviceInterface.setZernikeFactors(zernikesFactorIncreased);
        Thread.sleep(mSpatialPhaseModulatorDeviceInterface.getRelaxationTimeInMilliseconds());
        double lFactorIncreasedQuality = determineQuality(zernikesFactorIncreased);




        System.out.println(" Negative coeff Quality: " + lFactorDecreasedQuality);
        System.out.println(" Zero Coeff Quality: " + lDefaultQuality);
        System.out.println(" Poitive coeff Quality: " + lFactorIncreasedQuality);


        // Setting the zernike factor back to 0
        mZernikes[mZernikeFactor.get()] = 0;
        mSpatialPhaseModulatorDeviceInterface.setZernikeFactors(mZernikes);

        return true;
    }
    private double determineQuality(double[] pZernikes) throws InterruptedException {

        int lDefocusZernikeFactor = 3;
        // Take positive defocus image
        double[] lzernikesFactorPoitiveDefocus = new double[pZernikes.length];
        System.arraycopy(pZernikes, 0, lzernikesFactorPoitiveDefocus, 0, mZernikes.length);
        lzernikesFactorPoitiveDefocus[lDefocusZernikeFactor] = mDefocusStepSize.get();
        mSpatialPhaseModulatorDeviceInterface.setZernikeFactors(lzernikesFactorPoitiveDefocus);
        Thread.sleep(mSpatialPhaseModulatorDeviceInterface.getRelaxationTimeInMilliseconds());
        StackInterface lPositiveDefocusStack = image();

        // Take negative defocus image
        double[] lzernikesFactorNegativeDefocus = new double[pZernikes.length];
        System.arraycopy(pZernikes, 0, lzernikesFactorNegativeDefocus, 0, mZernikes.length);
        lzernikesFactorNegativeDefocus[lDefocusZernikeFactor] = -mDefocusStepSize.get();
        mSpatialPhaseModulatorDeviceInterface.setZernikeFactors(lzernikesFactorNegativeDefocus);
        Thread.sleep(mSpatialPhaseModulatorDeviceInterface.getRelaxationTimeInMilliseconds());
        StackInterface lNegativeDefocusStack = image();

        double error = MSE((OffHeapPlanarStack) lPositiveDefocusStack, (OffHeapPlanarStack) lNegativeDefocusStack);


        // Setting the zernike factor back to 0
        mZernikes[lDefocusZernikeFactor] = 0;
        mSpatialPhaseModulatorDeviceInterface.setZernikeFactors(mZernikes);

        return error;
    }

    private StackInterface image() {
        InterpolatedAcquisitionState currentState = (InterpolatedAcquisitionState) getLightSheetMicroscope().
                getDevice(AcquisitionStateManager.class, 0).getCurrentState();
        SingleViewPlaneImager lImager = new SingleViewPlaneImager(getLightSheetMicroscope(), mPositionZ.get());
        lImager.setImageWidth(currentState.getImageWidthVariable().get().intValue());
        lImager.setImageHeight(currentState.getImageHeightVariable().get().intValue());
        lImager.setExposureTimeInSeconds(currentState.getExposureInSecondsVariable().get().doubleValue());
        lImager.setDetectionArmIndex(0);
        lImager.setLightSheetIndex(0);
        StackInterface lStack = lImager.acquire();
        //ClearCLIJ.getInstance().show(lStack, "acquired stack");
        return lStack;
    }



    // Checked
    private double MSE(OffHeapPlanarStack lStackPositiveDefocus, OffHeapPlanarStack lStackNegativeDefocus){
        if(lStackPositiveDefocus.getDepth()!=1 || lStackNegativeDefocus.getDepth()!=1){
            System.out.println("Depths aren't 1");
            return 100000000000.0;
        }
        if(lStackPositiveDefocus.getHeight() != lStackNegativeDefocus.getHeight()){
            System.out.println("Height arent the same ");
            return 100000000000.0;
        }
        if(lStackPositiveDefocus.getWidth() != lStackNegativeDefocus.getWidth()){
            System.out.println("Width arent the same ");
            return 100000000000.0;
        }
        double mse = 0.0;
        long lHeight = lStackPositiveDefocus.getHeight();
        long lWidth = lStackPositiveDefocus.getWidth();
        double[][][] lDoublePositiveDefocusArray = clij.converter(lStackPositiveDefocus).getDouble3DArray();
        double[][][] lDoubleNegativeDefocusArray = clij.converter(lStackNegativeDefocus).getDouble3DArray();

        for(long x = 0; x < lWidth; x++){
            for(long y = 0; y < lHeight; y++){
                mse += Math.pow((lDoubleNegativeDefocusArray[0][(int) y][(int) x] -lDoublePositiveDefocusArray[0][(int) y][(int) x]),2);
            }

        }
        mse = mse/(lHeight*lWidth);
        return  mse;
    }




    //Checked
    private static double[] CalcParabolaVertex(double x1, double y1, double x2, double y2, double x3, double y3)
    {

        double denom = (x1 - x2) * (x1 - x3) * (x2 - x3);
        double A     = (x3 * (y2 - y1) + x2 * (y1 - y3) + x1 * (y3 - y2)) / denom;
        double B     = (x3*x3 * (y1 - y2) + x2*x2 * (y3 - y1) + x1*x1 * (y2 - y3)) / denom;
        double C     = (x2 * x3 * (x2 - x3) * y1 + x3 * x1 * (x3 - x1) * y2 + x1 * x2 * (x1 - x2) * y3) / denom;

        double xv = -B / (2*A);
        double yv = C - B*B / (4*A);

        double[] result = {xv,yv};

        return result;
    }

    public BoundedVariable<Double> getstepSize(){
        return mStepSize;
    }
    public BoundedVariable<Double> getPositionZ(){ return mPositionZ; }

    @Override
    public xwing.optimizer.SingleModeDeocusBasedSensorlessAOInstruction copy() {
        return new xwing.optimizer.SingleModeDeocusBasedSensorlessAOInstruction(getLightSheetMicroscope(), mSpatialPhaseModulatorDeviceInterface);
    }

}

