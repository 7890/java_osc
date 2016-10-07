package com.illposed.osc;

import java.util.List;
import java.util.ArrayList;

import java.nio.ByteBuffer;

public class OSCTypedBlob
{
	protected char type='?';
	protected int count=0;
	protected byte[] data;

	protected List<Object> list;

	//reader: create java representation of typed blob
	//parseItmes() will return a List<Object>
	//cast like (List<Integer>)(Object)list;
	public OSCTypedBlob(char type,int count,byte[] data)
	{
		this.type=type;
		this.count=count;
		this.data=data;
	}

	//writer: set properties and list to be encoded as typed blob
	//write() will return byte[] array of given List<Object>
	public OSCTypedBlob(char type,List<Object> list)
	{
		this.type=type;
		this.count=list.size();
		this.list=list;
	}

	public char getType()
	{
		return type;
	}

	public int getCount()
	{
		return count;
	}

	public List<Object> parseItems()
	{
		parseByteArray();
		return list;
	}

	public byte[] write()
	{
		createByteArray();
		return data;
	}

	public void parseByteArray()
	{
		list=new ArrayList<Object>();
		if(data==null || data.length<1)
		{
			return;
		}

		//read big-endian
		ByteBuffer bb=java.nio.ByteBuffer.wrap(data);

		switch (type)
		{
			case 'i':
				for(int i=0;i<count;i++)
				{
					list.add(bb.getInt());
				}
				break;
			case 'h':
				for(int i=0;i<count;i++)
				{
					list.add(bb.getLong());
				}
				break;
			case 'f':
				for(int i=0;i<count;i++)
				{
					list.add(bb.getFloat());
				}
				break;

			case 'd':
				for(int i=0;i<count;i++)
				{
					list.add(bb.getDouble());
				}
				break;
			default:
				throw new IllegalArgumentException("unknown type for typed blob: "+type);
		}
	}//end parseByteArray()

	//implementing class should create data[] from List<Object>
	public void createByteArray()
	{
		if(list==null || list.size()<1)
		{
			data=null; //!!
			return;
		}

		if(type=='i')
		{
			data=new byte[count*4];
			List<Integer> int_list=(List<Integer>)(Object)list;

			for(int i=0;i<count;i++)
			{
				writeInteger32ToByteArray((int)int_list.get(i),data,i*4);
			}
		}
		else if(type=='h')
		{
			data=new byte[count*8];
			List<Long> long_list=(List<Long>)(Object)list;

			for(int i=0;i<count;i++)
			{
				writeInteger64ToByteArray((long)long_list.get(i),data,i*8);
			}
		}
		else if(type=='f')
		{
			data=new byte[count*4];
			List<Float> float_list=(List<Float>)(Object)list;
			for(int i=0;i<count;i++)
			{
				writeInteger32ToByteArray(
					(int)Float.floatToIntBits(
						float_list.get(i)
					),data,i*4
				);
			}
		}
		else if(type=='d')
		{
			data=new byte[count*8];
			List<Double> double_list=(List<Double>)(Object)list;
			for(int i=0;i<count;i++)
			{
				writeInteger64ToByteArray(
					(long)Double.doubleToRawLongBits(
						double_list.get(i)
					),data,i*8
				);
			}
		}
	}//end createByteArray()

	//from OSCJavaToByteArrayConverter

	/**
	 * Write a 32 bit integer to the byte array without allocating memory.
	 * @param value a 32 bit integer.
	 */
	private void writeInteger32ToByteArray(int value, byte[] arr, int offset) {
		//byte[] intBytes = new byte[4];
		//I allocated the this buffer globally so the GC has less work

		arr[offset+3] = (byte)value; value >>>= 8;
		arr[offset+2] = (byte)value; value >>>= 8;
		arr[offset+1] = (byte)value; value >>>= 8;
		arr[offset+0] = (byte)value;
	}

	/**
	 * Write a 64 bit integer to the byte array without allocating memory.
	 * @param value a 64 bit integer.
	 */
	private void writeInteger64ToByteArray(long value, byte[] arr, int offset) {
		arr[offset+7] = (byte)value; value >>>= 8;
		arr[offset+6] = (byte)value; value >>>= 8;
		arr[offset+5] = (byte)value; value >>>= 8;
		arr[offset+4] = (byte)value; value >>>= 8;
		arr[offset+3] = (byte)value; value >>>= 8;
		arr[offset+2] = (byte)value; value >>>= 8;
		arr[offset+1] = (byte)value; value >>>= 8;
		arr[offset+0] = (byte)value;
	}
}//end class OSCTypedBlob
//EOF
