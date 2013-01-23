import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import java.util.*;
import java.io.*;
import javax.microedition.lcdui.game.*;
import javax.microedition.media.*;


public class nPong extends MIDlet implements CommandListener
{
	nPongGame pong;
	

		
	public nPong()
	{
		
		pong=new nPongGame(this,Display.getDisplay(this));
		
	}
	
	public void startApp() throws MIDletStateChangeException
	{
		Display.getDisplay(this).setCurrent(pong);
		try
		{
			Thread t = new Thread(pong);
    	    t.start();
    	}
    	catch (Error e) 
    	{
        	destroyApp(false);
        	notifyDestroyed();
	    }
    }
    
    public void pauseApp()
    {
    	
    }
    
    public void destroyApp(boolean unconditional) throws MIDletStateChangeException {	
		pong=null;
	    Display.getDisplay(this).setCurrent((Displayable)null);
    }
    
    public void commandAction(Command c, Displayable d)
     {
	     if (c == pong.cmdMenu)
	     {
		 	pong.menu();
	     }
	     else if(c==pong.cmdPausa)
	     {
	     	if(pong.estado==nPongGame.RUNNING || pong.estado==nPongGame.PAUSED)
	     		pong.pause();
	     }
     }
}

class nPongGame extends Canvas implements Runnable,PlayerListener
{
	public Display display;
	
	public Command cmdPausa  = new Command("Pausa", Command.CANCEL, 1);
	public Command cmdMenu  = new Command("Menu", Command.OK, 1);
	
	private byte key_map[];
	private int rotaciones[];
	
	public int estado;
	private int estado_ant;
	public int modo_mp;
	public nPong parent;
	
	public static final int STOP=0;
	public static final int RUNNING=1;
	public static final int PAUSED=2;
	public static final int MENU=3;
	
	public static final int BT_SERVER_WAIT=4;
	public static final int BT_CLIENT_WAIT=5;
	public static final int BT_SERVER_READY=6;
	public static final int BT_CLIENT_READY=7;
	
	public static final int RUNNING_MP=8;
	
	public static final int PLAYER_ONE=0;
	public static final int PLAYER_TWO=1;
		
	
	public int width;
	public int height;
	
	
	
	public nPongBall pelota;
	public nPongBar b1,b2;
	public int score_1;
	public int score_2;
	
	public int world_size;
	public int min_x;
	public int max_x;
	public int min_y;
	public int max_y;
	public int mitad_x;
	
	private int sc_width;
	private int sc_height;
	
	
	private int arriba;
	private int abajo;
	
	public boolean sonido;
	public int nivel;		//1-Facil 2-Medio 3-Dificil
	public boolean vibrar;
	public int rotacion;	//0-0 1-90 2-180 3-270
	
	public nPongMenu menu;
	
	public Random rnd;
	
	public Image buffer;
	private int buf_x;
	private int buf_y;
	
	private Vector sonidos;
	
	private Player snd1;
	private Player snd2;
	private Player snd3;
	
	private ClienteBT 	cliente;
	private ServidorBT 	servidor;
	
	private GameInfo info;
	
	public nPongGame(nPong parent, Display display)
	{
	    this.setCommandListener(parent);
	    systemMenu();
	    
		this.parent=parent;
		this.display=display;
		this.setFullScreenMode(true);
		score_1=0;
		score_2=0;
		world_size=5;
		
		width=getWidth();
		height=getHeight();
		
		mitad_x=(int)(width-world_size)/2;
		min_x=world_size;
		max_x=width-world_size;
		min_y=world_size;
		max_y=height-world_size;
		
		estado=MENU;
		modo_mp=PLAYER_ONE;
		
		sc_width=getWidth();
		sc_height=getHeight();
		
		buffer=Image.createImage(width,height);
		
		key_map=new byte[9];
		rnd=new Random();
		sonido=true;
		vibrar=true;
		nivel=1;
		rotaciones=new int[4];
		rotaciones[0]=Sprite.TRANS_NONE;
		rotaciones[1]=Sprite.TRANS_ROT90;
		rotaciones[2]=Sprite.TRANS_ROT180;
		rotaciones[3]=Sprite.TRANS_ROT270;
		rotacion=0;
		arriba=0;
		abajo=1;
		buf_x=(int)((getWidth()-width)/2);
		buf_y=(int)((getHeight()-height)/2);
		
		if(buf_x<0)
			buf_x=0;
		if(buf_y<0)
			buf_y=0;
			
		pelota=new nPongBall(this, mitad_x,(int)height/2,world_size);
		b1=new nPongBar(this, 10,(height-10)/2,world_size,30);
		b2=new nPongBar(this, width-world_size*3,(height-10)/2,world_size,30);		
		b1.comp=b2.comp=false;
		menu=new nPongMenu(this);
				

		sonidos=new Vector();
		
		try
		{
			snd1=Manager.createPlayer(getClass().getResourceAsStream("/sounds/pong1.wav"), "audio/x-wav");
			snd2=Manager.createPlayer(getClass().getResourceAsStream("/sounds/pong2.wav"), "audio/x-wav");
			snd3=Manager.createPlayer(getClass().getResourceAsStream("/sounds/pong3.wav"), "audio/x-wav");
		}
		catch (MediaException pe) {
			System.err.println("me " + pe);
		}
		catch (IOException ioe) {
			System.err.println("ioe" + ioe);
		}
		
	}
	
