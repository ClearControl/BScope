package xwing;

import clearcl.ClearCLContext;
import clearcontrol.devices.cameras.StackCameraDeviceInterface;
import clearcontrol.devices.cameras.devices.hamamatsu.HamStackCamera;
import clearcontrol.devices.filterwheel.instructions.FilterWheelInstruction;
import clearcontrol.devices.lasers.LaserDeviceInterface;
import clearcontrol.devices.lasers.devices.cobolt.CoboltLaserDevice;
import clearcontrol.devices.lasers.devices.omicron.OmicronLaserDevice;
import clearcontrol.devices.lasers.instructions.LaserOnOffInstruction;
import clearcontrol.devices.lasers.instructions.LaserPowerInstruction;
import clearcontrol.devices.optomech.filterwheels.devices.fli.FLIFilterWheelDevice;
import clearcontrol.devices.optomech.filterwheels.devices.sim.FilterWheelDeviceSimulator;
import clearcontrol.devices.signalgen.devices.nirio.NIRIOSignalGenerator;
import clearcontrol.microscope.lightsheet.component.detection.DetectionArm;
import clearcontrol.microscope.lightsheet.component.lightsheet.LightSheet;
import clearcontrol.microscope.lightsheet.component.opticalswitch.LightSheetOpticalSwitch;
import clearcontrol.microscope.lightsheet.imaging.sequential.BeamAcquisitionInstruction;
import clearcontrol.microscope.lightsheet.imaging.singleview.WriteSingleLightSheetImageAsTifToDiscInstruction;
import clearcontrol.microscope.lightsheet.postprocessing.measurements.instructions.MeasureDCTS2DOnStackInstruction;
import clearcontrol.microscope.lightsheet.signalgen.LightSheetSignalGeneratorDevice;
import clearcontrol.microscope.lightsheet.simulation.LightSheetMicroscopeSimulationDevice;
import clearcontrol.microscope.lightsheet.simulation.SimulatedLightSheetMicroscope;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.instructions.*;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.optimizer.defocusdiversity.DefocusDiversityInstruction;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.optimizer.geneticalgorithm.instructions.GeneticAlgorithmMirrorModeOptimizeInstruction;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.optimizer.gradientbased.GradientBasedZernikeModeOptimizerInstruction;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.optimizer.sensorlessAO.SensorLessAOForSinglePlaneInstruction;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.slms.devices.alpao.AlpaoDMDevice;
import clearcontrol.microscope.lightsheet.spatialphasemodulation.slms.devices.sim.SpatialPhaseModulatorDeviceSimulator;
import clearcontrol.microscope.lightsheet.warehouse.containers.StackInterfaceContainer;
import xwing.deformablemirror.SendActuatorPositionsToMirrorInstruction;
import xwing.deformablemirror.SendZernikeModesToMirrorInstruction;
import xwing.optimizer.SingleModeDefocusBasedSensorlessAOInstruction;

/**
 * BScope microscope
 *
 * @author royer
 * @author haesleinhuepf
 */
public class BScopeMicroscope extends SimulatedLightSheetMicroscope
{

  /**
   * Instantiates an BScope microscope
   * 
   * @param pStackFusionContext
   *          ClearCL context for stack fusion
   * @param pMaxStackProcessingQueueLength
   *          max stack processing queue length
   * @param pThreadPoolSize
   *          thread pool size
   */
  public BScopeMicroscope(ClearCLContext pStackFusionContext,
                         int pMaxStackProcessingQueueLength,
                         int pThreadPoolSize)
  {
    super("BSCope",
          pStackFusionContext,
          pMaxStackProcessingQueueLength,
          pThreadPoolSize);

  }

