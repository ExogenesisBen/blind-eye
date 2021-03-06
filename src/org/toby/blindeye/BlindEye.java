package org.toby.blindeye;

import KinectPV2.*;
import org.toby.blindeye.base.BaseLoader;
import org.toby.blindeye.bugs.BugLoader;
import org.toby.blindeye.features.FeatureLoader;
import processing.core.PApplet;
import processing.core.PImage;
import processing.sound.*;

import java.util.ArrayList;
import java.util.Random;

import static org.toby.blindeye.UtilitiesAndConstants.*;

public class BlindEye extends PApplet {

  private BaseLoader base;
  private FeatureLoader feature;
  private boolean currentlyFeaturing = false;
  private BugLoader bug;
  private boolean currentlyBugging = false;

  private TextOverlay textOverlay;
  private KinectPV2 kinect;
  private long timeBegin;
  private long timeOfLastFeature;

  private boolean someoneHere = false;
  private long timeOfLastSeen;
  private long timeSinceLastSeen;

  private PImage savedBackground;
  private PImage body;
  private Random rand;
  private PImage outputVideo;
  private SoundFile softFuzz;
  private long someoneFoundTime;
  private boolean loading = false;
  private boolean loaded;

  public static void main(String[] args) {
    PApplet.main("org.toby.blindeye.BlindEye");
  }

  public void settings() {
    fullScreen();
  }

  public void setup() {
    String background = "F:/SkyDrive/Work/Uni Work/Year 4/blind-eye/resources/bg.png";
    String vhsFont = "F:/SkyDrive/Work/Uni Work/Year 4/blind-eye/resources/vcr.ttf";

    savedBackground = loadImage(background);
    savedBackground.resize(MAIN_WIDTH, MAIN_HEIGHT);
    rand = new Random();
    base = new BaseLoader();
    feature = new FeatureLoader(this);
    bug = new BugLoader(this);
    textOverlay = new TextOverlay(this);
    timeBegin = System.currentTimeMillis();
    timeOfLastFeature = timeBegin;
    setUpKinect(this);
    setUpSounds();
    textFont(createFont(vhsFont, 48));
  }

  @SuppressWarnings("unchecked")
  public void draw() {
    background(0);
    noTint();
    long currentTime = System.currentTimeMillis() - timeBegin;
    body = Upscaler.upscaler(kinect.getBodyTrackImage().get(39, 32, KINECT_WIDTH, KINECT_HEIGHT), KINECT_WIDTH*KINECT_HEIGHT);
    body.filter(THRESHOLD);
    PImage liveVideo = kinect.getColorImage().get(LEFT_OFFSET, 0, MAIN_WIDTH, MAIN_HEIGHT);
    liveVideo.filter(GRAY);

    ArrayList<PImage> bodyList = kinect.getBodyTrackUser();
    bodyCounter(bodyList);

    boolean shouldBug = rand.nextInt(250) == 0;
    long timeSinceLastFeature = System.currentTimeMillis() - timeOfLastFeature;
    boolean toFeature = (timeSinceLastFeature > 18000 && rand.nextInt(250) == 0) || timeSinceLastFeature > 25000;

    if (timeSinceLastSeen > TIME_BEFORE_FADE + TIME_FADING || loading) {
      stopFuzz();
      image(new PImage(MAIN_WIDTH, MAIN_HEIGHT), LEFT_DISPLAY_OFFSET, 0);
      if (loading) {
        textOverlay.pauseScreen(currentTime, true);
      } else {
        textOverlay.pauseScreen(currentTime, false);
      }
    } else if (loaded) {
      image(bug.executeDownloadedStatic(outputVideo, body, savedBackground, kinect), LEFT_DISPLAY_OFFSET, 0);
    } else {
      startFuzz();
      if (toFeature || currentlyFeaturing || shouldBug || currentlyBugging) {
        if (toFeature || currentlyFeaturing) {
          //featuring
          outputVideo = feature.execute(liveVideo, body, savedBackground, kinect);
          currentlyFeaturing = feature.isCurrentlyFeaturing();
          timeOfLastFeature = System.currentTimeMillis();
        } else {
          outputVideo = liveVideo;
        }
        if (shouldBug || currentlyBugging) {
          //bugging
          outputVideo = bug.execute(outputVideo, body, savedBackground, kinect);
          currentlyBugging = bug.isCurrentlyBugging();
        }
      } else {
        //basing
        outputVideo = base.executeBase(liveVideo, body, savedBackground, kinect);
      }
      if (timeSinceLastSeen > TIME_BEFORE_FADE) {
        float op = floor(254-(((timeSinceLastSeen - TIME_BEFORE_FADE)/TIME_FADING)*255f));
        tint(128, op);
      }
      image(outputVideo, LEFT_DISPLAY_OFFSET, 0);
      textOverlay.info(currentTime, kinect);
    }
  }

  // ---------------------------------------------------------------------

  private void setUpSounds() {
    String softFuzzSound = "F:/SkyDrive/Work/Uni Work/Year 4/blind-eye/resources/audio/vhs.wav";
    softFuzz = new SoundFile(this, softFuzzSound);
    softFuzz.loop();
    softFuzz.amp(0.2f); //volume
 }

  private void startFuzz() {
    if (!softFuzz.isPlaying()) {
      softFuzz.loop();
    }
  }

  private void stopFuzz() {
    if (softFuzz.isPlaying()) {
      softFuzz.stop();
    }
  }

  private void setUpKinect(BlindEye blindEye) {
    kinect = new KinectPV2(blindEye);
    kinect.enableColorImg(true);
    kinect.enableBodyTrackImg(true);
    kinect.enableDepthImg(true);
    kinect.enableInfraredImg(true);
    kinect.init();
  }

  private void bodyCounter(ArrayList<PImage> bodyList) {
    if (bodyList.size() > 0 && (System.currentTimeMillis() - someoneFoundTime) < 1500) {
      //people tracking < 1 second
      loading = true;
      timeSinceLastSeen = 0;
      someoneHere = true;
    } else if (bodyList.size() > 0 && (System.currentTimeMillis() - someoneFoundTime) < 1700) {
      loaded = true;
      loading = false;
      timeSinceLastSeen = 0;
      someoneHere = true;
    } else if (bodyList.size() > 0) {
      //people tracking > 1.7 second
      loading = false;
      loaded = false;
    } else if (someoneHere) {
      loaded = false;
      //just lost people
      timeOfLastSeen = System.currentTimeMillis();
      timeSinceLastSeen = 0;
      someoneHere = false;
    } else {
      loaded = false;
      //no people
      timeSinceLastSeen = System.currentTimeMillis() - timeOfLastSeen;
      someoneFoundTime = System.currentTimeMillis();
    }
  }

  public void mousePressed() {
    if (mouseButton == RIGHT) {
      exit();
    }
  }

  public void keyPressed() {
    if (key == 32) {
      outputVideo.save("F:/SkyDrive/Work/Uni Work/Year 4/blind-eye/resources/bg.png");
      String background = "F:/SkyDrive/Work/Uni Work/Year 4/blind-eye/resources/bg.png";
      savedBackground = loadImage(background);
    }
  }
}