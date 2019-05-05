package org.toby.blindeye.features.instances;

import KinectPV2.KinectPV2;
import org.toby.blindeye.Upscaler;
import org.toby.blindeye.features.AbstractFeature;
import processing.core.PImage;

import static org.toby.blindeye.UtilitiesAndConstants.KINECT_HEIGHT;
import static org.toby.blindeye.UtilitiesAndConstants.KINECT_WIDTH;

public class GreyStatic extends AbstractFeature {

  public PImage executeFeature(PImage liveVideo, PImage body, PImage savedBackground, KinectPV2 kinect) {
    return Upscaler.upscaler(kinect.getDepthImage(), KINECT_WIDTH*KINECT_HEIGHT);
  }

}
