/*
Copyright (C) 2011 The University of Michigan

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Please send inquiries to powertutor@umich.edu
*/

package com.example.powertutor.phone;

import com.example.powertutor.components.LCD.LcdData;
import com.example.powertutor.components.CPU.CpuData;
import com.example.powertutor.components.Audio.AudioData;
import com.example.powertutor.components.GPS.GpsData;
import com.example.powertutor.components.Wifi.WifiData;
import com.example.powertutor.components.Threeg.ThreegData;

import android.content.Context;

/* Most of this file should be inheritted from DreamPowerCalculator as most of
 * the hardware model details will be the same modulo the coefficients.
 */
public class SapphirePowerCalculator extends DreamPowerCalculator {
  public SapphirePowerCalculator(Context context) {
    super(new SapphireConstants(context));
  }

  public SapphirePowerCalculator(PhoneConstants coeffs) {
    super(coeffs);
  }
}
