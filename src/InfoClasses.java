import java.io.*;

abstract class Info
{
	abstract public byte writeInfo(OutputStream os);
	abstract public byte readInfo(InputStream is);
	
}

class InitInfo extends Info
{
	int width;
	int height;
	int nivel;
	
	public byte writeInfo(OutputStream os)
	{
		try
		{
			DataOutputStream out=new DataOutputStream(os);
			out.writeInt(width);
			out.writeInt(height);
			out.writeInt(nivel);
			out.flush();
			return 0;
		}
		catch(IOException ioe)
		{
			System.err.println(ioe);
			return 1;
		}
		
	}
	
	public byte readInfo(InputStream is)
	{
		try
		{
			DataInputStream in=new DataInputStream(is);
			this.width=in.readInt();
			this.height=in.readInt();
			this.nivel=in.readInt();
			return 0;

		}
		catch(IOException ioe)
		{
			System.err.println(ioe);
			return 1;
		}
	}
}

class GameInfo extends Info
{
	int bar_pos_x;
	int bar_pos_y;
	int ball_pos_x;
	int ball_pos_y;
	int sc_1;
	int sc_2;
	
	public byte writeInfo(OutputStream os)
	{
		try
		{
			DataOutputStream out=new DataOutputStream(os);
			out.writeInt(bar_pos_x);
			out.writeInt(bar_pos_y);
			out.writeInt(ball_pos_x);
			out.writeInt(ball_pos_y);
			out.writeInt(sc_1);
			out.writeInt(sc_2);
			out.flush();
			return 0;
		}
		catch(IOException ioe)
		{
			System.err.println(ioe);
			return 1;
		}
	}
	
	public byte readInfo(InputStream is)
	{
		try
		{
			DataInputStream in=new DataInputStream(is);
			//if(in.available()>0)
			//{
				this.bar_pos_x=in.readInt();
				this.bar_pos_y=in.readInt();
				this.ball_pos_x=in.readInt();
				this.ball_pos_y=in.readInt();
				this.sc_1=in.readInt();
				this.sc_2=in.readInt();
				return 0;
			//}
			//return 1;
		}
		catch(IOException ioe)
		{
			System.err.println(ioe);
			return 2;
		}
	}
	
}