	public void set()
	{
		pelota.x=mitad_x;
		pelota.y=(int)height/2;
		pelota.rndDir();
		pelota.velocidad=nivel;
		
		b1.x=10;
		b2.x=width-world_size*3;
		if(b2.comp)
			b2.y=(height-10)/2;
		if(b1.comp)
			b1.y=(height-10)/2;
		b1.velocidad=b2.velocidad=nivel*2;
	}
	
	public int getEstado()
	{
		return estado;
	}
	
	public void start()
	{
		set();
		b1.comp=false;
		b2.comp=true;
		score_1=0;
		score_2=0;
		this.estado=RUNNING;
		systemMenu();
	}
	
	public void pause()
	{
		if(this.estado==PAUSED)
			this.estado=estado_ant;
		else
		{
			estado_ant=estado;
			estado=PAUSED;
		}
		systemMenu();
	}
	
	private void systemMenu()
	{
		this.removeCommand(cmdMenu);
		this.removeCommand(cmdPausa);
		
		this.addCommand(cmdMenu);
		if(estado==RUNNING)
		{
			cmdPausa= new Command("Pausa", Command.CANCEL, 2);
			this.addCommand(cmdPausa);
		}
		if(estado==PAUSED)
		{
			this.removeCommand(cmdPausa);
			cmdPausa= new Command("Continuar", Command.CANCEL, 2);
			this.addCommand(cmdPausa);
		}
		
	}
	
	public void stop()
	{
		this.estado=STOP;
	}
	
	public void menu()
	{
		if(this.estado==RUNNING_MP)
			conexionPerdida();
		this.estado=MENU;
		this.removeCommand(cmdPausa);
	}
	
	public void vibra(int ms)
	{
		if(vibrar)
			display.vibrate(ms);
	}
	
	public void sound(int num)
	{
		if(!sonido || num<0 || num>2 )
			return;
		try
		{
		    switch(num)
		    {
		    	case 0:
		    		snd1.start();
		    		break;
		    	case 1:
		    		snd2.start();
		    		break;
		    	case 2:
		    		snd3.start();
		    		break;
		    }	
		    	
		} catch (MediaException pe) {
			System.err.println("me " + pe);
		} /*catch (IOException ioe) {
			System.err.println("ioe" + ioe);
		}*/

	}
	
	public void playerUpdate(Player player, String event, Object eventData)
	{
		try
		{
			if(event.compareTo(PlayerListener.END_OF_MEDIA)==0)
			{
				player.stop();
				player.deallocate();
			}
		}
		catch(MediaException me)
		{
			System.err.println("me " + me);
		}

	}
	
	public void rotar()
	{
		rotacion++;
		if(rotacion>3)
			rotacion=0;
		switch(rotacion)
		{
			case 0:
				arriba=0;
				abajo=1;
				if(estado!=RUNNING_MP)
				{
					width=getWidth();
					height=getHeight();
					buf_x=(int)((getWidth()-width)/2);
					buf_y=(int)((getHeight()-height)/2);
				}
				break;
			case 1:
				arriba=3;	//Derecha
				abajo=2;	//Izquierda
				if(estado!=RUNNING_MP)
				{
					width=getHeight();
					height=getWidth();
					buf_x=(int)((getWidth()-height)/2);
					buf_y=(int)((getHeight()-width)/2);
				}	
				break;
			case 2:
				arriba=1;
				abajo=0;
				if(estado!=RUNNING_MP)
				{
					width=getWidth();
					height=getHeight();
					buf_x=(int)((getWidth()-width)/2);
					buf_y=(int)((getHeight()-height)/2);
				}
				break;
			case 3:
				arriba=2;
				abajo=3;
				if(estado!=RUNNING_MP)
				{
					width=getHeight();
					height=getWidth();
					buf_x=(int)((getWidth()-height)/2);
					buf_y=(int)((getHeight()-width)/2);
				}
				break;
		}	
			
		actualizar();
	}
	
