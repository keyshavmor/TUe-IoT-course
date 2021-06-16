package org.eclipse.leshan.client.demo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.sql.*;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.codec.json.LwM2mNodeJsonDecoder;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ParkingSpot extends BaseInstanceEnabler {

    boolean FLAG = false;
    boolean FLAGf = false;
    private static final Logger LOG = LoggerFactory.getLogger(ParkingSpot.class);
    private static final int PARKINGSPOTID = 32800;
    private static final int PARKINGSPOTSTATE = 32801;
    private static final int VEHICLEID = 32802;
    private static final int VEHICLENUMBERPLATE = 32804;
    private static final int BILLING = 32803;
    private static String ParkState = "Free";
    private  String PlateState = "Empty";
    private static final int SENSOR_VALUE = 5700;
    private static final int UNITS = 5701;
    private static final int MAX_MEASURED_VALUE = 5602;
    private static final int MIN_MEASURED_VALUE = 5601;
    private static final int RESET_MIN_MAX_MEASURED_VALUES = 5605;
    private static final List<Integer> supportedResources = Arrays.asList(SENSOR_VALUE, UNITS, MAX_MEASURED_VALUE,
            MIN_MEASURED_VALUE, RESET_MIN_MAX_MEASURED_VALUES);
    private final ScheduledExecutorService scheduler;
    public int parkspotid=12345;
    private static String ParkSpotID = "UNKNOWN";
    public ParkingSpot() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Temperature Sensor"));
        scheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
         	   if(FLAG)
        	   { try {
         				BufferedReader in = new BufferedReader(
         					   new InputStreamReader(
         			                      new FileInputStream("PIPE.txt"), "UTF8"));
         					        
         					String str;
         					      
         					while ((str = in.readLine()) != null) {
         						if(!str.equals("UNKNOWN"))
         						updatevehnum(str);
         						//FLAG=false;
         					}
         					in.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    try { String[] cmd={"sh","/home/pi/leshan/imagecapture.sh"};
            Runtime.getRuntime().exec(cmd);
            LOG.info("Will Scan Plate as Vehicle in sensed in the vicinity");
              } catch (IOException e) {
			   // TODO Auto-generated catch block
			e.printStackTrace();
	      }

	   
        	  }
         	if(FLAGf)
         	{
         		 String str2 = "UNKNOWN";
				    BufferedWriter writer;
					try {
						writer = new BufferedWriter(new FileWriter("PIPE.txt"));
						  writer.write(str2);
				
						  FLAGf =false;
						    writer.close();
					
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				  
         	}
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    @Override
    public synchronized ReadResponse read(int resourceId) {
        switch (resourceId) {
        case PARKINGSPOTID:
        	return ReadResponse.success(resourceId,"IoT-Pi121");
        case PARKINGSPOTSTATE:
        	return ReadResponse.success(resourceId,ParkState);
        case VEHICLEID:
        	
        	return ReadResponse.success(resourceId,ParkSpotID);
        case BILLING:
        	return ReadResponse.success(resourceId,0.1);
        case VEHICLENUMBERPLATE:
                      try { FileInputStream fis = new FileInputStream("/home/pi/leshan/matched.txt");
            Scanner in = new Scanner(fis);
            while(in.hasNext())
           {
             PlateState=in.nextLine();
             
           }
              } catch (IOException e) {
			   // TODO Auto-generated catch block
			e.printStackTrace();
	      }

         	
        	return ReadResponse.success(resourceId,PlateState);
        default:
            return super.read(resourceId);
        }
    }
    
    public WriteResponse write(int resourceid, LwM2mResource value) {
       switch (resourceid) {
       case PARKINGSPOTID:
       	return WriteResponse.success();
       case PARKINGSPOTSTATE:
    	   ParkState = value.toString();
    //      try { String[] cmd={"sh","/home/ti/leshan/imagecapture.sh"};
    //        Runtime.getRuntime().exec(cmd);
    //        LOG.info("Will Scan Plate as Vehicle in sensed in the vicinity");
    //          } catch (IOException e) {
			   // TODO Auto-generated catch block
//			e.printStackTrace();
//	      }
         		   
    	   if(value.getValue().equals("Reserve"))
    	   {       
                 
    		   FLAG = true;
    	   }
    	   if(value.getValue().equals("Free"))
    	   {
    		   
    		   FLAGf=true;
    		   FLAG = false;
    		   ParkSpotID = "UNKNOWN";
    	   }
           
    	   return WriteResponse.success();
       case VEHICLEID:
    	   ParkSpotID = value.toString();
    	   return WriteResponse.success();
       case BILLING:
    	   return WriteResponse.success();
       case VEHICLENUMBERPLATE:
           PlateState = value.toString();
    	   return WriteResponse.success();
        default:
            return super.write(resourceid, value);
        }
    }

    @Override
    public synchronized ExecuteResponse execute(int resourceId, String params) {
        switch (resourceId) {
        default:
            return super.execute(resourceId, params);
        }
    }
    
    public void updatevehnum(String newnum) {
    	ParkSpotID =newnum;
 	   ParkState = "Occupied";
 		fireResourcesChange(32801);
 	    
    	fireResourcesChange(32802);
    }



    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
}
