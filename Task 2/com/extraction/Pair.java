package com.extraction;

public class Pair {
	Integer x;
	Integer y;

	Pair() 
	{
		this.x=new Integer(0);
		this.y=new Integer(0);
	}

	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public int getX() {
		return x;
	}
	public void setY(int y) {
		this.y = y;
	}
}