	public void actualizar()
	{
		mitad_x=(int)(width-world_size)/2;
		min_x=world_size;
		max_x=width-world_size;
		min_y=world_size;
		max_y=height-world_size;
		buffer=Image.createImage(width,height);
		if(buf_x<0)
			buf_x=0;
		if(buf_y<0)
			buf_y=0;
	}
	
	public boolean initServer()
	{
		estado=BT_SERVER_WAIT;

		servidor=new ServidorBT(this);
		return true;
	}
	
	public boolean initClient()
	{
		estado=BT_CLIENT_WAIT;
		cliente=new ClienteBT(this);
		return true;
	}
	
	public void conexionLista()
	{
		if(estado==BT_SERVER_WAIT)
			estado=BT_SERVER_READY;
		else if(estado==BT_CLIENT_WAIT)
			estado=BT_CLIENT_READY;
		
		sincronizar();
	}
	
	public void conexionPerdida()
	{
		this.estado=MENU;
		if(servidor!=null)
			servidor.destroy();
		//if(cliente!=null)
		//	cliente.destroy();
		servidor=null;
		cliente=null;
	}
	
	public void sincronizar()
	{
		InitInfo init=new InitInfo();
		b1.comp=b2.comp=false;
		info=new GameInfo();
		//System.err.println("Sincronizando las quesadillas...");
		if(estado==BT_CLIENT_READY)
		{
			if(cliente.recibirInfo(init)==0)
			{
				this.width=init.width;
				this.height=init.height;
				this.nivel=init.nivel;
				//System.err.println("Cliente width=" + width + " height=" + height);
				actualizar();
				estado=RUNNING_MP;
				modo_mp=PLAYER_TWO;
			}
			else
			{
				conexionPerdida();
			}
		}
		else if(estado==BT_SERVER_READY)
		{
			init.width=this.width;
			init.height=this.height;
			init.nivel=this.nivel;
			//System.err.println("Servidor width=" + width + " height=" + height);
			if(servidor.enviarInfo(init)==0)
			{
				actualizar();
				estado=RUNNING_MP;
				modo_mp=PLAYER_ONE;
			}
			else
			{
				conexionPerdida();
			}
		}
	}
	