  /**
   * Assembles the microscope
   * 
   * @param pNumberOfDetectionArms
   *          number of detection arms
   * @param pNumberOfLightSheets
   *          number of lightsheets
   */
  public void addRealHardwareDevices(int pNumberOfDetectionArms,
                                     int pNumberOfLightSheets)
  {
    long lDefaultStackWidth = 512;
    long lDefaultStackHeight = 512;

    // Setting up lasers:
    {

      //final OmicronLaserDevice lLaserDevice405 =
      //    new OmicronLaserDevice(1);
      //addDevice(0, lLaserDevice405);

      final OmicronLaserDevice lLaserDevice488 =
          new OmicronLaserDevice(2);
      addDevice(0, lLaserDevice488);

      //final OmicronLaserDevice lLaserDevice515 =
      //    new OmicronLaserDevice(3);
      //addDevice(0, lLaserDevice515);

      //final CoboltLaserDevice lLaserDevice561 =
      //    new CoboltLaserDevice("Jive",
      //                          100,
      //                          4);
      //addDevice(1, lLaserDevice561);/**/

      //final CoboltLaserDevice lLaserDevice594 =
      //    new CoboltLaserDevice("Mambo",
      //                          100,
      //                          5);
      //addDevice(1, lLaserDevice594);/**/

      LaserDeviceInterface[] laserList={lLaserDevice488};
              //{lLaserDevice405, lLaserDevice488, lLaserDevice515, lLaserDevice561, lLaserDevice594};

      for (LaserDeviceInterface laser : laserList) {
        addDevice(0, new LaserPowerInstruction(laser, 0.0));
        addDevice(0, new LaserPowerInstruction(laser, 1.0));
        addDevice(0, new LaserPowerInstruction(laser, 5.0));
        addDevice(0, new LaserPowerInstruction(laser, 10.0));
        addDevice(0, new LaserPowerInstruction(laser, 20.0));
        addDevice(0, new LaserPowerInstruction(laser, 50.0));
        addDevice(0, new LaserPowerInstruction(laser, 100.0));

        addDevice(0, new LaserOnOffInstruction(laser, true));
        addDevice(0, new LaserOnOffInstruction(laser, false));
      }

    }

    
    // Setting up cameras:
    if (true)
    {
      for (int c = 0; c < pNumberOfDetectionArms; c++)
      {
        StackCameraDeviceInterface<?> lCamera =
                                              HamStackCamera.buildWithExternalTriggering(c);

        lCamera.getStackWidthVariable().set(lDefaultStackWidth);
        lCamera.getStackHeightVariable().set(lDefaultStackHeight);
        lCamera.getExposureInSecondsVariable().set(0.010);

        // lCamera.getStackVariable().addSetListener((o,n)->
        // {System.out.println("camera output:"+n);} );

        addDevice(c, lCamera);
      }
    }
   
    // Adding signal Generator:
    LightSheetSignalGeneratorDevice lLSSignalGenerator;
    {
      NIRIOSignalGenerator lNIRIOSignalGenerator =
                                                 new NIRIOSignalGenerator();
      lLSSignalGenerator =
                         LightSheetSignalGeneratorDevice.wrap(lNIRIOSignalGenerator,
                                                              true);
      // addDevice(0, lNIRIOSignalGenerator);
      addDevice(0, lLSSignalGenerator);
    }

    // Setting up detection arms:
    {
      for (int c = 0; c < pNumberOfDetectionArms; c++)
      {
        final DetectionArm lDetectionArm = new DetectionArm("D" + c);
        lDetectionArm.getPixelSizeInMicrometerVariable().set(getDevice(StackCameraDeviceInterface.class, c).getPixelSizeInMicrometersVariable().get());

        addDevice(c, lDetectionArm);
      }
    }

    // Setting up lightsheets:
    {

      for (int l = 0; l < pNumberOfLightSheets; l++)
      {
        final LightSheet lLightSheet =
                                     new LightSheet("I" + l,
                                                    9.4,
                                                    getNumberOfLaserLines());
        addDevice(l, lLightSheet);
      }
    }

    // syncing exposure between cameras and lightsheets, as well as camera image
    // height:
    {
      for (int l = 0; l < pNumberOfLightSheets; l++)
        for (int c = 0; c < pNumberOfDetectionArms; c++)
        {
          StackCameraDeviceInterface<?> lCamera =
                                                getDevice(StackCameraDeviceInterface.class,
                                                          c);
          LightSheet lLightSheet = getDevice(LightSheet.class, l);

          lCamera.getExposureInSecondsVariable()
                 .sendUpdatesTo(lLightSheet.getEffectiveExposureInSecondsVariable());

          lCamera.getStackHeightVariable()
                 .sendUpdatesTo(lLightSheet.getImageHeightVariable());

        }
    }
    // Setting up Laser Beam Acquisition module
    {
      addDevice(0, new BeamAcquisitionInstruction(this,0));
    }
    // Setting up lightsheets selector
    {
      LightSheetOpticalSwitch lLightSheetOpticalSwitch =
                                                       new LightSheetOpticalSwitch("OpticalSwitch",
                                                                                   pNumberOfLightSheets);

      addDevice(0, lLightSheetOpticalSwitch);
    }
    // Setting up Tiff writer
    {
      addDevice(0, new WriteSingleLightSheetImageAsTifToDiscInstruction(0, 0, this));

    }

    // setup filter wheel
    {
      FLIFilterWheelDevice lFLIFilterWheelDevice = new FLIFilterWheelDevice(1);
      addDevice(0, lFLIFilterWheelDevice);
      for(int f:lFLIFilterWheelDevice.getValidPositions()) {
        addDevice(0, new FilterWheelInstruction(lFLIFilterWheelDevice, f));
      }
    }

    // setup deformable mirror
    {
      //AlpaoDMDevice lAlpaoDMDevice = new AlpaoDMDevice(1);
      //addDevice(0, lAlpaoDMDevice);
      AlpaoDMDevice
          lAlpaoMirror = new AlpaoDMDevice(1);
      addDevice(0, lAlpaoMirror);


      LoadMirrorModesFromFolderInstruction lMirrorModeScheduler =
          new LoadMirrorModesFromFolderInstruction(lAlpaoMirror, this);
      addDevice(0, lMirrorModeScheduler);

      RandomZernikesInstruction
              lRandomZernikesScheduler =
              new RandomZernikesInstruction(lAlpaoMirror);
      addDevice(0, lRandomZernikesScheduler);


//      SetZernikeModeInstruction
//              lSetZernikeModeInstruction =
//              new SetZernikeModeInstruction(lAlpaoMirror);
//      addDevice(0, lSetZernikeModeInstruction);

      SendActuatorPositionsToMirrorInstruction lSendActuatorPos =
              new SendActuatorPositionsToMirrorInstruction(lAlpaoMirror);
      addDevice(0,lSendActuatorPos);
      addDevice(0,new SendZernikeModesToMirrorInstruction(lAlpaoMirror));

      SequentialZernikesInstruction lSequentialZernikesScheduler =
              new SequentialZernikesInstruction(lAlpaoMirror);
      addDevice(0, lSequentialZernikesScheduler);


      addDevice(0, new LogMirrorZernikeFactorsToFileInstruction(lAlpaoMirror, this));
      addDevice(0, new GradientBasedZernikeModeOptimizerInstruction(this, lAlpaoMirror, 3));
      addDevice(0, new GradientBasedZernikeModeOptimizerInstruction(this, lAlpaoMirror, 4));
      addDevice(0, new GradientBasedZernikeModeOptimizerInstruction(this, lAlpaoMirror, 5));

      //addDevice( 0, new GeneticAlgorithmMlaseirrorModeOptimizeInstruction(lAlpaoMirror, this));


      DefocusDiversityInstruction lDefocusDiversityInstruction =
              new DefocusDiversityInstruction(this, 2,0,0);
      addDevice(0, lDefocusDiversityInstruction);
      addDevice(0, new SensorLessAOForSinglePlaneInstruction(this, lAlpaoMirror));
      addDevice(0,new
              SingleModeDefocusBasedSensorlessAOInstruction(this,lAlpaoMirror));

        addDevice(0,new
                RandomSingleZernikeModesInstruction(lAlpaoMirror));
    }

    //Measure Image Quality Scheduler
    addDevice(0, new MeasureDCTS2DOnStackInstruction<>(StackInterfaceContainer.class, this));

    System.out.println("DEVICES ADDED");
  }

