/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.util;

import java.io.Serializable;

/**
 * Deedlit: we are using volatile variable types here. We dont need to additionally use synchronized, cause volatile vars are synced vars.
 */
public class Point3D implements Serializable
{
	private static final long serialVersionUID = 4638345252031872576L;
	
	private volatile int _x, _y, _z;
	
	public Point3D(int pX, int pY, int pZ)
	{
		_x = pX;
		_y = pY;
		_z = pZ;
	}
	
	@Override
	public String toString()
	{
		return "(" + _x + ", " + _y + ", " + _z + ")";
	}
	
	@Override
	public int hashCode()
	{
		return _x ^ _y ^ _z;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Point3D)
		{
			Point3D point3D = (Point3D) o;
			return (point3D._x == _x && point3D._y == _y && point3D._z == _z);
		}
		return false;
	}
	
	public boolean equals(int pX, int pY, int pZ)
	{
		return _x == pX && _y == pY && _z == pZ;
	}
	
	public int getX()
	{
		return _x;
	}
	
	public void setX(int pX)
	{
		_x = pX;
	}
	
	public int getY()
	{
		return _y;
	}
	
	public void setY(int pY)
	{
		_y = pY;
	}
	
	public int getZ()
	{
		return _z;
	}
	
	public void setZ(int pZ)
	{
		_z = pZ;
	}
	
	public void setXYZ(int pX, int pY, int pZ)
	{
		_x = pX;
		_y = pY;
		_z = pZ;
	}
}