	public void run()
	{
		
		while(estado!=STOP)
		{

			try
			{
				if(estado==RUNNING)
				{
					if(!b1.comp)
					{
						if(key_map[arriba]==1)
							b1.move(-1);		//Arriba
						if(key_map[abajo]==1)	
							b1.move(1);		//Abajo
						if(key_map[8]==1)
						{
							rotar();
							key_map[8]=0;
						}
							
					}
					else
						b1.move(0);
						
					if(b2.comp)
						b2.move(0);						
					
					pelota.move();
				}
				else if(estado==RUNNING_MP)
				{
					if(modo_mp==PLAYER_ONE)
					{
						if(key_map[arriba]==1)
								b1.move(-1);		//Arriba
						if(key_map[abajo]==1)	
								b1.move(1);		//Abajo
						if(key_map[8]==1)
						{
							rotar();
							key_map[8]=0;
						}
						
						if(servidor!=null)
							switch(servidor.recibirInfo(info))
							{
								case 0:
									b2.x=info.bar_pos_x;
									b2.y=info.bar_pos_y;
									break;
								case 2:
									conexionPerdida();
									continue;
									
							}
						
						pelota.move();
						
						info.bar_pos_x=(int)b1.x;
						info.bar_pos_y=(int)b1.y;
						info.ball_pos_x=(int)pelota.x;
						info.ball_pos_y=(int)pelota.y;
						info.sc_1=score_1;
						info.sc_2=score_2;
						
						if(servidor!=null)
							if(servidor.enviarInfo(info)==1)
							{
								conexionPerdida();
								continue;
							}
	
					}
					else
					{
						if(key_map[arriba]==1)
								b2.move(-1);		//Arriba
						if(key_map[abajo]==1)	
								b2.move(1);		//Abajo
						if(key_map[8]==1)
						{
							rotar();
							key_map[8]=0;
						}
						
						info.bar_pos_x=(int)b2.x;
						info.bar_pos_y=(int)b2.y;
						
						if(cliente!=null)
							if(cliente.enviarInfo(info)==1)
							{
								conexionPerdida();
								continue;
							}
								
						
						if(cliente!=null)
							switch(cliente.recibirInfo(info))
							{
								case 0:
									b1.x=info.bar_pos_x;
									b1.y=info.bar_pos_y;
									pelota.x=info.ball_pos_x;
									pelota.y=info.ball_pos_y;
									score_1=info.sc_1;
									score_2=info.sc_2;
									break;
								case 2:
									conexionPerdida();
									break;
							}
					}
					
				}
				else if(estado==MENU)
				{
					if(key_map[0]==1)
					{
						menu.arriba();
						key_map[0]=0;
					}
					if(key_map[1]==1)
					{	
						menu.abajo();
						key_map[1]=0;
					}
					if(key_map[4]==1)
					{
						menu.seleccionar();
						key_map[4]=0;
					}
				}
				
				repaint();
				Thread.sleep(20);
			}
			catch(InterruptedException ie)
			{
			}
		}
	}
	
	public void paint(Graphics g)
	{
		if(g==null)
			return;
		if(estado==RUNNING || estado==RUNNING_MP)
		{
			Graphics gi=buffer.getGraphics();
			gi.setColor(0x000000);	
			gi.fillRect(0,0,width,height);
			marco(gi);
			scores(gi);
			pelota.paint(gi);
			b1.paint(gi);
			b2.paint(gi);
			g.setColor(0);
			g.fillRect(0,0,getWidth(),getHeight());
			g.drawRegion(buffer,0,0,width,height,rotaciones[rotacion],buf_x,buf_y,Graphics.TOP|Graphics.LEFT);
		}
		else if(estado==MENU)
		{
			menu.paint(g);
		}

	}
	
	private void marco(Graphics g)
	{
		g.setColor(0xFFFFFF);
		g.fillRect(0,0,width,world_size);
		g.fillRect(width-world_size,0,world_size,height);
		g.fillRect(0,height-world_size,width,world_size);
		g.fillRect(0,0,world_size,height);
		g.fillRect(mitad_x-3,0,world_size,height);
	}
	
	private void scores(Graphics g)
	{
		g.setColor(0xFFFFFF);
		g.drawString(score_1 + "", mitad_x-10, 10,Graphics.TOP|Graphics.RIGHT);
		g.drawString(score_2 + "", mitad_x+10, 10,Graphics.TOP|Graphics.LEFT);
	}
	
	protected void keyPressed(int keyCode)
	{
		int key=getGameAction(keyCode);

		if(key==Canvas.UP)
				key_map[0]=1;
		else if(key==Canvas.DOWN)
				key_map[1]=1;
		else if(key==Canvas.LEFT)
				key_map[2]=1;
		else if(key==Canvas.RIGHT)
				key_map[3]=1;
		else if(key==Canvas.FIRE)
				key_map[4]=1;		
		else if(key==Canvas.GAME_A)
				key_map[5]=1;
		else if(key==Canvas.GAME_B)
				key_map[6]=1;
		else if(key==Canvas.GAME_C)
				key_map[7]=1;
		else if(key==Canvas.GAME_D)
				key_map[8]=1;
	}
	
	protected void keyReleased(int keyCode)
	{
		int key=getGameAction(keyCode);

		if(key==Canvas.UP)
				key_map[0]=0;
		else if(key==Canvas.DOWN)
				key_map[1]=0;
		else if(key==Canvas.LEFT)
				key_map[2]=0;
		else if(key==Canvas.RIGHT)
				key_map[3]=0;
		else if(key==Canvas.FIRE)
				key_map[4]=0;		
		else if(key==Canvas.GAME_A)
				key_map[5]=0;
		else if(key==Canvas.GAME_B)
				key_map[6]=0;
		else if(key==Canvas.GAME_C)
				key_map[7]=0;
		else if(key==Canvas.GAME_D)
				key_map[8]=0;

	}
	
	
	public void destroy()
	{
		this.estado=STOP;
		pelota=null;
		b1=null;
		b2=null;
		menu=null;
		try
		{
			parent.destroyApp(false);
		    parent.notifyDestroyed();
		}
		catch(javax.microedition.midlet.MIDletStateChangeException me)
		{
			System.err.println(me);
		}
	}
}