  @Override
  public void addSimulatedDevices(boolean pDummySimulation,
                                                    boolean pXYZRStage,
                                                    boolean pSharedLightSheetControl,
                                                    LightSheetMicroscopeSimulationDevice pSimulatorDevice){
    super.addSimulatedDevices(pDummySimulation, pXYZRStage, pSharedLightSheetControl, pSimulatorDevice);


    // setup filter wheel
    {
      FilterWheelDeviceSimulator
          lFilterWheelDevice = new FilterWheelDeviceSimulator("Simulated Filter wheel", 4);
      addDevice(0, lFilterWheelDevice);
    }


    // setup deformable mirror
    {
      //AlpaoDMDevice lAlpaoDMDevice = new AlpaoDMDevice(1);
      //addDevice(0, lAlpaoDMDevice);
      SpatialPhaseModulatorDeviceSimulator lSpatialPhaseModulatorDeviceSimulator = new SpatialPhaseModulatorDeviceSimulator("Simulated Spatial Phase Modulator Device", 11, 1, 6);
      addDevice(0, lSpatialPhaseModulatorDeviceSimulator);


      LoadMirrorModesFromFolderInstruction
          lMirrorModeScheduler =
          new LoadMirrorModesFromFolderInstruction(lSpatialPhaseModulatorDeviceSimulator, this);
      addDevice(0, lMirrorModeScheduler);

      RandomZernikesInstruction
              lRandomZernikesScheduler =
              new RandomZernikesInstruction(lSpatialPhaseModulatorDeviceSimulator);
      addDevice(0, lRandomZernikesScheduler);

    }
  }

}
