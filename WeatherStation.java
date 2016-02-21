/*
 * Author: Jeff Pape <jeff.pape@gmail.com>
 * 
 * Copyright (c) 2016 Pape Software.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/**
 * @file
 * @ingroup grove 
 * @brief Display temperature on LCD
 *
 * This project template includes the starting code needed
 * to use the various sensors that come with the Grove Starter Kit.
 * Update the temperature values and reflect the changes on the LCD.
 * 
 * @hardware - 
 * - Grove Red LED (GroveLed) connected to the Grove Base Shield Port D4
 * - Grove Green LED (GroveLed) connected to the Grove Base Shield Port D3
 * - Grove Blue LED (GroveLed) connected to the Grove Base Shield Port D2
 * - Grove Button (GroveButton) connected to the Grove Base Shield Port D5
 * - Grove Temperature Sensor (GroveTemp) connected to the Grove Base Shield Port A0
 * - Grove Light Sensor (GroveLight) connected to the Grove Base Shield Port A1
 * - Grove LCD (Jhd1313m1) connected to any I2C on the Grove Base Shield
 *
 *  @req upm_i2clcd.jar
 *  @req upm_grove.jar
 *  @req mraa.jar
 *
 * @date 19/08/2015
 */

import upm_gas.Gas;
import upm_gas.MQ2;
import upm_gas.MQ3;
import upm_gas.MQ4;
import upm_gas.MQ5;
import upm_gas.MQ6;
import upm_gas.MQ7;
import upm_gas.MQ8;
import upm_gas.MQ9;
import upm_gas.TP401;
import upm_gas.thresholdContext;
import upm_grove.GroveButton;
import upm_grove.GroveLed;
import upm_grove.GroveLight;
import upm_grove.GroveTemp;
import upm_i2clcd.Jhd1313m1;
import mraa.mraa;
import mraa.Platform;

public class WeatherStation {
    static {
        System.loadLibrary("javaupm_gas");
		System.loadLibrary("mraajava");
        System.loadLibrary("javaupm_grove");
        System.loadLibrary("javaupm_i2clcd");
    }

    // minimum and maximum temperatures registered, the initial values will be
    // replaced after the first read
    static int min_temperature = Integer.MAX_VALUE;
    static int max_temperature = Integer.MIN_VALUE;


    /*
     * Update the temperature values and reflect the changes on the LCD
     * - change LCD backlight color based on the measured temperature,
     *   a cooler color for low temperatures, a warmer one for high temperatures
     * - display current temperature
     * - record and display MIN and MAX temperatures
     * - reset MIN and MAX values if the button is being pushed
     * - blink the led to show the temperature was measured and data updated
     */
    static void data_update(
    		GroveTemp temp,
    		GroveLight light,
    		TP401 gas,
    		GroveButton button, 
    		GroveLed red_led,
    		GroveLed green_led,
    		GroveLed blue_led,
            Jhd1313m1 lcd) {

        lcd.clear();

        // TEMPERATURE SENSOR
        // the temperature range in degrees Celsius,
        // adapt to your room temperature for a nicer effect!
        final int TEMPERATURE_RANGE_MIN_VAL = 18;
        final int TEMPERATURE_RANGE_MAX_VAL = 31;

        // other helper variables
        int temperature; // temperature sensor value in degrees Celsius
        float fade; // fade value [0.0 .. 1.0]
        short r, g, b; // resulting LCD backlight color components [0 .. 255]

        // update the min and max temperature values, reset them if the button is
        // being pushed
        temperature = temp.value();
        if (button.value() == 1) {
            min_temperature = temperature;
            max_temperature = temperature;
        } else {
            if (temperature < min_temperature) {
                min_temperature = temperature;
            }
            if (temperature > max_temperature) {
                max_temperature = temperature;
            }
        }

        // display the temperature values on the LCD
        lcd.setCursor(0,0);
        lcd.write(String.format("Temp %d    ", temperature));
        lcd.setCursor(1,0);
        lcd.write(String.format("Min %d Max %d    ", min_temperature, 
                max_temperature));

        // set the fade value depending on where we are in the temperature range
        if (temperature <= TEMPERATURE_RANGE_MIN_VAL) {
            fade = 0.0f;
        } else if (temperature >= TEMPERATURE_RANGE_MAX_VAL) {
            fade = 1.0f;
        } else {
            fade = (float)(temperature - TEMPERATURE_RANGE_MIN_VAL) /
                    (TEMPERATURE_RANGE_MAX_VAL - TEMPERATURE_RANGE_MIN_VAL);
        }

        // fade the color components separately
        r = (short)(255 * fade);
        g = (short)(64 * fade);
        b = (short)(255 * (1 - fade));

        // blink the led for 50 ms to show the temperature was actually sampled
        red_led.on();
        green_led.on();
        blue_led.on();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            System.err.println("Sleep interrupted: " + e.toString());
        }
        red_led.off();
        green_led.off();
        blue_led.off();

