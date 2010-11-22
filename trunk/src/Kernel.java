import java.lang.Thread;
import java.io.*;
import java.util.*;

public class Kernel extends Thread
{
  // The number of virtual pages must be fixed at 63 due to
  // dependencies in the GUI
  private static int virtPageNum = 63;

  private String output = null;
  private static final String lineSeparator = System.getProperty("line.separator");
  private String command_file;
  private String config_file;
  private ControlPanel controlPanel ;
  private Vector<Page> memVector = new Vector<Page>();
  private Vector<Instruction> instructVector = new Vector<Instruction>();
  private boolean doStdoutLog = false;
  private boolean doFileLog = false;
  public int runs;
  public int runcycles;
  public long block = (int) Math.pow(2,12);
  public static byte addressradix = 16;

  public void init( String commands , String config )  
  {
    File f = new File( commands );
    command_file = commands;
    config_file = config;
    String line;
    String tmp = null;
    String command = "";
    byte R = 0;
    byte M = 0;
    int i = 0;
    int j = 0;
    int id = 0;
    int currentPhysicalPage = 0;
    int physical_count = 0;
    int inMemTime = 0;
    int lastTouchTime = 0;
    int map_count = 0;
    double power = 14;
    long high = 0;
    long low = 0;
    long addr = 0;
    long address_limit = (block * (virtPageNum+1))-1;
    int physicalPageCount = 2;
  
    if ( config != null )
    {
      f = new File ( config );
      
      
      try 
      {
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        while ((line = in.readLine()) != null) 
        {
          if (line.startsWith("numphyspages")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
              physicalPageCount = Common.s2i(st.nextToken());
              if ( physicalPageCount < 2 || physicalPageCount > 63 )
              {
                System.out.println("MemoryManagement: physicalPageCount out of bounds.");
                System.exit(-1);
              }
            }
          }
        }
        in.close();
      } catch (IOException e) { /* Handle exceptions */ }
      
      try 
      {
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        while ((line = in.readLine()) != null) 
        {
          if (line.startsWith("numpages")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
              virtPageNum = Common.s2i(st.nextToken()) - 1;
              if ( virtPageNum < 2 || virtPageNum > 63 )
              {
                System.out.println("MemoryManagement: numpages out of bounds.");
                System.exit(-1);
              }
              address_limit = (block * (virtPageNum+1))-1;
            }
          }
        }
        in.close();
      } catch (IOException e) { /* Handle exceptions */ }
      for (i = 0; i <= virtPageNum; i++) 
      {
        high = (block * (i + 1))-1;
        low = block * i;
        memVector.addElement(new Page(i, -1, R, M, 0, 0, high, low));
      }
      try 
      {
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        while ((line = in.readLine()) != null) 

        {
          if (line.startsWith("memset")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            st.nextToken();
            while (st.hasMoreTokens()) 
            { 
              id = Common.s2i(st.nextToken());
              tmp = st.nextToken();
              if (tmp.startsWith("x")) 
              {
                currentPhysicalPage = -1;
              } 
              else 
              {
                currentPhysicalPage = Common.s2i(tmp);
              }
              if ((0 > id || id > virtPageNum) || (-1 > currentPhysicalPage || currentPhysicalPage > (physicalPageCount)))
              {
            	System.out.printf("%d %d %d %d \n", id, currentPhysicalPage, virtPageNum, physicalPageCount);
                System.out.println("MemoryManagement: Invalid page value in " + config +" " + id + "Line:" + line);
                System.exit(-1);
              }
              R = Common.s2b(st.nextToken());
              if (R < 0 || R > 1)
              {
                System.out.println("MemoryManagement: Invalid R value in " + config);
                System.exit(-1);
              }
              M = Common.s2b(st.nextToken());
              if (M < 0 || M > 1)
              {
                 System.out.println("MemoryManagement: Invalid M value in " + config);
                 System.exit(-1);
              }
              inMemTime = Common.s2i(st.nextToken());
              if (inMemTime < 0)
              {
                System.out.println("MemoryManagement: Invalid inMemTime in " + config);
                System.exit(-1);
              }
              lastTouchTime = Common.s2i(st.nextToken());
              if (lastTouchTime < 0)
              {
                System.out.println("MemoryManagement: Invalid lastTouchTime in " + config);
                System.exit(-1);
              }
              Page page = (Page) memVector.elementAt(id);
              page.physical = currentPhysicalPage;
              page.R = R;
              page.M = M;
              page.inMemTime = inMemTime;
              page.lastTouchTime = lastTouchTime;
            }
          }
          if (line.startsWith("enable_logging")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              if ( st.nextToken().startsWith( "true" ) )
              {
                doStdoutLog = true;
              }              
            }
          }
          if (line.startsWith("log_file")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
            }
            if ( tmp.startsWith( "log_file" ) )
            {
              doFileLog = false;
              output = "tracefile";
            }              
            else
            {
              doFileLog = true;
              doStdoutLog = false;
              output = tmp;
            }
          }
          if (line.startsWith("pagesize")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
              tmp = st.nextToken();
              if ( tmp.startsWith( "power" ) )
              {
                power = (double) Integer.parseInt(st.nextToken());
                block = (int) Math.pow(2,power);
                
              }
              else
              {
                block = Long.parseLong(tmp,10);             
              }
              address_limit = (block * (virtPageNum+1))-1;
              System.out.println("Block size " + block + "Limit: " + address_limit);
            }
            if ( block < 64 || block > Math.pow(2,26))
            {
              System.out.println("MemoryManagement: pagesize is out of bounds");
              System.exit(-1);
            }
            for (i = 0; i <= virtPageNum; i++) 
            {
              Page page = memVector.elementAt(i);
              page.high = (block * (i + 1))-1;
              page.low = block * i;
            }
          }
          if (line.startsWith("addressradix")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
              tmp = st.nextToken();
              addressradix = Byte.parseByte(tmp);
              if ( addressradix < 0 || addressradix > 20 )
              {
                System.out.println("MemoryManagement: addressradix out of bounds.");
                System.exit(-1);
              }
            }
          }
        }
        in.close();
      } catch (IOException e) { /* Handle exceptions */ }
    }
    f = new File ( commands );
    try 
    {
      DataInputStream in = new DataInputStream(new FileInputStream(f));
      int lcount = 0;
      while ((line = in.readLine()) != null) 
      {
    	  lcount ++;
    	  if ((lcount % 100)==0)
    	  {
    		  System.out.printf("Line %d\n", lcount);
    	  }

        if (line.startsWith("READ") || line.startsWith("WRITE")) 
        {
          if (line.startsWith("READ")) 
          {
            command = "READ";
          }
          if (line.startsWith("WRITE")) 
          {
            command = "WRITE";
          }
          StringTokenizer st = new StringTokenizer(line);
          tmp = st.nextToken();
          tmp = st.nextToken();
          if (tmp.startsWith("random")) 
          {
            instructVector.addElement(new Instruction(command,Common.randomLong( address_limit )));
          } 
          else 
          { 
            if ( tmp.startsWith( "bin" ) )
            {
              addr = Long.parseLong(st.nextToken(),2);             
            }
            else if ( tmp.startsWith( "oct" ) )
            {
              addr = Long.parseLong(st.nextToken(),8);
            }
            else if ( tmp.startsWith( "hex" ) )
            {
              addr = Long.parseLong(st.nextToken(),16);
            }
            else
            {
              addr = Long.parseLong(tmp);
            }
            
            if (0 > addr || addr > address_limit)
            {
              System.out.printf("MemoryManagement: %x , Address out of range in %s.  Max address %x.", addr, commands, address_limit);
              System.exit(-1);
            }
            instructVector.addElement(new Instruction(command,addr));
          }
        }
      }
      in.close();
    } catch (IOException e) { /* Handle exceptions */ }
    runcycles = instructVector.size();
    if ( runcycles < 1 )
    {
      System.out.println("MemoryManagement: no instructions present for execution.");
      System.exit(-1);
    }
    if ( doFileLog )
    {
      File trace = new File(output);
      trace.delete();
    }
    runs = 0;
    for (i = 0; i < virtPageNum; i++) 
    {
      Page page = memVector.elementAt(i);
      if ( page.physical != -1 )
      {
        map_count++;
      }
      for (j = 0; j < virtPageNum; j++) 
      {
        Page tmp_page = memVector.elementAt(j);
        if (tmp_page.physical == page.physical && page.physical >= 0)
        {
          physical_count++;
        }
      }
      if (physical_count > 1)
      {
        System.out.println("MemoryManagement: Duplicate physical page's in " + config);
        System.exit(-1);
      }
      physical_count = 0;
    }
    if (map_count < physicalPageCount )
    {
      for (i = 0; i < virtPageNum; i++) 
      {
        Page page = memVector.elementAt(i);
        if ( page.physical == -1 && map_count < physicalPageCount )
        {
          page.physical = i;
          map_count++;
        }
      }
    }
    for (i = 0; i < virtPageNum; i++) 
    {
      Page page = memVector.elementAt(i);
      if (page.physical == -1) 
      {
        controlPanel.removePhysicalPage( i );
      } 
      else
      {
        controlPanel.addPhysicalPage( i , page.physical );
      }
    }
    for (i = 0; i < instructVector.size(); i++) 
    {
      high = block * (virtPageNum+1);
      Instruction instruct = ( Instruction ) instructVector.elementAt( i );
      if ( instruct.addr < 0 || instruct.addr > high )
      {
        System.out.printf("MemoryManagement: Instruction (%s %x) out of bounds. Range: %x %x\n", instruct.inst, instruct.addr, low, high);
        System.exit(-1);
      }
    }
  } 

  public void setControlPanel(ControlPanel newControlPanel) 
  {
    controlPanel = newControlPanel ;
  }

  public void getPage(int pageNum) 
  {
    Page page = ( Page ) memVector.elementAt( pageNum );
    controlPanel.paintPage( page );
  }
  
  private void printPageFaultCount()
  {
	  File trace = new File(output);
	 
	    try 
	    {
	      PrintStream out = new PrintStream( new FileOutputStream( output, true ) );

	      out.println("Number of page missed: " + PageFault.faultCount);

	      out.println("Total Memory Accesses: " + instructVector.size());
	      out.close();
	    } 
	    catch (IOException e) 
	    {
	      /* Do nothing */ 
	    }
  }

  private void printLogFile(String message)
  {
    String line;
    String temp = "";

    File trace = new File(output);
/* 
 * Don't do this idiotic thing.  Simply append to the file (though this is still kludgy...)   
 * if (trace.exists()) 
    {
      try 
      {
        DataInputStream in = new DataInputStream( new FileInputStream( output ) );
        while ((line = in.readLine()) != null) {
          temp = temp + line + lineSeparator;
        }
        in.close();
      }
      catch ( IOException e ) 
      {
       
      }
    }*/

    try 
    {
      PrintStream out = new PrintStream( new FileOutputStream( output, true ));
  //    out.print( temp );
      out.println( message );
      out.close();
    } 
    catch (IOException e) 
    {
      /* Do nothing */ 
    }
  }

  public void run()
  {
    step();
    while (runs != runcycles) 
    {
      step();
    }  
    printPageFaultCount();
  }

  public void step()
  {
    int i = 0;

    Instruction instruct = instructVector.elementAt( runs );
    controlPanel.instructionValueLabel.setText( instruct.inst );
    controlPanel.addressValueLabel.setText( Long.toString( instruct.addr , addressradix ) );
    getPage( Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ) );
    if ( controlPanel.pageFaultValueLabel.getText() == "YES" ) 
    {
      controlPanel.pageFaultValueLabel.setText( "NO" );
    }
    if ( instruct.inst.startsWith( "READ" ) ) 
    {
      Page page = ( Page ) memVector.elementAt( Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ) );
      if ( page.physical == -1 ) 
      {
        if ( doFileLog )
        {
          printLogFile( "READ " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        if ( doStdoutLog )
        {
          System.out.println( "READ " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        PageFault.replacePage( memVector , virtPageNum , Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ) , controlPanel );
        controlPanel.pageFaultValueLabel.setText( "YES" );
      } 
      else 
      {
        page.R = 1;
        page.lastTouchTime = 0;   
        if ( doFileLog )
        {
          printLogFile( "READ " + Long.toString( instruct.addr , addressradix ) + " ... okay" );
        }
        if ( doStdoutLog )
        {
          System.out.println( "READ " + Long.toString( instruct.addr , addressradix ) + " ... okay" );
        }
      }
    }
    if ( instruct.inst.startsWith( "WRITE" ) ) 
    {
      Page page = ( Page ) memVector.elementAt( Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ) );
      if ( page.physical == -1 ) 
      {
        if ( doFileLog )
        {
          printLogFile( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        if ( doStdoutLog )
        {
           System.out.println( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        PageFault.replacePage( memVector , virtPageNum , Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ) , controlPanel );          controlPanel.pageFaultValueLabel.setText( "YES" );
      } 
      else 
      {
        page.M = 1;
        page.lastTouchTime = 0;
        if ( doFileLog )
        {
          printLogFile( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... okay" );
        }
        if ( doStdoutLog )
        {
          System.out.println( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... okay" );
        }
      }
    }
    for ( i = 0; i < virtPageNum; i++ ) 
    {
      Page page = ( Page ) memVector.elementAt( i );
      if ( page.R == 1 && page.lastTouchTime == 10 ) 
      {
        page.R = 0;
      }
      if ( page.physical != -1 ) 
      {
        page.inMemTime = page.inMemTime + 10;
        page.lastTouchTime = page.lastTouchTime + 10;
      }
    }
    runs++;
    controlPanel.timeValueLabel.setText( Integer.toString( runs*10 ) + " (ns)" );
  }

  public void reset() {
    memVector.removeAllElements();
    instructVector.removeAllElements();
    controlPanel.statusValueLabel.setText( "STOP" ) ;
    controlPanel.timeValueLabel.setText( "0" ) ;
    controlPanel.instructionValueLabel.setText( "NONE" ) ;
    controlPanel.addressValueLabel.setText( "NULL" ) ;
    controlPanel.pageFaultValueLabel.setText( "NO" ) ;
    controlPanel.virtualPageValueLabel.setText( "x" ) ;
    controlPanel.physicalPageValueLabel.setText( "0" ) ;
    controlPanel.RValueLabel.setText( "0" ) ;
    controlPanel.MValueLabel.setText( "0" ) ;
    controlPanel.inMemTimeValueLabel.setText( "0" ) ;
    controlPanel.lastTouchTimeValueLabel.setText( "0" ) ;
    controlPanel.lowValueLabel.setText( "0" ) ;
    controlPanel.highValueLabel.setText( "0" ) ;
    init( command_file , config_file );
  }
}
