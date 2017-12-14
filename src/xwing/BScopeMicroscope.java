package xwing;

import clearcl.ClearCLContext;
import clearcontrol.deformablemirrors.DeformableMirrorDevice;
import clearcontrol.devices.cameras.StackCameraDeviceInterface;
import clearcontrol.devices.cameras.devices.hamamatsu.HamStackCamera;
import clearcontrol.devices.lasers.devices.cobolt.CoboltLaserDevice;
import clearcontrol.devices.lasers.devices.omicron.OmicronLaserDevice;
import clearcontrol.devices.optomech.filterwheels.devices.fli.FLIFilterWheelDevice;
import clearcontrol.devices.signalamp.devices.srs.SIM900MainframeDevice;
import clearcontrol.devices.signalamp.devices.srs.SIM983ScalingAmplifierDevice;
import clearcontrol.devices.signalgen.devices.nirio.NIRIOSignalGenerator;
import clearcontrol.devices.stages.StageType;
import clearcontrol.devices.stages.devices.tst.TSTStageDevice;
import clearcontrol.devices.stages.hub.StageHubDevice;
import clearcontrol.microscope.lightsheet.component.detection.DetectionArm;
import clearcontrol.microscope.lightsheet.component.lightsheet.LightSheet;
import clearcontrol.microscope.lightsheet.component.opticalswitch.LightSheetOpticalSwitch;
import clearcontrol.microscope.lightsheet.signalgen.LightSheetSignalGeneratorDevice;
import clearcontrol.microscope.lightsheet.simulation.SimulatedLightSheetMicroscope;

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
    long lDefaultStackWidth = 1024;
    long lDefaultStackHeight = 2048;

    // Setting up lasers:
    {

      final OmicronLaserDevice lLaserDevice405 =
          new OmicronLaserDevice(1);
      addDevice(0, lLaserDevice405);

      final OmicronLaserDevice lLaserDevice488 =
          new OmicronLaserDevice(2);
      addDevice(0, lLaserDevice488);

      final OmicronLaserDevice lLaserDevice515 =
          new OmicronLaserDevice(3);
      addDevice(0, lLaserDevice515);

      final CoboltLaserDevice lLaserDevice561 =
          new CoboltLaserDevice("Jive",
                                100,
                                4);
      addDevice(1, lLaserDevice561);/**/

      final CoboltLaserDevice lLaserDevice594 =
          new CoboltLaserDevice("Mambo",
                                100,
                                5);
      addDevice(1, lLaserDevice594);/**/

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
        lDetectionArm.getPixelSizeInMicrometerVariable().set(0.26);

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

    // Setting up lightsheets selector
    {
      LightSheetOpticalSwitch lLightSheetOpticalSwitch =
                                                       new LightSheetOpticalSwitch("OpticalSwitch",
                                                                                   pNumberOfLightSheets);

      addDevice(0, lLightSheetOpticalSwitch);
    }

    // setup filter wheel
    {
      FLIFilterWheelDevice lFLIFilterWheelDevice = new FLIFilterWheelDevice(1);
      addDevice(0, lFLIFilterWheelDevice);
    }

    // setup deformable mirror
    {
      //AlpaoDMDevice lAlpaoDMDevice = new AlpaoDMDevice(1);
      //addDevice(0, lAlpaoDMDevice);
      DeformableMirrorDevice lDeformableMirrorDevice = new DeformableMirrorDevice(1);
      addDevice(0, lDeformableMirrorDevice);
    }
  }
}