        // apply the calculated result
        lcd.setColor(r, g, b);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            System.err.println("Sleep interrupted: " + e.toString());
        }
        lcd.clear();
        
        // LIGHT SENSOR
        // in lumens
        int light_sensor_value = light.value();
        String light_senosr_data = Integer.toString(light_sensor_value);

        lcd.clear();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            System.err.println("Sleep interrupted: " + e.toString());
        }

        lcd.setCursor(0,0);
        lcd.write("light value ");
        lcd.setCursor(1,2);
        lcd.write("in lux: ");
        lcd.write(light_senosr_data);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            System.err.println("Sleep interrupted: " + e.toString());
        }
        lcd.clear();
        
        // AIR QUALITY SESNOR
        // air quality
        System.out.println(gas.name());
        System.out.println("Heating sensor for 3 minutes...");
        // wait 3 minutes for sensor to warm up
        for (int i = 0; i < 6; i++) {
        	System.out.println("Please wait, " + i * 30 + " seconds have passed..." );
            try {
//                Thread.sleep(30000);
              Thread.sleep(300);
            } catch (InterruptedException e) {
                System.err.println("Sleep interrupted: " + e.toString());
            }
        }
        System.out.println("Air sensor ready!");
        
        int air_value = gas.getSample();
        float air_ppm = gas.getPPM();
        String air_quality_value = air_quality(air_value);
        String air_numbers_value = new String("raw: " + air_value + " ppm: " + air_ppm);
        
        System.out.println(air_numbers_value + "  " + air_quality_value);
        // display the temperature values on the LCD
        lcd.setCursor(0,0);
        lcd.write(String.format("Air quality ", air_quality_value));
        lcd.setCursor(1,0);
        lcd.write(String.format(air_numbers_value));
       
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            System.err.println("Sleep interrupted: " + e.toString());
        }
        lcd.clear();
        
    }
    
    
    public static String air_quality(int value) {
    	if (value < 50) {
    		return "Fresh Air";
    	}
    	if (value < 200) {
    		return "Normal Indoor Air";
    	}
    	if (value < 400) {
    		return "Low Pollution";
    	}
    	if (value < 600) {
    		return "High Pollution - Action Recommended";
    	}
    	return "Very High Pollution - Take Action Immediately";
   
    }

    public static void main(String[] args) {
        // check that we are running on Galileo or Edison
        Platform platform = mraa.getPlatformType();
        if (platform != Platform.INTEL_GALILEO_GEN1 &&
                platform != Platform.INTEL_GALILEO_GEN2 &&
                platform != Platform.INTEL_EDISON_FAB_C) {
            System.err.println("Unsupported platform, exiting");
            return;
        }

        final int red_led_number = 4;
        final int green_led_number = 3;
        final int blue_led_number = 2;
        final int button_number = 5;
        final int temperature_sensor_number = 0;
        final int light_sensor_number = 1;
        final int gas_sensor_number = 2;
        
        // button connected to D4 (digital in)
        GroveButton button = new GroveButton(button_number);

        // red led connected to D5 (digital out)
        GroveLed red_led = new GroveLed(red_led_number);
        // green led connected to D5 (digital out)
        GroveLed green_led = new GroveLed(green_led_number);
        // blue led connected to D5 (digital out)
        GroveLed blue_led = new GroveLed(blue_led_number);

        // temperature sensor connected to A0 (analog in)
        GroveTemp temp = new GroveTemp(temperature_sensor_number);
        
        // light sensor connected to A1 (analog in)
        GroveLight light = new GroveLight(light_sensor_number);
        
        // gas sensor connected to A1 (analog in)
        TP401 gas = new TP401(gas_sensor_number);

        // LCD connected to the default I2C bus
        Jhd1313m1 lcd = new Jhd1313m1(0);

        // loop forever updating the temperature values every second
        while (true) {
            data_update(temp, light, gas, button, red_led, green_led, blue_led, lcd);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.err.println("Sleep interrupted: " + e.toString());
            }
        }

    }

}