class nPongMenu
{
	nPongGame parent;
	Image logo;
	Vector items_menu;
	int item_actual;
	int logo_x;
	int logo_y;
	int menu_x;
	int menu_y;
	
	public nPongMenu(nPongGame parent)
	{
		try
		{
			this.parent=parent;
			logo=Image.createImage("/images/logo.png");
			items_menu=new Vector();
			items_menu.addElement("Iniciar");
			items_menu.addElement("Multijugador - Esperar");
			items_menu.addElement("Multijugador - Buscar");
			items_menu.addElement("Sonido - ON");
			items_menu.addElement("Vibración - ON");
			items_menu.addElement("Nivel - Fácil");
			items_menu.addElement("Rotación - 0°");
			items_menu.addElement("Demo");
			items_menu.addElement("Salir");
			item_actual=0;
			
			logo_x=(int)(parent.getWidth()-logo.getWidth())/2;
			logo_y=(int)(parent.getHeight()-logo.getHeight())/2;
			menu_x=(int)(parent.getWidth()/2);
			menu_y=parent.getHeight()-50;
		}
		catch(java.io.IOException ioe)
		{
			System.err.println(ioe);
		}
		catch(Error e)
		{
			System.err.println(e);
		}
	
	}
	
	public void paint(Graphics g)
	{
		g.setColor(0xFFFFFF);
		g.fillRect(0,0,parent.getWidth(),parent.getHeight());
		g.drawImage(logo,logo_x,logo_y,Graphics.TOP|Graphics.LEFT);
		g.setColor(0x000000);
		
		g.drawLine(menu_x,menu_y-10,menu_x-5,menu_y-5);
		g.drawLine(menu_x-5,menu_y-5,menu_x+5,menu_y-5);
		g.drawLine(menu_x,menu_y-10,menu_x+5,menu_y-5);
		
		g.drawString((String)items_menu.elementAt(item_actual),menu_x,menu_y,Graphics.TOP|Graphics.HCENTER);
		
		g.drawLine(menu_x,menu_y+25,menu_x-5,menu_y+20);
		g.drawLine(menu_x-5,menu_y+20,menu_x+5,menu_y+20);
		g.drawLine(menu_x,menu_y+25,menu_x+5,menu_y+20);

	}
	
	public void arriba()
	{
		item_actual--;
		if(item_actual<0)
			item_actual=items_menu.size()-1;
	}
	
	public void abajo()
	{
		item_actual++;
		if(item_actual>=items_menu.size())
			item_actual=0;
	}
	
	public void seleccionar()
	{
		switch(item_actual)
		{
			case 0:
				parent.b1.comp=false;
				parent.b2.comp=true;
				parent.start();
				break;
			case 1:
				parent.initServer();
				break;
			case 2:
				parent.initClient();
				break;
			case 3:
				parent.sonido^=true;
				if(parent.sonido)
					items_menu.setElementAt("Sonido - ON",3);
				else
					items_menu.setElementAt("Sonido - OFF",3);
				break;
			case 4:
				parent.vibrar^=true;
				if(parent.vibrar)
					items_menu.setElementAt("Vibración - ON",4);
				else
					items_menu.setElementAt("Vibración - OFF",4);
				break;

			case 5:
				parent.nivel++;
				if(parent.nivel>3)
					parent.nivel=1;
				if(parent.nivel==1)
					items_menu.setElementAt("Nivel - Fácil",5);
				else if(parent.nivel==2)
					items_menu.setElementAt("Nivel - Medio",5);
				else if(parent.nivel==3)
					items_menu.setElementAt("Nivel - Difícil",5);
					
				break;
			case 6:
				parent.rotar();
				if(parent.rotacion==0)
					items_menu.setElementAt("Rotación - 0°",6);
				else if(parent.rotacion==1)
					items_menu.setElementAt("Rotación - 90°",6);
				else if(parent.rotacion==2)
					items_menu.setElementAt("Rotación - 180°",6);
				else if(parent.rotacion==3)
					items_menu.setElementAt("Rotación - 270°",6);
				break;
			case 7:
				parent.b1.comp=true;
				parent.b2.comp=true;
				parent.start();
				break;
			case 8:
				parent.destroy();
				break;
		}
	}
	
	
}

