
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import lejos.hardware.Brick;
import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.device.NXTCam;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.robotics.Color;
import lejos.robotics.SampleProvider;
import lejos.robotics.geometry.Rectangle2D;
import lejos.utility.Delay;
import lejos.utility.Timer;

public class Main {
	
	private static EV3LargeRegulatedMotor turningMotor;
	private static EV3MediumRegulatedMotor upMotor, jawMotor;
	
	private static EV3ColorSensor colourSensor;
	private static EV3TouchSensor startButton;
	private static NXTCam cam;
	private static int update_speed = 1000;
	
	private static Brick brick;
	private static Timer timer;
	private static Socket s;
	private static boolean rangPhone = false;
	
	private static final int PORT = 5678;
	private static final String IP = "10.0.1.12";
	
	private static NXTCam camera;
	final static int INTERVAL = 500; // milliseconds
	static String objects = "Objects: ";
	static int numObjects;
	
	static boolean found = false;
	
	private static int lastColour = 0;
	private static int currentColour = 0;
	private static int TURNING_LIMIT = 330;
	
	public static void main(String[] args) {
		
		brick = BrickFinder.getDefault();
		
		camera = new NXTCam(brick.getPort("S3"));
		
		camera.sendCommand('A'); // sort objects by size
		camera.sendCommand('E'); // start tracking
		
		upMotor =  new EV3MediumRegulatedMotor(brick.getPort("B"));
		upMotor.flt();
		upMotor.resetTachoCount();
		upMotor.setSpeed(7200);
		
		jawMotor =  new EV3MediumRegulatedMotor(brick.getPort("C"));
		//jawMotor.flt();
		jawMotor.resetTachoCount();
		jawMotor.setSpeed(7200);
		//jawMotor.rotateTo(-100, true);
		
		turningMotor =  new EV3LargeRegulatedMotor(brick.getPort("A"));
		turningMotor.flt();
		turningMotor.resetTachoCount();
		turningMotor.setSpeed(500); //7200
		
		colourSensor = new EV3ColorSensor(brick.getPort("S2"));
		colourSensor.setFloodlight(Color.BLUE);
		
		startButton = new EV3TouchSensor(brick.getPort("S4"));
		SampleProvider touchStart;
		
		touchStart = startButton.getTouchMode();
		
		float[] touchStartSample = new float[touchStart.sampleSize()];
		
		sockets();
		//sendColor(1);
		sayText("Hello, show me a ball");
		Sound.systemSound(true, Sound.ASCENDING);
		
		Delay.msDelay(1000);
		
		//turningMotor.rotateTo(360); // left
		//turningMotor.rotateTo(0);
		
		//turningMotor.rotateTo(360);
		//turningMotor.rotateTo(0);
		
		while (true)
		{
			showObjects();
			//moveJaw();
			
			if (Button.ESCAPE.isDown() )
			{
				try {
					s.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}

			
		}
		
		//upMotor.rotateTo(-7200); // down
		//upMotor.rotateTo(0); // up
	}
	
	static void sayText(String string)
	{
	    //byte[] b = string.getBytes();
	    byte[] b = string.getBytes(Charset.forName("UTF-8"));
	    
		try {
			s.getOutputStream().write(b);
			s.getOutputStream().flush();
			s.getOutputStream().write(0);
			s.getOutputStream().flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Delay.msDelay(1000);
	}
	
	static void showObjects()
	{
		LCD.clear();
		LCD.drawString(camera.getVendorID(), 0, 0);
		LCD.drawString(camera.getProductID(), 0, 1);
		LCD.drawString(camera.getVersion(), 9, 1);
		LCD.drawString(objects, 0, 2);
		LCD.drawInt(numObjects = camera.getNumberOfObjects(),1,9,2);
		
		if (numObjects >= 1 && numObjects <= 8) {
			for (int i=0;i<numObjects;i++) {
				Rectangle2D r = camera.getRectangle(i);
				if (r.getHeight() > 5 && r.getWidth() > 5) {
					
					LCD.drawInt(camera.getObjectColor(i), 3, 0, 3+i);
					LCD.drawInt((int) r.getWidth(), 3, 4, 3+i);
					LCD.drawInt((int) r.getHeight(), 3, 8, 3+i);
					
					System.out.println("C" + r.getCenterX() + " " + r.getCenterY() + " " + upMotor.getTachoCount());
					//Log.info("Simon");
					
					if (camera.getObjectColor(0) == 1)
					{
						colourSensor.setFloodlight(Color.RED);
						
						currentColour = 1;
						
					}
					else if (camera.getObjectColor(0) == 2)
					{
						colourSensor.setFloodlight(Color.BLUE);
						
						currentColour = 2;
					}
					
					if ( r.getCenterY() < 40 ) //upMotor.getTachoCount() > -7200  &&
					{
						upMotor.rotate(-360, true);
						//upMotor.backward();
						System.out.println("Backward");
						found = false;
					}
					else if (r.getCenterY() > 60) //upMotor.getTachoCount() < 0 &&
					{
						//upMotor.forward();
						upMotor.rotate(360 , true);
						System.out.println("Forward");
						found = false;
					}
					else
					{
						upMotor.stop();
						found = true;
					}
					
					if ( r.getCenterX() < 60 && turningMotor.getTachoCount() > -TURNING_LIMIT) //upMotor.getTachoCount() > -7200  &&
					{
						turningMotor.rotate(-36, true);
						//upMotor.backward();
						System.out.println("right");
						found = false;
					}
					else if (r.getCenterX() > 95  && turningMotor.getTachoCount() < TURNING_LIMIT) //upMotor.getTachoCount() < 0 &&
					{
						//upMotor.forward();
						turningMotor.rotate(36 , true);
						System.out.println("left");
						found = false;
					}
					else
					{
						turningMotor.stop();
					}
					
					if (found == true)
					{
						
						
						if (lastColour != 1 && currentColour == 1)
						{
							moveJaw();
							lastColour = 1;
							sayText("This is a red ball");
							moveJaw();

							
						}
						else if (lastColour != 2 && currentColour == 2)
						{
							moveJaw();
							lastColour = 2;
							sayText("This is a blue ball");
							moveJaw();
						}
					}
					
//					if (upMotor.getTachoCount() < 1 && upMotor.getTachoCount() > -7200)
//					{
//						if (r.getCenterY() < 40)
//						{
//							upMotor.backward();
//						}
//						else if (r.getCenterY() > 60)
//						{
//							upMotor.forward();
//						}
//						else
//						{
//							upMotor.stop();
//						}
//							
//					}
//					else
//					{
//						upMotor.stop();
//					}
				}
				else if (i == 0)
				{
					colourSensor.setFloodlight(Color.WHITE);
					
					upMotor.stop();
					currentColour = 0;
					lastColour = 0;
				}

			}
		}
		else
		{
			colourSensor.setFloodlight(Color.WHITE);
			currentColour = 0;
			lastColour = 0;
		}
		
		LCD.refresh();
		try {
			Thread.sleep(INTERVAL);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static void demo()
	{
		upMotor.rotateTo(-7200); // down
		upMotor.rotateTo(0); // up
		
		moveJaw();
		
		turningMotor.rotateTo(400);
		
		moveJaw();
		
		Delay.msDelay(1000);
		turningMotor.rotateTo(0);
		moveJaw();
	}
	
	static void moveJaw()
	{
		//Delay.msDelay(100);
		jawMotor.rotateTo(90,false); // Down
		jawMotor.rotateTo(-30, false);
	}
	
	static void sockets()
	{
		try {
			s = new Socket(IP,PORT);
			s.setTcpNoDelay(true);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
//	public static void sendColor(int colour)
//	{
//		lastColour = colour;
//		
//		try
//		   {
//			
//			s.getOutputStream().write(colour);
//			s.getOutputStream().flush();
//			
//			Delay.msDelay(1000);
//			
//			s.getOutputStream().write(0);
//			s.getOutputStream().flush();
//			 
//		   } catch (IOException e)
//		   {
//		       // TODO Auto-generated catch block
//			   e.printStackTrace();
//		   }
//	}
	
}
	




//		timer = new Timer(update_speed, new TimerListener() {
//			
//			@Override
//			public void timedOut() {
				// TODO Auto-generated method stub
				
//				if (touchPhoneSample[0] == 0 && rangPhone == true )
//				{
//					// slow steering down
//					turnSpeed = 5000;
//					speed = 100;
//					leftTrack.setPower(speed);
//					rightTrack.setPower(-speed);
//					carMotor.setSpeed(turnSpeed);
//				}
//				
//				if (speed > fire_speed && rangPhone == false)
//				{
//					rangPhone = true;
//					sendRing();
//				}
//				
//				if (speed < 100)
//				{
//					speed +=1;
//					leftTrack.setPower(speed);
//					rightTrack.setPower(-speed);
//				} else
//				{
//					turnSpeed -= 10;
//					carMotor.setSpeed(turnSpeed);
//				}
//					
//				
//				score += speed; 
//				
//				LCD.clear();
//				
//				if (touchPhoneSample[0] == 1)
//				{
//					LCD.drawString("Phone : down", 0, 7);
//				}
//				else
//				{
//					LCD.drawString("Phone: up" , 0, 7);
//				}
//				
//				LCD.drawString("Speed: " + speed, 0, 4);
//				LCD.drawString("Score: " + score, 0, 6);
//				LCD.drawString("High Score " + highScore, 0, 1);
//				 
//			}
//		});
		
//		ready();
		
//		while (true)
//		{
//			
//			if (Button.ESCAPE.isDown() )
//			{
//				try {
//					s.close();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				return;
//			}
//			
//			if (Button.DOWN.isDown() )
//			{
//				leftTrack.setPower(speed);
//				rightTrack.setPower(-speed);
//				
//				leftTrack.backward();
//				rightTrack.backward();
//				Delay.msDelay(1000);
//				
//				leftTrack.stop();
//				rightTrack.stop();
//			}
//			
//			if (steeringMotor.getTachoCount() > -90 && steeringMotor.getTachoCount() < 90)
//			{
//				carMotor.rotateTo(steeringMotor.getTachoCount() * rotationMultiplier, true);
//			}
//			
//			touchStart.fetchSample(touchStartSample, 0);
//			touchCrash.fetchSample(touchCrashSample, 0);
//			touchPhone.fetchSample(touchPhoneSample, 0);
//			
//			if (gameStarted)
//			{
//				if (touchCrashSample[0] == 1)
//				{
//					stop();
//				}
//			}
//			else
//			{
//				if (touchStartSample[0] == 1)
//				{
//					start();
//					timer.start();
//				}
//			}
//		}
//	
//	public static void sendRing()
//	{
//		try
//		   {
//			
//			s.getOutputStream().write(67);
//			s.getOutputStream().flush();
//			
//			Delay.msDelay(1000);
//			
//			s.getOutputStream().write(0);
//			s.getOutputStream().flush();
//			 
//		   } catch (IOException e)
//		   {
//		       // TODO Auto-generated catch block
//			   e.printStackTrace();
//		   }
//	}
	
//	public static void start()
//	{
//		LCD.clear();
//		Sound.systemSound(true, Sound.ASCENDING);
//		
//		rangPhone = false;
//		gameStarted = true;
//		score = 0;
//		speed = 60;
//		leftTrack.setPower(speed);
//		rightTrack.setPower(-speed);
//		turnSpeed = 7200;
//		carMotor.setSpeed(turnSpeed);
//		
//		leftTrack.forward();
//		rightTrack.forward();
//	}
//	
//	public static void ready()
//	{
//		LCD.clear();
//		LCD.drawString("Press start", 0, 4);
//		LCD.drawString("High Score " + highScore, 0, 1);
//		Sound.systemSound(true, Sound.DOUBLE_BEEP);
//	}
//
//	public static void stop()
//	{
//		leftTrack.stop();
//		rightTrack.stop();
//		timer.stop();
//		Sound.systemSound(true, Sound.DESCENDING);
//		LCD.clear();
//		
//		if (score > highScore)
//		{
//			highScore = score;
//			Sound.systemSound(true, Sound.BUZZ);
//			LCD.drawString("High Score " + highScore, 0, 1);
//		}
//		
//		gameStarted = false;
//		
//		LCD.drawString("Score " + score, 0, 4);
//		
//		
//		Delay.msDelay(10000);
//		
//		ready();
//	}
//}