class nPongBall
{
	public double x;
	public double y;
	public double dx;
	public double dy;
	public int size;
	public double velocidad;
	private nPongGame parent;
	public boolean comp;
	public double inc;

	public nPongBall(nPongGame parent, int x, int y, int size)
	{
		this.x=x;
		this.y=y;
		this.size=size;
		this.parent=parent;
		this.velocidad=3;
		this.inc=0.01;
		rndDir();
		
	}
	
	public void rndDir()
	{
		dx=(parent.rnd.nextFloat()*2+1)*((parent.rnd.nextInt(2)==1)?1:-1);
		dy=parent.rnd.nextFloat()*2*((parent.rnd.nextInt(2)==1)?1:-1);
	}
	
	public void move()
	{
		double ty;
		
		this.x+=dx*velocidad;
		this.y+=dy*velocidad;
		
		if(this.y < parent.min_y)
		{
			this.y=parent.min_y;
			this.dy=Math.abs(this.dy)+ inc;
			parent.sound(0);
		}
		else if(this.y+size > parent.max_y)
		{
			this.y=parent.max_y-size;
			this.dy=-(Math.abs(this.dy)+inc);
			parent.sound(0);
		}

		//hit test
		if(this.x<parent.mitad_x)
		{
			if(this.x<=parent.b1.x+parent.b1.width && (this.y+size)>=parent.b1.y && this.y<=parent.b1.y+parent.b1.height)
			{
				this.dx=Math.abs(this.dx) + inc;
				ty=this.y-parent.b1.y;
				ty/=parent.b1.height;
				if(ty<0.3)
					this.dy-=1.0;
				else if(ty<0.7)
					this.dy+=0.5;
				else if(ty<1.0)
					this.dy+=1.0;
				parent.sound(1);
				parent.vibra(100);
			}
			else if(this.x<=parent.min_x)
			{
				parent.sound(2);
				if(parent.modo_mp==nPongGame.PLAYER_ONE)
					parent.vibra(200);
				parent.set();
				parent.score_2++;
			}
		}
		else
		{
			if(this.x + this.size>=parent.b2.x && (this.y+size)>=parent.b2.y && this.y<=parent.b2.y+parent.b2.height)
			{
				this.dx=-(Math.abs(this.dx)+inc);
				ty=this.y-parent.b2.y;
				ty/=parent.b2.height;
				if(ty<0.3)
					this.dy-=1.0;
				else if(ty<0.7)
					this.dy+=0.5;
				else if(ty<1.0)
					this.dy+=1.0;
				parent.sound(1);
			}
			else if(this.x + this.size>=parent.max_x)
			{
				parent.sound(2);
				if(parent.modo_mp==nPongGame.PLAYER_TWO)
					parent.vibra(200);
				parent.set();
				parent.score_1++;
			}
		}
		
	}
	
	public void paint(Graphics g)
	{
		g.setColor(0xFFFFFF);
		g.fillRect((int)x,(int)y,size,size);
	}
}

class nPongBar
{
	public int y;
	public int x;
	public int dy;
	public int height;
	public int width;
	public int velocidad;
	private nPongGame parent;
	boolean comp;
	
	public nPongBar(nPongGame parent, int x, int y,int width , int height)
	{
		this.x=x;
		this.y=y;
		this.height=height;
		this.width=width;
		this.parent=parent;
		this.velocidad=2;
		comp=false;
	}
	
	public void move(int dy)
	{
		//System.err.println("Mov " + dy);
		
		if(comp)
		{
			if(parent.pelota.y<this.y)
				dy=-1;
			else if(parent.pelota.y>this.y+height)
				dy=1;
			else
				dy=0;
		}
		
		this.y+=(dy*velocidad);
		if(this.y < parent.min_y)
			this.y=parent.min_y;
		else if(this.y+height > parent.max_y)
			this.y=parent.max_y-height;
			
		
	}
	
	public void paint(Graphics g)
	{
		g.setColor(0xFFFFFF);
		g.fillRect(x,y,width,height);
		
	}
}
